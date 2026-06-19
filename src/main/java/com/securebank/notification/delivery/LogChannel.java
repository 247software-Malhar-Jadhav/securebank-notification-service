package com.securebank.notification.delivery;

import com.securebank.notification.domain.NotificationChannel;
import com.securebank.notification.messaging.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The only concrete {@link ChannelStrategy} implemented today: it "delivers" a
 * notification by writing it to the application log.
 *
 * <p>This is the seam where real delivery integrations plug in. To add a channel:
 * <ol>
 *   <li>create e.g. {@code EmailChannel implements ChannelStrategy} returning
 *       {@link NotificationChannel#EMAIL},</li>
 *   <li>call the SMTP / provider SDK inside {@link #deliver},</li>
 *   <li>annotate it {@code @Component} — the resolver picks it up automatically.</li>
 * </ol>
 * No other class needs to change. (See docs/notification-service.md → "Design patterns".)
 */
@Slf4j
@Component
public class LogChannel implements ChannelStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void deliver(NotificationMessage message) {
        // In a real EMAIL/SMS/PUSH channel this is where the provider call would go.
        log.info("[DELIVERY:LOG] notificationId={} account={} locale={} body=\"{}\"",
                message.notificationId(),
                message.accountRef(),
                message.locale(),
                message.body());
    }
}
