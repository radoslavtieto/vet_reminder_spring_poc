package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ClinicDto {

    private ClinicDto() {
    }

    public record ClinicRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 100) String city,
            @Size(max = 255) String address,
            @Size(max = 32) String phone,
            @Size(max = 254) String email,
            @NotNull Boolean active) {
    }

    public record ClinicView(long id, String publicId, String tenant, String name, String city,
                       String address, String phone, String email, boolean active,
                       Instant createdAt, Instant updatedAt) {
    }
}
