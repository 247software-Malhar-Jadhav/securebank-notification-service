package com.securebank.notification;

import com.securebank.notification.config.I18nConfig;
import com.securebank.notification.dto.TransactionCompletedEvent;
import com.securebank.notification.service.MessageLocalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test for {@link MessageLocalizer}: it wires the real {@link I18nConfig}
 * MessageSource (no Spring context needed) and asserts each locale renders, including
 * that Hindi/Marathi actually contain Devanagari script.
 */
class MessageLocalizerTest {

    private final MessageLocalizer localizer =
            new MessageLocalizer(new I18nConfig().messageSource());

    private TransactionCompletedEvent sampleEvent() {
        return new TransactionCompletedEvent(
                "TXN-123",
                "acc-001",
                "acc-002",
                new BigDecimal("150.00"),
                "INR",
                "COMPLETED",
                OffsetDateTime.parse("2026-06-19T12:00:00Z"));
    }

    @Test
    void rendersEnglish() {
        String body = localizer.localize(sampleEvent(), "en");
        assertThat(body).contains("acc-002").contains("TXN-123").contains("INR");
    }

    @Test
    void rendersHindiDevanagari() {
        String body = localizer.localize(sampleEvent(), "hi");
        // Reference + account still present, and the text contains Devanagari code points.
        assertThat(body).contains("TXN-123");
        assertThat(body.codePoints().anyMatch(cp -> cp >= 0x0900 && cp <= 0x097F)).isTrue();
    }

    @Test
    void rendersMarathiDevanagari() {
        String body = localizer.localize(sampleEvent(), "mr");
        assertThat(body).contains("TXN-123");
        assertThat(body.codePoints().anyMatch(cp -> cp >= 0x0900 && cp <= 0x097F)).isTrue();
    }
}
