package edu.esi.ds.esientradas.dto;

public class DtoEntradaCompra {
    private Long id;
    private String descripcion;
    private Long precio;

    public DtoEntradaCompra() {
    }

    public DtoEntradaCompra(Long id, String descripcion, Long precio) {
        this.id = id;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getPrecio() {
        return precio;
    }

    public void setPrecio(Long precio) {
        this.precio = precio;
    }
}
