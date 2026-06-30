package edu.esi.ds.esientradas.services;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.dto.DtoPagoResultado;
import edu.esi.ds.esientradas.events.EmailQueueCreatedEvent;
import edu.esi.ds.esientradas.model.EmailQueue;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;

@Service
public class PagosService {

    private static final Logger log = LoggerFactory.getLogger(PagosService.class);
    private static final long RESERVA_TTL_MILLIS = 10 * 60 * 1000;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.public-key:}")
    private String stripePublicKey;

    @Autowired
    private TokenDao tokenDao;

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Autowired
    private ColaVirtualService colaVirtualService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public DtoPagoIntent crearIntentoPago(String sesionId, String userEmail) {
        List<Token> reservas = this.getReservas(sesionId);
        long amount = reservas.stream().mapToLong(token -> token.getEntrada().getPrecio()).sum();

        this.configurarStripe();

        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("eur")
                    .addPaymentMethodType("card")
                    .setDescription("Compra de entradas")
                    .putMetadata("sessionId", sesionId)
                    .putMetadata("entryIds", this.serializeEntryIds(reservas));

            if (userEmail != null && !userEmail.isBlank()) {
                builder.putMetadata("userEmail", userEmail);
            }

            PaymentIntentCreateParams params = builder.build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            return new DtoPagoIntent(
                    stripePublicKey,
                    paymentIntent.getClientSecret(),
                    paymentIntent.getId(),
                    paymentIntent.getAmount(),
                    paymentIntent.getCurrency());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo crear el pago en Stripe");
        }
    }

    @Transactional
    public DtoPagoResultado confirmarPago(String sesionId, String paymentIntentId, String authenticatedUserEmail) {
        this.configurarStripe();
        PaymentIntent paymentIntent = this.recuperarPago(paymentIntentId);
        this.validarSesion(paymentIntent, sesionId);

        List<Token> reservas = this.getReservas(sesionId);
        this.validarPagoCoincideConReservas(paymentIntent, reservas);
        if ("succeeded".equals(paymentIntent.getStatus())) {
            String userEmail = this.resolveUserEmail(authenticatedUserEmail, paymentIntent);
            List<Long> espectaculoIds = reservas.stream()
                    .filter(token -> token.getEntrada() != null && token.getEntrada().getEspectaculo() != null)
                    .map(token -> token.getEntrada().getEspectaculo().getId())
                    .distinct()
                    .toList();

            if (!reservas.isEmpty()) {
                for (Token token : reservas) {
                    int updated = this.entradaDao.updateEstadoIf(token.getEntrada().getId(), Estado.VENDIDA.name(), Estado.RESERVADA.name());
                    if (updated == 0) {
                        // Alguna entrada ya no estaba reservada por esta sesión — rollback
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo confirmar la venta: alguna entrada no estaba reservada");
                    }
                }
                this.tokenDao.deleteBySesionId(sesionId);
            }

            if (userEmail != null && !userEmail.isBlank()) {
                String queueIdentity = "user:" + userEmail;
                for (Long espectaculoId : espectaculoIds) {
                    this.colaVirtualService.leave(espectaculoId, queueIdentity);
                }
            }

            boolean emailScheduled = false;
            if (userEmail != null && !userEmail.isBlank()) {
                String html = generarHtmlTicket(reservas);
                EmailQueue queuedEmail = this.emailDeliveryService.enqueue(
                        userEmail,
                        "Tus entradas - ESI Entradas",
                        html,
                        paymentIntentId);
                this.eventPublisher.publishEvent(new EmailQueueCreatedEvent(queuedEmail.getId()));
                emailScheduled = true;
            } else {
                log.warn("No hay email de usuario autenticado ni metadata userEmail en el paymentIntent {}", paymentIntentId);
            }

                String message = "Pago confirmado" + (emailScheduled ? " - email encolado" : " - email no encolado");
            return new DtoPagoResultado(
                    paymentIntent.getStatus(),
                    message,
                    this.amountOrReceived(paymentIntent),
                    paymentIntent.getCurrency(),
                    reservas.size());
        }

        return new DtoPagoResultado(
                paymentIntent.getStatus(),
                "El pago aun no se ha completado",
                this.amountOrReceived(paymentIntent),
                paymentIntent.getCurrency(),
                0);
    }

    @Transactional
    public void cancelarPago(String sesionId) {
        List<Token> reservas = this.tokenDao.findBySesionId(sesionId);

        for (Token token : reservas) {
            if (token.getEntrada() == null) {
                continue;
            }

            this.entradaDao.updateEstadoIf(
                    token.getEntrada().getId(),
                    Estado.DISPONIBLE.name(),
                    Estado.RESERVADA.name());
        }

        if (!reservas.isEmpty()) {
            this.tokenDao.deleteBySesionId(sesionId);
        }
    }

    private List<Token> getReservas(String sesionId) {
        List<Token> reservas = this.tokenDao.findBySesionId(sesionId);
        if (reservas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay entradas reservadas para pagar");
        }
        long horaLimite = System.currentTimeMillis() - RESERVA_TTL_MILLIS;
        boolean caducada = reservas.stream().anyMatch(token -> token.getHoraActiva() < horaLimite);
        if (caducada) {
            for (Token token : reservas) {
                if (token.getEntrada() != null && token.getEntrada().getEstado() == Estado.RESERVADA) {
                    this.entradaDao.updateEstado(token.getEntrada().getId(), Estado.DISPONIBLE.name());
                }
            }
            this.tokenDao.deleteBySesionId(sesionId);
            throw new ResponseStatusException(HttpStatus.GONE, "La reserva ha caducado");
        }
        return reservas;
    }

    private void configurarStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta configurar STRIPE_SECRET_KEY");
        }
        if (stripePublicKey == null || stripePublicKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta configurar STRIPE_PUBLIC_KEY");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    private PaymentIntent recuperarPago(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar el pago en Stripe");
        }
    }

    private void validarSesion(PaymentIntent paymentIntent, String sesionId) {
        String paymentSessionId = paymentIntent.getMetadata().get("sessionId");
        if (paymentSessionId == null || !paymentSessionId.equals(sesionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese pago no pertenece a la sesion actual");
        }
    }

    void validarPagoCoincideConReservas(PaymentIntent paymentIntent, List<Token> reservas) {
        long expectedAmount = reservas.stream()
                .map(Token::getEntrada)
                .mapToLong(entrada -> entrada != null && entrada.getPrecio() != null ? entrada.getPrecio() : 0L)
                .sum();
        Long stripeAmount = paymentIntent.getAmount();
        if (stripeAmount == null || stripeAmount.longValue() != expectedAmount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe del pago no coincide con las reservas actuales");
        }

        String currency = paymentIntent.getCurrency();
        if (currency == null || !"eur".equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La moneda del pago no coincide con la compra");
        }

        String metadataEntryIds = paymentIntent.getMetadata() == null ? null : paymentIntent.getMetadata().get("entryIds");
        if (!entryIdsFromMetadata(metadataEntryIds).equals(entryIdsFromReservations(reservas))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Las entradas del pago no coinciden con las reservas actuales");
        }
    }

    private Set<Long> entryIdsFromReservations(List<Token> reservas) {
        return reservas.stream()
                .map(Token::getEntrada)
                .filter(entrada -> entrada != null && entrada.getId() != null)
                .map(entrada -> entrada.getId())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Long> entryIdsFromMetadata(String value) {
        Set<Long> ids = new TreeSet<>();
        if (value == null || value.isBlank()) {
            return ids;
        }
        for (String part : value.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Las entradas del pago no son validas");
            }
        }
        return ids;
    }

    private String serializeEntryIds(List<Token> reservas) {
        return reservas.stream()
                .map(token -> token.getEntrada().getId().toString())
                .collect(Collectors.joining(","));
    }

    private long amountOrReceived(PaymentIntent paymentIntent) {
        Long amountReceived = paymentIntent.getAmountReceived();
        if (amountReceived != null && amountReceived > 0) {
            return amountReceived;
        }
        return paymentIntent.getAmount();
    }

    private String resolveUserEmail(String authenticatedUserEmail, PaymentIntent paymentIntent) {
        if (authenticatedUserEmail != null && !authenticatedUserEmail.isBlank()) {
            return authenticatedUserEmail;
        }
        if (paymentIntent.getMetadata() == null) {
            return null;
        }
        return paymentIntent.getMetadata().get("userEmail");
    }

    private String generarHtmlTicket(List<Token> reservas) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(Boolean.TRUE)) {
            return generarHtmlTicketBonito(reservas);
        }
        sb.append("<html><body>");
        sb.append("<h2>Tus entradas - ESI Entradas</h2>");
        sb.append("<ul>");
        for (Token token : reservas) {
            if (token.getEntrada() == null) continue;
            var entrada = token.getEntrada();
            var espectaculo = entrada.getEspectaculo();
            sb.append("<li>");
            sb.append("<strong>Espectáculo:</strong> ");
            sb.append(espectaculo != null ? espectaculo.getArtista() : "-");
            sb.append("<br/>");
            sb.append("<strong>Fecha:</strong> ");
            sb.append(espectaculo != null && espectaculo.getFecha() != null ? espectaculo.getFecha().toString() : "-");
            sb.append("<br/>");
            if (entrada instanceof edu.esi.ds.esientradas.model.Precisa) {
                edu.esi.ds.esientradas.model.Precisa p = (edu.esi.ds.esientradas.model.Precisa) entrada;
                sb.append("Asiento: planta " + p.getPlanta() + ", fila " + p.getFila() + ", columna " + p.getColumna());
            } else if (entrada instanceof edu.esi.ds.esientradas.model.DeZona) {
                edu.esi.ds.esientradas.model.DeZona z = (edu.esi.ds.esientradas.model.DeZona) entrada;
                sb.append("Zona: " + z.getZona());
            } else {
                sb.append("Entrada id: " + entrada.getId());
            }
            sb.append("</li>");
        }
        sb.append("</ul>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String generarHtmlTicketBonito(List<Token> reservas) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>");
        sb.append("<html lang=\"es\">");
        sb.append("<body style=\"margin:0;padding:0;background:#f3f5f7;font-family:Arial,Helvetica,sans-serif;color:#1f2933;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f3f5f7;padding:28px 12px;\">");
        sb.append("<tr><td align=\"center\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:680px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #dfe5ec;\">");
        sb.append("<tr><td style=\"background:#111827;padding:30px 28px;color:#ffffff;\">");
        sb.append("<div style=\"font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#93c5fd;font-weight:700;\">ESI Entradas</div>");
        sb.append("<h1 style=\"margin:10px 0 0;font-size:28px;line-height:1.2;font-weight:800;\">Tus entradas estan confirmadas</h1>");
        sb.append("<p style=\"margin:12px 0 0;font-size:15px;line-height:1.5;color:#d1d5db;\">Guarda este correo. Presenta tus entradas al acceder al espectaculo.</p>");
        sb.append("</td></tr>");
        sb.append("<tr><td style=\"padding:24px 28px;\">");
        sb.append("<div style=\"background:#ecfdf5;border:1px solid #a7f3d0;border-radius:10px;padding:14px 16px;margin-bottom:22px;color:#065f46;font-size:14px;\">");
        sb.append("<strong>Compra completada.</strong> Hemos incluido el detalle de tus entradas a continuacion.");
        sb.append("</div>");

        for (Token token : reservas) {
            if (token.getEntrada() == null) {
                continue;
            }
            var entrada = token.getEntrada();
            var espectaculo = entrada.getEspectaculo();
            sb.append("<div style=\"border:1px solid #d8dee8;border-radius:12px;margin:0 0 16px;overflow:hidden;\">");
            sb.append("<div style=\"background:#f8fafc;padding:16px 18px;border-bottom:1px solid #e5e7eb;\">");
            sb.append("<div style=\"font-size:12px;text-transform:uppercase;letter-spacing:.08em;color:#64748b;font-weight:700;\">Entrada</div>");
            sb.append("<div style=\"font-size:21px;font-weight:800;color:#111827;margin-top:4px;\">");
            sb.append(escapeHtml(espectaculo != null ? espectaculo.getArtista() : "-"));
            sb.append("</div></div>");
            sb.append("<div style=\"padding:16px 18px;\">");
            sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">");
            addTicketRow(sb, "Fecha", espectaculo != null && espectaculo.getFecha() != null ? espectaculo.getFecha().toString() : "-");
            addTicketRow(sb, "Ubicacion", ubicacionEntrada(entrada));
            addTicketRow(sb, "Precio", formatEuros(entrada.getPrecio()) + " EUR");
            sb.append("</table>");
            sb.append("<div style=\"margin-top:14px;padding:12px 14px;border-radius:8px;background:#111827;color:#ffffff;font-size:13px;font-weight:700;text-align:center;letter-spacing:.08em;\">");
            sb.append("ENTRADA #");
            sb.append(entrada.getId());
            sb.append("</div></div></div>");
        }

        sb.append("<p style=\"margin:22px 0 0;color:#64748b;font-size:13px;line-height:1.5;\">Si necesitas ayuda, contacta con soporte de ESI Entradas. Este mensaje se ha generado automaticamente.</p>");
        sb.append("</td></tr></table></td></tr></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void addTicketRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td style=\"padding:6px 0;color:#64748b;font-size:13px;width:34%;\">");
        sb.append(escapeHtml(label));
        sb.append("</td><td style=\"padding:6px 0;color:#111827;font-size:14px;font-weight:700;\">");
        sb.append(escapeHtml(value));
        sb.append("</td></tr>");
    }

    private String ubicacionEntrada(edu.esi.ds.esientradas.model.Entrada entrada) {
        if (entrada instanceof edu.esi.ds.esientradas.model.Precisa) {
            edu.esi.ds.esientradas.model.Precisa p = (edu.esi.ds.esientradas.model.Precisa) entrada;
            return "Planta " + p.getPlanta() + ", fila " + p.getFila() + ", asiento " + p.getColumna();
        }
        if (entrada instanceof edu.esi.ds.esientradas.model.DeZona) {
            edu.esi.ds.esientradas.model.DeZona z = (edu.esi.ds.esientradas.model.DeZona) entrada;
            return "Zona " + z.getZona();
        }
        return "Entrada " + entrada.getId();
    }

    private String formatEuros(Long cents) {
        if (cents == null) {
            return "0.00";
        }
        return String.format(java.util.Locale.ROOT, "%.2f", cents / 100.0);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
