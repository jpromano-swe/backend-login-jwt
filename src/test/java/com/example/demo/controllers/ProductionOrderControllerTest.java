package com.example.demo.controllers;

import com.example.demo.controller.ProductionOrderController;
import com.example.demo.dto.ProductionOrderDto;
import com.example.demo.dto.ProductionOrderItemDto;
import com.example.demo.dto.ProductionOrderSummaryDto;
import com.example.demo.model.ProductionOrderItem;
import com.example.demo.services.JwtService;
import com.example.demo.services.ProductionOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(ProductionOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductionOrderControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ProductionOrderService service;

  @MockitoBean
  private JwtService jwtService;

  @Test
  void create_shouldReturnOkAndOrder() throws Exception {
    ProductionOrderDto input = ProductionOrderDto.builder()
      .orderNumber("ORD-2026-10")
      .customerId(10L)
      .teamId(5L)
      .build();

    ProductionOrderDto created = ProductionOrderDto.builder()
      .id(1L)
      .orderUUID(UUID.randomUUID())
      .orderNumber("ORD-2026-1")
      .customerId(10L)
      .teamId(5L)
      .statusId((short) 1)
      .build();

    when(service.create(any(ProductionOrderDto.class))).thenReturn(created);

    mockMvc.perform(post("/auth/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(input)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.orderNumber").value("ORD-2026-1"))
      .andExpect(jsonPath("$.customerId").value(10));

    verify(service).create(any(ProductionOrderDto.class));
  }

  @Test
  void getAll_shouldReturnList() throws Exception {
    ProductionOrderDto order1 = ProductionOrderDto.builder()
      .id(1L)
      .orderNumber("ORD-1")
      .customerId(10L)
      .build();

    ProductionOrderDto order2 = ProductionOrderDto.builder()
      .id(2L)
      .orderNumber("ORD-2")
      .customerId(11L)
      .build();

    when(service.getAll()).thenReturn(List.of(order1, order2));

    mockMvc.perform(get("/auth/orders"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(1))
      .andExpect(jsonPath("$[0].orderNumber").value("ORD-1"))
      .andExpect(jsonPath("$[1].id").value(2))
      .andExpect(jsonPath("$[1].orderNumber").value("ORD-2"));
  }

  @Test
  void getById_shouldReturnOrder() throws Exception {
    ProductionOrderDto dto = ProductionOrderDto.builder()
      .id(7L)
      .orderNumber("ORD-1")
      .customerId(10L)
      .statusId((short) 2)
      .build();

    when(service.getById(7L)).thenReturn(dto);

    mockMvc.perform(get("/auth/orders/7"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(7))
      .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
      .andExpect(jsonPath("$.statusId").value(2));

    verify(service).getById(7L);
  }

  @Test
  void confirm_shouldReturnUpdatedOrder() throws Exception {
    ProductionOrderDto dto = ProductionOrderDto.builder()
      .id(7L)
      .orderNumber("ORD-7")
      .customerId(10L)
      .statusId((short) 2) //scheduled
      .build();

  when(service.confirm(7L)).thenReturn(dto);

  mockMvc.perform(post("/auth/orders/{id}/confirm", 7))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(7))
      .andExpect(jsonPath("$.orderNumber").value("ORD-7"))
      .andExpect(jsonPath("$.statusId").value(2));

  verify(service).confirm(7L);
  }

@Test
  void addItem_shouldReturnOkMessage() throws Exception {

  ProductionOrderItemDto item1 = new ProductionOrderItemDto();
  item1.setProductType("Window");
  item1.setWidthMm(1000);
  item1.setHeightMm(1200);
  item1.setQuantity(2);

  ProductionOrderItemDto item2 = new ProductionOrderItemDto();
  item1.setProductType("Door");
  item1.setWidthMm(1000);
  item1.setHeightMm(1200);
  item1.setQuantity(2);

  mockMvc.perform(post("/auth/orders/{id}/items", 10)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(List.of(item1, item2))))
          .andExpect(status().isOk())
          .andExpect(content().string("Items added successfully"));

  verify(service).addItems(eq(10L), anyList());
}

  @Test
    void getSummary_shouldReturnSummary() throws Exception{
    ProductionOrderSummaryDto summary = ProductionOrderSummaryDto.builder()
      .orderId(60L)
      .items(List.of(
        ProductionOrderSummaryDto.ItemSummary.builder()
          .id(1L)
          .productType("Window")
          .widthMm(1000)
          .heightMm(1200)
          .quantity(2)
          .profileMeters(8.8)
          .glassSquareMeters(2.4)
          .hardwareUnits(2)
          .build()
      ))
      .requirements(ProductionOrderSummaryDto.Requirements.builder()
        .totalProfileMeters(8.8)
        .totalGlassSquareMeters(2.4)
        .totalHardwareUnits(2)
        .build())
      .build();

    when(service.buildSummary(60L)).thenReturn(summary);

    mockMvc.perform(get("/auth/orders/{id}/summary", 60))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orderId").value(60))
      .andExpect(jsonPath("$.items.length()").value(1))
      .andExpect(jsonPath("$.items[0].productType").value("Window"))
      .andExpect(jsonPath("$.requirements.totalProfileMeters").value(8.8));

    verify(service).buildSummary(60L);
  }

  @Test
  void clearItems_shouldReturnNoContent() throws Exception{
    mockMvc.perform(delete("/auth/orders/{id}/items",80))
      .andExpect(status().isNoContent());

    verify(service).addItems(eq(80L), eq(List.of()));
  }

}
