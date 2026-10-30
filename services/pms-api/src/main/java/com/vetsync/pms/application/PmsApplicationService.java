package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.BreedEntity;
import com.vetsync.pms.adapter.out.persistence.entity.BusinessEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.InvoiceEntity;
import com.vetsync.pms.adapter.out.persistence.entity.MedicationEntity;
import com.vetsync.pms.adapter.out.persistence.entity.MedicalRecordEntryEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PatientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionItemEntity;
import com.vetsync.pms.adapter.out.persistence.entity.SpeciesEntity;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;
import com.vetsync.pms.adapter.out.persistence.repository.BreedRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.InvoiceRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicationRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicalRecordEntryRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PatientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PaymentRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionItemRepository;
import com.vetsync.pms.adapter.out.persistence.repository.SpeciesRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import com.vetsync.pms.adapter.out.persistence.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientRequest;
import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.BreedView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.MedicationView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.SpeciesView;
import static com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicRequest;
import static com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicView;
import static com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceRequest;
import static com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceView;
import static com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryRequest;
import static com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryView;
import static com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientRequest;
import static com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientView;
import static com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentRequest;
import static com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentView;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionRequest;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionView;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemRequest;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemView;
import static com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffRequest;
import static com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffView;
import static com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitRequest;
import static com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitView;

/**
 * Tenant-aware orchestration for the PMS HTTP adapter. All persistence access is scoped by tenant.
 */
@Service
@Transactional
public class PmsApplicationService {

    private final ClinicRepository clinics;
    private final StaffRepository staff;
    private final ClientRepository clients;
    private final PatientRepository patients;
    private final VisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final InvoiceRepository invoices;
    private final MedicationRepository medications;
    private final PaymentRepository payments;
    private final MedicalRecordEntryRepository history;
    private final PrescriptionItemRepository prescriptionItems;
    private final SpeciesRepository species;
    private final BreedRepository breeds;
    private final Clock clock;

    public PmsApplicationService(ClinicRepository clinics, StaffRepository staff,
                                 ClientRepository clients, PatientRepository patients,
                                 VisitRepository visits, PrescriptionRepository prescriptions,
                                 InvoiceRepository invoices, MedicationRepository medications,
                                 PaymentRepository payments, MedicalRecordEntryRepository history,
                                 PrescriptionItemRepository prescriptionItems, SpeciesRepository species,
                                 BreedRepository breeds, Clock clock) {
        this.clinics = clinics;
        this.staff = staff;
        this.clients = clients;
        this.patients = patients;
        this.visits = visits;
        this.prescriptions = prescriptions;
        this.invoices = invoices;
        this.medications = medications;
        this.payments = payments;
        this.history = history;
        this.prescriptionItems = prescriptionItems;
        this.species = species;
        this.breeds = breeds;
        this.clock = clock;
    }

    public String tenant(String region) {
        return TenantResolver.resolve(region);
    }

    public List<SpeciesView> species(String tenant) {
        return active(species.findAllByTenantId(tenant)).stream()
                .map(entity -> new SpeciesView(entity.getId(), entity.getPublicId(),
                        entity.getTenantId(), entity.getCode(), entity.getName()))
                .toList();
    }

    public List<BreedView> breeds(String tenant, long speciesId) {
        speciesEntity(tenant, speciesId);
        return active(breeds.findAllByTenantIdAndSpecies_Id(tenant, speciesId)).stream()
                .map(entity -> new BreedView(entity.getId(), entity.getPublicId(), entity.getTenantId(),
                        entity.getSpecies().getId(), entity.getCode(), entity.getName()))
                .toList();
    }

    public List<MedicationView> medications(String tenant) {
        return active(medications.findAllByTenantId(tenant)).stream()
                .map(entity -> new MedicationView(entity.getId(), entity.getPublicId(), entity.getTenantId(),
                        entity.getCode(), entity.getName(), entity.getForm(), entity.getStrength(),
                        entity.isControlled()))
                .toList();
    }

    public List<ClinicView> clinics(String tenant) {
        return active(clinics.findAllByTenantId(tenant)).stream().map(this::view).toList();
    }

    public ClinicView clinic(String tenant, long id) { return view(clinicEntity(tenant, id)); }

    public ClinicView createClinic(String tenant, ClinicRequest request) {
        return view(clinics.save(new ClinicEntity(tenant, request.name(), request.city(), request.address(),
                request.phone(), request.email(), request.active())));
    }

    public ClinicView updateClinic(String tenant, long id, ClinicRequest request) {
        ClinicEntity entity = clinicEntity(tenant, id);
        entity.update(request.name(), request.city(), request.address(), request.phone(), request.email(),
                request.active());
        return view(clinics.saveAndFlush(entity));
    }

    public void deleteClinic(String tenant, long id) { softDelete(clinicEntity(tenant, id)); }

    public List<StaffView> staffForClinic(String tenant, long clinicId) {
        clinicEntity(tenant, clinicId);
        return active(staff.findAllByTenantIdAndClinic_Id(tenant, clinicId)).stream().map(this::view).toList();
    }

    public List<StaffView> staff(String tenant) {
        return active(staff.findAllByTenantId(tenant)).stream().map(this::view).toList();
    }

    public StaffView staff(String tenant, long id) { return view(staffEntity(tenant, id)); }

    public StaffView createStaff(String tenant, long clinicId, StaffRequest request) {
        ClinicEntity clinic = clinicEntity(tenant, clinicId);
        if (request.clinicId() != null && request.clinicId() != clinicId) {
            throw badRequest("Clinic does not match path");
        }
        return view(staff.save(new StaffEntity(tenant, clinic, enumValue(StaffEntity.StaffRole.class, request.role()),
                request.fullName(), request.licenseNo(), request.active())));
    }

    public StaffView createStaff(String tenant, StaffRequest request) {
        if (request.clinicId() == null) throw badRequest("clinicId is required");
        return createStaff(tenant, request.clinicId(), request);
    }

    public StaffView updateStaff(String tenant, long id, StaffRequest request) {
        StaffEntity entity = staffEntity(tenant, id);
        long clinicId = request.clinicId() == null ? entity.getClinic().getId() : request.clinicId();
        entity.update(clinicEntity(tenant, clinicId),
                enumValue(StaffEntity.StaffRole.class, request.role()), request.fullName(),
                request.licenseNo(), request.active());
        return view(staff.saveAndFlush(entity));
    }

    public void deleteStaff(String tenant, long id) { softDelete(staffEntity(tenant, id)); }

    public List<ClientView> clients(String tenant, String query) {
        String needle = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        return active(clients.findAllByTenantId(tenant)).stream()
                .filter(c -> needle == null || needle.isEmpty()
                        || (c.getFirstName() + " " + c.getLastName()).toLowerCase(Locale.ROOT).contains(needle)
                        || contains(c.getEmail(), needle) || contains(c.getPhone(), needle))
                .map(this::view).toList();
    }

    public ClientView client(String tenant, long id) { return view(clientEntity(tenant, id)); }

    public ClientView createClient(String tenant, ClientRequest request) {
        ClinicEntity clinic = clinicEntity(tenant, request.registeredClinicId());
        return view(clients.save(new ClientEntity(tenant, clinic, request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), request.city(), request.preferredContact(),
                request.gdprConsentAt())));
    }

    public ClientView updateClient(String tenant, long id, ClientRequest request) {
        ClientEntity entity = clientEntity(tenant, id);
        entity.update(clinicEntity(tenant, request.registeredClinicId()), request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), request.city(), request.preferredContact(),
                request.gdprConsentAt());
        return view(clients.saveAndFlush(entity));
    }

    public void deleteClient(String tenant, long id) { softDelete(clientEntity(tenant, id)); }

    public List<PatientView> patients(String tenant, String microchip) {
        List<PatientEntity> result = microchip == null || microchip.isBlank()
                ? patients.findAllByTenantId(tenant)
                : patients.findByTenantIdAndMicrochipNo(tenant, microchip.trim()).stream().toList();
        return active(result).stream().map(this::view).toList();
    }

    public PatientView patient(String tenant, long id) { return view(patientEntity(tenant, id)); }

    public List<PatientView> patientsForClient(String tenant, long clientId) {
        clientEntity(tenant, clientId);
        return active(patients.findAllByTenantIdAndClient_Id(tenant, clientId)).stream().map(this::view).toList();
    }

    public PatientView createPatientForClient(String tenant, long clientId, PatientRequest request) {
        clientEntity(tenant, clientId);
        if (request.clientId() != null && request.clientId() != clientId) throw badRequest("Client does not match path");
        PatientRequest bound = new PatientRequest(clientId, request.clinicId(), request.name(), request.speciesId(),
                request.breedId(), request.sex(), request.birthDate(), request.microchipNo(), request.neutered(),
                request.weightKg(), request.status(), request.deceasedAt());
        return createPatient(tenant, bound);
    }

    public PatientView createPatient(String tenant, PatientRequest request) {
        if (request.clientId() == null) throw badRequest("clientId is required");
        return view(patients.save(patientFrom(tenant, request)));
    }

    public PatientView updatePatient(String tenant, long id, PatientRequest request) {
        if (request.clientId() == null) throw badRequest("clientId is required");
        PatientEntity entity = patientEntity(tenant, id);
        patientFrom(tenant, request, entity);
        return view(patients.saveAndFlush(entity));
    }

    public PatientView transferPatientToClinic(String tenant, long patientId, long clinicId) {
        PatientEntity patient = patientEntity(tenant, patientId);
        ClinicEntity destination = clinicEntity(tenant, clinicId);
        if (patient.getStatus() != PatientEntity.PatientStatus.ACTIVE) {
            throw badRequest("Only active patients can be transferred");
        }
        if (!destination.isActive()) {
            throw badRequest("Patients can only be transferred to an active clinic");
        }
        boolean hasOpenVisit = active(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc(tenant, patientId))
                .stream()
                .anyMatch(visit -> visit.getStatus() == VisitEntity.VisitStatus.SCHEDULED
                        || visit.getStatus() == VisitEntity.VisitStatus.IN_PROGRESS);
        if (hasOpenVisit) {
            throw badRequest("Patient cannot be transferred while scheduled or in-progress visits exist");
        }
        patient.transferToClinic(destination);
        return view(patients.saveAndFlush(patient));
    }

    public void deletePatient(String tenant, long id) { softDelete(patientEntity(tenant, id)); }

    public List<HistoryView> history(String tenant, long patientId) {
        patientEntity(tenant, patientId);
        return active(history.findAllByTenantIdAndPatient_IdOrderByRecordedAtDesc(tenant, patientId))
                .stream().map(this::view).toList();
    }

    public HistoryView addHistory(String tenant, long patientId, HistoryRequest request) {
        PatientEntity patient = patientEntity(tenant, patientId);
        VisitEntity visit = request.visitId() == null ? null : visitEntity(tenant, request.visitId());
        if (visit != null && !Objects.equals(visit.getPatient().getId(), patientId)) {
            throw missing("Visit does not belong to patient");
        }
        MedicalRecordEntryEntity entity = new MedicalRecordEntryEntity(tenant, patient, visit,
                enumValue(MedicalRecordEntryEntity.EntryType.class, request.entryType()), request.description(),
                request.recordedAt(), request.recordedBy());
        return view(history.save(entity));
    }

    public HistoryView historyEntry(String tenant, long patientId, long entryId) {
        MedicalRecordEntryEntity entity = find(history.findByTenantIdAndId(tenant, entryId));
        if (!Objects.equals(entity.getPatient().getId(), patientId)) {
            throw missing("History entry does not belong to patient");
        }
        patientEntity(tenant, patientId);
        return view(entity);
    }

    public List<VisitView> visits(String tenant) {
        return active(visits.findAllByTenantId(tenant)).stream().map(this::view).toList();
    }

    public VisitView visit(String tenant, long id) { return view(visitEntity(tenant, id)); }

    public List<VisitView> visitsForPatient(String tenant, long patientId) {
        patientEntity(tenant, patientId);
        return active(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc(tenant, patientId))
                .stream().map(this::view).toList();
    }

    public VisitView createVisitForPatient(String tenant, long patientId, VisitRequest request) {
        patientEntity(tenant, patientId);
        if (request.patientId() != null && request.patientId() != patientId) throw badRequest("Patient does not match path");
        VisitRequest bound = new VisitRequest(patientId, request.clinicId(), request.vetId(), request.visitType(),
                request.status(), request.scheduledAt(), request.startedAt(), request.completedAt(), request.reason(),
                request.diagnosis(), request.notes());
        return createVisit(tenant, bound);
    }

    public VisitView createVisit(String tenant, VisitRequest request) {
        if (request.patientId() == null) throw badRequest("patientId is required");
        return view(visits.save(visitFrom(tenant, request)));
    }

    public VisitView updateVisit(String tenant, long id, VisitRequest request) {
        if (request.patientId() == null) throw badRequest("patientId is required");
        VisitEntity entity = visitEntity(tenant, id);
        VisitEntity.VisitStatus requestedStatus = request.status() == null
                ? entity.getStatus()
                : enumValue(VisitEntity.VisitStatus.class, request.status());
        if (requestedStatus != entity.getStatus()) {
            throw badRequest("Use the visit status transition action to change status");
        }
        entity.update(patientEntity(tenant, request.patientId()), clinicEntity(tenant, request.clinicId()),
                staffEntity(tenant, request.vetId()), request.visitType(),
                entity.getStatus(),
                request.scheduledAt(), request.startedAt(), request.completedAt(), request.reason(),
                request.diagnosis(), request.notes());
        verifyVisitLinks(entity.getPatient(), entity.getClinic(), entity.getVet());
        return view(visits.saveAndFlush(entity));
    }

    public VisitView transitionVisit(String tenant, long id, String targetStatus) {
        VisitEntity entity = visitEntity(tenant, id);
        VisitEntity.VisitStatus target = enumValue(VisitEntity.VisitStatus.class, targetStatus);
        boolean allowed = switch (entity.getStatus()) {
            case SCHEDULED -> target == VisitEntity.VisitStatus.IN_PROGRESS
                    || target == VisitEntity.VisitStatus.CANCELLED;
            case IN_PROGRESS -> target == VisitEntity.VisitStatus.COMPLETED
                    || target == VisitEntity.VisitStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw badRequest("Visit cannot transition from " + entity.getStatus() + " to " + target);
        }
        entity.transitionTo(target, Instant.now(clock));
        return view(visits.saveAndFlush(entity));
    }

    public void deleteVisit(String tenant, long id) { softDelete(visitEntity(tenant, id)); }

    public List<PrescriptionView> prescriptions(String tenant) {
        return active(prescriptions.findAllByTenantId(tenant)).stream().map(this::view).toList();
    }

    public PrescriptionView prescription(String tenant, long id) { return view(prescriptionEntity(tenant, id)); }

    public List<PrescriptionView> prescriptionsForVisit(String tenant, long visitId) {
        visitEntity(tenant, visitId);
        return active(prescriptions.findAllByTenantIdAndVisit_Id(tenant, visitId)).stream().map(this::view).toList();
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
        return view(prescriptions.save(prescriptionFrom(tenant, request)));
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
        return view(prescriptions.saveAndFlush(entity));
    }

    public void deletePrescription(String tenant, long id) { softDelete(prescriptionEntity(tenant, id)); }

    public List<PrescriptionItemView> prescriptionItems(String tenant, long prescriptionId) {
        prescriptionEntity(tenant, prescriptionId);
        return active(prescriptionItems.findAllByTenantIdAndPrescription_Id(tenant, prescriptionId))
                .stream().map(this::view).toList();
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
        return view(prescriptionItems.save(item));
    }

    public List<InvoiceView> invoices(String tenant, String status) {
        if (status != null && !status.isBlank()) enumValue(InvoiceEntity.InvoiceStatus.class, status);
        return active(invoices.findAllByTenantId(tenant)).stream()
                .filter(invoice -> status == null || status.isBlank()
                        || invoice.getStatus().name().equalsIgnoreCase(status))
                .map(this::view).toList();
    }

    public InvoiceView invoice(String tenant, long id) { return view(invoiceEntity(tenant, id)); }

    public InvoiceView createInvoice(String tenant, InvoiceRequest request) {
        return view(invoices.save(invoiceFrom(tenant, request)));
    }

    public InvoiceView updateInvoice(String tenant, long id, InvoiceRequest request) {
        InvoiceEntity entity = invoiceEntity(tenant, id);
        InvoiceEntity.InvoiceStatus requestedStatus = request.status() == null
                ? entity.getStatus() : enumValue(InvoiceEntity.InvoiceStatus.class, request.status());
        if (requestedStatus != entity.getStatus()) {
            throw badRequest("Invoice status can only be changed through issue, void, and payment actions");
        }
        if (request.paidAmount().compareTo(entity.getPaidAmount()) != 0) {
            throw badRequest("paidAmount is managed through payment registration");
        }
        entity.update(clinicEntity(tenant, request.clinicId()), clientEntity(tenant, request.clientId()),
                request.visitId() == null ? null : visitEntity(tenant, request.visitId()), request.invoiceNo(),
                entity.getStatus(),
                request.issuedOn(), request.dueOn(), defaultValue(request.currency(), "EUR"),
                request.totalAmount(), entity.getPaidAmount());
        validateInvoice(entity);
        return view(invoices.saveAndFlush(entity));
    }

    public InvoiceView issueInvoice(String tenant, long id) {
        InvoiceEntity invoice = invoiceEntity(tenant, id);
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.ISSUED) {
            return view(invoice);
        }
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.DRAFT
                || invoice.getPaidAmount().signum() != 0) {
            throw badRequest("Only unpaid draft invoices can be issued");
        }
        invoice.transitionTo(InvoiceEntity.InvoiceStatus.ISSUED);
        return view(invoices.saveAndFlush(invoice));
    }

    public InvoiceView voidInvoice(String tenant, long id) {
        InvoiceEntity invoice = invoiceEntity(tenant, id);
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.VOID) {
            return view(invoice);
        }
        if (invoice.getPaidAmount().signum() != 0) {
            throw badRequest("An invoice with payments cannot be voided");
        }
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceEntity.InvoiceStatus.ISSUED) {
            throw badRequest("Only draft or unpaid issued invoices can be voided");
        }
        invoice.transitionTo(InvoiceEntity.InvoiceStatus.VOID);
        return view(invoices.saveAndFlush(invoice));
    }

    public void deleteInvoice(String tenant, long id) { softDelete(invoiceEntity(tenant, id)); }

    public List<PaymentView> paymentsForInvoice(String tenant, long invoiceId) {
        invoiceEntity(tenant, invoiceId);
        return active(payments.findAllByTenantIdAndInvoice_IdOrderByPaidAtDesc(tenant, invoiceId))
                .stream().map(this::view).toList();
    }

    public PaymentView paymentForInvoice(String tenant, long invoiceId, long paymentId) {
        PaymentEntity entity = find(payments.findByTenantIdAndId(tenant, paymentId));
        if (!Objects.equals(entity.getInvoice().getId(), invoiceId)) {
            throw missing("Payment does not belong to invoice");
        }
        invoiceEntity(tenant, invoiceId);
        return view(entity);
    }

    public PaymentView addPayment(String tenant, long invoiceId, PaymentRequest request) {
        InvoiceEntity invoice = invoiceEntity(tenant, invoiceId);
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceEntity.InvoiceStatus.PARTIALLY_PAID) {
            throw badRequest("Payments can only be registered for issued invoices");
        }
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(request.amount());
        if (newPaidAmount.compareTo(invoice.getTotalAmount()) > 0) {
            throw badRequest("Payment exceeds invoice balance");
        }
        PaymentEntity payment = payments.save(new PaymentEntity(tenant, invoice, request.paidAt(),
                request.amount(), enumValue(PaymentEntity.PaymentMethod.class, request.method())));
        InvoiceEntity.InvoiceStatus updatedStatus = newPaidAmount.compareTo(invoice.getTotalAmount()) == 0
                ? InvoiceEntity.InvoiceStatus.PAID : InvoiceEntity.InvoiceStatus.PARTIALLY_PAID;
        invoice.update(invoice.getClinic(), invoice.getClient(), invoice.getVisit(), invoice.getInvoiceNo(),
                updatedStatus, invoice.getIssuedOn(), invoice.getDueOn(), invoice.getCurrency(),
                invoice.getTotalAmount(), newPaidAmount);
        invoices.save(invoice);
        return view(payment);
    }

    private PatientEntity patientFrom(String tenant, PatientRequest request) {
        ClientEntity client = clientEntity(tenant, request.clientId());
        ClinicEntity clinic = clinicEntity(tenant, request.clinicId());
        SpeciesEntity speciesEntity = speciesEntity(tenant, request.speciesId());
        BreedEntity breed = breedEntity(tenant, request.breedId(), request.speciesId());
        PatientEntity.PatientStatus status = enumValue(PatientEntity.PatientStatus.class,
                defaultValue(request.status(), "ACTIVE"));
        return new PatientEntity(tenant, client, clinic, request.name(), speciesEntity, breed,
                enumValue(PatientEntity.Sex.class, request.sex()), request.birthDate(), request.microchipNo(),
                request.neutered(), request.weightKg(), status, request.deceasedAt());
    }

    private PatientEntity patientFrom(String tenant, PatientRequest request, PatientEntity existing) {
        ClientEntity client = clientEntity(tenant, request.clientId());
        ClinicEntity clinic = clinicEntity(tenant, request.clinicId());
        SpeciesEntity speciesEntity = speciesEntity(tenant, request.speciesId());
        BreedEntity breed = breedEntity(tenant, request.breedId(), request.speciesId());
        PatientEntity.PatientStatus status = enumValue(PatientEntity.PatientStatus.class,
                defaultValue(request.status(), "ACTIVE"));
        existing.update(client, clinic, request.name(), speciesEntity, breed,
                enumValue(PatientEntity.Sex.class, request.sex()), request.birthDate(), request.microchipNo(),
                request.neutered(), request.weightKg(), status, request.deceasedAt());
        return existing;
    }

    private VisitEntity visitFrom(String tenant, VisitRequest request) {
        PatientEntity patient = patientEntity(tenant, request.patientId());
        ClinicEntity clinic = clinicEntity(tenant, request.clinicId());
        StaffEntity vet = staffEntity(tenant, request.vetId());
        verifyVisitLinks(patient, clinic, vet);
        return new VisitEntity(tenant, patient, clinic, vet, request.visitType(),
                enumValue(VisitEntity.VisitStatus.class, defaultValue(request.status(), "SCHEDULED")),
                request.scheduledAt(), request.startedAt(), request.completedAt(), request.reason(),
                request.diagnosis(), request.notes());
    }

    private PrescriptionEntity prescriptionFrom(String tenant, PrescriptionRequest request) {
        PrescriptionEntity entity = new PrescriptionEntity(tenant, patientEntity(tenant, request.patientId()),
                visitEntity(tenant, request.visitId()), staffEntity(tenant, request.vetId()),
                enumValue(PrescriptionEntity.PrescriptionStatus.class, defaultValue(request.status(), "ACTIVE")),
                request.issuedOn(), request.validUntil(), request.notes());
        verifyPrescriptionLinks(entity);
        if (request.validUntil().isBefore(request.issuedOn())) throw badRequest("validUntil must not precede issuedOn");
        return entity;
    }

    private InvoiceEntity invoiceFrom(String tenant, InvoiceRequest request) {
        InvoiceEntity entity = new InvoiceEntity(tenant, clinicEntity(tenant, request.clinicId()),
                clientEntity(tenant, request.clientId()),
                request.visitId() == null ? null : visitEntity(tenant, request.visitId()), request.invoiceNo(),
                enumValue(InvoiceEntity.InvoiceStatus.class, defaultValue(request.status(), "DRAFT")),
                request.issuedOn(), request.dueOn(), defaultValue(request.currency(), "EUR"),
                request.totalAmount(), request.paidAmount());
        validateInvoice(entity);
        return entity;
    }

    private void verifyVisitLinks(PatientEntity patient, ClinicEntity clinic, StaffEntity vet) {
        if (!Objects.equals(patient.getClinic().getId(), clinic.getId())
                || !Objects.equals(vet.getClinic().getId(), clinic.getId())) {
            throw missing("Patient, clinic and vet must belong to the same clinic");
        }
    }

    private void verifyPrescriptionLinks(PrescriptionEntity prescription) {
        if (!Objects.equals(prescription.getPatient().getId(), prescription.getVisit().getPatient().getId())
                || !Objects.equals(prescription.getVet().getId(), prescription.getVisit().getVet().getId())) {
            throw missing("Prescription patient and vet must match its visit");
        }
    }

    private void validateInvoice(InvoiceEntity invoice) {
        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) > 0) {
            throw badRequest("paidAmount must not exceed totalAmount");
        }
        if (invoice.getDueOn().isBefore(invoice.getIssuedOn())) {
            throw badRequest("dueOn must not precede issuedOn");
        }
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.PAID
                && invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) != 0) {
            throw badRequest("A paid invoice must be fully settled");
        }
        if (invoice.getVisit() != null
                && (!Objects.equals(invoice.getVisit().getClinic().getId(), invoice.getClinic().getId())
                || !Objects.equals(invoice.getVisit().getPatient().getClient().getId(),
                invoice.getClient().getId()))) {
            throw missing("Visit must belong to the invoice clinic and client");
        }
    }

    private ClinicEntity clinicEntity(String tenant, long id) { return find(clinics.findByTenantIdAndId(tenant, id)); }
    private StaffEntity staffEntity(String tenant, long id) { return find(staff.findByTenantIdAndId(tenant, id)); }
    private ClientEntity clientEntity(String tenant, long id) { return find(clients.findByTenantIdAndId(tenant, id)); }
    private PatientEntity patientEntity(String tenant, long id) { return find(patients.findByTenantIdAndId(tenant, id)); }
    private VisitEntity visitEntity(String tenant, long id) { return find(visits.findByTenantIdAndId(tenant, id)); }
    private PrescriptionEntity prescriptionEntity(String tenant, long id) {
        return find(prescriptions.findByTenantIdAndId(tenant, id));
    }
    private InvoiceEntity invoiceEntity(String tenant, long id) { return find(invoices.findByTenantIdAndId(tenant, id)); }
    private SpeciesEntity speciesEntity(String tenant, long id) { return find(species.findByTenantIdAndId(tenant, id)); }
    private BreedEntity breedEntity(String tenant, Long id, long speciesId) {
        if (id == null) return null;
        BreedEntity breed = find(breeds.findByTenantIdAndId(tenant, id));
        if (!Objects.equals(breed.getSpecies().getId(), speciesId)) throw missing("Breed does not belong to species");
        return breed;
    }

    private <T extends BusinessEntity> T find(java.util.Optional<T> entity) {
        return entity.filter(value -> value.getDeletedAt() == null).orElseThrow(() -> missing("Resource not found"));
    }

    private <T extends BusinessEntity> List<T> active(List<T> entities) {
        return entities.stream().filter(entity -> entity.getDeletedAt() == null).toList();
    }

    private void softDelete(BusinessEntity entity) {
        entity.setDeletedAt(Instant.now(clock));
    }

    private ClinicView view(ClinicEntity e) {
        return new ClinicView(e.getId(), e.getPublicId(), e.getTenantId(), e.getName(), e.getCity(),
                e.getAddress(), e.getPhone(), e.getEmail(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private StaffView view(StaffEntity e) {
        return new StaffView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClinic().getId(),
                e.getRole().name(), e.getFullName(), e.getLicenseNo(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private ClientView view(ClientEntity e) {
        return new ClientView(e.getId(), e.getPublicId(), e.getTenantId(), e.getRegisteredClinic().getId(),
                e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(), e.getAddress(), e.getCity(),
                e.getPreferredContact(), e.getGdprConsentAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private PatientView view(PatientEntity e) {
        return new PatientView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClient().getId(),
                e.getClinic().getId(), e.getName(), e.getSpecies().getId(),
                e.getBreed() == null ? null : e.getBreed().getId(), name(e.getSex()), e.getBirthDate(),
                e.getMicrochipNo(), e.isNeutered(), e.getWeightKg(), e.getStatus().name(), e.getDeceasedAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
    private VisitView view(VisitEntity e) {
        return new VisitView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getClinic().getId(), e.getVet().getId(), e.getVisitType(), e.getStatus().name(),
                e.getScheduledAt(), e.getStartedAt(), e.getCompletedAt(), e.getReason(), e.getDiagnosis(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private PrescriptionView view(PrescriptionEntity e) {
        return new PrescriptionView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getVisit().getId(), e.getVet().getId(), e.getStatus().name(), e.getIssuedOn(), e.getValidUntil(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private InvoiceView view(InvoiceEntity e) {
        return new InvoiceView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClinic().getId(),
                e.getClient().getId(), e.getVisit() == null ? null : e.getVisit().getId(), e.getInvoiceNo(),
                e.getStatus().name(), e.getIssuedOn(), e.getDueOn(), e.getCurrency(), e.getTotalAmount(),
                e.getPaidAmount(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private HistoryView view(MedicalRecordEntryEntity e) {
        return new HistoryView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getVisit() == null ? null : e.getVisit().getId(), e.getEntryType().name(), e.getDescription(),
                e.getRecordedAt(), e.getRecordedBy());
    }
    private PaymentView view(PaymentEntity e) {
        return new PaymentView(e.getId(), e.getPublicId(), e.getTenantId(), e.getInvoice().getId(),
                e.getPaidAt(), e.getAmount(), e.getMethod().name());
    }
    private PrescriptionItemView view(PrescriptionItemEntity e) {
        return new PrescriptionItemView(e.getId(), e.getPublicId(), e.getTenantId(),
                e.getPrescription().getId(), e.getMedication().getId(), e.getMedication().getCode(),
                e.getMedication().getName(), e.getDosage(), e.getFrequency(), e.getDurationDays(),
                e.getQuantity(), e.getRefillsAllowed());
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw badRequest("Invalid " + type.getSimpleName() + " value: " + value);
        }
    }
    private static PmsResourceNotFoundException missing(String message) {
        return new PmsResourceNotFoundException(message);
    }
    private static PmsInvalidRequestException badRequest(String message) {
        return new PmsInvalidRequestException(message);
    }
}
