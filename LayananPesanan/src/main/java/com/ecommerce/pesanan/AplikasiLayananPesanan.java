package com.ecommerce.pesanan;

import com.ecommerce.pesanan.model.Pesanan;
import com.ecommerce.pesanan.service.PesananProducer;
import com.ecommerce.pesanan.service.PesananStatusConsumer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.ecommerce.pesanan.util.RabbitMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aplikasi utama untuk layanan pesanan
 */
public class AplikasiLayananPesanan {
    private static final Logger logger = LoggerFactory.getLogger(AplikasiLayananPesanan.class);
    
    // Counter untuk ID pesanan, menggunakan AtomicInteger untuk thread-safety
    private static final AtomicInteger pesananCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        logger.info("Memulai Aplikasi Layanan Pesanan");
        
        Connection connection = null;
        Channel channel = null;
        PesananStatusConsumer statusConsumer = null;
        PesananProducer pesananProducer = null;
        
        try {
            // Membuat koneksi ke RabbitMQ
            logger.info("Menghubungkan ke RabbitMQ server...");
            connection = RabbitMQUtil.createConnection();
            channel = connection.createChannel();
            
            // Verifikasi koneksi telah terbentuk
            if (!connection.isOpen()) {
                throw new Exception("Koneksi RabbitMQ tidak terbuka");
            }
            
            // Inisialisasi exchange dan queue dengan penanganan error
            try {
                logger.info("Menginisialisasi exchange dan queue RabbitMQ...");
                RabbitMQUtil.initializeExchangesAndQueues(channel);
            } catch (Exception e) {
                logger.error("Gagal menginisialisasi exchange dan queue: {}", e.getMessage());
                throw e;
            }
            
            // Verifikasi queue sudah dibuat
            if (!RabbitMQUtil.verifyAllQueues(channel)) {
                throw new Exception("Gagal memverifikasi queue RabbitMQ");
            }
            
            // Consumer untuk menerima update status pengiriman
            logger.info("Memulai consumer untuk status pengiriman...");
            statusConsumer = new PesananStatusConsumer(connection);
            Thread consumerThread = new Thread(statusConsumer);
            consumerThread.start();
            
            // Producer untuk mengirim pesanan baru
            logger.info("Memulai producer untuk pesanan baru...");
            pesananProducer = new PesananProducer(connection);
            
            // Menu interaktif
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                printMenu();
                int pilihan = getUserChoice(scanner);
                
                switch (pilihan) {
                    case 1:
                        try {
                            // Verifikasi sekali lagi bahwa queue aktif
                            if (!RabbitMQUtil.verifyQueueExists(channel, RabbitMQUtil.QUEUE_PESANAN_BARU)) {
                                logger.warn("Queue pesanan baru tidak tersedia, mencoba reinisialisasi...");
                                RabbitMQUtil.initializeExchangesAndQueues(channel);
                            }
                            
                            Pesanan pesanan = createNewOrder(scanner);
                            // Simpan pesanan ke daftar sebelum mengirim ke queue
                            statusConsumer.tambahkanPesanan(pesanan);
                            pesananProducer.tambahkanPesanan(pesanan);
                            logger.info("Pesanan baru telah dibuat dan dikirim ke queue: {}", pesanan.getId());
                        } catch (Exception e) {
                            logger.error("Gagal membuat pesanan baru: {}", e.getMessage());
                            System.out.println("Terjadi kesalahan saat membuat pesanan. Silakan coba lagi.");
                        }
                        break;
                    case 2:
                        statusConsumer.tampilkanDaftarPesanan();
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        logger.warn("Pilihan tidak valid");
                }
            }
            
            // Tutup koneksi
            logger.info("Menutup aplikasi...");
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan fatal: {}", e.getMessage(), e);
            System.out.println("Aplikasi terhenti karena kesalahan: " + e.getMessage());
        } finally {
            // Tutup resource dalam blok finally untuk memastikan cleanup
            try {
                if (statusConsumer != null) {
                    statusConsumer.stop();
                    logger.info("Consumer status dihentikan");
                }
                
                if (pesananProducer != null) {
                    pesananProducer.stop();
                    logger.info("Producer pesanan dihentikan");
                }
                
                if (channel != null && channel.isOpen()) {
                    channel.close();
                    logger.info("Channel RabbitMQ ditutup");
                }
                
                if (connection != null && connection.isOpen()) {
                    connection.close();
                    logger.info("Koneksi RabbitMQ ditutup");
                }
            } catch (Exception e) {
                logger.error("Kesalahan saat menutup resource: {}", e.getMessage(), e);
            }
        }
    }
    
    private static void printMenu() {
        System.out.println("\n=== Aplikasi Layanan Pesanan E-Commerce ===");
        System.out.println("1. Buat Pesanan Baru");
        System.out.println("2. Lihat Daftar Pesanan");
        System.out.println("0. Keluar");
        System.out.print("Pilihan Anda: ");
    }
    
    private static int getUserChoice(Scanner scanner) {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private static Pesanan createNewOrder(Scanner scanner) {
        System.out.println("\n=== Buat Pesanan Baru ===");
        
        // Generate ID pesanan baru menggunakan counter lokal
        int idPesanan = pesananCounter.getAndIncrement();
        
        System.out.print("Nama Pelanggan: ");
        String namaPelanggan = scanner.nextLine();
        
        System.out.print("Alamat Pengiriman: ");
        String alamatPengiriman = scanner.nextLine();
        
        System.out.print("Total Harga: ");
        double totalHarga = Double.parseDouble(scanner.nextLine());
        
        return new Pesanan(idPesanan, namaPelanggan, alamatPengiriman, totalHarga);
    }

    private static void tampilkanDaftarPesanan(Map<Integer, Pesanan> daftarPesanan) {
        System.out.println("\n=== Daftar Pesanan ===");
        System.out.println("ID\tNama Pelanggan\t\tStatus");
        System.out.println("----------------------------------------");
        for (Pesanan pesanan : daftarPesanan.values()) {
            System.out.printf("%d\t%s\t%s%n", 
                pesanan.getId(), 
                pesanan.getNamaPelanggan(), 
                pesanan.getStatus());
        }
    }
} 