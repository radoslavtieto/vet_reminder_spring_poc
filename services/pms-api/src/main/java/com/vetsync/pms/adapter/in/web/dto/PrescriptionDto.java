package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDate;

public final class PrescriptionDto {

    private PrescriptionDto() {
    }

    public record PrescriptionRequest(
            @NotNull @Positive Long patientId,
            @Positive Long visitId,
            @NotNull @Positive Long vetId,
            String status,
            @NotNull LocalDate issuedOn,
            @NotNull LocalDate validUntil,
            String notes) {
    }

    public record PrescriptionView(long id, String publicId, String tenant, long patientId,
                       long visitId, long vetId, String status, LocalDate issuedOn,
                       LocalDate validUntil, String notes, Instant createdAt, Instant updatedAt) {
    }
}
