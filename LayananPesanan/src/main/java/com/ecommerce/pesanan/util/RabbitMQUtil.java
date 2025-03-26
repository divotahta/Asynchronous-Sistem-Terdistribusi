package com.ecommerce.pesanan.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
        logger.info("Mencoba membuat koneksi ke RabbitMQ server...");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        
        // Timeout koneksi dalam milidetik (5 detik)
        factory.setConnectionTimeout(5000);
        
        logger.info("Membuat koneksi ke RabbitMQ server di {}:{} dengan username: {}", HOST, PORT, USERNAME);
        Connection connection = factory.newConnection();
        logger.info("Koneksi ke RabbitMQ berhasil dibuat");
        return connection;
    }

    /**
     * Menginisialisasi exchange dan queue yang dibutuhkan
     */
    public static void initializeExchangesAndQueues(Channel channel) throws Exception {
        logger.info("Mulai inisialisasi exchange dan queue RabbitMQ...");
        
        try {
            // Membuat exchange
            logger.info("Membuat exchange: {}, tipe: direct, durable: true", EXCHANGE_PESANAN);
            channel.exchangeDeclare(EXCHANGE_PESANAN, "direct", true);
            logger.info("Exchange {} berhasil dibuat", EXCHANGE_PESANAN);
            
            // Membuat queue pesanan baru dan binding
            logger.info("Membuat queue: {}, durable: true", QUEUE_PESANAN_BARU);
            channel.queueDeclare(QUEUE_PESANAN_BARU, true, false, false, null);
            logger.info("Queue {} berhasil dibuat", QUEUE_PESANAN_BARU);
            
            logger.info("Binding queue {} ke exchange {} dengan routing key {}", 
                QUEUE_PESANAN_BARU, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_BARU);
            channel.queueBind(QUEUE_PESANAN_BARU, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_BARU);
            logger.info("Binding untuk queue {} berhasil dibuat", QUEUE_PESANAN_BARU);
            
            // Membuat queue status pesanan dan binding
            logger.info("Membuat queue: {}, durable: true", QUEUE_PESANAN_STATUS);
            channel.queueDeclare(QUEUE_PESANAN_STATUS, true, false, false, null);
            logger.info("Queue {} berhasil dibuat", QUEUE_PESANAN_STATUS);
            
            logger.info("Binding queue {} ke exchange {} dengan routing key {}", 
                QUEUE_PESANAN_STATUS, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_STATUS);
            channel.queueBind(QUEUE_PESANAN_STATUS, EXCHANGE_PESANAN, ROUTING_KEY_PESANAN_STATUS);
            logger.info("Binding untuk queue {} berhasil dibuat", QUEUE_PESANAN_STATUS);
            
            logger.info("Exchange dan Queue RabbitMQ telah berhasil diinisialisasi");
        } catch (Exception e) {
            logger.error("Gagal menginisialisasi exchange dan queue: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Memeriksa apakah queue sudah ada dan berfungsi
     * @param channel Channel RabbitMQ yang akan digunakan
     * @param queueName Nama queue yang akan diperiksa
     * @return true jika queue ada dan berfungsi
     */
    public static boolean verifyQueueExists(Channel channel, String queueName) {
        try {
            logger.info("Memeriksa keberadaan queue: {}", queueName);
            // Metode queueDeclarePassive akan menghasilkan exception jika queue tidak ada
            // Ini cara yang lebih aman untuk memeriksa keberadaan queue tanpa membuatnya
            DeclareOk response = channel.queueDeclarePassive(queueName);
            logger.info("Queue {} ditemukan dengan {} pesan yang menunggu", 
                queueName, response.getMessageCount());
            return true;
        } catch (Exception e) {
            logger.warn("Queue {} tidak ditemukan atau tidak berfungsi: {}", queueName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Memeriksa semua queue yang dibutuhkan
     * @param channel Channel RabbitMQ yang akan digunakan
     * @return true jika semua queue ada dan berfungsi
     */
    public static boolean verifyAllQueues(Channel channel) {
        try {
            logger.info("Memeriksa semua queue yang dibutuhkan...");
            boolean pesananBaruExists = verifyQueueExists(channel, QUEUE_PESANAN_BARU);
            boolean pesananStatusExists = verifyQueueExists(channel, QUEUE_PESANAN_STATUS);
            
            boolean allQueuesExist = pesananBaruExists && pesananStatusExists;
            
            if (allQueuesExist) {
                logger.info("Semua queue yang dibutuhkan ada dan berfungsi");
            } else {
                logger.warn("Beberapa queue tidak ditemukan atau tidak berfungsi. " +
                           "pesanan.baru: {}, pesanan.status: {}", 
                           pesananBaruExists, pesananStatusExists);
            }
            
            return allQueuesExist;
        } catch (Exception e) {
            logger.error("Error saat memeriksa queue: {}", e.getMessage(), e);
            return false;
        }
    }
} 