package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PmsUserRepository extends JpaRepository<PmsUserEntity, Long> {
    Optional<PmsUserEntity> findByTenantIdAndUsername(String tenantId, String username);
    boolean existsByTenantId(String tenantId);
    boolean existsByTenantIdAndUsername(String tenantId, String username);
}
