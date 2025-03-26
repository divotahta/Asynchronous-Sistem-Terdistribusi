# Pengembangan Aplikasi E-Commerce dengan Komunikasi Asinkronus: Producer-Queue-Consumer

---

## Daftar Isi

1. Tahapan Pengembangan Aplikasi
2. Arsitektur Sistem dengan Producer-Queue-Consumer
3. Konfigurasi RabbitMQ
4. Implementasi Aplikasi
5. Fitur Keamanan dan Monitoring
6. Demo Aplikasi

---

## 1. Tahapan Pengembangan Aplikasi

### 1.1 Analisis Kebutuhan

- Identifikasi kebutuhan bisnis e-commerce:
  - Layanan pemesanan yang dapat mencatat pesanan pelanggan
  - Layanan pengiriman yang memproses pesanan untuk dikirim ke pelanggan
  - Komunikasi asinkronus antar layanan dengan pendekatan Producer-Queue-Consumer
  - Pelacakan status pesanan dan pengiriman secara real-time
  - Penanganan error dan retry mechanism
  - Logging komprehensif untuk monitoring

### 1.2 Perancangan Sistem

- Pemisahan tanggung jawab:
  - Layanan Pesanan: mengelola data pesanan pelanggan
  - Layanan Pengiriman: mengelola proses pengiriman pesanan
- Perancangan struktur pesan antar layanan menggunakan JSON
- Perancangan alur komunikasi asinkronus dengan Producer-Queue-Consumer
- Implementasi thread management untuk operasi non-blocking
- Mekanisme acknowledgment untuk reliable messaging

### 1.3 Pemilihan Teknologi

- Bahasa Pemrograman: Java 11+
- Message Broker: RabbitMQ
- Build Tool: Maven
- Logger: SLF4J dengan Simple Logger
- JSON Processing: org.json library
- Thread Management: Java Concurrency Utilities

---

## 2. Arsitektur Sistem dengan Producer-Queue-Consumer

### 2.1 Diagram Arsitektur

```
                        ┌─────────────────┐
                        │                 │
                        │    RabbitMQ     │
                        │                 │
                        └────────┬────────┘
                                 │
                    ┌────────────┴───────────┐
                    │                        │
       ┌────────────▼───────────┐   ┌────────▼─────────────┐
       │                        │   │                      │
┌──────┴────────┐        ┌──────┴───┴────┐        ┌───────┴──────────┐
│ PesananProducer│        │PesananConsumer│        │PengirimanProducer│
└──────┬────────┘        └──────┬────────┘        └───────┬──────────┘
       │                        │                         │
┌──────┴────────┐        ┌──────┴───────────┐     ┌──────┴────────────────┐
│LayananPesanan │        │LayananPengiriman │     │PesananStatusConsumer  │
└───────────────┘        └──────────────────┘     └───────────────────────┘
```

### 2.2 Komponen Utama

1. **Producer**:
   - Menggunakan BlockingQueue untuk buffer lokal
   - Thread terpisah untuk pengiriman asinkronus
   - Konversi pesan ke JSON
   - Penanganan error dan retry

2. **Queue**:
   - RabbitMQ sebagai message broker
   - Durable queue untuk persistensi
   - Direct exchange untuk routing
   - Manual acknowledgment

3. **Consumer**:
   - Thread terpisah untuk pemrosesan
   - Callback-based message handling
   - JSON message parsing
   - Error handling dan logging

### 2.3 Alur Data

1. **Pembuatan Pesanan**:
   ```java
   // Di LayananPesanan
   Pesanan pesanan = createNewOrder(scanner);
   pesananProducer.tambahkanPesanan(pesanan);
   ```

2. **Pengiriman ke RabbitMQ**:
   ```java
   // Di PesananProducer
   String pesananJson = JSONUtil.toJSON(pesanan);
   channel.basicPublish(EXCHANGE_PESANAN, 
                       ROUTING_KEY_PESANAN_BARU,
                       null, 
                       pesananJson.getBytes());
   ```

3. **Penerimaan Pesan**:
   ```java
   // Di PesananConsumer
   channel.basicConsume(QUEUE_PESANAN_BARU, 
                       false, 
                       deliverCallback,
                       consumerTag -> {});
   ```

---

## 3. Konfigurasi RabbitMQ

### 3.1 Koneksi dan Channel

```java
// Konfigurasi koneksi
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
factory.setPort(5672);
factory.setUsername("guest");
factory.setPassword("guest");
factory.setConnectionTimeout(5000);

// Membuat koneksi dan channel
Connection connection = factory.newConnection();
Channel channel = connection.createChannel();
```

### 3.2 Exchange dan Queue

```java
// Membuat exchange
channel.exchangeDeclare(EXCHANGE_PESANAN, "direct", true);

// Membuat queue pesanan baru
channel.queueDeclare(QUEUE_PESANAN_BARU, 
                     true,    // durable
                     false,   // exclusive
                     false,   // auto-delete
                     null);   // arguments

// Binding queue ke exchange
channel.queueBind(QUEUE_PESANAN_BARU,
                 EXCHANGE_PESANAN,
                 ROUTING_KEY_PESANAN_BARU);
```

### 3.3 Verifikasi Koneksi

```java
// Verifikasi koneksi
if (!connection.isOpen()) {
    throw new Exception("Koneksi RabbitMQ tidak terbuka");
}

// Verifikasi queue
if (!RabbitMQUtil.verifyAllQueues(channel)) {
    throw new Exception("Gagal memverifikasi queue RabbitMQ");
}
```

---

## 4. Implementasi Producer

### 4.1 Thread Management

```java
public class PesananProducer {
    private final BlockingQueue<Pesanan> pesananQueue;
    private volatile boolean running = true;
    private Thread producerThread;
    
    private void start() {
        producerThread = new Thread(() -> {
            try (Channel channel = connection.createChannel()) {
                while (running) {
                    Pesanan pesanan = pesananQueue.take();
                    // Proses pengiriman...
                }
            }
        });
        producerThread.setDaemon(true);
        producerThread.start();
    }
}
```

### 4.2 Message Publishing

```java
// Konversi ke JSON
String pesananJson = JSONUtil.toJSON(pesanan);

// Kirim ke RabbitMQ
channel.basicPublish(
    EXCHANGE_PESANAN,
    ROUTING_KEY_PESANAN_BARU,
    null,
    pesananJson.getBytes(StandardCharsets.UTF_8)
);
```

### 4.3 Error Handling

```java
try {
    // Proses pengiriman...
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    break;
} catch (Exception e) {
    logger.error("Error saat mengirim pesan: {}", 
                 e.getMessage(), e);
    // Implementasi retry jika diperlukan
}
```

---

## 5. Implementasi Consumer

### 5.1 Message Handling

```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), 
                              StandardCharsets.UTF_8);
    try {
        processPesananBaru(message);
        channel.basicAck(delivery.getEnvelope()
                        .getDeliveryTag(), false);
    } catch (Exception e) {
        logger.error("Gagal memproses pesan: {}", 
                    e.getMessage(), e);
    }
};
```

### 5.2 Consumer Registration

```java
channel.basicConsume(
    QUEUE_PESANAN_BARU,
    false,  // manual ack
    deliverCallback,
    consumerTag -> {}
);
```

### 5.3 Thread Management

```java
@Override
public void run() {
    try (Channel channel = connection.createChannel()) {
        // Setup consumer...
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

---

## 6. Fitur Keamanan dan Monitoring

### 6.1 Keamanan

- Kredensial RabbitMQ yang aman
- Enkripsi pesan sensitif
- Batasi akses ke Management Plugin
- Validasi input dan sanitasi data

### 6.2 Logging

```java
// Logging koneksi
logger.info("Membuat koneksi ke RabbitMQ server di {}:{}", 
            HOST, PORT);

// Logging operasi
logger.info("Pesanan berhasil dikirim: ID={}", 
            pesanan.getId());

// Logging error
logger.error("Gagal memproses pesan: {}", 
             e.getMessage(), e);
```

### 6.3 Monitoring

- RabbitMQ Management Plugin
- Log aplikasi
- Status koneksi dan queue
- Metrik performa

---

## 7. Demo Aplikasi

### 7.1 Menjalankan Aplikasi

1. Start RabbitMQ Server
2. Jalankan LayananPesanan
3. Jalankan LayananPengiriman
4. Buat pesanan baru
5. Monitor status pengiriman

### 7.2 Monitoring

1. Akses RabbitMQ Management
2. Periksa status queue
3. Monitor log aplikasi
4. Verifikasi alur pesan

### 7.3 Troubleshooting

1. Cek status server
2. Verifikasi koneksi
3. Periksa log error
4. Analisis queue metrics

---

## Terima Kasih

Pertanyaan dan Diskusi? 