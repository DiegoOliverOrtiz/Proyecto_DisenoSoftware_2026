package edu.esi.ds.esientradas.services;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import edu.esi.ds.esientradas.dao.EmailQueueDao;
import edu.esi.ds.esientradas.model.EmailQueue;
import edu.esi.ds.esientradas.model.EmailStatus;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    @Value("${email.external-url:http://localhost:8081/external/sendTicket}")
    private String externalUrl;

    @Value("${app.outbound.allowed-hosts:localhost,127.0.0.1}")
    private String allowedOutboundHosts;

    @Value("${email.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.internal.api.secret:}")
    private String internalApiSecret;

    @Autowired
    private EmailQueueDao queueDao;

    private final RestTemplate rest = new RestTemplate();

    @Transactional
    public EmailQueue enqueue(String to, String subject, String html, String reference) {
        EmailQueue q = new EmailQueue();
        q.setToAddress(to);
        q.setSubject(subject);
        q.setBodyHtml(html);
        q.setReference(reference);
        q.setStatus(EmailStatus.PENDING);
        q.setAttempts(0);
        q = this.queueDao.save(q);
        return q;
    }

    @Transactional
    public void enqueueAndTrySend(String to, String subject, String html, String reference) {
        EmailQueue q = this.enqueue(to, subject, html, reference);
        try {
            this.trySendNow(q);
        } catch (Exception e) {
            log.warn("Initial send failed, queued for retry: {}", this.safeError(e.getMessage()));
        }
    }

    @Transactional
    public void trySendNow(EmailQueue q) {
        try {
            var body = java.util.Map.of("to", q.getToAddress(), "subject", q.getSubject(), "html", q.getBodyHtml());
            HttpHeaders headers = new HttpHeaders();
            if (internalApiSecret != null && !internalApiSecret.isBlank()) {
                headers.set("X-Internal-Secret", internalApiSecret);
            }
            HttpEntity<java.util.Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = rest.postForEntity(this.validatedUrl(this.externalUrl), request, String.class);
            q.setLastAttempt(System.currentTimeMillis());
            q.setAttempts(q.getAttempts() + 1);
            if (resp.getStatusCode().is2xxSuccessful()) {
                q.setStatus(EmailStatus.SENT);
                q.setLastError(null);
            } else {
                q.setStatus(EmailStatus.PENDING);
                q.setLastError("Respuesta no exitosa de esiusuarios: HTTP " + resp.getStatusCode().value());
            }
            this.queueDao.save(q);
        } catch (HttpStatusCodeException ex) {
            q.setLastAttempt(System.currentTimeMillis());
            q.setAttempts(q.getAttempts() + 1);
            if (q.getAttempts() >= this.maxAttempts) {
                q.setStatus(EmailStatus.FAILED);
            } else {
                q.setStatus(EmailStatus.PENDING);
            }
            q.setLastError(this.safeError("HTTP " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString()));
            this.queueDao.save(q);
            throw ex;
        } catch (RestClientException ex) {
            q.setLastAttempt(System.currentTimeMillis());
            q.setAttempts(q.getAttempts() + 1);
            if (q.getAttempts() >= this.maxAttempts) {
                q.setStatus(EmailStatus.FAILED);
            } else {
                q.setStatus(EmailStatus.PENDING);
            }
            q.setLastError(this.safeError(ex.getMessage()));
            this.queueDao.save(q);
            throw ex;
        }
    }

    @Transactional
    public void processQueueById(Long id) {
        EmailQueue q = this.queueDao.findById(id).orElse(null);
        if (q == null) return;
        if (q.getStatus() == EmailStatus.SENT) return;
        try {
            this.trySendNow(q);
        } catch (Exception e) {
            log.warn("Processing queued email {} failed: {}", id, this.safeError(e.getMessage()));
        }
    }

    @Scheduled(fixedDelayString = "${email.retry.ms:60000}")
    public void retryPending() {
        try {
            List<EmailQueue> pending = this.queueDao.findByStatusAndAttemptsLessThan(EmailStatus.PENDING, this.maxAttempts);
            for (EmailQueue q : pending) {
                try {
                    this.trySendNow(q);
                } catch (Exception e) {
                    log.warn("Retry failed for email id {} attempts {}: {}", q.getId(), q.getAttempts(), this.safeError(e.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("Error while retrying pending emails: {}", this.safeError(e.getMessage()));
        }
    }

    private String safeError(String detail) {
        if (detail == null || detail.isBlank()) {
            return "Sin detalle";
        }
        String cleaned = detail.replaceAll("xkeysib-[A-Za-z0-9_-]+", "xkeysib-***");
        return cleaned.length() <= 1000 ? cleaned : cleaned.substring(0, 1000);
    }

    private String validatedUrl(String value) {
        URI uri = URI.create(value == null ? "" : value.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "URL externa no valida");
        }
        if (!allowedHosts().contains(host.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Host externo no permitido");
        }
        return value.trim();
    }

    private Set<String> allowedHosts() {
        return Arrays.stream(allowedOutboundHosts.split(","))
            .map(host -> host.trim().toLowerCase(Locale.ROOT))
            .filter(host -> !host.isBlank())
            .collect(Collectors.toSet());
    }
}
