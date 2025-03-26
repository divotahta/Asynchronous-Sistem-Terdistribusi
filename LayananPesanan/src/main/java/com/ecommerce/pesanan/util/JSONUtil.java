package com.ecommerce.pesanan.util;

import com.ecommerce.pesanan.model.Pesanan;
import org.json.JSONObject;

/**
 * Kelas utilitas untuk konversi objek ke/dari JSON
 */
public class JSONUtil {

    /**
     * Mengkonversi objek Pesanan menjadi JSON string
     */
    public static String toJSON(Pesanan pesanan) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", pesanan.getId());
        jsonObject.put("namaPelanggan", pesanan.getNamaPelanggan());
        jsonObject.put("alamatPengiriman", pesanan.getAlamatPengiriman());
        jsonObject.put("totalHarga", pesanan.getTotalHarga());
        jsonObject.put("tanggalPesanan", pesanan.getTanggalPesanan().getTime());
        jsonObject.put("status", pesanan.getStatus());
        
        return jsonObject.toString();
    }
    
    /**
     * Mengkonversi JSON string menjadi objek Pesanan
     */
    public static Pesanan toPesanan(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        
        Pesanan pesanan = new Pesanan();
        pesanan.setId(jsonObject.getInt("id"));
        pesanan.setNamaPelanggan(jsonObject.getString("namaPelanggan"));
        pesanan.setAlamatPengiriman(jsonObject.getString("alamatPengiriman"));
        pesanan.setTotalHarga(jsonObject.getDouble("totalHarga"));
        pesanan.setStatus(jsonObject.getString("status"));
        
        return pesanan;
    }
} 