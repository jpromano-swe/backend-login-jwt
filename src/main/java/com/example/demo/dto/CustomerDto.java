package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {
    private Long id;
    private UUID customerUUID;

    @NotBlank(message="Name is mandatory")
    private String name;

    private String address;

    @Email(message="Email should be valid")
    private String email;

    private String phone;
    private Instant createdAt;
}
