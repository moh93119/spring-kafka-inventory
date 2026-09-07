package com.mohsen.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void stockFallbackReturns503() {
        webTestClient.get().uri("/fallback/stock")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void reservationsFallbackReturns503() {
        webTestClient.get().uri("/fallback/reservations")
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}
