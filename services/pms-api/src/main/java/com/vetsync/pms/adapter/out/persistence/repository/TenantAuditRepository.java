package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.AuditEntry;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.Instant;
import java.util.List;

@NoRepositoryBean
public interface TenantAuditRepository<T extends AuditEntry> extends Repository<T, Long> {
    List<T> findAllByTenantIdAndChangedAtGreaterThanEqualOrderByChangedAtAscAuditIdAsc(
            String tenantId, Instant changedAt);
}
