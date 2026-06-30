package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;

public class DtoEspectaculo {
    private Long id;
    private String artista;
    private LocalDateTime fecha;
    private String escenario;
    private boolean altaDemanda;
    private LocalDateTime aperturaTaquilla;

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setEscenario(String nombre) {
        this.escenario = nombre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isAltaDemanda() {
        return altaDemanda;
    }

    public void setAltaDemanda(boolean altaDemanda) {
        this.altaDemanda = altaDemanda;
    }

    public LocalDateTime getAperturaTaquilla() {
        return aperturaTaquilla;
    }

    public void setAperturaTaquilla(LocalDateTime aperturaTaquilla) {
        this.aperturaTaquilla = aperturaTaquilla;
    }

    public String getArtista() {
        return artista;
    }
    public LocalDateTime getFecha() {
        return fecha;
    }
    public String getEscenario() {
        return escenario;
    }
    public Long getId() {
        return id;
    }
}
