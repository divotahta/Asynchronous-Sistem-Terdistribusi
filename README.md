# Proyek E-Commerce dengan Komunikasi Asinkronus (Producer-Queue-Consumer)

Proyek ini terdiri dari dua aplikasi Java yang merepresentasikan dua layanan E-Commerce yang berkomunikasi secara asinkronus menggunakan pendekatan Producer-Queue-Consumer dengan RabbitMQ.

## Komponen Proyek

Proyek ini terdiri dari 2 layanan:

1. **Layanan Pesanan** - Mengelola pesanan pelanggan
2. **Layanan Pengiriman** - Mengelola proses pengiriman pesanan

## Prasyarat

Sebelum menjalankan aplikasi, pastikan Anda telah menginstal:

1. Java JDK 11 atau lebih tinggi
2. Maven
3. RabbitMQ Server (berjalan di localhost:5672)
4. Visual Studio Code dengan ekstensi Java

## Cara Menjalankan

### Instalasi RabbitMQ

1. Unduh dan instal RabbitMQ dari [situs resmi RabbitMQ](https://www.rabbitmq.com/download.html)
2. Pastikan RabbitMQ Server berjalan di `localhost` dengan port `5672`
3. RabbitMQ Management Plugin dapat diakses di `http://localhost:15672/` (opsional, untuk memantau antrian)
   - Username default: guest
   - Password default: guest

### Menjalankan Aplikasi dengan Visual Studio Code

1. Buka folder proyek di Visual Studio Code
2. Jalankan Maven Build untuk kedua aplikasi:

   ```bash
   # Untuk Layanan Pesanan
   cd LayananPesanan
   mvn clean package

   # Untuk Layanan Pengiriman
   cd LayananPengiriman
   mvn clean package
   ```

3. Jalankan kedua aplikasi dari VSCode:
   - Buka file `AplikasiLayananPesanan.java` dan `AplikasiLayananPengiriman.java`
   - Klik tombol "Run" di VSCode atau klik kanan dan pilih "Run Java"

## Alur Kerja Aplikasi

1. **Layanan Pesanan**:
   - Membuat pesanan baru melalui menu interaktif
   - Menyimpan pesanan ke queue lokal menggunakan `BlockingQueue`
   - Producer thread mengambil pesanan dari queue lokal dan mengirim ke RabbitMQ
   - Menerima update status pengiriman melalui consumer

2. **Layanan Pengiriman**:
   - Menerima pesanan baru dari RabbitMQ melalui consumer
   - Memproses pesanan dan memperbarui status pengiriman
   - Menyimpan update status ke queue lokal
   - Producer thread mengirim status ke RabbitMQ

## Fitur Teknis

1. **Koneksi RabbitMQ**:
   - Timeout koneksi: 5 detik
   - Verifikasi koneksi otomatis
   - Reinisialisasi queue jika diperlukan

2. **Queue dan Exchange**:
   - Exchange: `ecommerce.pesanan` (tipe: direct, durable: true)
   - Queue Pesanan: `pesanan.baru` (durable: true)
   - Queue Status: `pesanan.status` (durable: true)
   - Routing key sesuai dengan nama queue

3. **Penanganan Pesan**:
   - Acknowledgment manual untuk memastikan pemrosesan pesan
   - Konversi pesan menggunakan JSON
   - Penanganan error dan retry mechanism

4. **Thread Management**:
   - Producer menggunakan thread terpisah (daemon)
   - Consumer berjalan di thread terpisah
   - Cleanup resources otomatis saat aplikasi ditutup

## Struktur Proyek

```
.
├── LayananPesanan
│   ├── src/main/java/com/ecommerce/pesanan
│   │   ├── model
│   │   │   └── Pesanan.java
│   │   ├── service
│   │   │   ├── PesananProducer.java
│   │   │   └── PesananStatusConsumer.java
│   │   ├── util
│   │   │   ├── JSONUtil.java
│   │   │   └── RabbitMQUtil.java
│   │   └── AplikasiLayananPesanan.java
│   └── pom.xml
└── LayananPengiriman
    ├── src/main/java/com/ecommerce/pengiriman
    │   ├── model
    │   │   └── DetailPengiriman.java
    │   ├── service
    │   │   ├── PesananConsumer.java
    │   │   └── PengirimanProducer.java
    │   ├── util
    │   │   ├── JSONUtil.java
    │   │   └── RabbitMQUtil.java
    │   └── AplikasiLayananPengiriman.java
    └── pom.xml
```

## Pola Producer-Queue-Consumer

Proyek ini mengimplementasikan pola Producer-Queue-Consumer dengan fitur:

1. **Asinkronus**:
   - Producer dan consumer berjalan di thread terpisah
   - Komunikasi non-blocking menggunakan BlockingQueue
   - Penanganan pesan secara asinkronus

2. **Penyangga**:
   - Queue lokal menggunakan BlockingQueue untuk buffer
   - RabbitMQ queue sebagai buffer distributed
   - Penanganan backpressure otomatis

3. **Keandalan**:
   - Acknowledgment manual untuk memastikan pemrosesan
   - Penanganan error dan retry
   - Cleanup resources yang proper
   - Logging komprehensif untuk monitoring

4. **Monitoring**:
   - Log detail untuk setiap operasi
   - Integrasi dengan RabbitMQ Management Plugin
   - Status queue dan koneksi dapat dipantau

## Catatan Penting

1. **Keamanan**:
   - Gunakan kredensial RabbitMQ yang aman di production
   - Enkripsi pesan sensitif sebelum dikirim
   - Batasi akses ke RabbitMQ Management Plugin

2. **Performa**:
   - Producer menggunakan BlockingQueue untuk menghindari overhead
   - Consumer menggunakan acknowledgment manual untuk kontrol lebih baik
   - Thread management yang efisien

3. **Pemeliharaan**:
   - Monitor log aplikasi untuk error
   - Pantau status queue melalui RabbitMQ Management
   - Backup data pesanan secara berkala

4. **Troubleshooting**:
   - Cek status RabbitMQ Server
   - Verifikasi koneksi dan queue
   - Periksa log aplikasi untuk error
   - Gunakan RabbitMQ Management untuk monitoring

## Catatan

- Pastikan RabbitMQ Server berjalan sebelum menjalankan aplikasi
- Jalankan kedua aplikasi dalam instance yang berbeda
- Buat pesanan baru melalui Layanan Pesanan, kemudian perbarui status pengirimannya melalui Layanan Pengiriman 