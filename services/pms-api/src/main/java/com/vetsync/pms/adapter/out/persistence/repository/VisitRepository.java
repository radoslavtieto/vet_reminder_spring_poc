package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;

import java.time.Instant;
import java.util.List;

public interface VisitRepository extends TenantBusinessRepository<VisitEntity> {
    List<VisitEntity> findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc(String tenantId, Long patientId);

    List<VisitEntity> findAllByTenantIdAndClinic_IdAndScheduledAtBetween(
            String tenantId, Long clinicId, Instant start, Instant end);
}
