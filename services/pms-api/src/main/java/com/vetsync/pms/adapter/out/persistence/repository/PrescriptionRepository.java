package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionEntity;

import java.util.List;

public interface PrescriptionRepository extends TenantBusinessRepository<PrescriptionEntity> {
    List<PrescriptionEntity> findAllByTenantIdAndPatient_Id(String tenantId, Long patientId);

    List<PrescriptionEntity> findAllByTenantIdAndVisit_Id(String tenantId, Long visitId);
}
