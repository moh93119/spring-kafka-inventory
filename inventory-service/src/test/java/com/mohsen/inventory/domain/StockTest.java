package com.mohsen.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void newStockHasNoReservations() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);

        assertThat(stock.available()).isEqualTo(10);
    }

    @Test
    void reserveReducesAvailable() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);

        stock.reserve(4);

        assertThat(stock.getQuantityReserved()).isEqualTo(4);
        assertThat(stock.available()).isEqualTo(6);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);
        stock.reserve(8);

        assertThatThrownBy(() -> stock.reserve(3)).isInstanceOf(InsufficientStockException.class);
        assertThat(stock.getQuantityReserved()).isEqualTo(8);
    }

    @Test
    void releaseNeverGoesNegative() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);
        stock.reserve(2);

        stock.release(10);

        assertThat(stock.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    void adjustCannotDropOnHandBelowReserved() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);
        stock.reserve(7);

        assertThatThrownBy(() -> stock.adjust(-5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adjustIncreasesOnHand() {
        Stock stock = new Stock("SKU-1", "WH-1", 10);

        stock.adjust(5);

        assertThat(stock.getQuantityOnHand()).isEqualTo(15);
    }
}
