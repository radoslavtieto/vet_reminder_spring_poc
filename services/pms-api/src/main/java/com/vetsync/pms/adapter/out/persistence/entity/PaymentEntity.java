package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class PaymentEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    protected PaymentEntity() {
    }

    public PaymentEntity(String tenantId, InvoiceEntity invoice, Instant paidAt,
                         BigDecimal amount, PaymentMethod method) {
        super(tenantId);
        this.invoice = invoice;
        this.paidAt = paidAt;
        this.amount = amount;
        this.method = method;
    }

    public InvoiceEntity getInvoice() { return invoice; }
    public Instant getPaidAt() { return paidAt; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }

    public enum PaymentMethod {
        CASH, CARD, BANK_TRANSFER
    }
}
