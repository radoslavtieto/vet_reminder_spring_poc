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
@Table(name = "medical_record_entry")
public class MedicalRecordEntryEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private VisitEntity visit;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, length = 160)
    private String recordedBy;

    protected MedicalRecordEntryEntity() {
    }

    public MedicalRecordEntryEntity(String tenantId, PatientEntity patient, VisitEntity visit,
                                    EntryType entryType, String description, Instant recordedAt,
                                    String recordedBy) {
        super(tenantId);
        this.patient = patient;
        this.visit = visit;
        this.entryType = entryType;
        this.description = description;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
    }

    public PatientEntity getPatient() { return patient; }
    public VisitEntity getVisit() { return visit; }
    public EntryType getEntryType() { return entryType; }
    public String getDescription() { return description; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }

    public enum EntryType {
        DIAGNOSIS, VACCINATION, ALLERGY, PROCEDURE, NOTE
    }
}
