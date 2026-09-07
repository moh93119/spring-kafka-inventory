package com.mohsen.inventory.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The inbox-side counterpart to the outbox pattern. Kafka only guarantees
 * at-least-once delivery, so every consumer that isn't naturally idempotent
 * needs this: record the event id in the same transaction as the business
 * change, and skip processing if it's already there.
 */
@Component
public class IdempotencyGuard {

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyGuard(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /** Returns true if this event was already processed (caller should skip it). */
    @Transactional
    public boolean alreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsById(eventId)) {
            return true;
        }
        processedEventRepository.save(new ProcessedEventEntity(eventId, Instant.now()));
        return false;
    }
}
