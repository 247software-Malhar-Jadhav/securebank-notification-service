package com.securebank.notification.service;

import com.securebank.notification.dto.TransactionCompletedEvent;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Turns a {@link TransactionCompletedEvent} into a human, localized one-line message.
 *
 * <p>All wording lives in the i18n bundles (see {@code I18nConfig}); this class only
 * supplies the placeholder arguments and picks the {@link Locale}. Hindi (hi) and
 * Marathi (mr) bundles contain real Devanagari.
 */
@Component
public class MessageLocalizer {

    /** Message key for a completed transaction (the only event type today). */
    private static final String KEY_TXN_COMPLETED = "notification.transaction.completed";

    private final MessageSource messageSource;

    // Constructor injection only.
    public MessageLocalizer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Render the notification body for the given event and locale.
     *
     * @param event       the source transaction event
     * @param localeTag   BCP-47 language tag: "en", "hi" or "mr"
     * @return the localized message text
     */
    public String localize(TransactionCompletedEvent event, String localeTag) {
        Locale locale = Locale.forLanguageTag(localeTag);
        // Arguments map positionally to {0},{1},{2},{3} in the bundle templates.
        Object[] args = {
                event.amount(),       // {0} amount
                event.currency(),     // {1} currency
                event.toAccountId(),  // {2} destination account
                event.reference()     // {3} reference
        };
        return messageSource.getMessage(KEY_TXN_COMPLETED, args, locale);
    }
}
