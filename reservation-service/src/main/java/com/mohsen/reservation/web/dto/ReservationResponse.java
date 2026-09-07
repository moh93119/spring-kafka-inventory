package com.mohsen.reservation.web.dto;

import com.mohsen.reservation.domain.Reservation;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String sku,
        int quantity,
        String status,
        String rejectionReason,
        Instant requestedAt,
        Instant resolvedAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSku(),
                reservation.getQuantity(),
                reservation.getStatus().name(),
                reservation.getRejectionReason(),
                reservation.getRequestedAt(),
                reservation.getResolvedAt());
    }
}
