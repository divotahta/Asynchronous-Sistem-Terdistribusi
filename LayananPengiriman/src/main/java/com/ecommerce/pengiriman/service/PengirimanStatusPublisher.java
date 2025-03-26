package com.ecommerce.pengiriman.service;

import com.ecommerce.pengiriman.model.DetailPengiriman;
import com.ecommerce.pengiriman.util.JSONUtil;
import com.ecommerce.pengiriman.util.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Kelas untuk mempublikasikan status pengiriman ke RabbitMQ
 */
public class PengirimanStatusPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PengirimanStatusPublisher.class);
    
    private final Connection connection;
    
    public PengirimanStatusPublisher(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Mempublikasikan status pengiriman ke RabbitMQ
     */
    public void publishPengirimanStatus(DetailPengiriman pengiriman) {
        try (Channel channel = connection.createChannel()) {
            // Konversi DetailPengiriman ke JSON
            String statusJson = JSONUtil.toJSON(pengiriman);
            
            // Publikasikan ke exchange dengan routing key
            channel.basicPublish(
                RabbitMQUtil.EXCHANGE_PESANAN,
                RabbitMQUtil.ROUTING_KEY_PENGIRIMAN_STATUS,
                null,
                statusJson.getBytes(StandardCharsets.UTF_8)
            );
            
            logger.info("Status pengiriman berhasil dipublikasikan: ID={}, Status={}", 
                    pengiriman.getId(), pengiriman.getStatusPengiriman());
        } catch (Exception e) {
            logger.error("Gagal mempublikasikan status pengiriman: {}", e.getMessage(), e);
        }
    }
} 