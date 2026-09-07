package com.mohsen.reservation.inbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class IdempotencyGuard {

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyGuard(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean alreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsById(eventId)) {
            return true;
        }
        processedEventRepository.save(new ProcessedEventEntity(eventId, Instant.now()));
        return false;
    }
}
