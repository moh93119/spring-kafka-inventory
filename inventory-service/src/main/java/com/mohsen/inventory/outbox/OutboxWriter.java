package com.mohsen.inventory.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Appends an outbox row in whatever transaction is already open - the
 * caller's @Transactional service method. A separate poller
 * (KafkaOutboxDispatcher) is the only thing that ever talks to Kafka
 * directly, so a crash between "business change committed" and "event
 * published" just means the poller catches up on the next tick instead of
 * the event being lost.
 */
@Component
public class OutboxWriter {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String topic, String messageKey, Object event) {
        outboxMessageRepository.save(new OutboxMessageEntity(
                topic, messageKey, event.getClass().getSimpleName(), serialize(event), Instant.now()));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event " + event.getClass(), e);
        }
    }
}
