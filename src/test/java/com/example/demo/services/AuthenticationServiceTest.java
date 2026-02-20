package com.example.demo.services;

import com.example.demo.dto.RegisterUserDto;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private EmailService emailService;

  @InjectMocks
  private AuthenticationService authenticationService;

  @Test
  void signup_shouldCreateDisabledUserWithVerificationCodeAndSendEmail() throws Exception{
    RegisterUserDto dto = new RegisterUserDto();
    dto.setUsername("juan");
    dto.setEmail("juan@example.com");
    dto.setPassword("123456");

    when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    LocalDateTime before = LocalDateTime.now();

    User result = authenticationService.signup(dto);

    LocalDateTime after = LocalDateTime.now();

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getUsername()).isEqualTo("juan");
    assertThat(savedUser.getEmail()).isEqualTo("juan@example.com");
    assertThat(savedUser.getPassword()).isEqualTo("encoded-123456");
    assertThat(savedUser.isEnabled()).isFalse();
    assertThat(savedUser.getVerificationCode()).matches("\\d{6}");
    assertThat(savedUser.getVerificationCodeExpiresAt())
      .isAfterOrEqualTo(before.plusMinutes(14))
      .isBeforeOrEqualTo(after.plusMinutes(16));

    verify(emailService).sendVerificationEmail(
      eq("juan@example.com"),
      eq("Account Verification"),
      contains(savedUser.getVerificationCode())
    );

    assertThat(result).isSameAs(savedUser);
  }

  @Test
  void authenticate_shouldThrowWhenUserIsNotVerified(){
    com.example.demo.dto.LoginUserDto dto = new com.example.demo.dto.LoginUserDto();
    dto.setEmail("juan@example.com");
    dto.setPassword("123456");

    User user = new User("juan", "juan@example.com", "encoded-123456");
    user.setEnabled(false);

    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> authenticationService.authenticate(dto));

    org.mockito.Mockito.verifyNoInteractions(authenticationManager);
  }

  @Test
  void authenticate_shouldThrowWhenUserDoesNotExist(){
    com.example.demo.dto.LoginUserDto dto = new com.example.demo.dto.LoginUserDto();
    dto.setEmail("undefined@example.com");
    dto.setPassword("123456");

    when(userRepository.findByEmail("undefined@example.com")).thenReturn(java.util.Optional.empty());

    RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> authenticationService.authenticate(dto));

    assertThat(exception.getMessage()).isEqualTo("User not found");
    org.mockito.Mockito.verifyNoInteractions(authenticationManager);
  }

  @Test
  void authenticate_shouldReturnUserAndCallAuthenticationManagerWhenUserIsVerified() {
    com.example.demo.dto.LoginUserDto dto = new com.example.demo.dto.LoginUserDto();
    dto.setEmail("juan@example.com");
    dto.setPassword("123456");

    User user = new User("juan", "juan@example.com", "encoded-123456");
    user.setEnabled(true);

    when(userRepository.findByEmail("juan@example.com"))
      .thenReturn(java.util.Optional.of(user));
    when(authenticationManager.authenticate(any(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)))
      .thenReturn(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        dto.getEmail(),
        dto.getPassword()
      ));

    User result = authenticationService.authenticate(dto);

    ArgumentCaptor<org.springframework.security.authentication.UsernamePasswordAuthenticationToken> captor =
      ArgumentCaptor.forClass(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class);
    verify(authenticationManager).authenticate(captor.capture());

    assertThat(captor.getValue().getPrincipal()).isEqualTo("juan@example.com");
    assertThat(captor.getValue().getCredentials()).isEqualTo("123456");
    assertThat(result).isSameAs(user);
  }

  @Test
  void verifyUser_shouldEnableUserAndClearVerificationFieldsWhenCodeMatches() {
    com.example.demo.dto.VerifyUserDto dto = new com.example.demo.dto.VerifyUserDto();
    dto.setEmail("juan@example.com");
    dto.setVerificationCode("123456");

    User user = new User("juan", "juan@example.com", "encoded");
    user.setEnabled(false);
    user.setVerificationCode("123456");
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

    when(userRepository.findByEmail("juan@example.com")).thenReturn(java.util.Optional.of(user));

    authenticationService.verifyUser(dto);

    assertThat(user.isEnabled()).isTrue();
    assertThat(user.getVerificationCode()).isNull();
    assertThat(user.getVerificationCodeExpiresAt()).isNull();
    verify(userRepository).save(user);
  }

  @Test
  void verifyUser_shouldThrowWhenCodeIsInvalid(){
    com.example.demo.dto.VerifyUserDto dto = new com.example.demo.dto.VerifyUserDto();
    dto.setEmail("juan@example.com");
    dto.setVerificationCode("1111");

    User user = new User("juan", "juan@example.com", "encoded");
    user.setEnabled(false);
    user.setVerificationCode("123456");
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

    when(userRepository.findByEmail("juan@example.com"))
      .thenReturn(java.util.Optional.of(user));

    RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> authenticationService.verifyUser(dto));
    assertThat(exception.getMessage()).isEqualTo("Invalid verification code");
  }

  @Test
  void verifyUser_shouldThrowWhenCodeExpired(){
    com.example.demo.dto.VerifyUserDto dto = new com.example.demo.dto.VerifyUserDto();
    dto.setEmail("juan@example.com");
    dto.setVerificationCode("1111");

    User user = new User("juan", "juan@example.com", "encoded");
    user.setEnabled(false);
    user.setVerificationCode("123456");
    user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

    when(userRepository.findByEmail("juan@example.com"))
      .thenReturn(java.util.Optional.of(user));

    RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
      RuntimeException.class,
      () -> authenticationService.verifyUser(dto)
    );

    assertThat(ex.getMessage()).isEqualTo("Verification code has expired");
  }

  @Test
  void resendVerificationCode_shouldThrowWhenUserIsAlreadyEnabled() {
    User user = new User("juan", "juan@example.com", "encoded");
    user.setEnabled(true);

    when(userRepository.findByEmail("juan@example.com"))
      .thenReturn(java.util.Optional.of(user));

    RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
      RuntimeException.class,
      () -> authenticationService.resendVerificationCode("juan@example.com")
    );

    assertThat(ex.getMessage()).isEqualTo("Account is already verified");
    verify(userRepository).findByEmail("juan@example.com");
    org.mockito.Mockito.verifyNoMoreInteractions(userRepository);
  }
}
