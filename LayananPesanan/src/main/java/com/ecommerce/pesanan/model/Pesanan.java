package com.ecommerce.pesanan.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Kelas model untuk merepresentasikan pesanan pelanggan
 */
public class Pesanan implements Serializable {
    private int id;
    private String namaPelanggan;
    private String alamatPengiriman;
    private double totalHarga;
    private Date tanggalPesanan;
    private String status;

    public Pesanan() {
        this.tanggalPesanan = new Date();
        this.status = "BARU";
    }

    public Pesanan(int id, String namaPelanggan, String alamatPengiriman, double totalHarga) {
        this();
        this.id = id;
        this.namaPelanggan = namaPelanggan;
        this.alamatPengiriman = alamatPengiriman;
        this.totalHarga = totalHarga;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getTotalHarga() {
        return totalHarga;
    }

    public void setTotalHarga(double totalHarga) {
        this.totalHarga = totalHarga;
    }

    public Date getTanggalPesanan() {
        return tanggalPesanan;
    }

    public void setTanggalPesanan(Date tanggalPesanan) {
        this.tanggalPesanan = tanggalPesanan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Pesanan{" +
                "id=" + id +
                ", namaPelanggan='" + namaPelanggan + '\'' +
                ", alamatPengiriman='" + alamatPengiriman + '\'' +
                ", totalHarga=" + totalHarga +
                ", tanggalPesanan=" + tanggalPesanan +
                ", status='" + status + '\'' +
                '}';
    }
} 