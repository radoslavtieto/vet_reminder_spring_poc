package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.BreedEntity;

import java.util.List;
import java.util.Optional;

public interface BreedRepository extends TenantBusinessRepository<BreedEntity> {
    List<BreedEntity> findAllByTenantIdAndSpecies_Id(String tenantId, Long speciesId);

    Optional<BreedEntity> findByTenantIdAndSpecies_IdAndCode(String tenantId, Long speciesId, String code);
}
