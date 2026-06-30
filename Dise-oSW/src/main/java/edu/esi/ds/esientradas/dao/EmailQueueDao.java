package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.EmailQueue;
import edu.esi.ds.esientradas.model.EmailStatus;

public interface EmailQueueDao extends JpaRepository<EmailQueue, Long> {
    List<EmailQueue> findByStatusAndAttemptsLessThan(EmailStatus status, int attempts);
}
