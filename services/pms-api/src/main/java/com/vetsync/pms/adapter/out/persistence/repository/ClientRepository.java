package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;

import java.util.List;

public interface ClientRepository extends TenantBusinessRepository<ClientEntity> {
    List<ClientEntity> findAllByTenantIdAndRegisteredClinic_Id(String tenantId, Long clinicId);

    List<ClientEntity> findAllByTenantIdAndPhone(String tenantId, String phone);
}
