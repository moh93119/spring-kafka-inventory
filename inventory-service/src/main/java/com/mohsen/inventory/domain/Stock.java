package com.mohsen.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * No Lombok @Data here either - same reasoning as dotnet-to-spring-orders:
 * identity-based equals/hashCode only, field mutation kept explicit.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter(AccessLevel.PACKAGE)
@NoArgsConstructor
public class Stock {

    @Id
    private String sku;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    public Stock(String sku, String warehouseId, int quantityOnHand) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
    }

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    public void reserve(int quantity) {
        if (quantity > available()) {
            throw new InsufficientStockException(sku, quantity, available());
        }
        this.quantityReserved += quantity;
    }

    public void release(int quantity) {
        this.quantityReserved = Math.max(0, quantityReserved - quantity);
    }

    public void adjust(int delta) {
        int updated = quantityOnHand + delta;
        if (updated < quantityReserved) {
            throw new IllegalArgumentException(
                    "Cannot reduce %s on-hand below reserved quantity (%d)".formatted(sku, quantityReserved));
        }
        this.quantityOnHand = updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stock other)) return false;
        return sku != null && sku.equals(other.sku);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
