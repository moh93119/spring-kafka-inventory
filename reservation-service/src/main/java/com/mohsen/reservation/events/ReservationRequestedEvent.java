package com.mohsen.reservation.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to reservation-requests. Deliberately duplicated from
 * inventory-service's copy of the same shape rather than shared via a
 * common library - each service owns its view of the contract, same
 * reasoning as not sharing DTOs across a .NET service boundary.
 */
public record ReservationRequestedEvent(
        UUID eventId,
        UUID reservationId,
        String sku,
        int quantity,
        Instant occurredAt) {
}
