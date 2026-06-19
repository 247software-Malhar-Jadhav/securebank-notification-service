package com.securebank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for the SecureBank notification microservice.
 *
 * <p>This service is intentionally small in surface area: it has no public business
 * REST API and speaks no gRPC. It only:
 * <ol>
 *   <li>consumes {@code TransactionCompleted} events from the Kafka topic
 *       {@code securebank.transactions},</li>
 *   <li>builds a localized message and writes a notification-log row, and</li>
 *   <li>fans the notification out over RabbitMQ for "delivery".</li>
 * </ol>
 *
 * <p>Virtual threads are enabled via {@code spring.threads.virtual.enabled=true} in
 * application.yml, so the servlet container and (where applicable) listener work runs
 * on JDK 21 virtual threads.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
