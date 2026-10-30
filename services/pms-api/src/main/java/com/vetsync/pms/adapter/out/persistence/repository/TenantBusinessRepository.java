package com.vetsync.pms.adapter.out.persistence.repository;

import com.vetsync.pms.adapter.out.persistence.entity.BusinessEntity;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantBusinessRepository<T extends BusinessEntity> extends Repository<T, Long> {
    <S extends T> S save(S entity);

    <S extends T> S saveAndFlush(S entity);

    List<T> findAllByTenantId(String tenantId);

    Optional<T> findByTenantIdAndId(String tenantId, Long id);

    Optional<T> findByTenantIdAndPublicId(String tenantId, String publicId);
}
