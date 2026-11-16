package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.MedicationEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PatientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionItemEntity;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;
import com.vetsync.pms.adapter.out.persistence.repository.MedicationRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PatientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionItemRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import com.vetsync.pms.adapter.out.persistence.repository.VisitRepository;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionRequest;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionView;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemRequest;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemView;
import static com.vetsync.pms.application.PmsApplicationSupport.*;

@Service
@Transactional
class PmsPrescriptionApplicationService {
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository prescriptionItems;
    private final MedicationRepository medications;
    private final PatientRepository patients;
    private final VisitRepository visits;
    private final StaffRepository staff;
    private final Clock clock;

    PmsPrescriptionApplicationService(PrescriptionRepository prescriptions,
                                      PrescriptionItemRepository prescriptionItems,
                                      MedicationRepository medications, PatientRepository patients,
                                      VisitRepository visits, StaffRepository staff, Clock clock) {
        this.prescriptions = prescriptions;
        this.prescriptionItems = prescriptionItems;
        this.medications = medications;
        this.patients = patients;
        this.visits = visits;
        this.staff = staff;
        this.clock = clock;
    }

    // Prescriptions
    public List<PrescriptionView> prescriptions(String tenant) {
        return active(prescriptions.findAllByTenantId(tenant)).stream().map(PmsApplicationMapper::view).toList();
    }

    public PrescriptionView prescription(String tenant, long id) {
        return PmsApplicationMapper.view(prescriptionEntity(tenant, id));
    }
    public List<PrescriptionView> prescriptionsForVisit(String tenant, long visitId) {
        visitEntity(tenant, visitId);
        return active(prescriptions.findAllByTenantIdAndVisit_Id(tenant, visitId)).stream()
                .map(PmsApplicationMapper::view).toList();
    }

    public PrescriptionView createPrescriptionForVisit(String tenant, long visitId, PrescriptionRequest request) {
        visitEntity(tenant, visitId);
        if (request.visitId() != null && request.visitId() != visitId) throw badRequest("Visit does not match path");
        PrescriptionRequest bound = new PrescriptionRequest(request.patientId(), visitId, request.vetId(),
                request.status(), request.issuedOn(), request.validUntil(), request.notes());
        return createPrescription(tenant, bound);
    }

    public PrescriptionView createPrescription(String tenant, PrescriptionRequest request) {
        if (request.visitId() == null) throw badRequest("visitId is required");
        return PmsApplicationMapper.view(prescriptions.save(prescriptionFrom(tenant, request)));
    }

    public PrescriptionView updatePrescription(String tenant, long id, PrescriptionRequest request) {
        if (request.visitId() == null) throw badRequest("visitId is required");
        PrescriptionEntity entity = prescriptionEntity(tenant, id);
        entity.update(patientEntity(tenant, request.patientId()), visitEntity(tenant, request.visitId()),
                staffEntity(tenant, request.vetId()),
                enumValue(PrescriptionEntity.PrescriptionStatus.class, defaultValue(request.status(), "ACTIVE")),
                request.issuedOn(), request.validUntil(), request.notes());
        verifyPrescriptionLinks(entity);
        if (request.validUntil().isBefore(request.issuedOn())) {
            throw badRequest("validUntil must not precede issuedOn");
        }
        return PmsApplicationMapper.view(prescriptions.saveAndFlush(entity));
    }

    public void deletePrescription(String tenant, long id) {
        PmsApplicationSupport.softDelete(prescriptionEntity(tenant, id), clock);
    }

    // Prescription items
    public List<PrescriptionItemView> prescriptionItems(String tenant, long prescriptionId) {
        prescriptionEntity(tenant, prescriptionId);
        return active(prescriptionItems.findAllByTenantIdAndPrescription_Id(tenant, prescriptionId))
                .stream().map(PmsApplicationMapper::view).toList();
    }

    public PrescriptionItemView addPrescriptionItem(String tenant, long prescriptionId,
                                                     PrescriptionItemRequest request) {
        PrescriptionEntity prescription = prescriptionEntity(tenant, prescriptionId);
        MedicationEntity medication = find(medications.findByTenantIdAndId(tenant, request.medicationId()));
        if (medication.isControlled()
                && (prescription.getVet().getRole() != StaffEntity.StaffRole.VET
                || prescription.getVet().getLicenseNo() == null
                || prescription.getVet().getLicenseNo().isBlank())) {
            throw badRequest("Controlled medication requires a licensed vet");
        }
        PrescriptionItemEntity item = new PrescriptionItemEntity(tenant, prescription, medication,
                request.dosage(), request.frequency(), request.durationDays(),
                request.quantity(), request.refillsAllowed());
        return PmsApplicationMapper.view(prescriptionItems.save(item));
    }

    private PrescriptionEntity prescriptionFrom(String tenant, PrescriptionRequest request) {
        PrescriptionEntity entity = new PrescriptionEntity(tenant, patientEntity(tenant, request.patientId()),
                visitEntity(tenant, request.visitId()), staffEntity(tenant, request.vetId()),
                enumValue(PrescriptionEntity.PrescriptionStatus.class, defaultValue(request.status(), "ACTIVE")),
                request.issuedOn(), request.validUntil(), request.notes());
        verifyPrescriptionLinks(entity);
        if (request.validUntil().isBefore(request.issuedOn())) {
            throw badRequest("validUntil must not precede issuedOn");
        }
        return entity;
    }

    private void verifyPrescriptionLinks(PrescriptionEntity prescription) {
        if (!Objects.equals(prescription.getPatient().getId(), prescription.getVisit().getPatient().getId())
                || !Objects.equals(prescription.getVet().getId(), prescription.getVisit().getVet().getId())) {
            throw missing("Prescription patient and vet must match its visit");
        }
    }

    private PatientEntity patientEntity(String tenant, long id) {
        return find(patients.findByTenantIdAndId(tenant, id));
    }
    private VisitEntity visitEntity(String tenant, long id) { return find(visits.findByTenantIdAndId(tenant, id)); }
    private StaffEntity staffEntity(String tenant, long id) { return find(staff.findByTenantIdAndId(tenant, id)); }
    private PrescriptionEntity prescriptionEntity(String tenant, long id) {
        return find(prescriptions.findByTenantIdAndId(tenant, id));
    }
}
