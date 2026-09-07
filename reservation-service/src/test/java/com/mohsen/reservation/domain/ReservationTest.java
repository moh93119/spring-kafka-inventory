package com.mohsen.reservation.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTest {

    @Test
    void requestStartsPending() {
        Reservation reservation = Reservation.request("SKU-1", 2);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getResolvedAt()).isNull();
    }

    @Test
    void confirmSetsStatusAndResolvedAt() {
        Reservation reservation = Reservation.request("SKU-1", 2);

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getResolvedAt()).isNotNull();
    }

    @Test
    void rejectSetsStatusReasonAndResolvedAt() {
        Reservation reservation = Reservation.request("SKU-1", 2);

        reservation.reject("insufficient stock");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.getRejectionReason()).isEqualTo("insufficient stock");
        assertThat(reservation.getResolvedAt()).isNotNull();
    }
}
