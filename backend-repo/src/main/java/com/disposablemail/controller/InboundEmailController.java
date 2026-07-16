package com.disposablemail.controller;

import com.disposablemail.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoint for receiving inbound email from a hosted email-relay
 * provider (Mailgun Routes, SendGrid Inbound Parse, ImprovMX, etc.).
 *
 * Use this INSTEAD of the embedded SMTP listener when deploying to a PaaS
 * like Railway, which blocks inbound port 25.
 *
 * Setup (example with Mailgun):
 *   1. Add and verify your domain in Mailgun.
 *   2. Create a Route: match recipient "@yourdomain.com",
 *      forward to: https://your-app.up.railway.app/api/inbound-email
 *   3. Point your domain's MX records to Mailgun (shown in their dashboard).
 *   4. Set SMTP_ENABLED=false in Railway's env vars (no embedded SMTP needed).
 *
 * Mailgun POSTs as multipart/form-data with fields: recipient, sender,
 * subject, body-plain, body-html. Adjust field names below if you use a
 * different provider (SendGrid uses "to", "from", "subject", "text", "html").
 */
@RestController
@RequestMapping("/api/inbound-email")
public class InboundEmailController {

    private final EmailService emailService;

    public InboundEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<String> receiveEmail(
            @RequestParam(name = "recipient", required = false) String recipient,
            @RequestParam(name = "sender", required = false) String sender,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "body-plain", required = false) String bodyText,
            @RequestParam(name = "body-html", required = false) String bodyHtml) {

        if (recipient == null || sender == null) {
            return ResponseEntity.badRequest().body("Missing recipient or sender");
        }

        try {
            emailService.saveEmail(recipient, sender, subject, bodyText, bodyHtml);
            return ResponseEntity.ok("received");
        } catch (RuntimeException e) {
            // No active inbox found for this address — not an error from the
            // relay provider's perspective, so return 200 to avoid retries.
            return ResponseEntity.ok("ignored: " + e.getMessage());
        }
    }
}
