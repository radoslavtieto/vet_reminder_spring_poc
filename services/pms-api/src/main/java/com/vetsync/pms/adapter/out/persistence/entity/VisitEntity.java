package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "visit")
public class VisitEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private ClinicEntity clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vet_id", nullable = false)
    private StaffEntity vet;

    @Column(name = "visit_type", nullable = false, length = 40)
    private String visitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(length = 500)
    private String reason;

    @Column(length = 500)
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected VisitEntity() {
    }

    public VisitEntity(String tenantId, PatientEntity patient, ClinicEntity clinic, StaffEntity vet,
                       String visitType, VisitStatus status, Instant scheduledAt, Instant startedAt,
                       Instant completedAt, String reason, String diagnosis, String notes) {
        super(tenantId);
        this.patient = patient;
        this.clinic = clinic;
        this.vet = vet;
        this.visitType = visitType;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.reason = reason;
        this.diagnosis = diagnosis;
        this.notes = notes;
    }

    public PatientEntity getPatient() { return patient; }
    public ClinicEntity getClinic() { return clinic; }
    public StaffEntity getVet() { return vet; }
    public String getVisitType() { return visitType; }
    public VisitStatus getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getReason() { return reason; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }

    public void update(PatientEntity patient, ClinicEntity clinic, StaffEntity vet, String visitType,
                       VisitStatus status, Instant scheduledAt, Instant startedAt, Instant completedAt,
                       String reason, String diagnosis, String notes) {
        this.patient = patient;
        this.clinic = clinic;
        this.vet = vet;
        this.visitType = visitType;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.reason = reason;
        this.diagnosis = diagnosis;
        this.notes = notes;
    }

    public void transitionTo(VisitStatus status, Instant transitionedAt) {
        this.status = status;
        if (status == VisitStatus.IN_PROGRESS && startedAt == null) {
            this.startedAt = transitionedAt;
        } else if (status == VisitStatus.COMPLETED) {
            this.completedAt = transitionedAt;
        }
    }

    public enum VisitStatus {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
