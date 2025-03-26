package com.ecommerce.pengiriman.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Kelas model untuk merepresentasikan detail pengiriman pesanan
 */
public class DetailPengiriman implements Serializable {
    private int id;
    private int idPesanan;
    private String namaPelanggan;
    private String alamatPengiriman;
    private Date tanggalPengiriman;
    private String statusPengiriman;
    private String kurirPengiriman;
    private String nomorResi;

    public DetailPengiriman() {
        this.tanggalPengiriman = new Date();
        this.statusPengiriman = "MENUNGGU_PENGIRIMAN";
    }

    public DetailPengiriman(int id, int idPesanan, String namaPelanggan, String alamatPengiriman) {
        this();
        this.id = id;
        this.idPesanan = idPesanan;
        this.namaPelanggan = namaPelanggan;
        this.alamatPengiriman = alamatPengiriman;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdPesanan() {
        return idPesanan;
    }

    public void setIdPesanan(int idPesanan) {
        this.idPesanan = idPesanan;
    }

    public String getNamaPelanggan() {
        return namaPelanggan;
    }

    public void setNamaPelanggan(String namaPelanggan) {
        this.namaPelanggan = namaPelanggan;
    }

    public String getAlamatPengiriman() {
        return alamatPengiriman;
    }

    public void setAlamatPengiriman(String alamatPengiriman) {
        this.alamatPengiriman = alamatPengiriman;
    }

    public Date getTanggalPengiriman() {
        return tanggalPengiriman;
    }

    public void setTanggalPengiriman(Date tanggalPengiriman) {
        this.tanggalPengiriman = tanggalPengiriman;
    }

    public String getStatusPengiriman() {
        return statusPengiriman;
    }

    public void setStatusPengiriman(String statusPengiriman) {
        this.statusPengiriman = statusPengiriman;
    }

    public String getKurirPengiriman() {
        return kurirPengiriman;
    }

    public void setKurirPengiriman(String kurirPengiriman) {
        this.kurirPengiriman = kurirPengiriman;
    }

    public String getNomorResi() {
        return nomorResi;
    }

    public void setNomorResi(String nomorResi) {
        this.nomorResi = nomorResi;
    }

    @Override
    public String toString() {
        return "DetailPengiriman{" +
                "id=" + id +
                ", idPesanan=" + idPesanan +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", alamatPengiriman='" + alamatPengiriman + '\'' +
                ", tanggalPengiriman=" + tanggalPengiriman +
                ", statusPengiriman='" + statusPengiriman + '\'' +
                ", kurirPengiriman='" + kurirPengiriman + '\'' +
                ", nomorResi='" + nomorResi + '\'' +
                '}';
    }
} 