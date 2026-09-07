package com.mohsen.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohsen.inventory.domain.Stock;
import com.mohsen.inventory.domain.StockRepository;
import com.mohsen.inventory.events.ReservationRequestedEvent;
import com.mohsen.inventory.outbox.OutboxMessageRepository;
import com.redis.testcontainers.RedisContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the outbox -> Kafka -> idempotent-consumer -> outbox round trip
 * end to end against real containers, including that publishing the same
 * event twice (Kafka's at-least-once delivery) only reserves stock once.
 */
@SpringBootTest
@Testcontainers
@Tag("slow")
class ReservationChoreographyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse(System.getProperty("testcontainers.postgres.image", "postgres:16-alpine"))
                    .asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse(System.getProperty("testcontainers.kafka.image", "apache/kafka:3.7.0"))
                    .asCompatibleSubstituteFor("apache/kafka"));

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse(System.getProperty("testcontainers.redis.image", "redis:7-alpine"))
                    .asCompatibleSubstituteFor("redis"));

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void duplicateDeliveryOfTheSameEventReservesStockOnlyOnce() throws Exception {
        stockRepository.save(new Stock("SKU-CHOREO-1", "WH-1", 10));

        UUID eventId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        ReservationRequestedEvent event = new ReservationRequestedEvent(eventId, reservationId, "SKU-CHOREO-1", 3, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        // Same event id published twice - simulates Kafka's at-least-once
        // redelivery (e.g. a consumer offset commit that never landed).
        kafkaTemplate.send("reservation-requests", "SKU-CHOREO-1", json).get();
        kafkaTemplate.send("reservation-requests", "SKU-CHOREO-1", json).get();

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Stock reloaded = stockRepository.findById("SKU-CHOREO-1").orElseThrow();
            assertThat(reloaded.getQuantityReserved()).isEqualTo(3);
        });

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxMessageRepository.findAll())
                        .filteredOn(m -> m.getMessageKey().equals(reservationId.toString()))
                        .hasSize(1)
                        .allSatisfy(m -> assertThat(m.getDispatchedAt()).isNotNull()));
    }
}
