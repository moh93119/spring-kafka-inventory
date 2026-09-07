package com.mohsen.reservation.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(@NotBlank String sku, @Positive int quantity) {
}
