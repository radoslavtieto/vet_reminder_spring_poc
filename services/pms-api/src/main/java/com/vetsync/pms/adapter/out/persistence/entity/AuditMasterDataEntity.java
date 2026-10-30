package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_master_data")
public class AuditMasterDataEntity extends AuditEntry {
    protected AuditMasterDataEntity() {
    }
}
