package com.mohsen.inventory.messaging;

import com.mohsen.inventory.events.ReservationRequestedEvent;
import com.mohsen.inventory.service.StockReservationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
public class ReservationRequestListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationRequestListener.class);

    private final StockReservationService stockReservationService;

    public ReservationRequestListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 500, multiplier = 2),
            dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "reservation-requests", groupId = "inventory-service")
    public void onReservationRequested(ReservationRequestedEvent event) {
        stockReservationService.handle(event);
    }

    @DltHandler
    public void onDeadLetter(ReservationRequestedEvent event, ConsumerRecord<String, String> record) {
        // A poisoned message ends up here after every retry has been
        // exhausted - logged loudly rather than silently dropped, since
        // nothing else will ever look at it again.
        log.error("Reservation request {} for SKU {} moved to the dead-letter topic after exhausting retries "
                        + "(partition={}, offset={})",
                event.reservationId(), event.sku(), record.partition(), record.offset());
    }
}
