package com.mohsen.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohsen.inventory.domain.StockNotFoundException;
import com.mohsen.inventory.service.StockAdjustmentService;
import com.mohsen.inventory.service.StockQueryService;
import com.mohsen.inventory.service.StockView;
import com.mohsen.inventory.web.dto.AdjustStockRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockQueryService stockQueryService;

    @MockitoBean
    private StockAdjustmentService stockAdjustmentService;

    @Test
    void getStockReturns200WithBody() throws Exception {
        given(stockQueryService.getStock("SKU-1")).willReturn(new StockView("SKU-1", "WH-1", 10, 2, 8));

        mockMvc.perform(get("/api/stock/{sku}", "SKU-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(8));
    }

    @Test
    void getUnknownStockReturns404() throws Exception {
        willThrow(new StockNotFoundException("SKU-X")).given(stockQueryService).getStock("SKU-X");

        mockMvc.perform(get("/api/stock/{sku}", "SKU-X"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adjustStockReturns204() throws Exception {
        AdjustStockRequest request = new AdjustStockRequest("WH-1", 5);

        mockMvc.perform(post("/api/stock/{sku}/adjust", "SKU-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adjustStockWithBlankWarehouseReturns400() throws Exception {
        AdjustStockRequest request = new AdjustStockRequest("", 5);

        mockMvc.perform(post("/api/stock/{sku}/adjust", "SKU-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
