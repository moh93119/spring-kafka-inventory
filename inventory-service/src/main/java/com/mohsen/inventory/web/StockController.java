package com.mohsen.inventory.web;

import com.mohsen.inventory.service.StockAdjustmentService;
import com.mohsen.inventory.service.StockQueryService;
import com.mohsen.inventory.service.StockView;
import com.mohsen.inventory.web.dto.AdjustStockRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockQueryService stockQueryService;
    private final StockAdjustmentService stockAdjustmentService;

    public StockController(StockQueryService stockQueryService, StockAdjustmentService stockAdjustmentService) {
        this.stockQueryService = stockQueryService;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @GetMapping("/{sku}")
    public StockView getStock(@PathVariable String sku) {
        return stockQueryService.getStock(sku);
    }

    @PostMapping("/{sku}/adjust")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjustStock(@PathVariable String sku, @Valid @RequestBody AdjustStockRequest request) {
        stockAdjustmentService.adjust(sku, request.warehouseId(), request.delta());
    }
}
