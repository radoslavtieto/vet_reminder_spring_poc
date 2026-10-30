package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;

import java.util.List;

public interface ClinicRepository extends TenantBusinessRepository<ClinicEntity> {
    List<ClinicEntity> findAllByTenantIdAndActiveTrue(String tenantId);
}
