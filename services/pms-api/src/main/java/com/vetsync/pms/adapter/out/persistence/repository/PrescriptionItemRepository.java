package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionItemEntity;

import java.util.List;

public interface PrescriptionItemRepository extends TenantBusinessRepository<PrescriptionItemEntity> {
    List<PrescriptionItemEntity> findAllByTenantIdAndPrescription_Id(String tenantId, Long prescriptionId);
}
