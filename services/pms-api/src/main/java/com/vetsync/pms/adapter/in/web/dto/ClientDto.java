package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ClientDto {

    private ClientDto() {
    }

    public record ClientRequest(
            @NotNull @Positive Long registeredClinicId,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Email @Size(max = 254) String email,
            @Size(max = 32) String phone,
            @Size(max = 255) String address,
            @Size(max = 100) String city,
            @Size(max = 20) String preferredContact,
            Instant gdprConsentAt) {
    }

    public record ClientView(long id, String publicId, String tenant, long registeredClinicId,
                       String firstName, String lastName, String email, String phone,
                       String address, String city, String preferredContact,
                       Instant gdprConsentAt, Instant createdAt, Instant updatedAt) {
    }
}
