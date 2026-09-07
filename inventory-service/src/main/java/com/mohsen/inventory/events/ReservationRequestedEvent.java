package com.mohsen.inventory.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumed from the reservation-requests topic, published by
 * reservation-service's own outbox. eventId is the idempotency key.
 */
public record ReservationRequestedEvent(
        UUID eventId,
        UUID reservationId,
        String sku,
        int quantity,
        Instant occurredAt) {
}
