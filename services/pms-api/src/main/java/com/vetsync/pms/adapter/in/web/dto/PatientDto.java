package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PatientDto {

    private PatientDto() {
    }

    public record PatientRequest(
            @Positive Long clientId,
            @NotNull @Positive Long clinicId,
            @NotBlank @Size(max = 100) String name,
            @NotNull @Positive Long speciesId,
            @Positive Long breedId,
            String sex,
            LocalDate birthDate,
            @Size(max = 32) String microchipNo,
            @NotNull Boolean neutered,
            @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
            String status,
            Instant deceasedAt) {
    }

    public record PatientView(long id, String publicId, String tenant, long clientId, long clinicId,
                       String name, long speciesId, Long breedId, String sex, LocalDate birthDate,
                       String microchipNo, boolean neutered, BigDecimal weightKg,
                       String status, Instant deceasedAt, Instant createdAt, Instant updatedAt) {
    }

    public record ClinicTransferRequest(
            @NotNull @Positive Long clinicId) {
    }
}
