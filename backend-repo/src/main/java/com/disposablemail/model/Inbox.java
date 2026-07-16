package com.disposablemail.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generated temporary email address (inbox).
 * One inbox can hold many received emails.
 */
@Entity
@Table(name = "inbox")
@Data
@NoArgsConstructor
public class Inbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The full email address, e.g. xk7r2p9q@tempmail.local */
    @Column(nullable = false, unique = true)
    private String address;

    /** Timestamp when this inbox was created. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Timestamp after which the cleanup service will delete this inbox. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** False once the inbox is deactivated or deleted. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * All emails received by this inbox.
     * CascadeType.ALL + orphanRemoval ensures emails are
     * deleted automatically when the inbox is deleted.
     */
    @OneToMany(mappedBy = "inbox",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Email> emails = new ArrayList<>();

    /**
     * Convenience constructor used by InboxService.
     */
    public Inbox(String address, LocalDateTime expiresAt) {
        this.address   = address;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.isActive  = true;
    }
}
