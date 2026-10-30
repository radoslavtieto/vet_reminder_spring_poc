package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.InvoiceLineEntity;

import java.util.List;

public interface InvoiceLineRepository extends TenantBusinessRepository<InvoiceLineEntity> {
    List<InvoiceLineEntity> findAllByTenantIdAndInvoice_Id(String tenantId, Long invoiceId);
}
