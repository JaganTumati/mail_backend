package com.disposablemail.controller;

import com.disposablemail.model.Email;
import com.disposablemail.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for individual email operations.
 *
 * GET    /api/email/{id}   → fetch full email content (marks as read)
 * DELETE /api/email/{id}   → delete a single email
 */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // ── GET /api/email/{id} ──────────────────────────────────────────

    /**
     * Returns the full content of one email (including HTML/text body).
     * Automatically marks the email as read on first access.
     *
     * Response shape:
     * { id, sender, subject, bodyText, bodyHtml, receivedAt, isRead }
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEmail(@PathVariable Long id) {
        Optional<Email> emailOpt = emailService.getEmailById(id);

        if (emailOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Email e = emailOpt.get();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id",         e.getId());
        resp.put("sender",     e.getSender());
        resp.put("subject",    e.getSubject());
        resp.put("bodyText",   e.getBodyText());
        resp.put("bodyHtml",   e.getBodyHtml());
        resp.put("receivedAt", e.getReceivedAt().toString());
        resp.put("isRead",     e.getIsRead());

        return ResponseEntity.ok(resp);
    }

    // ── DELETE /api/email/{id} ───────────────────────────────────────

    /**
     * Permanently deletes a single email by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable Long id) {
        emailService.deleteEmail(id);
        return ResponseEntity.noContent().build();
    }
}
