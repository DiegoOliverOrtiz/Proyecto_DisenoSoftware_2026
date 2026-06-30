package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoReservaRequest;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import edu.esi.ds.esientradas.services.ReservasService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/reservas")
@Validated
public class ReservasController {
    private static final String SESSION_COOKIE = "session_id";

    @Autowired
    private ReservasService service;

    @Autowired
    private ColaVirtualService colaVirtualService;

    @Autowired
    private UsuarioService usuarioService;
    
    @PutMapping("/reservar")
    public Long reservar(
        HttpServletRequest servletRequest,
        HttpSession session,
        @RequestParam @Positive Long idEntrada,
        @RequestHeader(value = "X-Queue-Access", required = false) String queueAccessToken,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        colaVirtualService.assertAccessForEntradas(
            List.of(idEntrada),
            queueIdentity(servletRequest, session, queueClientId),
            queueAccessToken
        );
        String reservaId = reservationIdentity(session, queueClientId);
        this.service.reservar(idEntrada, reservaId);
        return this.service.getResumen(reservaId).getPrecioTotal();
    }

    @PutMapping("/reservar-lote")
    public DtoReservaResponse reservarLote(
        HttpServletRequest servletRequest,
        HttpSession session,
        @Valid @RequestBody DtoReservaRequest reservaRequest,
        @RequestHeader(value = "X-Queue-Access", required = false) String queueAccessToken,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        colaVirtualService.assertAccessForEntradas(
            reservaRequest.getEntradaIds(),
            queueIdentity(servletRequest, session, queueClientId),
            queueAccessToken
        );
        String reservaId = reservationIdentity(session, queueClientId);
        this.service.reservarEntradas(reservaRequest.getEntradaIds(), reservaId);
        return this.service.getResumen(reservaId);
    }

    @GetMapping("/summary")
    public DtoReservaResponse resumen(
        HttpSession session,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        return this.service.getResumen(reservationIdentity(session, queueClientId));
    }

    private String queueIdentity(HttpServletRequest request, HttpSession session, String queueClientId) {
        String userToken = sessionToken(request);
        if (userToken != null && !userToken.isBlank()) {
            return "user:" + usuarioService.checkToken(userToken);
        }
        if (queueClientId == null || queueClientId.isBlank()) {
            return session.getId();
        }
        return session.getId() + ":" + queueClientId.strip();
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
