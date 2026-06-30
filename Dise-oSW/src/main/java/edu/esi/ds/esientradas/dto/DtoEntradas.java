package edu.esi.ds.esientradas.dto;

public class DtoEntradas {
    Integer total;
    Integer libres;
    Integer reservadas;
    Integer vendidas;

    public DtoEntradas() {
    }

    public DtoEntradas(Integer total, Integer libres, Integer reservadas, Integer vendidas) {
        this.total = total;
        this.libres = libres;
        this.reservadas = reservadas;
        this.vendidas = vendidas;
    }

    // Getters and Setters
    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getLibres() {
        return libres;
    }

    public void setLibres(Integer libres) {
        this.libres = libres;
    }

    public Integer getReservadas() {
        return reservadas;
    }

    public void setReservadas(Integer reservadas) {
        this.reservadas = reservadas;
    }

    public Integer getVendidas() {
        return vendidas;
    }

    public void setVendidas(Integer vendidas) {
        this.vendidas = vendidas;
    }
}
