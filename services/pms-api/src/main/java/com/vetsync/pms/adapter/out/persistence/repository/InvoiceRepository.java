package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.InvoiceEntity;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends TenantBusinessRepository<InvoiceEntity> {
    Optional<InvoiceEntity> findByTenantIdAndClinic_IdAndInvoiceNo(
            String tenantId, Long clinicId, String invoiceNo);

    List<InvoiceEntity> findAllByTenantIdAndClient_Id(String tenantId, Long clientId);
}
