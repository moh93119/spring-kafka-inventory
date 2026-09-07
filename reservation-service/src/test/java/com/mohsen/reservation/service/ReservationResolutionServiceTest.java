package com.mohsen.reservation.service;

import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationNotFoundException;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.events.InventoryEventMessage;
import com.mohsen.reservation.events.InventoryEventType;
import com.mohsen.reservation.inbox.IdempotencyGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationResolutionServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private IdempotencyGuard idempotencyGuard;

    @Test
    void alreadyProcessedEventsAreSkipped() {
        ReservationResolutionService service = new ReservationResolutionService(reservationRepository, idempotencyGuard);
        InventoryEventMessage event = reservedEvent();
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(true);

        service.handle(event);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void reservedEventConfirmsTheReservation() {
        ReservationResolutionService service = new ReservationResolutionService(reservationRepository, idempotencyGuard);
        InventoryEventMessage event = reservedEvent();
        Reservation reservation = Reservation.request(event.sku(), event.quantity());
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(reservationRepository.findById(event.reservationId())).thenReturn(Optional.of(reservation));

        service.handle(event);

        assertThat(reservation.getStatus().name()).isEqualTo("CONFIRMED");
    }

    @Test
    void rejectedEventRejectsTheReservationWithReason() {
        ReservationResolutionService service = new ReservationResolutionService(reservationRepository, idempotencyGuard);
        InventoryEventMessage event = new InventoryEventMessage(
                UUID.randomUUID(), UUID.randomUUID(), "SKU-1", InventoryEventType.REJECTED, 2, "insufficient stock", Instant.now());
        Reservation reservation = Reservation.request(event.sku(), event.quantity());
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(reservationRepository.findById(event.reservationId())).thenReturn(Optional.of(reservation));

        service.handle(event);

        assertThat(reservation.getStatus().name()).isEqualTo("REJECTED");
        assertThat(reservation.getRejectionReason()).isEqualTo("insufficient stock");
    }

    @Test
    void unknownReservationThrows() {
        ReservationResolutionService service = new ReservationResolutionService(reservationRepository, idempotencyGuard);
        InventoryEventMessage event = reservedEvent();
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(reservationRepository.findById(event.reservationId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(ReservationNotFoundException.class);
    }

    private static InventoryEventMessage reservedEvent() {
        return new InventoryEventMessage(
                UUID.randomUUID(), UUID.randomUUID(), "SKU-1", InventoryEventType.RESERVED, 2, null, Instant.now());
    }
}
