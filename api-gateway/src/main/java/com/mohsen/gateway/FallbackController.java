package com.mohsen.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Where the Gateway's CircuitBreaker filter forwards to when a downstream route is open. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/stock")
    public Mono<ProblemDetail> stockFallback() {
        return Mono.just(ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "inventory-service is temporarily unavailable - please retry shortly"));
    }

    @RequestMapping("/fallback/reservations")
    public Mono<ProblemDetail> reservationsFallback() {
        return Mono.just(ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "reservation-service is temporarily unavailable - please retry shortly"));
    }
}
