package com.securebank.notification.delivery;

import com.securebank.notification.domain.NotificationChannel;
import com.securebank.notification.messaging.NotificationMessage;

/**
 * Strategy interface for "how a notification is actually delivered".
 *
 * <p>This is the <b>Strategy pattern</b>: the delivery consumer holds a family of
 * interchangeable algorithms (one per channel) and picks one at runtime by channel type,
 * without knowing the concrete class. Adding Email/SMS/Push later means adding a new
 * {@code @Component} implementing this interface — the consumer code does not change.
 *
 * <p>Implementations register themselves with the {@link ChannelStrategyResolver} simply
 * by being Spring beans; the resolver indexes them by {@link #channel()}.
 */
public interface ChannelStrategy {

    /** Which channel this strategy handles. */
    NotificationChannel channel();

    /**
     * Deliver the message through this channel.
     *
     * @param message the localized notification payload
     * @throws Exception if delivery fails (the caller marks the row FAILED)
     */
    void deliver(NotificationMessage message) throws Exception;
}
