package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.esi.ds.esientradas.events.TicketSendEvent;
import edu.esi.ds.esientradas.events.EmailQueueCreatedEvent;

@Component
public class TicketEventListener {

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketSend(TicketSendEvent event) {
        // Try immediate send, if it fails the service will keep it queued
        this.emailDeliveryService.enqueueAndTrySend(event.getTo(), event.getSubject(), event.getHtml(), event.getReference());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailQueueCreated(EmailQueueCreatedEvent event) {
        this.emailDeliveryService.processQueueById(event.getQueueId());
    }
}
