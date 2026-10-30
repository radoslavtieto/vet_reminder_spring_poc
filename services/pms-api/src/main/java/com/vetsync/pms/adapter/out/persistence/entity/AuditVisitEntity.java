package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_visit")
public class AuditVisitEntity extends AuditEntry {
    protected AuditVisitEntity() {
    }
}
