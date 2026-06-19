package com.securebank.notification.config;

import com.securebank.notification.dto.TransactionCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer wiring for the inbound {@code TransactionCompleted} events.
 *
 * <p>We deserialize JSON straight into {@link TransactionCompletedEvent} using Spring
 * Kafka's {@link JsonDeserializer}, wrapped in an {@link ErrorHandlingDeserializer} so a
 * single poison/garbled message cannot crash the listener container — it is routed to
 * the error handler instead of throwing during the poll loop.
 *
 * <p>Bootstrap servers and group id come from application.yml (different per profile so
 * the Docker profile can point at {@code kafka:29092}).
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Consumer factory producing {@code TransactionCompletedEvent} values from JSON.
     * We trust the producer's package for type mapping but, more importantly, we set a
     * default value type so we are independent of any {@code __TypeId__} header the
     * producer may or may not send.
     */
    @Bean
    public ConsumerFactory<String, TransactionCompletedEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // Start from the earliest offset on a brand-new group so we don't miss events
        // produced before this service first came up.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Keys are plain strings; values are JSON. Both deserializers are wrapped in
        // ErrorHandlingDeserializer to make bad records non-fatal.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer settings: trust all packages (internal platform topic) and
        // bind to our event record regardless of producer type headers.
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionCompletedEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * The listener container factory referenced by the {@code @KafkaListener} in
     * {@code TransactionEventConsumer}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCompletedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionCompletedEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
