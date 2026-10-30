package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class VisitDto {

    private VisitDto() {
    }

    public record VisitRequest(
            @Positive Long patientId,
            @NotNull @Positive Long clinicId,
            @NotNull @Positive Long vetId,
            @NotBlank @Size(max = 40) String visitType,
            String status,
            @NotNull Instant scheduledAt,
            Instant startedAt,
            Instant completedAt,
            @Size(max = 500) String reason,
            @Size(max = 500) String diagnosis,
            String notes) {
    }

    public record VisitView(long id, String publicId, String tenant, long patientId, long clinicId,
                       long vetId, String visitType, String status, Instant scheduledAt,
                       Instant startedAt, Instant completedAt, String reason, String diagnosis,
                       String notes, Instant createdAt, Instant updatedAt) {
    }
}
