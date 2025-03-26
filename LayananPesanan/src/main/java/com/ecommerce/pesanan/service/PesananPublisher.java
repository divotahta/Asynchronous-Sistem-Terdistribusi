package com.ecommerce.pesanan.service;

import com.ecommerce.pesanan.model.Pesanan;
import com.ecommerce.pesanan.util.JSONUtil;
import com.ecommerce.pesanan.util.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Kelas untuk mempublikasikan pesan pesanan baru ke RabbitMQ
 */
public class PesananPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PesananPublisher.class);
    
    private final Connection connection;
    
    public PesananPublisher(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Mempublikasikan pesan pesanan baru ke RabbitMQ
     */
    public void publishPesananBaru(Pesanan pesanan) {
        try (Channel channel = connection.createChannel()) {
            // Konversi pesanan ke JSON
            String pesananJson = JSONUtil.toJSON(pesanan);
            
            // Publikasikan ke exchange dengan routing key
            channel.basicPublish(
                RabbitMQUtil.EXCHANGE_PESANAN,
                RabbitMQUtil.ROUTING_KEY_PESANAN_BARU,
                null,
                pesananJson.getBytes(StandardCharsets.UTF_8)
            );
            
            logger.info("Pesanan berhasil dipublikasikan: {}", pesanan.getId());
        } catch (Exception e) {
            logger.error("Gagal mempublikasikan pesanan: {}", e.getMessage(), e);
        }
    }
} 