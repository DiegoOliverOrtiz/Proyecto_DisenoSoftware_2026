package edu.esi.ds.esientradas.services;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    @Value("${app.esiusuarios.url:http://localhost:8081}")
    private String usuariosBaseUrl;

    @Value("${app.outbound.allowed-hosts:localhost,127.0.0.1}")
    private String allowedOutboundHosts;

    @Value("${app.internal.api.secret:}")
    private String internalApiSecret;

    public String checkToken(String userToken) {
        String endpoint = validatedBaseUrl(usuariosBaseUrl) + "/external/checkToken";
        RestTemplate rest = new RestTemplate();
        try{
            HttpHeaders headers = new HttpHeaders();
            if (internalApiSecret != null && !internalApiSecret.isBlank()) {
                headers.set("X-Internal-Secret", internalApiSecret);
            }
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("token", userToken), headers);
            String username = rest.postForObject(endpoint, entity, String.class);
            if(username == null || username.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }
            return username;
        }
        catch(HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        catch(HttpClientErrorException.Forbidden e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se pudo validar el token de usuario");
        }
        catch(RestClientException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al validar el token");

        }
    }

    private String validatedBaseUrl(String value) {
        URI uri = URI.create(value == null ? "" : value.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "URL interna no valida");
        }
        if (!allowedHosts().contains(host.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Host interno no permitido");
        }
        return value.trim().replaceAll("/+$", "");
    }

    private Set<String> allowedHosts() {
        return Arrays.stream(allowedOutboundHosts.split(","))
            .map(host -> host.trim().toLowerCase(Locale.ROOT))
            .filter(host -> !host.isBlank())
            .collect(Collectors.toSet());
    }
}
