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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patient")
public class PatientEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private ClinicEntity clinic;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private BreedEntity breed;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Sex sex;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "microchip_no", length = 32)
    private String microchipNo;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private boolean neutered;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatientStatus status;

    @Column(name = "deceased_at")
    private Instant deceasedAt;

    protected PatientEntity() {
    }

    public PatientEntity(String tenantId, ClientEntity client, ClinicEntity clinic, String name,
                         SpeciesEntity species, BreedEntity breed, Sex sex, LocalDate birthDate,
                         String microchipNo, boolean neutered, BigDecimal weightKg,
                         PatientStatus status, Instant deceasedAt) {
        super(tenantId);
        this.client = client;
        this.clinic = clinic;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.birthDate = birthDate;
        this.microchipNo = microchipNo;
        this.neutered = neutered;
        this.weightKg = weightKg;
        this.status = status;
        this.deceasedAt = deceasedAt;
    }

    public ClientEntity getClient() { return client; }
    public ClinicEntity getClinic() { return clinic; }
    public String getName() { return name; }
    public SpeciesEntity getSpecies() { return species; }
    public BreedEntity getBreed() { return breed; }
    public Sex getSex() { return sex; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getMicrochipNo() { return microchipNo; }
    public boolean isNeutered() { return neutered; }
    public BigDecimal getWeightKg() { return weightKg; }
    public PatientStatus getStatus() { return status; }
    public Instant getDeceasedAt() { return deceasedAt; }

    public void update(ClientEntity client, ClinicEntity clinic, String name, SpeciesEntity species,
                       BreedEntity breed, Sex sex, LocalDate birthDate, String microchipNo,
                       boolean neutered, BigDecimal weightKg, PatientStatus status, Instant deceasedAt) {
        this.client = client;
        this.clinic = clinic;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.birthDate = birthDate;
        this.microchipNo = microchipNo;
        this.neutered = neutered;
        this.weightKg = weightKg;
        this.status = status;
        this.deceasedAt = deceasedAt;
    }

    public void transferToClinic(ClinicEntity clinic) {
        this.clinic = clinic;
    }

    public enum Sex {
        MALE, FEMALE, UNKNOWN
    }

    public enum PatientStatus {
        ACTIVE, DECEASED, TRANSFERRED
    }
}
