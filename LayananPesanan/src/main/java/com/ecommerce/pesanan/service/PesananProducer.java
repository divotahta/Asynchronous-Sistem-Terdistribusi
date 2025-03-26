package com.ecommerce.pesanan.service;

import com.ecommerce.pesanan.model.Pesanan;
import com.ecommerce.pesanan.util.JSONUtil;
import com.ecommerce.pesanan.util.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Kelas Producer untuk mengirim pesanan ke queue
 * Kelas ini mengimplementasikan pola Producer dalam arsitektur Producer-Queue-Consumer
 * Producer bertugas menerima pesanan dan menyimpannya dalam queue lokal,
 * kemudian mengirimkannya ke RabbitMQ melalui thread terpisah
 */
public class PesananProducer {
    // Logger untuk mencatat aktivitas producer
    private static final Logger logger = LoggerFactory.getLogger(PesananProducer.class);
    
    // Koneksi ke server RabbitMQ
    private final Connection connection;
    
    // Queue lokal untuk menyimpan pesanan sebelum dikirim ke RabbitMQ
    // BlockingQueue digunakan untuk komunikasi thread-safe antara thread utama dan producer thread
    private final BlockingQueue<Pesanan> pesananQueue;
    
    // Flag untuk menandakan apakah producer thread masih berjalan
    private volatile boolean running = true;
    
    // Thread terpisah untuk mengirim pesan ke RabbitMQ
    private Thread producerThread;
    
    /**
     * Konstruktor untuk inisialisasi producer
     * @param connection Koneksi RabbitMQ yang sudah dibuat
     */
    public PesananProducer(Connection connection) {
        // Menyimpan koneksi RabbitMQ
        this.connection = connection;
        
        // Inisialisasi queue lokal dengan implementasi LinkedBlockingQueue
        // LinkedBlockingQueue cocok untuk skenario producer-consumer karena thread-safe
        this.pesananQueue = new LinkedBlockingQueue<>();
        
        // Memulai thread producer
        this.start();
    }
    
    /**
     * Metode untuk memulai thread producer yang akan mengirim pesanan ke RabbitMQ
     * Dijalankan pada thread terpisah agar tidak memblokir thread utama aplikasi
     */
    private void start() {
        // Membuat thread baru dengan lambda expression
        producerThread = new Thread(() -> {
            logger.info("Producer thread started dengan channel RabbitMQ");
            
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
                        // Mengambil pesanan dari queue local
                        // take() akan memblokir thread jika queue kosong sampai ada item masuk
                        logger.debug("Menunggu pesanan dari queue local...");
                        Pesanan pesanan = pesananQueue.take();
                        logger.info("Pesanan diambil dari queue local: ID={}", pesanan.getId());
                        
                        // Konversi pesanan ke JSON untuk dikirim ke RabbitMQ
                        String pesananJson = JSONUtil.toJSON(pesanan);
                        logger.debug("Pesanan telah dikonversi ke JSON: {}", pesananJson);
                        
                        // Mengirim pesan ke RabbitMQ exchange dengan routing key pesanan.baru
                        // Parameter null adalah untuk properti pesan (tidak ada properti khusus)
                        logger.info("Mengirim pesanan ke RabbitMQ, exchange={}, routing key={}", 
                            RabbitMQUtil.EXCHANGE_PESANAN, RabbitMQUtil.ROUTING_KEY_PESANAN_BARU);
                            
                        channel.basicPublish(
                            RabbitMQUtil.EXCHANGE_PESANAN,
                            RabbitMQUtil.ROUTING_KEY_PESANAN_BARU,
                            null,
                            pesananJson.getBytes(StandardCharsets.UTF_8)
                        );
                        
                        logger.info("Pesanan berhasil dikirim ke RabbitMQ queue: ID={}", pesanan.getId());
                    } catch (InterruptedException e) {
                        // Menangani interupsi thread, misalnya ketika aplikasi ditutup
                        logger.info("Producer thread diinterupsi");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        // Menangani error lain yang mungkin terjadi saat mengirim pesanan
                        logger.error("Gagal mengirim pesanan ke queue RabbitMQ: {}", e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                // Menangani error pada producer thread, misalnya ketika membuat channel
                logger.error("Error pada producer thread saat membuat channel: {}", e.getMessage(), e);
            }
            
            logger.info("Producer thread stopped");
        });
        
        // Set thread sebagai daemon agar otomatis berakhir ketika program utama berakhir
        producerThread.setDaemon(true);
        
        // Memulai thread producer
        producerThread.start();
        logger.info("Producer thread berhasil dimulai dengan ID: {}", producerThread.getId());
    }
    
    /**
     * Metode untuk menambahkan pesanan ke queue lokal
     * Dipanggil oleh aplikasi utama untuk mengirim pesanan baru
     * @param pesanan Objek pesanan yang akan dikirim
     */
    public void tambahkanPesanan(Pesanan pesanan) {
        if (pesanan == null) {
            logger.error("Gagal menambahkan pesanan ke queue: pesanan adalah null");
            return;
        }

        try {
            logger.info("Menambahkan pesanan ke queue local: ID={}, Pelanggan={}", 
                pesanan.getId(), pesanan.getNamaPelanggan());
            
            // put() akan menambahkan pesanan ke queue
            // Jika queue penuh, thread akan diblokir sampai ada ruang kosong
            pesananQueue.put(pesanan);
            
            logger.info("Pesanan berhasil ditambahkan ke queue local: ID={}, ukuran queue={}", 
                pesanan.getId(), pesananQueue.size());
        } catch (InterruptedException e) {
            // Menangani interupsi thread
            Thread.currentThread().interrupt();
            logger.error("Gagal menambahkan pesanan ke queue: {}", e.getMessage(), e);
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