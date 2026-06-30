package edu.esi.ds.esientradas.events;

public class EmailQueueCreatedEvent {
    private final Long queueId;

    public EmailQueueCreatedEvent(Long queueId) {
        this.queueId = queueId;
    }

    public Long getQueueId() {
        return queueId;
    }
}
