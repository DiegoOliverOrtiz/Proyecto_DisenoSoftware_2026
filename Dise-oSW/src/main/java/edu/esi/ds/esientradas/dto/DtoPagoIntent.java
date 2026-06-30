package edu.esi.ds.esientradas.dto;

public class DtoPagoIntent {
    private String publicKey;
    private String clientSecret;
    private String paymentIntentId;
    private Long amount;
    private String currency;

    public DtoPagoIntent() {
    }

    public DtoPagoIntent(String publicKey, String clientSecret, String paymentIntentId, Long amount, String currency) {
        this.publicKey = publicKey;
        this.clientSecret = clientSecret;
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
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
}
