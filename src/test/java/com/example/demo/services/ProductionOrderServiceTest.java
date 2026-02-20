package com.example.demo.services;

import com.example.demo.dto.ProductionOrderDto;
import com.example.demo.dto.ProductionOrderItemDto;
import com.example.demo.dto.ProductionOrderSummaryDto;
import com.example.demo.model.ProductionOrder;
import com.example.demo.model.ProductionOrderItem;
import com.example.demo.repositories.ProductionOrderItemRepository;
import com.example.demo.repositories.ProductionOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ProductionOrderServiceTest {

  @Mock
  private ProductionOrderRepository repository;

  @Mock
  private ProductionOrderItemRepository itemRepository;

  @InjectMocks
  private ProductionOrderService service;

  @Test
  void confirm_shouldMoveStatusFromInProgressToScheduled(){
    ProductionOrder order = new ProductionOrder();
    order.setId(10L);
    order.setStatusId((short) 1);
    order.setOrderNumber("ORD-2026-10");

    when(repository.findById(10L)).thenReturn(java.util.Optional.of(order));

    ProductionOrderDto result = service.confirm(10L);

    assertThat(result.getStatusId()).isEqualTo((short)2);

    verify(repository).save(order);

  }

  @Test
  void confirm_shouldThrowWhenStatusIsNotInProgress(){
    ProductionOrder order = new ProductionOrder();
    order.setId(10L);
    order.setStatusId((short) 2);

    when(repository.findById(10L)).thenReturn(java.util.Optional.of(order));

    IllegalStateException exception = assertThrows(IllegalStateException.class,()->service.confirm(10L));

    assertThat(exception).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void start_shouldMoveStatusFromScheduledToInProgress() {
    ProductionOrder po = new ProductionOrder();
    po.setId(20L);
    po.setStatusId((short) 2); // SCHEDULED

    when(repository.findById(20L)).thenReturn(Optional.of(po));

    ProductionOrderDto result = service.start(20L);

    assertThat(result.getStatusId()).isEqualTo((short) 1); // IN_PROGRESS
    verify(repository).save(po);
  }

  @Test
  void finish_shouldMoveStatusFromInProgressToForDelivery() {
    ProductionOrder po = new ProductionOrder();
    po.setId(30L);
    po.setStatusId((short) 1); // IN_PROGRESS

    when(repository.findById(30L)).thenReturn(Optional.of(po));

    ProductionOrderDto result = service.finish(30L);

    assertThat(result.getStatusId()).isEqualTo((short) 3); // FOR_DELIVERY
    verify(repository).save(po);
  }

  @Test
  void deliver_shouldMoveStatusFromForDeliveryToCompleted() {
    ProductionOrder po = new ProductionOrder();
    po.setId(40L);
    po.setStatusId((short) 3); // FOR_DELIVERY

    when(repository.findById(40L)).thenReturn(Optional.of(po));

    ProductionOrderDto result = service.deliver(40L);

    assertThat(result.getStatusId()).isEqualTo((short) 4); // COMPLETED
    verify(repository).save(po);
  }

  @Test
  void start_shouldThrowWhenStatusIsInvalid() {
    ProductionOrder po = new ProductionOrder();
    po.setId(20L);
    po.setStatusId((short) 1); // debería ser 2 para start

    when(repository.findById(20L)).thenReturn(Optional.of(po));

    IllegalStateException ex = assertThrows(
      IllegalStateException.class,
      () -> service.start(20L)
    );

    assertThat(ex.getMessage()).contains("Esperado=2");
  }

  @Test
  void addItems_shouldDeletePreviousItemsAndSaveNewOnes() {
    ProductionOrder po = new ProductionOrder();
    po.setId(50L);

    ProductionOrderItemDto item1 = new ProductionOrderItemDto();
    item1.setProductType("Window");
    item1.setWidthMm(1000);
    item1.setHeightMm(1200);
    item1.setQuantity(2);

    ProductionOrderItemDto item2 = new ProductionOrderItemDto();
    item2.setProductType("Door");
    item2.setWidthMm(900);
    item2.setHeightMm(2100);
    item2.setQuantity(1);

    when(repository.findById(50L)).thenReturn(Optional.of(po));

    service.addItems(50L, List.of(item1, item2));

    verify(itemRepository).deleteByOrderId(50L);

    ArgumentCaptor<ProductionOrderItem> captor = ArgumentCaptor.forClass(ProductionOrderItem.class);
    verify(itemRepository, times(2)).save(captor.capture());

    List<ProductionOrderItem> saved = captor.getAllValues();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).getOrderId()).isEqualTo(50L);
    assertThat(saved.get(0).getProductType()).isEqualTo("Window");
    assertThat(saved.get(1).getProductType()).isEqualTo("Door");
  }

  @Test
  void buildSummary_shouldCalculateTotalsAndRoundToThreeDecimals() {
    ProductionOrder po = new ProductionOrder();
    po.setId(60L);

    ProductionOrderItem i1 = new ProductionOrderItem();
    i1.setId(1L);
    i1.setOrderId(60L);
    i1.setProductType("Window");
    i1.setWidthMm(1000);
    i1.setHeightMm(1200);
    i1.setQuantity(2);

    ProductionOrderItem i2 = new ProductionOrderItem();
    i2.setId(2L);
    i2.setOrderId(60L);
    i2.setProductType("Door");
    i2.setWidthMm(900);
    i2.setHeightMm(2100);
    i2.setQuantity(1);

    when(repository.findById(60L)).thenReturn(Optional.of(po));
    when(itemRepository.findByOrderId(60L)).thenReturn(List.of(i1, i2));

    ProductionOrderSummaryDto summary = service.buildSummary(60L);

    assertThat(summary.getOrderId()).isEqualTo(60L);
    assertThat(summary.getItems()).hasSize(2);

    // i1: profile=8.8, glass=2.4, hardware=2
    assertThat(summary.getItems().get(0).getProfileMeters()).isEqualTo(8.8);
    assertThat(summary.getItems().get(0).getGlassSquareMeters()).isEqualTo(2.4);
    assertThat(summary.getItems().get(0).getHardwareUnits()).isEqualTo(2);

    // i2: profile=6.0, glass=1.89, hardware=1
    assertThat(summary.getItems().get(1).getProfileMeters()).isEqualTo(6.0);
    assertThat(summary.getItems().get(1).getGlassSquareMeters()).isEqualTo(1.89);
    assertThat(summary.getItems().get(1).getHardwareUnits()).isEqualTo(1);

    // totales: profile=14.8, glass=4.29, hardware=3
    assertThat(summary.getRequirements().getTotalProfileMeters()).isEqualTo(14.8);
    assertThat(summary.getRequirements().getTotalGlassSquareMeters()).isEqualTo(4.29);
    assertThat(summary.getRequirements().getTotalHardwareUnits()).isEqualTo(3);
  }


}
