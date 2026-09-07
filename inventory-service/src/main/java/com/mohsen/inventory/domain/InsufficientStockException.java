package com.mohsen.inventory.domain;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for %s: requested %d, available %d".formatted(sku, requested, available));
    }
}
