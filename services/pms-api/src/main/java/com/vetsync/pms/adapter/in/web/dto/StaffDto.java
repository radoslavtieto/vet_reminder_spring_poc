package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class StaffDto {

    private StaffDto() {
    }

    public record StaffRequest(
            @Positive Long clinicId,
            @NotBlank String role,
            @NotBlank @Size(max = 160) String fullName,
            @Size(max = 80) String licenseNo,
            @NotNull Boolean active) {
    }

    public record StaffView(long id, String publicId, String tenant, long clinicId, String role,
                       String fullName, String licenseNo, boolean active,
                       Instant createdAt, Instant updatedAt) {
    }
}
