package edu.esi.ds.esientradas.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Token {

    @Id
    @Column(length = 36)
    private String valor;

    // Momento de activación (epoch millis, por ejemplo)
    private long horaActiva;

    // Token -> Entrada (la FK está en Token)
    // Una entrada puede no tener token => nullable = true en la FK
    // Y si quieres 1 token por entrada, pon unique = true
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = true, unique = true)
    private Entrada entrada;

    // Sesión actual guardada (ajusta tipo si tu "sesión" es otra entidad)
    private String sesionId;

    public Token() {
        this.valor = UUID.randomUUID().toString().replace("-", ""); // 32 chars
        this.horaActiva = System.currentTimeMillis(); // tiempo actual en ms desde epoch
    }

    // (Opcional) constructor útil si quieres asignar entrada/sesión al crear
    public Token(Entrada entrada, String sesionActual) {
        this();
        this.entrada = entrada;
        this.sesionId = sesionActual;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public long getHoraActiva() {
        return horaActiva;
    }

    public void setHoraActiva(long horaActiva) {
        this.horaActiva = horaActiva;
    }

    public Entrada getEntrada() {
        return entrada;
    }

    public void setEntrada(Entrada entrada) {
        this.entrada = entrada;
    }

    public String getSesionId() {
        return sesionId;
    }

    public void setSesionId(String sesionId) {
        this.sesionId = sesionId;
    }
}