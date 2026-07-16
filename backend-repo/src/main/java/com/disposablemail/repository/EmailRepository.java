package com.disposablemail.repository;

import com.disposablemail.model.Email;
import com.disposablemail.model.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data-access layer for the Email entity.
 */
@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    /**
     * Returns all emails for a given inbox, sorted newest-first.
     * Used to populate the inbox list in the UI.
     */
    List<Email> findByInboxOrderByReceivedAtDesc(Inbox inbox);

    /**
     * Case-insensitive keyword search across subject and sender fields.
     * Powers the search bar on the frontend.
     */
    @Query("""
           SELECT e FROM Email e
           WHERE e.inbox = :inbox
             AND (LOWER(e.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.sender)  LIKE LOWER(CONCAT('%', :keyword, '%')))
           ORDER BY e.receivedAt DESC
           """)
    List<Email> searchByInboxAndKeyword(
            @Param("inbox")   Inbox inbox,
            @Param("keyword") String keyword);

    /**
     * Returns the count of unread emails for an inbox.
     * Shown as the unread badge in the address box.
     */
    long countByInboxAndIsReadFalse(Inbox inbox);
}
