package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
public class InvoiceEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private ClinicEntity clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private VisitEntity visit;

    @Column(name = "invoice_no", nullable = false, length = 60)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    protected InvoiceEntity() {
    }

    public InvoiceEntity(String tenantId, ClinicEntity clinic, ClientEntity client, VisitEntity visit,
                         String invoiceNo, InvoiceStatus status, LocalDate issuedOn, LocalDate dueOn,
                         String currency, BigDecimal totalAmount, BigDecimal paidAmount) {
        super(tenantId);
        this.clinic = clinic;
        this.client = client;
        this.visit = visit;
        this.invoiceNo = invoiceNo;
        this.status = status;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
    }

    public ClinicEntity getClinic() { return clinic; }
    public ClientEntity getClient() { return client; }
    public VisitEntity getVisit() { return visit; }
    public String getInvoiceNo() { return invoiceNo; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public LocalDate getDueOn() { return dueOn; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }

    public void update(ClinicEntity clinic, ClientEntity client, VisitEntity visit, String invoiceNo,
                       InvoiceStatus status, LocalDate issuedOn, LocalDate dueOn, String currency,
                       BigDecimal totalAmount, BigDecimal paidAmount) {
        this.clinic = clinic;
        this.client = client;
        this.visit = visit;
        this.invoiceNo = invoiceNo;
        this.status = status;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
    }

    public enum InvoiceStatus {
        DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOID
    }
}
