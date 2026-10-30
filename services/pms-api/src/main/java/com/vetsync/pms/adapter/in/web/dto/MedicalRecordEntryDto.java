package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class MedicalRecordEntryDto {

    private MedicalRecordEntryDto() {
    }

    public record HistoryRequest(
            @NotBlank String entryType,
            @NotBlank String description,
            @Positive Long visitId,
            @NotNull Instant recordedAt,
            @NotBlank @Size(max = 160) String recordedBy) {
    }

    public record HistoryView(long id, String publicId, String tenant, long patientId, Long visitId,
                       String entryType, String description, Instant recordedAt, String recordedBy) {
    }
}
