package com.ecommerce.pesanan;

import com.ecommerce.pesanan.model.Pesanan;
import com.ecommerce.pesanan.service.PesananPublisher;
import com.ecommerce.pesanan.service.PesananStatusListener;
import com.ecommerce.pesanan.util.IDGenerator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.ecommerce.pesanan.util.RabbitMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.Map;

/**
 * Aplikasi utama untuk layanan pesanan
 */
public class AplikasiLayananPesanan {
    private static final Logger logger = LoggerFactory.getLogger(AplikasiLayananPesanan.class);

    public static void main(String[] args) {
        logger.info("Memulai Aplikasi Layanan Pesanan");
        
        try {
            // Membuat koneksi ke RabbitMQ
            Connection connection = RabbitMQUtil.createConnection();
            Channel channel = connection.createChannel();
            
            // Inisialisasi exchange dan queue
            RabbitMQUtil.initializeExchangesAndQueues(channel);
            
            // Mulai listener untuk status pengiriman
            PesananStatusListener statusListener = new PesananStatusListener(connection);
            Thread listenerThread = new Thread(statusListener);
            listenerThread.start();
            
            // Publisher untuk mengirim pesanan baru
            PesananPublisher pesananPublisher = new PesananPublisher(connection);
            
            // Menu interaktif
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                printMenu();
                int pilihan = getUserChoice(scanner);
                
                switch (pilihan) {
                    case 1:
                        Pesanan pesanan = createNewOrder(scanner);
                        // Simpan pesanan ke daftar sebelum mengirim ke RabbitMQ
                        statusListener.tambahkanPesanan(pesanan);
                        pesananPublisher.publishPesananBaru(pesanan);
                        logger.info("Pesanan baru telah dibuat dan dikirim: {}", pesanan.getId());
                        break;
                    case 2:
                        statusListener.tampilkanDaftarPesanan();
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
            statusListener.stop();
            channel.close();
            connection.close();
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan: {}", e.getMessage(), e);
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
        
        // Generate ID pesanan baru
        int idPesanan = IDGenerator.generatePesananId();
        
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