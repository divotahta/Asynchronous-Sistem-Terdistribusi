package com.ecommerce.pesanan.util;

/**
 * Utilitas untuk menghasilkan ID unik berurutan
 */
public class IDGenerator {
    private static int currentPesananId = 1;
    private static int currentPengirimanId = 1;

    public static synchronized int generatePesananId() {
        return currentPesananId++;
    }

    public static synchronized int generatePengirimanId() {
        return currentPengirimanId++;
    }
} 