package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.dto.DtoPagoResultado;
import edu.esi.ds.esientradas.services.PagosService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/pagos")
@Validated
public class PagoController {
    private static final String SESSION_COOKIE = "session_id";

    @Autowired
    private PagosService pagosService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/payment-intent")
    public DtoPagoIntent createPaymentIntent(
        HttpServletRequest request,
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        String userEmail = requireAuthenticatedUser(request);
        session.setAttribute("userEmail", userEmail);
        return this.pagosService.crearIntentoPago(reservationIdentity(session, queueClientId), userEmail);
    }

    @PostMapping("/confirmar")
    public DtoPagoResultado confirmarPago(
        HttpServletRequest request,
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId,
        @RequestParam @Size(max = 120) @Pattern(regexp = "^[A-Za-z0-9_\\-]+$") String paymentIntentId
    ) {
        String userEmail = requireAuthenticatedUser(request);
        session.setAttribute("userEmail", userEmail);
        DtoPagoResultado resultado = this.pagosService.confirmarPago(reservationIdentity(session, queueClientId), paymentIntentId, userEmail);
        return resultado;
    }

    @PostMapping("/cancelar")
    public void cancelarPago(
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId,
        @RequestParam(value = "clientId", required = false) String queryClientId
    ) {
        this.pagosService.cancelarPago(reservationIdentity(session, queueClientId != null ? queueClientId : queryClientId));
    }

    private String requireAuthenticatedUser(HttpServletRequest request) {
        String token = sessionToken(request);

        if (token == null || token.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Autenticacion requerida"
            );
        }

        return this.usuarioService.checkToken(token);
    }

    private String sessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String reservationIdentity(HttpSession session, String queueClientId) {
        if (queueClientId == null || queueClientId.isBlank()) {
            return session.getId();
        }
        return session.getId() + ":" + queueClientId.strip();
    }
}
