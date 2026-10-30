package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "staff")
public class StaffEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private ClinicEntity clinic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "license_no", length = 80)
    private String licenseNo;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private boolean active;

    protected StaffEntity() {
    }

    public StaffEntity(String tenantId, ClinicEntity clinic, StaffRole role,
                       String fullName, String licenseNo, boolean active) {
        super(tenantId);
        this.clinic = clinic;
        this.role = role;
        this.fullName = fullName;
        this.licenseNo = licenseNo;
        this.active = active;
    }

    public ClinicEntity getClinic() { return clinic; }
    public StaffRole getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getLicenseNo() { return licenseNo; }
    public boolean isActive() { return active; }

    public void update(ClinicEntity clinic, StaffRole role, String fullName, String licenseNo, boolean active) {
        this.clinic = clinic;
        this.role = role;
        this.fullName = fullName;
        this.licenseNo = licenseNo;
        this.active = active;
    }

    public enum StaffRole {
        VET, NURSE, RECEPTION, ADMIN
    }
}
