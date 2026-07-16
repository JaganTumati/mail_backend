-- ============================================================
--  Disposable Mail  –  MySQL Schema
--  Run once before starting the Spring Boot application.
-- ============================================================

CREATE DATABASE IF NOT EXISTS disposable_mail
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE disposable_mail;

-- ── Inbox ──────────────────────────────────────────────────────────
-- Stores every generated temporary email address.
CREATE TABLE IF NOT EXISTS inbox (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    address     VARCHAR(255)    NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME,
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,

    PRIMARY KEY (id),
    UNIQUE KEY uq_inbox_address (address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Email ──────────────────────────────────────────────────────────
-- Stores every received email, linked to its inbox.
CREATE TABLE IF NOT EXISTS email (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    inbox_id    BIGINT          NOT NULL,
    sender      VARCHAR(255),
    subject     VARCHAR(500),
    body_text   LONGTEXT,
    body_html   LONGTEXT,
    received_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read     TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT fk_email_inbox
        FOREIGN KEY (inbox_id) REFERENCES inbox (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Indexes ────────────────────────────────────────────────────────
CREATE INDEX idx_inbox_address    ON inbox (address);
CREATE INDEX idx_inbox_active     ON inbox (is_active);
CREATE INDEX idx_inbox_expires    ON inbox (expires_at);

CREATE INDEX idx_email_inbox_id   ON email (inbox_id);
CREATE INDEX idx_email_received   ON email (received_at);
CREATE INDEX idx_email_is_read    ON email (is_read);
