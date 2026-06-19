package com.securebank.notification.delivery;

import com.securebank.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the right {@link ChannelStrategy} for a given {@link NotificationChannel}.
 *
 * <p>This is the registry half of the Strategy pattern. Spring injects every
 * {@code ChannelStrategy} bean it can find; we index them by their declared channel into
 * an {@link EnumMap}. The delivery consumer then asks for a strategy by channel and stays
 * blissfully unaware of concrete implementations.
 *
 * <p>Constructor injection only (platform convention).
 */
@Component
public class ChannelStrategyResolver {

    private final Map<NotificationChannel, ChannelStrategy> strategies =
            new EnumMap<>(NotificationChannel.class);

    /**
     * @param channelStrategies all ChannelStrategy beans on the classpath, supplied by
     *                          Spring. Today that is just {@code LogChannel}.
     */
    public ChannelStrategyResolver(List<ChannelStrategy> channelStrategies) {
        for (ChannelStrategy strategy : channelStrategies) {
            strategies.put(strategy.channel(), strategy);
        }
    }

    /**
     * @return the strategy for the requested channel
     * @throws IllegalStateException if no strategy is registered for it (e.g. someone
     *         routed an EMAIL notification before the EmailChannel bean exists)
     */
    public ChannelStrategy resolve(NotificationChannel channel) {
        ChannelStrategy strategy = strategies.get(channel);
        if (strategy == null) {
            throw new IllegalStateException(
                    "No ChannelStrategy registered for channel " + channel
                            + " — implement and register a ChannelStrategy bean.");
        }
        return strategy;
    }
}
