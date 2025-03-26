package com.ecommerce.pesanan.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kelas utilitas untuk mengelola koneksi RabbitMQ
 */
public class RabbitMQUtil {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQUtil.class);
    
    private static final String HOST = "localhost"; // Ganti dengan URL RabbitMQ server jika tidak berjalan secara lokal
    private static final int PORT = 5672; // Port default RabbitMQ
    private static final String USERNAME = "guest"; // Username default RabbitMQ
    private static final String PASSWORD = "guest"; // Password default RabbitMQ
    
    // Nama-nama exchange dan queue
    public static final String EXCHANGE_PESANAN = "ecommerce.pesanan";
    public static final String QUEUE_PESANAN_BARU = "pesanan.baru";
    public static final String QUEUE_PESANAN_STATUS = "pesanan.status";
    public static final String ROUTING_KEY_PESANAN_BARU = "pesanan.baru";
    public static final String ROUTING_KEY_PESANAN_STATUS = "pesanan.status";

    /**
     * Membuat koneksi ke RabbitMQ server
     */
    public static Connection createConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        
        logger.info("Membuat koneksi ke RabbitMQ server di {}:{}", HOST, PORT);
        return factory.newConnection();
    }

    /**
     * Menginisialisasi exchange dan queue yang dibutuhkan
     */
    public static void initializeExchangesAndQueues(Channel channel) throws Exception {
        // Membuat exchange
        channel.exchangeDeclare(EXCHANGE_PESANAN, "direct", true);
        
        // Membuat queue dan binding
        channel.queueDeclare(QUEUE_PESANAN_BARU, true, false, false, null);
        channel.queueBind(QUEUE_PESANAN_BARU, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_BARU);
        
        channel.queueDeclare(QUEUE_PESANAN_STATUS, true, false, false, null);
        channel.queueBind(QUEUE_PESANAN_STATUS, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_STATUS);
        
        logger.info("Exchange dan Queue RabbitMQ telah diinisialisasi");
    }
} 