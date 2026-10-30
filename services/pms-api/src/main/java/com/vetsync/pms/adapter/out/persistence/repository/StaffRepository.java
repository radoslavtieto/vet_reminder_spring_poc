package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;

import java.util.List;

public interface StaffRepository extends TenantBusinessRepository<StaffEntity> {
    List<StaffEntity> findAllByTenantIdAndClinic_Id(String tenantId, Long clinicId);
}
