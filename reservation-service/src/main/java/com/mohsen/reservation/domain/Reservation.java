package com.mohsen.reservation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor
public class Reservation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public static Reservation request(String sku, int quantity) {
        Reservation reservation = new Reservation();
        reservation.id = UUID.randomUUID();
        reservation.sku = sku;
        reservation.quantity = quantity;
        reservation.status = ReservationStatus.PENDING;
        reservation.requestedAt = Instant.now();
        return reservation;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.resolvedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = ReservationStatus.REJECTED;
        this.rejectionReason = reason;
        this.resolvedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
