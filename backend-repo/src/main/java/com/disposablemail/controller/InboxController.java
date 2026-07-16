package com.disposablemail.controller;

import com.disposablemail.model.Email;
import com.disposablemail.model.Inbox;
import com.disposablemail.service.EmailService;
import com.disposablemail.service.InboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for inbox lifecycle and email listing.
 *
 * POST   /api/inbox/generate              → generate a new address
 * GET    /api/inbox/{address}/emails      → list emails (with optional ?search=)
 * DELETE /api/inbox/{id}                  → permanently delete inbox
 */
@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final InboxService inboxService;
    private final EmailService emailService;

    public InboxController(InboxService inboxService, EmailService emailService) {
        this.inboxService = inboxService;
        this.emailService = emailService;
    }

    // ── POST /api/inbox/generate ─────────────────────────────────────

    /**
     * Creates a fresh temporary inbox and returns its details.
     * Response contains the address, ID, and expiry timestamp.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateInbox() {
        Inbox inbox = inboxService.generateInbox();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id",        inbox.getId());
        resp.put("address",   inbox.getAddress());
        resp.put("createdAt", inbox.getCreatedAt().toString());
        resp.put("expiresAt", inbox.getExpiresAt().toString());

        return ResponseEntity.ok(resp);
    }

    // ── GET /api/inbox/{address}/emails ──────────────────────────────

    /**
     * Returns the email list for a given address.
     * Accepts an optional ?search= query parameter for filtering.
     *
     * Response shape:
     * {
     *   address, expiresAt, unread,
     *   emails: [ { id, sender, subject, receivedAt, isRead } ]
     * }
     */
    @GetMapping("/{address}/emails")
    public ResponseEntity<?> getEmails(
            @PathVariable String address,
            @RequestParam(required = false) String search) {

        Optional<Inbox> inboxOpt = inboxService.findByAddress(address);
        if (inboxOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Inbox inbox = inboxOpt.get();

        List<Email> emails = (search != null && !search.isBlank())
                ? emailService.searchEmails(inbox, search)
                : emailService.getEmailsForInbox(inbox);

        long unread = emailService.getUnreadCount(inbox);

        // Return lightweight summary objects — full body is fetched on demand
        List<Map<String, Object>> emailSummaries = emails.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",         e.getId());
            m.put("sender",     e.getSender());
            m.put("subject",    e.getSubject());
            m.put("receivedAt", e.getReceivedAt().toString());
            m.put("isRead",     e.getIsRead());
            return m;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("address",   inbox.getAddress());
        resp.put("expiresAt", inbox.getExpiresAt().toString());
        resp.put("unread",    unread);
        resp.put("emails",    emailSummaries);

        return ResponseEntity.ok(resp);
    }

    // ── DELETE /api/inbox/{id} ───────────────────────────────────────

    /**
     * Permanently deletes the inbox and all its emails (CASCADE).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInbox(@PathVariable Long id) {
        inboxService.deleteInbox(id);
        return ResponseEntity.noContent().build();
    }
}
