package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.SpeciesEntity;
import com.vetsync.pms.adapter.out.persistence.repository.BreedRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicationRepository;
import com.vetsync.pms.adapter.out.persistence.repository.SpeciesRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.BreedView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.MedicationView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.SpeciesView;
import static com.vetsync.pms.application.PmsApplicationSupport.*;

@Service
@Transactional
class PmsCatalogApplicationService {
    private final SpeciesRepository species;
    private final BreedRepository breeds;
    private final MedicationRepository medications;

    PmsCatalogApplicationService(SpeciesRepository species, BreedRepository breeds,
                                 MedicationRepository medications) {
        this.species = species;
        this.breeds = breeds;
        this.medications = medications;
    }

    // Species
    public List<SpeciesView> species(String tenant) {
        return active(species.findAllByTenantId(tenant)).stream()
                .map(entity -> new SpeciesView(entity.getId(), entity.getPublicId(),
                        entity.getTenantId(), entity.getCode(), entity.getName()))
                .toList();
    }

    // Breeds
    public List<BreedView> breeds(String tenant, long speciesId) {
        speciesEntity(tenant, speciesId);
        return active(breeds.findAllByTenantIdAndSpecies_Id(tenant, speciesId)).stream()
                .map(entity -> new BreedView(entity.getId(), entity.getPublicId(), entity.getTenantId(),
                        entity.getSpecies().getId(), entity.getCode(), entity.getName()))
                .toList();
    }

    // Medications
    public List<MedicationView> medications(String tenant) {
        return active(medications.findAllByTenantId(tenant)).stream()
                .map(entity -> new MedicationView(entity.getId(), entity.getPublicId(), entity.getTenantId(),
                        entity.getCode(), entity.getName(), entity.getForm(), entity.getStrength(),
                        entity.isControlled()))
                .toList();
    }

    private SpeciesEntity speciesEntity(String tenant, long id) {
        return find(species.findByTenantIdAndId(tenant, id));
    }
}
