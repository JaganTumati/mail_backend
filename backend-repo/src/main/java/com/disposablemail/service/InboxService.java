package com.disposablemail.service;

import com.disposablemail.model.Inbox;
import com.disposablemail.repository.InboxRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles all business logic related to temporary inbox management:
 * generation, lookup, deactivation, and deletion.
 */
@Service
public class InboxService {

    private final InboxRepository inboxRepository;

    @Value("${mail.domain}")
    private String mailDomain;

    @Value("${mail.cleanup.hours}")
    private int cleanupHours;

    public InboxService(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    /**
     * Generates a unique random 10-character alphanumeric local-part,
     * appends the configured domain, persists the inbox, and returns it.
     * Retries automatically if there is a collision (extremely rare).
     */
    @Transactional
    public Inbox generateInbox() {
        String localPart;
        String address;

        // Loop guarantees uniqueness even under concurrent access
        do {
            localPart = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
            address   = localPart + "@" + mailDomain;
        } while (inboxRepository.existsByAddress(address));

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(cleanupHours);
        Inbox inbox = new Inbox(address, expiresAt);
        return inboxRepository.save(inbox);
    }

    /**
     * Returns the active inbox matching the given address, if it exists.
     * Used by SMTP delivery and the REST email-list endpoint.
     */
    @Transactional(readOnly = true)
    public Optional<Inbox> findByAddress(String address) {
        return inboxRepository.findByAddressAndIsActiveTrue(address);
    }

    /**
     * Soft-deletes an inbox by setting isActive = false.
     * Emails remain in the database until the cleanup service runs.
     */
    @Transactional
    public void deactivateInbox(Long inboxId) {
        inboxRepository.findById(inboxId).ifPresent(inbox -> {
            inbox.setIsActive(false);
            inboxRepository.save(inbox);
        });
    }

    /**
     * Hard-deletes an inbox and all its emails (via CASCADE).
     */
    @Transactional
    public void deleteInbox(Long inboxId) {
        inboxRepository.deleteById(inboxId);
    }
}
