package com.ecommerce.pengiriman.util;

// import com.ecommerce.pengiriman.IDGenerator;
import com.ecommerce.pengiriman.model.DetailPengiriman;
import org.json.JSONObject;

/**
 * Kelas utilitas untuk konversi objek ke/dari JSON
 */
public class JSONUtil {

    /**
     * Mengkonversi objek DetailPengiriman menjadi JSON string
     */
    public static String toJSON(DetailPengiriman pengiriman) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", pengiriman.getId());
        jsonObject.put("idPesanan", pengiriman.getIdPesanan());
        jsonObject.put("namaPelanggan", pengiriman.getNamaPelanggan());
        jsonObject.put("alamatPengiriman", pengiriman.getAlamatPengiriman());
        jsonObject.put("tanggalPengiriman", pengiriman.getTanggalPengiriman().getTime());
        jsonObject.put("statusPengiriman", pengiriman.getStatusPengiriman());
        
        if (pengiriman.getKurirPengiriman() != null) {
            jsonObject.put("kurirPengiriman", pengiriman.getKurirPengiriman());
        }
        
        if (pengiriman.getNomorResi() != null) {
            jsonObject.put("nomorResi", pengiriman.getNomorResi());
        }
        
        return jsonObject.toString();
    }
    
    /**
     * Mengkonversi JSON string menjadi objek DetailPengiriman
     */
    public static DetailPengiriman toDetailPengiriman(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        
        DetailPengiriman pengiriman = new DetailPengiriman();
        pengiriman.setId(jsonObject.getInt("id"));
        pengiriman.setIdPesanan(jsonObject.getInt("idPesanan"));
        pengiriman.setNamaPelanggan(jsonObject.getString("namaPelanggan"));
        pengiriman.setAlamatPengiriman(jsonObject.getString("alamatPengiriman"));
        pengiriman.setStatusPengiriman(jsonObject.getString("statusPengiriman"));
        
        if (jsonObject.has("kurirPengiriman")) {
            pengiriman.setKurirPengiriman(jsonObject.getString("kurirPengiriman"));
        }
        
        if (jsonObject.has("nomorResi")) {
            pengiriman.setNomorResi(jsonObject.getString("nomorResi"));
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
        
        // Generate ID pengiriman baru berurutan
        int idPengiriman = IDGenerator.generatePengirimanId();
        
        return new DetailPengiriman(idPengiriman, idPesanan, namaPelanggan, alamatPengiriman);
    }
} 