package com.securebank.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Inbound Kafka event: a transaction has completed in transaction-service.
 *
 * <p>This is a DTO, deliberately NOT a JPA entity (per platform convention: DTOs &ne;
 * entities). It mirrors the JSON that transaction-service publishes to the
 * {@code securebank.transactions} topic after it finishes its transfer saga and writes
 * its ledger rows. The shape lines up with the {@code Transaction} message in
 * {@code transaction.proto}:
 *
 * <pre>
 * {
 *   "reference":      "TXN-9f3c…",      // unique transfer reference
 *   "fromAccountId":  "acc-001",
 *   "toAccountId":    "acc-002",
 *   "amount":         150.00,
 *   "currency":       "INR",
 *   "status":         "COMPLETED",      // COMPLETED | REJECTED | FAILED
 *   "createdAt":      "2026-06-19T12:34:56Z"
 * }
 * </pre>
 *
 * <p>Modeled as a Java {@code record} for an immutable, value-style event. Jackson can
 * bind JSON straight onto the record's canonical constructor. We tolerate unknown
 * fields so that additive changes on the producer side (e.g. a new {@code description})
 * do not break this consumer.
 *
 * @param reference     the unique transaction reference (also our idempotency key)
 * @param fromAccountId the debited (source) account
 * @param toAccountId   the credited (destination) account
 * @param amount        the transfer amount as a BigDecimal (never a float — money!)
 * @param currency      ISO-4217 currency code, e.g. INR / USD
 * @param status        the final transaction status
 * @param createdAt     when the transaction completed (producer timestamp)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionCompletedEvent(

        @NotBlank
        String reference,

        @NotBlank
        String fromAccountId,

        @NotBlank
        String toAccountId,

        @NotNull
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotBlank
        String status,

        @NotNull
        OffsetDateTime createdAt
) {
}
