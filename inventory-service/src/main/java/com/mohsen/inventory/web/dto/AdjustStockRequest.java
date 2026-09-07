package com.mohsen.inventory.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(@NotBlank String warehouseId, @NotNull Integer delta) {
}
