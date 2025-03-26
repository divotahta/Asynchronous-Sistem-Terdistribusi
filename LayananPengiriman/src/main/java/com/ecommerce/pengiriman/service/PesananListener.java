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
 * Kelas untuk mendengarkan pesanan baru dari LayananPesanan
 */
public class PesananListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PesananListener.class);
    
    private final Connection connection;
    private volatile boolean running = true;
    private final Map<Integer, DetailPengiriman> daftarPengiriman = new HashMap<>();
    
    public PesananListener(Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public void run() {
        try (Channel channel = connection.createChannel()) {
            // Membuat callback untuk menerima pesan
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("Menerima pesanan baru: {}", message);
                
                try {
                    // Proses pesanan baru
                    processPesananBaru(message);
                } catch (Exception e) {
                    logger.error("Gagal memproses pesan: {}", e.getMessage(), e);
                } finally {
                    // Konfirmasi pesan telah diproses
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            
            // Mendaftar sebagai consumer untuk queue pesanan baru
            channel.basicConsume(RabbitMQUtil.QUEUE_PESANAN_BARU, false, deliverCallback, consumerTag -> {});
            
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
            logger.error("Kesalahan pada listener pesanan baru: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Memproses pesanan baru yang diterima dari RabbitMQ
     */
    private void processPesananBaru(String pesananJson) {
        // Konversi JSON pesanan ke DetailPengiriman
        DetailPengiriman pengiriman = JSONUtil.pesananToDetailPengiriman(pesananJson);
        
        // Simpan ke daftar pengiriman
        synchronized (daftarPengiriman) {
            daftarPengiriman.put(pengiriman.getId(), pengiriman);
        }
        
        logger.info("Pesanan baru ditambahkan ke daftar pengiriman: ID={}", pengiriman.getId());
    }
    
    /**
     * Menampilkan daftar pesanan yang perlu dikirim
     */
    public void tampilkanDaftarPengiriman() {
        synchronized (daftarPengiriman) {
            List<DetailPengiriman> pengirimanList = new ArrayList<>(daftarPengiriman.values());
            
            if (pengirimanList.isEmpty()) {
                System.out.println("\nTidak ada pesanan yang perlu dikirim");
                return;
            }
            
            System.out.println("\n=== Daftar Pesanan untuk Pengiriman ===");
            for (DetailPengiriman pengiriman : pengirimanList) {
                System.out.println("ID: " + pengiriman.getId());
                System.out.println("ID Pesanan: " + pengiriman.getIdPesanan());
                System.out.println("Pelanggan: " + pengiriman.getNamaPelanggan());
                System.out.println("Alamat: " + pengiriman.getAlamatPengiriman());
                System.out.println("Status: " + pengiriman.getStatusPengiriman());
                
                if (pengiriman.getKurirPengiriman() != null) {
                    System.out.println("Kurir: " + pengiriman.getKurirPengiriman());
                }
                
                if (pengiriman.getNomorResi() != null) {
                    System.out.println("No. Resi: " + pengiriman.getNomorResi());
                }
                
                System.out.println("-------------------------");
            }
        }
    }
    
    /**
     * Mendapatkan detail pengiriman berdasarkan ID
     */
    public DetailPengiriman getPengirimanById(int id) {
        synchronized (daftarPengiriman) {
            return daftarPengiriman.get(id);
        }
    }
    
    /**
     * Menghentikan listener
     */
    public void stop() {
        this.running = false;
    }
} 