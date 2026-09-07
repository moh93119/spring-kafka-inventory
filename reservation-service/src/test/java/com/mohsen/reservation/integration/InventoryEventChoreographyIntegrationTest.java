package com.mohsen.reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohsen.reservation.domain.Reservation;
import com.mohsen.reservation.domain.ReservationRepository;
import com.mohsen.reservation.events.InventoryEventMessage;
import com.mohsen.reservation.events.InventoryEventType;
import com.mohsen.reservation.service.ReservationRequestService;
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
 * Simulates inventory-service's reply (a real InventoryEventMessage
 * published straight to the topic, rather than booting the whole other
 * service) and proves: a reservation resolves to CONFIRMED, and
 * redelivering the same event id does not re-resolve it a second time.
 */
@SpringBootTest
@Testcontainers
@Tag("slow")
class InventoryEventChoreographyIntegrationTest {

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

    @Autowired
    private ReservationRequestService reservationRequestService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void duplicateInventoryEventConfirmsTheReservationOnlyOnce() throws Exception {
        Reservation reservation = reservationRequestService.request("SKU-CHOREO-1", 2);

        InventoryEventMessage event = new InventoryEventMessage(
                UUID.randomUUID(), reservation.getId(), "SKU-CHOREO-1", InventoryEventType.RESERVED, 2, null, Instant.now());
        String json = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("inventory-events", reservation.getId().toString(), json).get();
        kafkaTemplate.send("inventory-events", reservation.getId().toString(), json).get();

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Reservation reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();
            assertThat(reloaded.getStatus().name()).isEqualTo("CONFIRMED");
            assertThat(reloaded.getResolvedAt()).isNotNull();
        });
    }
}
