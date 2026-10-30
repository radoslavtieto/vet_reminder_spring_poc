package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "breed")
public class BreedEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private SpeciesEntity species;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    protected BreedEntity() {
    }

    public BreedEntity(String tenantId, SpeciesEntity species, String code, String name) {
        super(tenantId);
        this.species = species;
        this.code = code;
        this.name = name;
    }

    public SpeciesEntity getSpecies() { return species; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
