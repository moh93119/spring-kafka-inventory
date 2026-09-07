package com.mohsen.inventory.service;

import com.mohsen.inventory.domain.Stock;
import com.mohsen.inventory.domain.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdjustmentService {

    private final StockRepository stockRepository;
    private final StockQueryService stockQueryService;

    public StockAdjustmentService(StockRepository stockRepository, StockQueryService stockQueryService) {
        this.stockRepository = stockRepository;
        this.stockQueryService = stockQueryService;
    }

    /** Creates the stock row (delta becomes the initial on-hand quantity) if it doesn't exist yet, otherwise adjusts it. */
    @Transactional
    public void adjust(String sku, String warehouseId, int delta) {
        Stock stock = stockRepository.findById(sku).orElse(null);
        if (stock == null) {
            stockRepository.save(new Stock(sku, warehouseId, delta));
        } else {
            stock.adjust(delta);
            stockRepository.save(stock);
        }
        stockQueryService.evict(sku);
    }
}
