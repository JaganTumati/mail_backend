package com.disposablemail.service;

import com.disposablemail.model.Email;
import com.disposablemail.model.Inbox;
import com.disposablemail.repository.EmailRepository;
import com.disposablemail.repository.InboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles all business logic for email storage, retrieval,
 * search, read-marking, and deletion.
 */
@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final InboxRepository inboxRepository;

    public EmailService(EmailRepository emailRepository,
                        InboxRepository inboxRepository) {
        this.emailRepository = emailRepository;
        this.inboxRepository = inboxRepository;
    }

    /**
     * Called by the SMTP listener when an email arrives.
     * Finds the matching active inbox and persists the email.
     *
     * @param toAddress  The recipient address (must match an active inbox)
     * @param from       Sender's address from the SMTP envelope
     * @param subject    Email subject header
     * @param bodyText   Plain-text MIME part (may be null)
     * @param bodyHtml   HTML MIME part (may be null)
     * @return           The saved Email entity
     */
    @Transactional
    public Email saveEmail(String toAddress, String from,
                           String subject, String bodyText, String bodyHtml) {

        Inbox inbox = inboxRepository
                .findByAddressAndIsActiveTrue(toAddress)
                .orElseThrow(() -> new RuntimeException(
                        "No active inbox found for: " + toAddress));

        Email email = new Email();
        email.setInbox(inbox);
        email.setSender(from);
        email.setSubject(subject);
        email.setBodyText(bodyText);
        email.setBodyHtml(bodyHtml);

        return emailRepository.save(email);
    }

    /**
     * Returns all emails for the given inbox, ordered newest-first.
     */
    @Transactional(readOnly = true)
    public List<Email> getEmailsForInbox(Inbox inbox) {
        return emailRepository.findByInboxOrderByReceivedAtDesc(inbox);
    }

    /**
     * Fetches a single email by ID and marks it as read on first open.
     */
    @Transactional
    public Optional<Email> getEmailById(Long emailId) {
        Optional<Email> emailOpt = emailRepository.findById(emailId);
        emailOpt.ifPresent(email -> {
            if (!email.getIsRead()) {
                email.setIsRead(true);
                emailRepository.save(email);
            }
        });
        return emailOpt;
    }

    /**
     * Case-insensitive keyword search on subject and sender within an inbox.
     */
    @Transactional(readOnly = true)
    public List<Email> searchEmails(Inbox inbox, String keyword) {
        return emailRepository.searchByInboxAndKeyword(inbox, keyword);
    }

    /**
     * Permanently deletes a single email by ID.
     */
    @Transactional
    public void deleteEmail(Long emailId) {
        emailRepository.deleteById(emailId);
    }

    /**
     * Returns the count of unread emails for an inbox.
     * Used to drive the unread badge on the frontend.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Inbox inbox) {
        return emailRepository.countByInboxAndIsReadFalse(inbox);
    }
}
