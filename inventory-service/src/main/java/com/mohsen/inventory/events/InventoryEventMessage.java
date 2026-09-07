package com.mohsen.inventory.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to the inventory-events topic - a single discriminated-union
 * style message rather than two polymorphic Kafka payload types, so the
 * consumer side needs no type-header/polymorphic deserialization setup.
 * reason is null when type is RESERVED.
 */
public record InventoryEventMessage(
        UUID eventId,
        UUID reservationId,
        String sku,
        InventoryEventType type,
        int quantity,
        String reason,
        Instant occurredAt) {
}
