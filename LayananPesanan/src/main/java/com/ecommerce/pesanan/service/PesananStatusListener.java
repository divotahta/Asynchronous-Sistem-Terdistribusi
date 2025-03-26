package com.ecommerce.pesanan.service;

import com.ecommerce.pesanan.model.Pesanan;
// import com.ecommerce.pesanan.util.JSONUtil;
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
 * Kelas untuk mendengarkan status pengiriman dari LayananPengiriman
 */
public class PesananStatusListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PesananStatusListener.class);
    
    private final Connection connection;
    private volatile boolean running = true;
    private final Map<Integer, Pesanan> daftarPesanan = new HashMap<>();
    
    public PesananStatusListener(Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public void run() {
        try (Channel channel = connection.createChannel()) {
            // Membuat callback untuk menerima pesan
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("Menerima update status pengiriman: {}", message);
                
                try {
                    // Proses pesan status pengiriman
                    processPengirimanStatus(message);
                } catch (Exception e) {
                    logger.error("Gagal memproses pesan: {}", e.getMessage(), e);
                } finally {
                    // Konfirmasi pesan telah diproses
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            
            // Mendaftar sebagai consumer untuk queue status pengiriman
            channel.basicConsume(RabbitMQUtil.QUEUE_PESANAN_STATUS, false, deliverCallback, consumerTag -> {});
            
            // Tetap running sampai aplikasi di-stop
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Kesalahan pada listener status pengiriman: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Memproses pesan update status pengiriman
     */
    private void processPengirimanStatus(String message) {
        // Parse pesan sebagai JSONObject
        org.json.JSONObject statusObj = new org.json.JSONObject(message);
        
        int idPesanan = statusObj.getInt("idPesanan");
        String statusPengiriman = statusObj.getString("statusPengiriman");
        
        // Update status pesanan
        synchronized (daftarPesanan) {
            Pesanan pesanan = daftarPesanan.get(idPesanan);
            
            if (pesanan == null) {
                // Jika pesanan tidak ditemukan, buat pesanan baru
                pesanan = new Pesanan();
                pesanan.setId(idPesanan);
                
                if (statusObj.has("namaPelanggan")) {
                    pesanan.setNamaPelanggan(statusObj.getString("namaPelanggan"));
                }
                
                if (statusObj.has("alamatPengiriman")) {
                    pesanan.setAlamatPengiriman(statusObj.getString("alamatPengiriman"));
                }
            }
            
            // Map status pengiriman ke status pesanan
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
            
            pesanan.setStatus(statusPesanan);
            daftarPesanan.put(idPesanan, pesanan);
            
            logger.info("Status pesanan diperbarui: ID={}, Status={}", idPesanan, statusPesanan);
        }
    }
    
    /**
     * Menampilkan daftar pesanan
     */
    public void tampilkanDaftarPesanan() {
        synchronized (daftarPesanan) {
            List<Pesanan> pesananList = new ArrayList<>(daftarPesanan.values());
            
            if (pesananList.isEmpty()) {
                System.out.println("\nTidak ada pesanan yang sedang diproses");
                return;
            }
            
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
     */
    public void tambahkanPesanan(Pesanan pesanan) {
        synchronized (daftarPesanan) {
            daftarPesanan.put(pesanan.getId(), pesanan);
            logger.info("Pesanan baru ditambahkan ke daftar: ID={}", pesanan.getId());
        }
    }
    
    /**
     * Menghentikan listener
     */
    public void stop() {
        this.running = false;
    }
} 