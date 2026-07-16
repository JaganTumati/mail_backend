package com.disposablemail.smtp;

import com.disposablemail.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Embedded SMTP server that accepts incoming email on the configured port.
 *
 * How it works:
 *   1. Postfix receives a real email sent to *@tempmail.local.
 *   2. Postfix transport map routes it to 127.0.0.1:25.
 *   3. This listener accepts the connection, parses the raw MIME message,
 *      and hands it off to EmailService for storage.
 *
 * Started via @PostConstruct and cleanly stopped via @PreDestroy.
 */
@Component
public class SmtpListener implements SimpleMessageListener {

    private final EmailService emailService;
    private SMTPServer smtpServer;

    @Value("${smtp.port}")
    private int smtpPort;

    @Value("${mail.domain}")
    private String mailDomain;

    public SmtpListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    @Value("${smtp.enabled:true}")
    private boolean smtpEnabled;

    @PostConstruct
    public void start() {
        // Cloud PaaS platforms (Railway, Render, Heroku, etc.) block inbound
        // port 25 and won't let you bind privileged ports without root, so the
        // embedded SMTP listener must be disabled there via smtp.enabled=false.
        // Set SMTP_ENABLED=false in Railway's environment variables.
        if (!smtpEnabled) {
            System.out.println("[SMTP] Embedded SMTP server disabled (smtp.enabled=false). " +
                    "Use the /api/inbound-email webhook instead if running on a PaaS.");
            return;
        }
        smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(this));
        smtpServer.setPort(smtpPort);
        smtpServer.setHostName("localhost");
        smtpServer.start();
        System.out.println("[SMTP] Embedded SMTP server started on port " + smtpPort);
    }

    @PreDestroy
    public void stop() {
        if (smtpServer != null) {
            smtpServer.stop();
            System.out.println("[SMTP] Embedded SMTP server stopped.");
        }
    }

    // ── SimpleMessageListener ────────────────────────────────────────

    /**
     * Accept only emails addressed to our own domain.
     * Rejects anything else at the SMTP protocol level.
     */
    @Override
    public boolean accept(String from, String recipient) {
        boolean accepted = recipient.toLowerCase().endsWith("@" + mailDomain);
        System.out.printf("[SMTP] %s → %s : %s%n",
                from, recipient, accepted ? "ACCEPTED" : "REJECTED");
        return accepted;
    }

    /**
     * Called once per accepted email with the raw RFC-2822 data stream.
     * Parses the MIME message and delegates persistence to EmailService.
     */
    @Override
    public void deliver(String from, String recipient, InputStream data)
            throws IOException {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, data);

            String subject  = message.getSubject();
            String bodyText = extractPart(message, "text/plain");
            String bodyHtml = extractPart(message, "text/html");

            emailService.saveEmail(
                    recipient.toLowerCase(), from, subject, bodyText, bodyHtml);

            System.out.printf("[SMTP] Delivered email to %s from %s (subject: %s)%n",
                    recipient, from, subject);

        } catch (Exception e) {
            System.err.println("[SMTP] Error processing email: " + e.getMessage());
            throw new IOException("Email processing failed", e);
        }
    }

    // ── MIME Helpers ─────────────────────────────────────────────────

    /**
     * Recursively walks a MIME tree to extract the first part
     * matching the given MIME type (e.g. "text/plain", "text/html").
     */
    private String extractPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String result = extractPart(mp.getBodyPart(i), mimeType);
                if (result != null) return result;
            }
        }
        return null;
    }
}
