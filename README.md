# Proyek E-Commerce dengan Komunikasi Asinkronus

Proyek ini terdiri dari dua aplikasi Java yang merepresentasikan dua layanan E-Commerce yang berkomunikasi secara asinkronus menggunakan RabbitMQ.

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

1. **Layanan Pesanan** membuat pesanan baru dan mempublikasikannya ke RabbitMQ
2. **Layanan Pengiriman** menerima pesanan baru dan memproses pengiriman
3. **Layanan Pengiriman** memperbarui status pengiriman dan mempublikasikannya ke RabbitMQ
4. **Layanan Pesanan** menerima update status pengiriman dan memperbarui status pesanan

## Struktur Proyek

```
.
├── LayananPesanan
│   ├── src/main/java/com/ecommerce/pesanan
│   │   ├── model
│   │   │   └── Pesanan.java
│   │   ├── service
│   │   │   ├── PesananPublisher.java
│   │   │   └── PesananStatusListener.java
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
    │   │   ├── PesananListener.java
    │   │   └── PengirimanStatusPublisher.java
    │   ├── util
    │   │   ├── JSONUtil.java
    │   │   └── RabbitMQUtil.java
    │   └── AplikasiLayananPengiriman.java
    └── pom.xml
```

## Catatan

- Pastikan RabbitMQ Server berjalan sebelum menjalankan aplikasi
- Jalankan kedua aplikasi dalam instance yang berbeda
- Buat pesanan baru melalui Layanan Pesanan, kemudian perbarui status pengirimannya melalui Layanan Pengiriman 