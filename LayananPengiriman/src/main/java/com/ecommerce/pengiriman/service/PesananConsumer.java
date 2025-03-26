package com.ecommerce.pengiriman.service;

import com.ecommerce.pengiriman.model.DetailPengiriman;
import com.ecommerce.pengiriman.util.JSONUtil;
import com.ecommerce.pengiriman.util.RabbitMQUtil;
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
 * Kelas Consumer untuk menerima pesanan baru dari LayananPesanan
 * Kelas ini mengimplementasikan pola Consumer dalam arsitektur Producer-Queue-Consumer
 * Bertugas mengambil dan memproses pesanan baru dari queue RabbitMQ
 */
public class PesananConsumer implements Runnable {
    // Logger untuk mencatat aktivitas consumer
    private static final Logger logger = LoggerFactory.getLogger(PesananConsumer.class);
    
    // Koneksi ke server RabbitMQ
    private final Connection connection;
    
    // Flag untuk menandakan apakah consumer thread masih berjalan
    private volatile boolean running = true;
    
    // Map untuk menyimpan daftar pengiriman yang sedang diproses
    // Menggunakan HashMap yang disinkronisasi secara manual untuk thread-safety
    private final Map<Integer, DetailPengiriman> daftarPengiriman = new HashMap<>();
    
    /**
     * Konstruktor untuk inisialisasi consumer
     * @param connection Koneksi RabbitMQ yang sudah dibuat
     */
    public PesananConsumer(Connection connection) {
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
                logger.info("Menerima pesanan baru: {}", message);
                
                try {
                    // Proses pesanan baru
                    processPesananBaru(message);
                } catch (Exception e) {
                    // Menangani error yang mungkin terjadi saat memproses pesan
                    logger.error("Gagal memproses pesan: {}", e.getMessage(), e);
                } finally {
                    // Konfirmasi pesan telah diproses (acknowledge)
                    // Parameter false berarti hanya acknowledge satu pesan ini saja
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            
            // Mendaftar sebagai consumer untuk queue pesanan baru
            // Parameter false = manual acknowledgment untuk memastikan pesan diproses
            channel.basicConsume(RabbitMQUtil.QUEUE_PESANAN_BARU, false, deliverCallback, consumerTag -> {});
            
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
            logger.error("Kesalahan pada consumer pesanan baru: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Memproses pesanan baru yang diterima dari queue
     * @param pesananJson Pesan JSON yang berisi informasi pesanan baru
     */
    private void processPesananBaru(String pesananJson) {
        // Konversi JSON pesanan ke DetailPengiriman menggunakan utility
        // Utility ini akan men-deserialize JSON ke objek DetailPengiriman
        DetailPengiriman pengiriman = JSONUtil.pesananToDetailPengiriman(pesananJson);
        
        // Simpan ke daftar pengiriman
        // synchronized untuk thread-safety karena daftarPengiriman dapat diakses dari thread lain
        synchronized (daftarPengiriman) {
            daftarPengiriman.put(pengiriman.getId(), pengiriman);
        }
        
        logger.info("Pesanan baru ditambahkan ke daftar pengiriman: ID={}", pengiriman.getId());
    }
    
    /**
     * Menampilkan daftar pesanan yang perlu dikirim
     * Metode ini dapat dipanggil dari thread aplikasi utama
     */
    public void tampilkanDaftarPengiriman() {
        // synchronized untuk thread-safety saat membaca daftarPengiriman
        synchronized (daftarPengiriman) {
            // Konversi nilai map ke list untuk diproses
            List<DetailPengiriman> pengirimanList = new ArrayList<>(daftarPengiriman.values());
            
            if (pengirimanList.isEmpty()) {
                System.out.println("\nTidak ada pesanan yang perlu dikirim");
                return;
            }
            
            // Menampilkan informasi pengiriman dalam format yang mudah dibaca
            System.out.println("\n=== Daftar Pesanan untuk Pengiriman ===");
            for (DetailPengiriman pengiriman : pengirimanList) {
                System.out.println("ID: " + pengiriman.getId());
                System.out.println("ID Pesanan: " + pengiriman.getIdPesanan());
                System.out.println("Pelanggan: " + pengiriman.getNamaPelanggan());
                System.out.println("Alamat: " + pengiriman.getAlamatPengiriman());
                System.out.println("Status: " + pengiriman.getStatusPengiriman());
                
                // Tampilkan informasi kurir jika sudah ada
                if (pengiriman.getKurirPengiriman() != null) {
                    System.out.println("Kurir: " + pengiriman.getKurirPengiriman());
                }
                
                // Tampilkan nomor resi jika sudah ada
                if (pengiriman.getNomorResi() != null) {
                    System.out.println("No. Resi: " + pengiriman.getNomorResi());
                }
                
                System.out.println("-------------------------");
            }
        }
    }
    
    /**
     * Mendapatkan detail pengiriman berdasarkan ID
     * @param id ID pengiriman yang dicari
     * @return Objek DetailPengiriman atau null jika tidak ditemukan
     */
    public DetailPengiriman getPengirimanById(int id) {
        // synchronized untuk thread-safety saat membaca daftarPengiriman
        synchronized (daftarPengiriman) {
            return daftarPengiriman.get(id);
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