package com.mohsen.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohsen.inventory.domain.Stock;
import com.mohsen.inventory.domain.StockNotFoundException;
import com.mohsen.inventory.domain.StockRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Cache-aside read through Redis, explicit via StringRedisTemplate rather
 * than @Cacheable - combining declarative caching and a Resilience4j
 * annotation on the very same method has real proxy-ordering subtleties
 * not worth the risk; this way the fallback path is a plain method call.
 *
 * @Retry then @CircuitBreaker only ever engage when Redis itself throws
 * (connection refused, timeout) - a cache miss is not a failure and never
 * reaches either annotation.
 */
@Service
public class StockQueryService {

    private static final Logger log = LoggerFactory.getLogger(StockQueryService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final StockRepository stockRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public StockQueryService(StockRepository stockRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.stockRepository = stockRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Retry(name = "stockCache")
    @CircuitBreaker(name = "stockCache", fallbackMethod = "getStockFromDb")
    public StockView getStock(String sku) {
        String cached = redisTemplate.opsForValue().get(cacheKey(sku));
        if (cached != null) {
            return deserialize(cached);
        }
        return loadFromDbAndCache(sku);
    }

    /** Resilience4j fallback signature: same params, trailing Throwable. */
    private StockView getStockFromDb(String sku, Throwable t) {
        log.warn("Redis unavailable, serving {} straight from the database", sku, t);
        return loadFromDb(sku);
    }

    public void evict(String sku) {
        redisTemplate.delete(cacheKey(sku));
    }

    private StockView loadFromDbAndCache(String sku) {
        StockView view = loadFromDb(sku);
        try {
            redisTemplate.opsForValue().set(cacheKey(sku), objectMapper.writeValueAsString(view), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to populate cache for {}, serving uncached", sku, e);
        }
        return view;
    }

    private StockView loadFromDb(String sku) {
        Stock stock = stockRepository.findById(sku).orElseThrow(() -> new StockNotFoundException(sku));
        return StockView.from(stock);
    }

    private StockView deserialize(String json) {
        try {
            return objectMapper.readValue(json, StockView.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt cache entry", e);
        }
    }

    private String cacheKey(String sku) {
        return "stock:" + sku;
    }
}
