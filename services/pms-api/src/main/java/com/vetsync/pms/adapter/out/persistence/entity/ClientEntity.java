package com.vetsync.pms.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "client")
public class ClientEntity extends BusinessEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_clinic_id", nullable = false)
    private ClinicEntity registeredClinic;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "preferred_contact", length = 20)
    private String preferredContact;

    @Column(name = "gdpr_consent_at")
    private Instant gdprConsentAt;

    protected ClientEntity() {
    }

    public ClientEntity(String tenantId, ClinicEntity registeredClinic, String firstName,
                        String lastName, String email, String phone, String address,
                        String city, String preferredContact, Instant gdprConsentAt) {
        super(tenantId);
        this.registeredClinic = registeredClinic;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.preferredContact = preferredContact;
        this.gdprConsentAt = gdprConsentAt;
    }

    public ClinicEntity getRegisteredClinic() { return registeredClinic; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getPreferredContact() { return preferredContact; }
    public Instant getGdprConsentAt() { return gdprConsentAt; }

    public void update(ClinicEntity registeredClinic, String firstName, String lastName, String email,
                       String phone, String address, String city, String preferredContact,
                       Instant gdprConsentAt) {
        this.registeredClinic = registeredClinic;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.preferredContact = preferredContact;
        this.gdprConsentAt = gdprConsentAt;
    }
}
