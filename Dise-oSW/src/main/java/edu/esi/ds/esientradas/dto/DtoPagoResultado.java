package edu.esi.ds.esientradas.dto;

public class DtoPagoResultado {
    private String status;
    private String message;
    private Long amount;
    private String currency;
    private Integer entradasConfirmadas;

    public DtoPagoResultado() {
    }

    public DtoPagoResultado(String status, String message, Long amount, String currency, Integer entradasConfirmadas) {
        this.status = status;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.entradasConfirmadas = entradasConfirmadas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getEntradasConfirmadas() {
        return entradasConfirmadas;
    }

    public void setEntradasConfirmadas(Integer entradasConfirmadas) {
        this.entradasConfirmadas = entradasConfirmadas;
    }
}
