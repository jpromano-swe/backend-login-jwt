package com.example.demo.controllers;

import com.example.demo.controller.CustomerController;
import com.example.demo.dto.CustomerDto;
import com.example.demo.services.CustomerService;
import com.example.demo.services.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomerService customerService;

  @Test
  void createCustomer_shouldReturnOkAndCustomer() throws Exception {
    CustomerDto input = CustomerDto.builder()
      .name("Juan Perez")
      .email("juan@example.com")
      .address("Calle 123")
      .phone("123456789")
      .build();

    CustomerDto created = CustomerDto.builder()
      .id(1L)
      .name("Juan Perez")
      .email("juan@example.com")
      .address("Calle 123")
      .phone("123456789")
      .build();

    when(customerService.createCustomer(any(CustomerDto.class))).thenReturn(created);

    mockMvc.perform(post("/auth/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(input)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.name").value("Juan Perez"))
      .andExpect(jsonPath("$.email").value("juan@example.com"));

    verify(customerService).createCustomer(any(CustomerDto.class));
  }

  @Test
  void createCustomer_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    CustomerDto input = CustomerDto.builder()
      .name("")
      .email("juan@example.com")
      .address("Calle 123")
      .phone("123456789")
      .build();

    mockMvc.perform(post("/auth/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isBadRequest());

    org.mockito.Mockito.verifyNoInteractions(customerService);
  }

  @Test
  void getAllCustomers_shouldReturnEmptyList() throws Exception {
    when(customerService.getAllCustomers()).thenReturn(java.util.List.of());

    mockMvc.perform(get("/auth/customers"))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isEmpty());

    verify(customerService).getAllCustomers();
  }

  @Test
  void getCustomerById_shouldPropagateErrorWhenServiceThrows() {
    when(customerService.getCustomerById(99L))
      .thenThrow(new RuntimeException("Customer not found"));

    org.junit.jupiter.api.Assertions.assertThrows(
      jakarta.servlet.ServletException.class,
      () -> mockMvc.perform(get("/auth/customers/{id}", 99))
    );

    verify(customerService).getCustomerById(99L);
  }

  @Test
  void updateCustomer_shouldReturnBadRequest_whenNameIsBlank() throws Exception{
    CustomerDto input = CustomerDto.builder()
      .name("")
      .email("juan@example.com")
      .build();

    mockMvc.perform(put("/auth/customers/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isBadRequest());

    org.mockito.Mockito.verifyNoInteractions(customerService);
  }

  @Test
  void updateCustomer_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception{
    CustomerDto input = CustomerDto.builder()
      .name("Juan Perez")
      .email("invalid@")
      .build();

    mockMvc.perform(put("/auth/customers/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isBadRequest());

    org.mockito.Mockito.verifyNoInteractions(customerService);
  }

  @Test
  void createCustomer_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception{
    CustomerDto input = CustomerDto.builder()
      .name("Juan Perez")
      .email("invalid@")
      .build();

    mockMvc.perform(post("/auth/customers")
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(input)))
      .andExpect(status().isBadRequest());

    org.mockito.Mockito.verifyNoInteractions(customerService);
  }

  @Test
  void deleteCustomer_shouldCallServiceWithPathId() throws Exception {
    mockMvc.perform(delete("/auth/customers/123"))
      .andExpect(status().isNoContent());

    verify(customerService).deleteCustomer(eq(123L));
  }
}
