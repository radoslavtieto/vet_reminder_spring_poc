package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.PatientEntity;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends TenantBusinessRepository<PatientEntity> {
    Optional<PatientEntity> findByTenantIdAndMicrochipNo(String tenantId, String microchipNo);

    List<PatientEntity> findAllByTenantIdAndClient_Id(String tenantId, Long clientId);

    List<PatientEntity> findAllByTenantIdAndClinic_Id(String tenantId, Long clinicId);
}
