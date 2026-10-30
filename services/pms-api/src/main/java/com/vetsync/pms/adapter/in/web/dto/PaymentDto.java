package com.vetsync.pms.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDto {

    private PaymentDto() {
    }

    public record PaymentRequest(
            @NotNull Instant paidAt,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank String method) {
    }

    public record PaymentView(long id, String publicId, String tenant, long invoiceId,
                       Instant paidAt, BigDecimal amount, String method) {
    }
}
