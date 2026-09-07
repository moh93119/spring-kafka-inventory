package com.mohsen.reservation.service;

import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationNotFoundException;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.events.InventoryEventMessage;
import com.mohsen.reservation.inbox.IdempotencyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ReservationResolutionService.class);

    private final ReservationRepository reservationRepository;
    private final IdempotencyGuard idempotencyGuard;

    public ReservationResolutionService(ReservationRepository reservationRepository, IdempotencyGuard idempotencyGuard) {
        this.reservationRepository = reservationRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void handle(InventoryEventMessage event) {
        if (idempotencyGuard.alreadyProcessed(event.eventId())) {
            log.info("Skipping already-processed inventory event {}", event.eventId());
            return;
        }

        Reservation reservation = reservationRepository.findById(event.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(event.reservationId()));

        switch (event.type()) {
            case RESERVED -> reservation.confirm();
            case REJECTED -> reservation.reject(event.reason());
        }
        reservationRepository.save(reservation);
    }
}
