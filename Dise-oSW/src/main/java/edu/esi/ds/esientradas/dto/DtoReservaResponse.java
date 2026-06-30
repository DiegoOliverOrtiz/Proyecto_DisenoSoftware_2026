package edu.esi.ds.esientradas.dto;

public class DtoReservaResponse {
    private Long precioTotal;
    private Integer numeroEntradas;

    public DtoReservaResponse() {
    }

    public DtoReservaResponse(Long precioTotal, Integer numeroEntradas) {
        this.precioTotal = precioTotal;
        this.numeroEntradas = numeroEntradas;
    }

    public Long getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(Long precioTotal) {
        this.precioTotal = precioTotal;
    }

    public Integer getNumeroEntradas() {
        return numeroEntradas;
    }

    public void setNumeroEntradas(Integer numeroEntradas) {
        this.numeroEntradas = numeroEntradas;
    }
}
