package com.mohsen.inventory.domain;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String sku) {
        super("No stock record for SKU: " + sku);
    }
}
