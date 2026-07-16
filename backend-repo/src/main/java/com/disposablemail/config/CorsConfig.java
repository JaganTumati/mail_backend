package com.disposablemail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration.
 *
 * Allowed origins come from the ALLOWED_ORIGINS environment variable
 * (comma-separated), e.g.:
 *   ALLOWED_ORIGINS=https://your-app.netlify.app,http://localhost:5500
 *
 * Falls back to "*" for local development if unset.
 */
@Configuration
public class CorsConfig {

    @Value("${allowed.origins:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String[] origins = allowedOrigins.split(",");

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
