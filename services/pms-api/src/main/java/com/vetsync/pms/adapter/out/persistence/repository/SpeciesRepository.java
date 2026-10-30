package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.SpeciesEntity;

import java.util.Optional;

public interface SpeciesRepository extends TenantBusinessRepository<SpeciesEntity> {
    Optional<SpeciesEntity> findByTenantIdAndCode(String tenantId, String code);
}
