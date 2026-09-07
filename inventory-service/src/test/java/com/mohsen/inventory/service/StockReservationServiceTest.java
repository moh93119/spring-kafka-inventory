package com.mohsen.inventory.service;

import com.mohsen.inventory.domain.Stock;
import com.mohsen.inventory.domain.StockRepository;
import com.mohsen.inventory.events.InventoryEventMessage;
import com.mohsen.inventory.events.InventoryEventType;
import com.mohsen.inventory.events.ReservationRequestedEvent;
import com.mohsen.inventory.inbox.IdempotencyGuard;
import com.mohsen.inventory.outbox.OutboxWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private IdempotencyGuard idempotencyGuard;

    @Mock
    private OutboxWriter outboxWriter;

    @Test
    void alreadyProcessedEventsAreSkipped() {
        StockReservationService service = new StockReservationService(stockRepository, idempotencyGuard, outboxWriter);
        ReservationRequestedEvent event = anEvent("SKU-1", 2);
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(true);

        service.handle(event);

        verifyNoInteractions(stockRepository, outboxWriter);
    }

    @Test
    void sufficientStockReservesAndPublishesReserved() {
        StockReservationService service = new StockReservationService(stockRepository, idempotencyGuard, outboxWriter);
        ReservationRequestedEvent event = anEvent("SKU-1", 2);
        Stock stock = new Stock("SKU-1", "WH-1", 10);
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(stockRepository.findById("SKU-1")).thenReturn(Optional.of(stock));

        service.handle(event);

        assertThat(stock.getQuantityReserved()).isEqualTo(2);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxWriter).append(eq("inventory-events"), eq(event.reservationId().toString()), captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(InventoryEventMessage.class,
                msg -> assertThat(msg.type()).isEqualTo(InventoryEventType.RESERVED));
    }

    @Test
    void insufficientStockPublishesRejectedAndDoesNotThrow() {
        StockReservationService service = new StockReservationService(stockRepository, idempotencyGuard, outboxWriter);
        ReservationRequestedEvent event = anEvent("SKU-1", 20);
        Stock stock = new Stock("SKU-1", "WH-1", 10);
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(stockRepository.findById("SKU-1")).thenReturn(Optional.of(stock));

        service.handle(event);

        verify(stockRepository, never()).save(stock);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxWriter).append(eq("inventory-events"), eq(event.reservationId().toString()), captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(InventoryEventMessage.class,
                msg -> assertThat(msg.type()).isEqualTo(InventoryEventType.REJECTED));
    }

    @Test
    void unknownSkuPublishesRejected() {
        StockReservationService service = new StockReservationService(stockRepository, idempotencyGuard, outboxWriter);
        ReservationRequestedEvent event = anEvent("UNKNOWN-SKU", 1);
        when(idempotencyGuard.alreadyProcessed(event.eventId())).thenReturn(false);
        when(stockRepository.findById("UNKNOWN-SKU")).thenReturn(Optional.empty());

        service.handle(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxWriter).append(eq("inventory-events"), eq(event.reservationId().toString()), captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(InventoryEventMessage.class,
                msg -> assertThat(msg.type()).isEqualTo(InventoryEventType.REJECTED));
    }

    private static ReservationRequestedEvent anEvent(String sku, int quantity) {
        return new ReservationRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), sku, quantity, Instant.now());
    }
}
