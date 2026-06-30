package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dto.DtoPagoIntent;
import edu.esi.ds.esientradas.services.PagosService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/compras")
@Validated
public class ComprasController {
    private static final String SESSION_COOKIE = "session_id";

    @Autowired
    private UsuarioService usuariosService;

    @Autowired
    private PagosService pagosService;

    @PutMapping("/comprar")
    public DtoPagoIntent comprar(
        HttpServletRequest request,
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        String userEmail = requireAuthenticatedUser(request);
        session.setAttribute("userEmail", userEmail);
        return this.pagosService.crearIntentoPago(reservationIdentity(session, queueClientId), userEmail);
    }

    private String requireAuthenticatedUser(HttpServletRequest request) {
        String token = sessionToken(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
        }
        return this.usuariosService.checkToken(token);
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
