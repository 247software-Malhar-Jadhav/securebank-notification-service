package com.securebank.notification.domain;

/**
 * The delivery channel a notification is destined for.
 *
 * <p>Today only {@link #LOG} is implemented (see {@code LogChannel}). EMAIL / SMS / PUSH
 * are declared here as the future channels the Strategy pattern is designed to host —
 * adding one means writing a new {@code ChannelStrategy} bean, with no change to the
 * consumer or producer code.
 */
public enum NotificationChannel {
    /** Implemented: writes the notification to the application log. */
    LOG,
    /** Future: SMTP / transactional email provider (e.g. SES, SendGrid). */
    EMAIL,
    /** Future: SMS gateway (e.g. Twilio, MSG91). */
    SMS,
    /** Future: mobile push (e.g. FCM / APNs). */
    PUSH
}
