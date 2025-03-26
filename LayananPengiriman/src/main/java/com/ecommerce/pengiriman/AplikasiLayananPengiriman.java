package com.ecommerce.pengiriman;

import com.ecommerce.pengiriman.model.DetailPengiriman;
import com.ecommerce.pengiriman.service.PesananListener;
import com.ecommerce.pengiriman.service.PengirimanStatusPublisher;
import com.ecommerce.pengiriman.util.IDGenerator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.ecommerce.pengiriman.util.RabbitMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Aplikasi utama untuk layanan pengiriman
 */
public class AplikasiLayananPengiriman {
    private static final Logger logger = LoggerFactory.getLogger(AplikasiLayananPengiriman.class);

    public static void main(String[] args) {
        logger.info("Memulai Aplikasi Layanan Pengiriman");
        
        try {
            // Membuat koneksi ke RabbitMQ
            Connection connection = RabbitMQUtil.createConnection();
            Channel channel = connection.createChannel();
            
            // Inisialisasi exchange dan queue
            RabbitMQUtil.initializeExchangesAndQueues(channel);
            
            // Service untuk menangani pesanan baru
            PesananListener pesananListener = new PesananListener(connection);
            Thread listenerThread = new Thread(pesananListener);
            listenerThread.start();
            
            // Publisher untuk mengirim status pengiriman
            PengirimanStatusPublisher statusPublisher = new PengirimanStatusPublisher(connection);
            
            // Menu interaktif
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                printMenu();
                int pilihan = getUserChoice(scanner);
                
                switch (pilihan) {
                    case 1:
                        pesananListener.tampilkanDaftarPengiriman();
                        break;
                    case 2:
                        updatePengirimanStatus(scanner, pesananListener, statusPublisher);
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
            pesananListener.stop();
            channel.close();
            connection.close();
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan: {}", e.getMessage(), e);
        }
    }
    
    private static void printMenu() {
        System.out.println("\n=== Aplikasi Layanan Pengiriman E-Commerce ===");
        System.out.println("1. Lihat Daftar Pesanan untuk Pengiriman");
        System.out.println("2. Update Status Pengiriman");
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
    
    private static void updatePengirimanStatus(
            Scanner scanner, 
            PesananListener pesananListener, 
            PengirimanStatusPublisher statusPublisher) {
        
        // Tampilkan daftar pengiriman terlebih dahulu
        pesananListener.tampilkanDaftarPengiriman();
        
        System.out.print("\nMasukkan ID Pengiriman yang ingin diupdate: ");
        int id;
        try {
            id = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID tidak valid! Harap masukkan angka.");
            return;
        }
        
        DetailPengiriman pengiriman = pesananListener.getPengirimanById(id);
        if (pengiriman == null) {
            System.out.println("Pengiriman dengan ID " + id + " tidak ditemukan!");
            return;
        }
        
        System.out.println("\nUpdate Status Pengiriman:");
        System.out.println("1. Dikemas");
        System.out.println("2. Dikirim");
        System.out.println("3. Terkirim");
        System.out.print("Pilih status baru: ");
        
        try {
            int statusChoice = Integer.parseInt(scanner.nextLine());
            String newStatus;
            
            switch (statusChoice) {
                case 1:
                    newStatus = "DIKEMAS";
                    break;
                case 2:
                    newStatus = "DIKIRIM";
                    System.out.print("Masukkan Kurir Pengiriman: ");
                    String kurir = scanner.nextLine();
                    pengiriman.setKurirPengiriman(kurir);
                    
                    System.out.print("Masukkan Nomor Resi: ");
                    String resi = scanner.nextLine();
                    pengiriman.setNomorResi(resi);
                    break;
                case 3:
                    newStatus = "TERKIRIM";
                    break;
                default:
                    System.out.println("Pilihan status tidak valid!");
                    return;
            }
            
            pengiriman.setStatusPengiriman(newStatus);
            
            // Publikasikan update status
            statusPublisher.publishPengirimanStatus(pengiriman);
            
            System.out.println("Status pengiriman berhasil diperbarui!");
            
        } catch (NumberFormatException e) {
            System.out.println("Input tidak valid!");
        }
    }
} 