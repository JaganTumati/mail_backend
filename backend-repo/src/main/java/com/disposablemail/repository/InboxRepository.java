package com.disposablemail.repository;

import com.disposablemail.model.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for the Inbox entity.
 * Spring Data JPA generates all SQL implementations at runtime.
 */
@Repository
public interface InboxRepository extends JpaRepository<Inbox, Long> {

    /**
     * Finds an active inbox by its full email address.
     * Used by the SMTP listener and the email-list API endpoint.
     */
    Optional<Inbox> findByAddressAndIsActiveTrue(String address);

    /**
     * Finds all active inboxes whose expiry time is in the past.
     * Called by CleanupService every 30 minutes.
     */
    List<Inbox> findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime now);

    /**
     * Quick existence check used during address generation
     * to guarantee uniqueness without fetching the full entity.
     */
    boolean existsByAddress(String address);
}
