package com.ecommerce.pesanan.service;

import com.ecommerce.pesanan.model.Pesanan;
import com.ecommerce.pesanan.util.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelas Consumer untuk menerima update status pengiriman dari LayananPengiriman
 * Kelas ini mengimplementasikan pola Consumer dalam arsitektur Producer-Queue-Consumer
 * Consumer bertugas mengambil pesan dari queue RabbitMQ dan memprosesnya
 */
public class PesananStatusConsumer implements Runnable {
    // Logger untuk mencatat aktivitas consumer
    private static final Logger logger = LoggerFactory.getLogger(PesananStatusConsumer.class);
    
    // Koneksi ke server RabbitMQ
    private final Connection connection;
    
    // Flag untuk menandakan apakah consumer thread masih berjalan
    private volatile boolean running = true;
    
    // Map untuk menyimpan daftar pesanan yang diproses
    // Menggunakan HashMap yang disinkronisasi secara manual untuk thread-safety
    private final Map<Integer, Pesanan> daftarPesanan = new HashMap<>();
    
    /**
     * Konstruktor untuk inisialisasi consumer
     * @param connection Koneksi RabbitMQ yang sudah dibuat
     */
    public PesananStatusConsumer(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Implementasi metode run() dari interface Runnable
     * Metode ini dijalankan pada thread terpisah untuk menerima pesan secara asinkronus
     */
    @Override
    public void run() {
        // try-with-resources untuk otomatis menutup channel ketika selesai
        try (Channel channel = connection.createChannel()) {
            // Membuat callback untuk menerima pesan
            // DeliverCallback adalah functional interface dari RabbitMQ client
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                // Mengkonversi body pesan dari byte array ke String
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("Menerima update status pengiriman: {}", message);
                
                try {
                    // Proses pesan status pengiriman
                    processPengirimanStatus(message);
                } catch (Exception e) {
                    // Menangani error yang mungkin terjadi saat memproses pesan
                    logger.error("Gagal memproses pesan: {}", e.getMessage(), e);
                } finally {
                    // Konfirmasi pesan telah diproses (acknowledge)
                    // Parameter false berarti hanya acknowledge satu pesan ini saja
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            
            // Mendaftar sebagai consumer untuk queue status pengiriman
            // Parameter false = manual acknowledgment untuk memastikan pesan diproses
            channel.basicConsume(RabbitMQUtil.QUEUE_PESANAN_STATUS, false, deliverCallback, consumerTag -> {});
            
            // Tetap running sampai aplikasi di-stop
            while (running) {
                try {
                    // Tidur 1 detik untuk mengurangi penggunaan CPU
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Menangani interupsi thread
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            // Menangani error pada consumer thread
            logger.error("Kesalahan pada consumer status pengiriman: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Memproses pesan update status pengiriman yang diterima dari RabbitMQ
     * @param message Pesan JSON yang berisi informasi status pengiriman
     */
    private void processPengirimanStatus(String message) {
        // Parse pesan sebagai JSONObject
        // Menggunakan library org.json untuk parsing
        org.json.JSONObject statusObj = new org.json.JSONObject(message);
        
        // Mengambil data dari JSON object
        int idPesanan = statusObj.getInt("idPesanan");
        String statusPengiriman = statusObj.getString("statusPengiriman");
        
        // Update status pesanan
        // synchronized untuk thread-safety karena daftarPesanan dapat diakses dari thread lain
        synchronized (daftarPesanan) {
            // Mencari pesanan di daftar berdasarkan ID
            Pesanan pesanan = daftarPesanan.get(idPesanan);
            
            if (pesanan == null) {
                // Jika pesanan tidak ditemukan, buat pesanan baru
                // Biasanya terjadi jika status pengiriman datang sebelum pesanan disimpan
                pesanan = new Pesanan();
                pesanan.setId(idPesanan);
                
                // Menyimpan informasi tambahan dari pesan jika ada
                if (statusObj.has("namaPelanggan")) {
                    pesanan.setNamaPelanggan(statusObj.getString("namaPelanggan"));
                }
                
                if (statusObj.has("alamatPengiriman")) {
                    pesanan.setAlamatPengiriman(statusObj.getString("alamatPengiriman"));
                }
            }
            
            // Map status pengiriman ke status pesanan
            // Konversi nama status dari format pengiriman ke format pesanan
            String statusPesanan;
            switch (statusPengiriman) {
                case "DIKEMAS":
                    statusPesanan = "SEDANG_DIKEMAS";
                    break;
                case "DIKIRIM":
                    statusPesanan = "SEDANG_DIKIRIM";
                    break;
                case "TERKIRIM":
                    statusPesanan = "SELESAI";
                    break;
                default:
                    statusPesanan = "DIPROSES";
            }
            
            // Update status pesanan dan simpan ke daftar
            pesanan.setStatus(statusPesanan);
            daftarPesanan.put(idPesanan, pesanan);
            
            logger.info("Status pesanan diperbarui: ID={}, Status={}", idPesanan, statusPesanan);
        }
    }
    
    /**
     * Menampilkan daftar pesanan dengan status terbaru
     * Metode ini dapat dipanggil dari thread aplikasi utama
     */
    public void tampilkanDaftarPesanan() {
        // synchronized untuk thread-safety saat membaca daftarPesanan
        synchronized (daftarPesanan) {
            // Konversi nilai map ke list untuk diproses
            List<Pesanan> pesananList = new ArrayList<>(daftarPesanan.values());
            
            if (pesananList.isEmpty()) {
                System.out.println("\nTidak ada pesanan yang sedang diproses");
                return;
            }
            
            // Menampilkan informasi pesanan dalam format yang mudah dibaca
            System.out.println("\n=== Daftar Pesanan ===");
            for (Pesanan pesanan : pesananList) {
                System.out.println("ID: " + pesanan.getId());
                System.out.println("Pelanggan: " + pesanan.getNamaPelanggan());
                System.out.println("Alamat: " + pesanan.getAlamatPengiriman());
                System.out.println("Status: " + pesanan.getStatus());
                System.out.println("-------------------------");
            }
        }
    }
    
    /**
     * Menambahkan pesanan baru ke daftar pesanan
     * Dipanggil ketika pesanan baru dibuat di aplikasi
     * @param pesanan Objek pesanan baru yang akan ditambahkan
     */
    public void tambahkanPesanan(Pesanan pesanan) {
        // synchronized untuk thread-safety saat menulis ke daftarPesanan
        synchronized (daftarPesanan) {
            daftarPesanan.put(pesanan.getId(), pesanan);
            logger.info("Pesanan baru ditambahkan ke daftar: ID={}", pesanan.getId());
        }
    }
    
    /**
     * Menghentikan consumer thread
     * Dipanggil ketika aplikasi berakhir untuk mengakhiri thread dengan bersih
     */
    public void stop() {
        // Mengubah flag running menjadi false, thread akan berakhir setelah iterasi berikutnya
        this.running = false;
    }
} 