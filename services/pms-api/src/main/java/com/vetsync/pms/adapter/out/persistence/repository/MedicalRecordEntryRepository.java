package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.MedicalRecordEntryEntity;

import java.util.List;

public interface MedicalRecordEntryRepository extends TenantBusinessRepository<MedicalRecordEntryEntity> {
    List<MedicalRecordEntryEntity> findAllByTenantIdAndPatient_IdOrderByRecordedAtDesc(
            String tenantId, Long patientId);
}
