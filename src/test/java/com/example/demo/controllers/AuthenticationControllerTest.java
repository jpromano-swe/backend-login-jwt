package com.example.demo.controllers;

import com.example.demo.controller.AuthenticationController;
import com.example.demo.dto.LoginUserDto;
import com.example.demo.dto.RegisterUserDto;
import com.example.demo.dto.VerifyUserDto;
import com.example.demo.model.User;
import com.example.demo.services.AuthenticationService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthenticationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private AuthenticationService authenticationService;

  @Test
  void register_shouldReturnOkAndUser() throws Exception{
    RegisterUserDto dto = new RegisterUserDto();
    dto.setUsername("juan");
    dto.setEmail("juan@example.com");
    dto.setPassword("123456");

    User user = new User("juan", "juan@example.com", "encoded");
    when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(user);

    mockMvc.perform(post("/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.email").value("juan@example.com"));
  }

  @Test
  void login_shouldReturnTokenAndExpiration() throws Exception {
    LoginUserDto dto = new LoginUserDto();
    dto.setEmail("juan@example.com");
    dto.setPassword("123456");

    User user = new User("juan", "juan@example.com", "encoded");
    when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(user);
    when(jwtService.generateToken(user)).thenReturn("token-123");
    when(jwtService.getExpirationTime()).thenReturn(3600000L);

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").value("token-123"))
      .andExpect(jsonPath("$.expiresIn").value(3600000));
  }

  @Test
  void verify_shouldReturnOk_whenServiceSucceeds() throws Exception {
    VerifyUserDto dto = new VerifyUserDto();
    dto.setEmail("juan@example.com");
    dto.setVerificationCode("123456");

    doNothing().when(authenticationService).verifyUser(any(VerifyUserDto.class));

    mockMvc.perform(post("/auth/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isOk());
  }

  @Test
  void verify_shouldReturnBadRequest_whenServiceThrows() throws Exception {
    VerifyUserDto dto = new VerifyUserDto();
    dto.setEmail("juan@example.com");
    dto.setVerificationCode("000000");

    doThrow(new RuntimeException("Invalid verification code"))
      .when(authenticationService).verifyUser(any(VerifyUserDto.class));

    mockMvc.perform(post("/auth/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isBadRequest())
      .andExpect(content().string("Invalid verification code"));
  }

  @Test
  void resend_shouldReturnBadRequest_whenServiceThrows() throws Exception {
    doThrow(new RuntimeException("User not found"))
      .when(authenticationService).resendVerificationCode("missing@example.com");

    mockMvc.perform(post("/auth/resend")
        .param("email", "missing@example.com"))
      .andExpect(status().isBadRequest())
      .andExpect(content().string("User not found"));
  }
}
