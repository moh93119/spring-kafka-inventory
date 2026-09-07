package com.mohsen.reservation.service;

import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.events.ReservationRequestedEvent;
import com.mohsen.reservation.outbox.OutboxWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationRequestService {

    private static final String RESERVATION_REQUESTS_TOPIC = "reservation-requests";

    private final ReservationRepository reservationRepository;
    private final OutboxWriter outboxWriter;

    public ReservationRequestService(ReservationRepository reservationRepository, OutboxWriter outboxWriter) {
        this.reservationRepository = reservationRepository;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public Reservation request(String sku, int quantity) {
        Reservation reservation = Reservation.request(sku, quantity);
        reservationRepository.save(reservation);

        ReservationRequestedEvent event = new ReservationRequestedEvent(
                UUID.randomUUID(), reservation.getId(), sku, quantity, Instant.now());
        outboxWriter.append(RESERVATION_REQUESTS_TOPIC, sku, event);

        return reservation;
    }
}
