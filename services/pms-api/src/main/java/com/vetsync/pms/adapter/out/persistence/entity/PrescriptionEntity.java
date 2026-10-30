package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "prescription")
public class PrescriptionEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private VisitEntity visit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vet_id", nullable = false)
    private StaffEntity vet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected PrescriptionEntity() {
    }

    public PrescriptionEntity(String tenantId, PatientEntity patient, VisitEntity visit, StaffEntity vet,
                              PrescriptionStatus status, LocalDate issuedOn, LocalDate validUntil,
                              String notes) {
        super(tenantId);
        this.patient = patient;
        this.visit = visit;
        this.vet = vet;
        this.status = status;
        this.issuedOn = issuedOn;
        this.validUntil = validUntil;
        this.notes = notes;
    }

    public PatientEntity getPatient() { return patient; }
    public VisitEntity getVisit() { return visit; }
    public StaffEntity getVet() { return vet; }
    public PrescriptionStatus getStatus() { return status; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getNotes() { return notes; }

    public void update(PatientEntity patient, VisitEntity visit, StaffEntity vet, PrescriptionStatus status,
                       LocalDate issuedOn, LocalDate validUntil, String notes) {
        this.patient = patient;
        this.visit = visit;
        this.vet = vet;
        this.status = status;
        this.issuedOn = issuedOn;
        this.validUntil = validUntil;
        this.notes = notes;
    }

    public enum PrescriptionStatus {
        ACTIVE, EXPIRED, CANCELLED, FULFILLED
    }
}
