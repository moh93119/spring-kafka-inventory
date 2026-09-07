package com.mohsen.inventory.service;

import com.mohsen.inventory.domain.InsufficientStockException;
import com.mohsen.inventory.domain.Stock;
import com.mohsen.inventory.domain.StockNotFoundException;
import com.mohsen.inventory.domain.StockRepository;
import com.mohsen.inventory.events.InventoryEventMessage;
import com.mohsen.inventory.events.InventoryEventType;
import com.mohsen.inventory.events.ReservationRequestedEvent;
import com.mohsen.inventory.inbox.IdempotencyGuard;
import com.mohsen.inventory.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);
    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    private final StockRepository stockRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final OutboxWriter outboxWriter;

    public StockReservationService(StockRepository stockRepository, IdempotencyGuard idempotencyGuard, OutboxWriter outboxWriter) {
        this.stockRepository = stockRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public void handle(ReservationRequestedEvent event) {
        if (idempotencyGuard.alreadyProcessed(event.eventId())) {
            log.info("Skipping already-processed reservation request {}", event.eventId());
            return;
        }

        // Unknown SKU / insufficient stock are business outcomes, not
        // transient failures - they resolve to a REJECTED event rather
        // than propagating and triggering @RetryableTopic's retry/DLT path.
        String reason = null;
        try {
            Stock stock = stockRepository.findById(event.sku())
                    .orElseThrow(() -> new StockNotFoundException(event.sku()));
            stock.reserve(event.quantity());
            stockRepository.save(stock);
        } catch (StockNotFoundException | InsufficientStockException e) {
            reason = e.getMessage();
        }

        InventoryEventMessage result = new InventoryEventMessage(
                UUID.randomUUID(),
                event.reservationId(),
                event.sku(),
                reason == null ? InventoryEventType.RESERVED : InventoryEventType.REJECTED,
                event.quantity(),
                reason,
                Instant.now());

        outboxWriter.append(INVENTORY_EVENTS_TOPIC, event.reservationId().toString(), result);
    }
}
