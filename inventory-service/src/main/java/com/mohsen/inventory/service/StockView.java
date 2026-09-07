package com.mohsen.inventory.service;

import com.mohsen.inventory.domain.Stock;

/** Cached in Redis as JSON - never the JPA entity itself. */
public record StockView(String sku, String warehouseId, int quantityOnHand, int quantityReserved, int available) {

    public static StockView from(Stock stock) {
        return new StockView(stock.getSku(), stock.getWarehouseId(), stock.getQuantityOnHand(),
                stock.getQuantityReserved(), stock.available());
    }
}
