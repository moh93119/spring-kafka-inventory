package com.mohsen.reservation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.service.ReservationRequestService;
import com.mohsen.reservation.web.dto.CreateReservationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationRequestService reservationRequestService;

    @MockitoBean
    private ReservationRepository reservationRepository;

    @Test
    void creatingAReservationReturns201WithPendingStatus() throws Exception {
        Reservation reservation = Reservation.request("SKU-1", 2);
        given(reservationRequestService.request("SKU-1", 2)).willReturn(reservation);

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateReservationRequest("SKU-1", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void creatingAReservationWithZeroQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateReservationRequest("SKU-1", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gettingAnUnknownReservationReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(reservationRepository.findById(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/reservations/{id}", id))
                .andExpect(status().isNotFound());
    }
}
