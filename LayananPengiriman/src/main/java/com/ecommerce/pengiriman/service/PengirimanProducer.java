package com.ecommerce.pengiriman.service;

import com.ecommerce.pengiriman.model.DetailPengiriman;
import com.ecommerce.pengiriman.util.JSONUtil;
import com.ecommerce.pengiriman.util.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Kelas Producer untuk mengirim status pengiriman ke queue
 * Kelas ini mengimplementasikan pola Producer dalam arsitektur Producer-Queue-Consumer
 * Producer bertugas menerima status pengiriman dan menyimpannya dalam queue lokal,
 * kemudian mengirimkannya ke RabbitMQ melalui thread terpisah
 */
public class PengirimanProducer {
    // Logger untuk mencatat aktivitas producer
    private static final Logger logger = LoggerFactory.getLogger(PengirimanProducer.class);
    
    // Koneksi ke server RabbitMQ
    private final Connection connection;
    
    // Queue lokal untuk menyimpan pengiriman sebelum dikirim ke RabbitMQ
    // BlockingQueue digunakan untuk komunikasi thread-safe antara thread utama dan producer thread
    private final BlockingQueue<DetailPengiriman> pengirimanQueue;
    
    // Flag untuk menandakan apakah producer thread masih berjalan
    private volatile boolean running = true;
    
    // Thread terpisah untuk mengirim pesan ke RabbitMQ
    private Thread producerThread;
    
    /**
     * Konstruktor untuk inisialisasi producer
     * @param connection Koneksi RabbitMQ yang sudah dibuat
     */
    public PengirimanProducer(Connection connection) {
        // Menyimpan koneksi RabbitMQ
        this.connection = connection;
        
        // Inisialisasi queue lokal dengan implementasi LinkedBlockingQueue
        // LinkedBlockingQueue cocok untuk skenario producer-consumer karena thread-safe
        this.pengirimanQueue = new LinkedBlockingQueue<>();
        
        // Memulai thread producer
        this.start();
    }
    
    /**
     * Metode untuk memulai thread producer yang akan mengirim status pengiriman ke RabbitMQ
     * Dijalankan pada thread terpisah agar tidak memblokir thread utama aplikasi
     */
    private void start() {
        // Membuat thread baru dengan lambda expression
        producerThread = new Thread(() -> {
            logger.info("Producer thread started");
            
            // try-with-resources untuk otomatis menutup channel ketika selesai
            try (Channel channel = connection.createChannel()) {
                logger.info("Channel RabbitMQ berhasil dibuat untuk producer thread");
                
                // Verifikasi semua queue tersedia sebelum mulai mengirim pesan
                boolean queuesAvailable = RabbitMQUtil.verifyAllQueues(channel);
                if (!queuesAvailable) {
                    logger.warn("Beberapa queue tidak tersedia, mencoba inisialisasi ulang...");
                    RabbitMQUtil.initializeExchangesAndQueues(channel);
                    
                    // Verifikasi lagi setelah inisialisasi
                    queuesAvailable = RabbitMQUtil.verifyAllQueues(channel);
                    if (!queuesAvailable) {
                        logger.error("Gagal membuat queue yang diperlukan, producer tidak dapat berjalan");
                        return;
                    }
                }
                
                logger.info("Semua queue tersedia, producer siap mengirim pesan");
                
                // Loop terus berjalan selama flag running bernilai true
                while (running) {
                    try {
                        // Mengambil pengiriman dari queue local
                        // take() akan memblokir thread jika queue kosong sampai ada item masuk
                        DetailPengiriman pengiriman = pengirimanQueue.take();
                        
                        // Konversi pengiriman ke JSON untuk dikirim ke RabbitMQ
                        String pengirimanJson = JSONUtil.toJSON(pengiriman);
                        
                        // Mengirim pesan ke RabbitMQ exchange dengan routing key pengiriman.status
                        // Parameter null adalah untuk properti pesan (tidak ada properti khusus)
                        channel.basicPublish(
                            RabbitMQUtil.EXCHANGE_PESANAN,
                            RabbitMQUtil.ROUTING_KEY_PENGIRIMAN_STATUS,
                            null,
                            pengirimanJson.getBytes(StandardCharsets.UTF_8)
                        );
                        
                        logger.info("Status pengiriman berhasil dikirim ke queue: ID={}, Status={}",
                            pengiriman.getId(), pengiriman.getStatusPengiriman());
                    } catch (InterruptedException e) {
                        // Menangani interupsi thread, misalnya ketika aplikasi ditutup
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        // Menangani error lain yang mungkin terjadi saat mengirim pengiriman
                        logger.error("Gagal mengirim status pengiriman ke queue: {}", e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                // Menangani error pada producer thread, misalnya ketika membuat channel
                logger.error("Error pada producer thread: {}", e.getMessage(), e);
            }
            
            logger.info("Producer thread stopped");
        });
        
        // Set thread sebagai daemon agar otomatis berakhir ketika program utama berakhir
        producerThread.setDaemon(true);
        
        // Memulai thread producer
        producerThread.start();
    }
    
    /**
     * Metode untuk menambahkan pengiriman ke queue lokal
     * Dipanggil oleh aplikasi utama untuk mengirim update status pengiriman
     * @param pengiriman Objek DetailPengiriman yang akan dikirim
     */
    public void tambahkanPengiriman(DetailPengiriman pengiriman) {
        try {
            // put() akan menambahkan pengiriman ke queue
            // Jika queue penuh, thread akan diblokir sampai ada ruang kosong
            pengirimanQueue.put(pengiriman);
            logger.info("Pengiriman telah ditambahkan ke queue local: ID={}, Status={}", 
                pengiriman.getId(), pengiriman.getStatusPengiriman());
        } catch (InterruptedException e) {
            // Menangani interupsi thread
            Thread.currentThread().interrupt();
            logger.error("Gagal menambahkan pengiriman ke queue: {}", e.getMessage());
        }
    }
    
    /**
     * Metode untuk menghentikan producer thread
     * Dipanggil ketika aplikasi berakhir untuk mengakhiri thread dengan bersih
     */
    public void stop() {
        // Mengubah flag running menjadi false untuk menghentikan loop di producer thread
        running = false;
        
        // Mengirim interupsi ke producer thread (jika sedang memblokir di take())
        producerThread.interrupt();
    }
} 