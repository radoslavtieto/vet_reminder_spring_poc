package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity;

import java.util.List;

public interface PaymentRepository extends TenantBusinessRepository<PaymentEntity> {
    List<PaymentEntity> findAllByTenantIdAndInvoice_IdOrderByPaidAtDesc(String tenantId, Long invoiceId);
}
