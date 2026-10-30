package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clinic")
public class ClinicEntity extends BusinessEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private boolean active;

    protected ClinicEntity() {
    }

    public ClinicEntity(String tenantId, String name, String city, String address,
                        String phone, String email, boolean active) {
        super(tenantId);
        this.name = name;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public boolean isActive() {
        return active;
    }

    public void update(String name, String city, String address, String phone, String email, boolean active) {
        this.name = name;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }
}
