package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.MedicationEntity;

import java.util.Optional;

public interface MedicationRepository extends TenantBusinessRepository<MedicationEntity> {
    Optional<MedicationEntity> findByTenantIdAndCode(String tenantId, String code);
}
