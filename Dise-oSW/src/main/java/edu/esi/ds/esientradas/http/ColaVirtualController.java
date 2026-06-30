package edu.esi.ds.esientradas.http;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.esi.ds.esientradas.dto.DtoColaEstado;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/colas")
@Validated
public class ColaVirtualController {
    private static final String SESSION_COOKIE = "session_id";

    private final ColaVirtualService colaVirtualService;
    private final UsuarioService usuarioService;

    public ColaVirtualController(ColaVirtualService colaVirtualService, UsuarioService usuarioService) {
        this.colaVirtualService = colaVirtualService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/join")
    public DtoColaEstado join(
        HttpServletRequest request,
        HttpSession session,
        @RequestParam @Positive Long espectaculoId,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        return colaVirtualService.join(espectaculoId, queueIdentity(request, session, queueClientId));
    }

    @GetMapping("/status")
    public DtoColaEstado status(
        HttpServletRequest request,
        HttpSession session,
        @RequestParam @Positive Long espectaculoId,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        return colaVirtualService.status(espectaculoId, queueIdentity(request, session, queueClientId));
    }

    @DeleteMapping("/leave")
    public void leave(
        HttpServletRequest request,
        HttpSession session,
        @RequestParam @Positive Long espectaculoId,
        @RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        colaVirtualService.leave(espectaculoId, queueIdentity(request, session, queueClientId));
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
}
