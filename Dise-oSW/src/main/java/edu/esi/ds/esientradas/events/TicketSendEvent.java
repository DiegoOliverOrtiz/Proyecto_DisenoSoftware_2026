package edu.esi.ds.esientradas.events;

public class TicketSendEvent {
    private final String to;
    private final String subject;
    private final String html;
    private final String reference;

    public TicketSendEvent(String to, String subject, String html, String reference) {
        this.to = to;
        this.subject = subject;
        this.html = html;
        this.reference = reference;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtml() {
        return html;
    }

    public String getReference() {
        return reference;
    }
}
