package com.mohsen.reservation.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class KafkaOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxDispatcher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxDispatcher(OutboxMessageRepository outboxMessageRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void dispatchPending() {
        List<OutboxMessageEntity> pending = outboxMessageRepository.findTop50ByDispatchedAtIsNullOrderByOccurredAtAsc();
        for (OutboxMessageEntity message : pending) {
            try {
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload()).get();
                message.setDispatchedAt(Instant.now());
            } catch (Exception e) {
                log.warn("Failed to dispatch outbox message {} to {}, will retry next tick",
                        message.getId(), message.getTopic(), e);
            }
        }
        outboxMessageRepository.saveAll(pending);
    }
}
