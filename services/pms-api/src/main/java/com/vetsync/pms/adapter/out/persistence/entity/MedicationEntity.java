package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "medication")
public class MedicationEntity extends BusinessEntity {

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String form;

    @Column(nullable = false, length = 80)
    private String strength;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private boolean controlled;

    protected MedicationEntity() {
    }

    public MedicationEntity(String tenantId, String code, String name, String form,
                            String strength, boolean controlled) {
        super(tenantId);
        this.code = code;
        this.name = name;
        this.form = form;
        this.strength = strength;
        this.controlled = controlled;
    }
}
