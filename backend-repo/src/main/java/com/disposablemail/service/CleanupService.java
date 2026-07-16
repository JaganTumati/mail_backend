package com.disposablemail.service;

import com.disposablemail.model.Inbox;
import com.disposablemail.repository.InboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that automatically removes expired inboxes
 * and their associated emails every 30 minutes.
 *
 * Requires @EnableScheduling on the main application class.
 * ON DELETE CASCADE on the email table handles email removal.
 */
@Service
public class CleanupService {

    private final InboxRepository inboxRepository;

    public CleanupService(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    /**
     * Runs every 30 minutes (1_800_000 ms).
     * Finds all active inboxes whose expiresAt is in the past
     * and permanently deletes them — emails are cascade-deleted.
     */
    @Scheduled(fixedDelay = 1_800_000)
    @Transactional
    public void cleanExpiredInboxes() {
        List<Inbox> expired = inboxRepository
                .findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime.now());

        if (expired.isEmpty()) {
            System.out.println("[Cleanup] No expired inboxes found.");
            return;
        }

        for (Inbox inbox : expired) {
            System.out.printf("[Cleanup] Deleting expired inbox: %s (expired at %s)%n",
                    inbox.getAddress(), inbox.getExpiresAt());
            inboxRepository.delete(inbox);
        }

        System.out.printf("[Cleanup] Done — removed %d expired inbox(es).%n", expired.size());
    }
}
