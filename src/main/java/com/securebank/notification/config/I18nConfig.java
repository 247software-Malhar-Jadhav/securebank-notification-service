package com.securebank.notification.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Localization wiring. Notification bodies are built from message bundles so we can ship
 * English, Hindi and Marathi (real Devanagari) without touching code.
 *
 * <p>Bundles live under {@code src/main/resources/i18n/messages*.properties}:
 * <ul>
 *   <li>{@code messages.properties}    — English (default fallback)</li>
 *   <li>{@code messages_hi.properties} — Hindi (हिन्दी)</li>
 *   <li>{@code messages_mr.properties} — Marathi (मराठी)</li>
 * </ul>
 *
 * <p>The {@code .properties} files are stored UTF-8 and we tell the {@code MessageSource}
 * to read them as UTF-8, so Devanagari survives intact.
 */
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        // Critical for Devanagari: read property files as UTF-8, not ISO-8859-1.
        source.setDefaultEncoding("UTF-8");
        // If a key is missing in a locale, fall back to the default bundle (English)
        // rather than throwing.
        source.setUseCodeAsDefaultMessage(false);
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
