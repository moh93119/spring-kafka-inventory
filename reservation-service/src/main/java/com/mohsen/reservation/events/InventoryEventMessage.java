package com.mohsen.reservation.events;

import java.time.Instant;
import java.util.UUID;

/** Consumed from inventory-events, published by inventory-service's outbox. */
public record InventoryEventMessage(
        UUID eventId,
        UUID reservationId,
        String sku,
        InventoryEventType type,
        int quantity,
        String reason,
        Instant occurredAt) {
}
