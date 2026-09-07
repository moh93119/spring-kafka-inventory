package com.mohsen.reservation.messaging;

import com.mohsen.reservation.events.InventoryEventMessage;
import com.mohsen.reservation.service.ReservationResolutionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final ReservationResolutionService reservationResolutionService;

    public InventoryEventListener(ReservationResolutionService reservationResolutionService) {
        this.reservationResolutionService = reservationResolutionService;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 500, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "inventory-events", groupId = "reservation-service")
    public void onInventoryEvent(InventoryEventMessage event) {
        reservationResolutionService.handle(event);
    }

    @DltHandler
    public void onDeadLetter(InventoryEventMessage event, ConsumerRecord<String, String> record) {
        log.error("Inventory event {} for reservation {} moved to the dead-letter topic after exhausting retries "
                        + "(partition={}, offset={})",
                event.eventId(), event.reservationId(), record.partition(), record.offset());
    }
}
