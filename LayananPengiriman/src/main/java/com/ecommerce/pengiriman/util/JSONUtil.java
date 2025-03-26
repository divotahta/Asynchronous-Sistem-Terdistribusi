package com.ecommerce.pengiriman.util;

import com.ecommerce.pengiriman.model.DetailPengiriman;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilitas untuk konversi JSON
 */
public class JSONUtil {
    // Counter untuk ID pengiriman, menggunakan AtomicInteger untuk thread-safety
    private static final AtomicInteger pengirimanCounter = new AtomicInteger(1);
    
    /**
     * Mengkonversi DetailPengiriman ke string JSON
     */
    public static String toJSON(DetailPengiriman pengiriman) {
        JSONObject json = new JSONObject();
        
        // Tambahkan properti dari DetailPengiriman ke JSON
        json.put("id", pengiriman.getId());
        json.put("idPesanan", pengiriman.getIdPesanan());
        json.put("namaPelanggan", pengiriman.getNamaPelanggan());
        json.put("alamatPengiriman", pengiriman.getAlamatPengiriman());
        json.put("statusPengiriman", pengiriman.getStatusPengiriman());
        
        // Tambahkan properti opsional jika tidak null
        if (pengiriman.getKurirPengiriman() != null) {
            json.put("kurirPengiriman", pengiriman.getKurirPengiriman());
        }
        
        if (pengiriman.getNomorResi() != null) {
            json.put("nomorResi", pengiriman.getNomorResi());
        }
        
        return json.toString();
    }
    
    /**
     * Mengkonversi string JSON ke DetailPengiriman
     */
    public static DetailPengiriman toDetailPengiriman(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        
        int id = json.getInt("id");
        int idPesanan = json.getInt("idPesanan");
        String namaPelanggan = json.getString("namaPelanggan");
        String alamatPengiriman = json.getString("alamatPengiriman");
        String statusPengiriman = json.getString("statusPengiriman");
        
        DetailPengiriman pengiriman = new DetailPengiriman(id, idPesanan, namaPelanggan, alamatPengiriman);
        pengiriman.setStatusPengiriman(statusPengiriman);
        
        if (json.has("kurirPengiriman")) {
            pengiriman.setKurirPengiriman(json.getString("kurirPengiriman"));
        }
        
        if (json.has("nomorResi")) {
            pengiriman.setNomorResi(json.getString("nomorResi"));
        }
        
        return pengiriman;
    }
    
    /**
     * Mengkonversi JSON pesanan menjadi DetailPengiriman
     */
    public static DetailPengiriman pesananToDetailPengiriman(String jsonPesanan) {
        JSONObject jsonObject = new JSONObject(jsonPesanan);
        
        int idPesanan = jsonObject.getInt("id");
        String namaPelanggan = jsonObject.getString("namaPelanggan");
        String alamatPengiriman = jsonObject.getString("alamatPengiriman");
        
        // Generate ID pengiriman baru dari counter lokal
        int idPengiriman = pengirimanCounter.getAndIncrement();
        
        return new DetailPengiriman(idPengiriman, idPesanan, namaPelanggan, alamatPengiriman);
    }
} 