package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;

public class DtoColaEstado {
    private boolean requiereCola;
    private boolean taquillaAbierta;
    private boolean enCola;
    private boolean turnoActivo;
    private int posicion;
    private int personasDelante;
    private long segundosTurnoRestantes;
    private String accessToken;
    private LocalDateTime aperturaTaquilla;
    private String message;
    private boolean entradasAgotadas;

    public boolean isRequiereCola() { return requiereCola; }
    public void setRequiereCola(boolean requiereCola) { this.requiereCola = requiereCola; }
    public boolean isTaquillaAbierta() { return taquillaAbierta; }
    public void setTaquillaAbierta(boolean taquillaAbierta) { this.taquillaAbierta = taquillaAbierta; }
    public boolean isEnCola() { return enCola; }
    public void setEnCola(boolean enCola) { this.enCola = enCola; }
    public boolean isTurnoActivo() { return turnoActivo; }
    public void setTurnoActivo(boolean turnoActivo) { this.turnoActivo = turnoActivo; }
    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }
    public int getPersonasDelante() { return personasDelante; }
    public void setPersonasDelante(int personasDelante) { this.personasDelante = personasDelante; }
    public long getSegundosTurnoRestantes() { return segundosTurnoRestantes; }
    public void setSegundosTurnoRestantes(long segundosTurnoRestantes) { this.segundosTurnoRestantes = segundosTurnoRestantes; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public LocalDateTime getAperturaTaquilla() { return aperturaTaquilla; }
    public void setAperturaTaquilla(LocalDateTime aperturaTaquilla) { this.aperturaTaquilla = aperturaTaquilla; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isEntradasAgotadas() { return entradasAgotadas; }
    public void setEntradasAgotadas(boolean entradasAgotadas) { this.entradasAgotadas = entradasAgotadas; }
}
