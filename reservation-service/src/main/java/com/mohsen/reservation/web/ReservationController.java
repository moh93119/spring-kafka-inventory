package com.mohsen.reservation.web;

import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationNotFoundException;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.service.ReservationRequestService;
import com.mohsen.reservation.web.dto.CreateReservationRequest;
import com.mohsen.reservation.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRequestService reservationRequestService;
    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationRequestService reservationRequestService, ReservationRepository reservationRepository) {
        this.reservationRequestService = reservationRequestService;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationRequestService.request(request.sku(), request.quantity());
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.getId()))
                .body(ReservationResponse.from(reservation));
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@PathVariable UUID id) {
        return reservationRepository.findById(id)
                .map(ReservationResponse::from)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }
}
