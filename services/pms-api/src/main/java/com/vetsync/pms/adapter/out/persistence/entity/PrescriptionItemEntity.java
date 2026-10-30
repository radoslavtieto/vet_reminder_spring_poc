package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "prescription_item")
public class PrescriptionItemEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private PrescriptionEntity prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private MedicationEntity medication;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 100)
    private String frequency;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "refills_allowed", nullable = false)
    private int refillsAllowed;

    protected PrescriptionItemEntity() {
    }

    public PrescriptionItemEntity(String tenantId, PrescriptionEntity prescription, MedicationEntity medication,
                                  String dosage, String frequency, int durationDays,
                                  BigDecimal quantity, int refillsAllowed) {
        super(tenantId);
        this.prescription = prescription;
        this.medication = medication;
        this.dosage = dosage;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.quantity = quantity;
        this.refillsAllowed = refillsAllowed;
    }

    public PrescriptionEntity getPrescription() { return prescription; }
    public MedicationEntity getMedication() { return medication; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public int getDurationDays() { return durationDays; }
    public BigDecimal getQuantity() { return quantity; }
    public int getRefillsAllowed() { return refillsAllowed; }
}
