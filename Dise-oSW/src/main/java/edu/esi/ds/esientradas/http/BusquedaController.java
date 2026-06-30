package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoEntradaCompra;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.dto.DtoEspectaculo;
import edu.esi.ds.esientradas.dto.DtoEspectaculoDetalle;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.services.BusquedaService;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


@RestController
@RequestMapping("/busqueda")
@Validated
public class BusquedaController {
    private static final String SESSION_COOKIE = "session_id";

    @Autowired //Cuando arranca el servicio en esta clase lo crea, si lo encuentra en otro lado no lo hace
    private BusquedaService service;

    @Autowired
    private ColaVirtualService colaVirtualService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/getEspectaculos")
    public List<DtoEspectaculo> getEspectaculos(@RequestParam(required = false) @Size(max = 80) String artista) {
        List<Espectaculo> espectaculos = this.service.getEspectaculosPorArtista(safeText(artista, 80));
        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            dto.setAltaDemanda(e.isAltaDemanda());
            dto.setAperturaTaquilla(e.getAperturaTaquilla());
            return dto;
        }).toList();
        return dtos;
    }
    
    @GetMapping("/getEspectaculos/{idEscenario}")
    public List<DtoEspectaculo> getEspectaculos(@PathVariable @Positive Long idEscenario) {
        List<Espectaculo> espectaculos = this.service.getEspectaculos(idEscenario);
        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            dto.setAltaDemanda(e.isAltaDemanda());
            dto.setAperturaTaquilla(e.getAperturaTaquilla());
            return dto;
        }).toList();
        return dtos;
    }

    @GetMapping("/getEscenarios")
    public List<Escenario> getEscenarios() {
        return this.service.getEscenarios();
    }

    @GetMapping("/getEntradas")
    public List<Entrada> getEntradas(@RequestParam @Positive Long espectaculoId) {
        return this.service.getEntradas(espectaculoId);
    }

    @GetMapping("/getEntradasDisponibles")
    public List<DtoEntradaCompra> getEntradasDisponibles(
        HttpServletRequest request,
        HttpSession session,
        @RequestParam @Positive Long espectaculoId,
        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Queue-Access", required = false) String queueAccessToken,
        @org.springframework.web.bind.annotation.RequestHeader(value = "X-Queue-Client", required = false) String queueClientId
    ) {
        colaVirtualService.assertAccess(espectaculoId, queueIdentity(request, session, queueClientId), queueAccessToken);
        return this.service.getEntradasDisponibles(espectaculoId);
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

    @GetMapping("/getNumeroDeEntradas")
    public Integer getNumeroDeEntradas(@RequestParam @Positive Long espectaculoId) {
        return this.service.getNumeroDeEntradas(espectaculoId);
    }

    @GetMapping("/getNumeroDeEntradasComoDto")
    public DtoEntradas getNumeroDeEntradasComoDto(@RequestParam @Positive Long espectaculoId) {
        DtoEntradas dto = this.service.getNumeroDeEntradasComoDto(espectaculoId);
        
        return dto;
    }

    @GetMapping("/getEntradasLibres")
    public Integer getEntradasLibres(@RequestParam @Positive Long espectaculoId) {
        return this.service.getEntradasLibres(espectaculoId);
    }

    @GetMapping("/espectaculos/{id}/detalle")
    public DtoEspectaculoDetalle getEspectaculoDetalle(@PathVariable @Positive Long id) {
        Espectaculo e = this.service.getEspectaculoById(id);
        DtoEspectaculoDetalle dto = new DtoEspectaculoDetalle();
        dto.setId(e.getId());
        dto.setArtista(e.getArtista());
        dto.setFecha(e.getFecha());
        dto.setEscenario(e.getEscenario().getNombre());
        dto.setAltaDemanda(e.isAltaDemanda());
        dto.setAperturaTaquilla(e.getAperturaTaquilla());
        dto.setEntradas(this.service.getEntradas(id));
        return dto;
    }



    private String safeText(String value, int maxLength) {
        String cleaned = value == null ? "" : value.strip().replaceAll("\\p{Cntrl}", "");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
