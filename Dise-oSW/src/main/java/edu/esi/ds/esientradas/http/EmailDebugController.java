package edu.esi.ds.esientradas.http;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dao.EmailQueueDao;
import edu.esi.ds.esientradas.model.EmailQueue;
import edu.esi.ds.esientradas.services.EmailDeliveryService;

@RestController
@Profile("dev")
@RequestMapping("/dev/email-queue")
public class EmailDebugController {

    private final EmailQueueDao queueDao;
    private final EmailDeliveryService emailDeliveryService;

    public EmailDebugController(EmailQueueDao queueDao, EmailDeliveryService emailDeliveryService) {
        this.queueDao = queueDao;
        this.emailDeliveryService = emailDeliveryService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return queueDao.findAll().stream()
            .map(this::summary)
            .toList();
    }

    @PostMapping("/retry")
    public Map<String, Object> retry(@RequestParam Long id) {
        emailDeliveryService.processQueueById(id);
        return queueDao.findById(id)
            .map(this::summary)
            .orElseGet(() -> Map.of("message", "Email no encontrado"));
    }

    private Map<String, Object> summary(EmailQueue email) {
        return Map.of(
            "id", email.getId(),
            "to", email.getToAddress(),
            "subject", email.getSubject(),
            "status", email.getStatus(),
            "attempts", email.getAttempts(),
            "reference", email.getReference() == null ? "" : email.getReference(),
            "lastError", email.getLastError() == null ? "" : email.getLastError()
        );
    }
}
