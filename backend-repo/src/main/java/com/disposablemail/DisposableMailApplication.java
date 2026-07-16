package com.disposablemail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Disposable Mail application.
 *
 * @EnableScheduling activates the @Scheduled cleanup task
 * that removes expired inboxes every 30 minutes.
 */
@SpringBootApplication
@EnableScheduling
public class DisposableMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisposableMailApplication.class, args);
    }
}
