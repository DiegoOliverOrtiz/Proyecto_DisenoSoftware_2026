package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;
import java.util.List;

import edu.esi.ds.esientradas.model.Entrada;

public class DtoEspectaculoDetalle {
    private Long id;
    private String artista;
    private LocalDateTime fecha;
    private String escenario;
    private boolean altaDemanda;
    private LocalDateTime aperturaTaquilla;
    private List<Entrada> entradas;

    public DtoEspectaculoDetalle() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getEscenario() {
        return escenario;
    }

    public void setEscenario(String escenario) {
        this.escenario = escenario;
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

    public List<Entrada> getEntradas() {
        return entradas;
    }

    public void setEntradas(List<Entrada> entradas) {
        this.entradas = entradas;
    }
}
