package com.disposablemail.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single received email stored against an Inbox.
 * Supports both plain-text and HTML body content.
 */
@Entity
@Table(name = "email")
@Data
@NoArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The inbox this email belongs to.
     * LAZY fetch avoids loading the full inbox on every email query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbox_id", nullable = false)
    private Inbox inbox;

    /** Sender's email address extracted from the SMTP envelope. */
    @Column(name = "sender")
    private String sender;

    /** Subject header of the email. */
    @Column(name = "subject", length = 500)
    private String subject;

    /** Plain-text body part (text/plain MIME part). */
    @Lob
    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;

    /** HTML body part (text/html MIME part). Rendered in an iframe. */
    @Lob
    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;

    /** Server-side timestamp when the email was received. */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    /** Tracks whether the user has opened this email in the viewer. */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
}
