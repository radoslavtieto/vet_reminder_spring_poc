package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class PrescriptionItemDto {

    private PrescriptionItemDto() {
    }

    public record PrescriptionItemRequest(
            @NotNull @Positive Long medicationId,
            @NotBlank @Size(max = 100) String dosage,
            @NotBlank @Size(max = 100) String frequency,
            @Positive int durationDays,
            @NotNull @Positive BigDecimal quantity,
            @PositiveOrZero int refillsAllowed) {
    }

    public record PrescriptionItemView(
            long id,
            String publicId,
            String tenant,
            long prescriptionId,
            long medicationId,
            String medicationCode,
            String medicationName,
            String dosage,
            String frequency,
            int durationDays,
            BigDecimal quantity,
            int refillsAllowed) {
    }
}
