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

public final class InvoiceDto {

    private InvoiceDto() {
    }

    public record InvoiceRequest(
            @NotNull @Positive Long clinicId,
            @NotNull @Positive Long clientId,
            @Positive Long visitId,
            @NotBlank @Size(max = 60) String invoiceNo,
            String status,
            @NotNull LocalDate issuedOn,
            @NotNull LocalDate dueOn,
            @Size(min = 3, max = 3) String currency,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal paidAmount) {
    }

    public record InvoiceView(long id, String publicId, String tenant, long clinicId, long clientId,
                       Long visitId, String invoiceNo, String status, LocalDate issuedOn,
                       LocalDate dueOn, String currency, BigDecimal totalAmount,
                       BigDecimal paidAmount, Instant createdAt, Instant updatedAt) {
    }
}
