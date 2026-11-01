package com.vetsync.pms.application;

import com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientRequest;
import com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicRequest;
import com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceRequest;
import com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryRequest;
import com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientRequest;
import com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionRequest;
import com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffRequest;
import com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitRequest;
import com.vetsync.pms.adapter.out.persistence.entity.BreedEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("bdd")
class PmsApplicationServiceCrudBehaviorTests {

    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");

    @Mock private ClinicRepository clinics;
    @Mock private StaffRepository staff;
    @Mock private ClientRepository clients;
    @Mock private PatientRepository patients;
    @Mock private VisitRepository visits;
    @Mock private PrescriptionRepository prescriptions;
    @Mock private InvoiceRepository invoices;
    @Mock private MedicationRepository medications;
    @Mock private PaymentRepository payments;
    @Mock private MedicalRecordEntryRepository history;
    @Mock private PrescriptionItemRepository prescriptionItems;
    @Mock private SpeciesRepository species;
    @Mock private BreedRepository breeds;

    private PmsApplicationService service;
    private ClinicEntity clinic;
    private ClientEntity client;
    private SpeciesEntity speciesEntity;
    private StaffEntity vet;
    private PatientEntity patient;
    private VisitEntity visit;
    private PrescriptionEntity prescription;
    private InvoiceEntity invoice;
    private PaymentEntity payment;
    private MedicalRecordEntryEntity historyEntry;

    @BeforeEach
    void setUp() {
        service = new PmsApplicationService(clinics, staff, clients, patients, visits, prescriptions,
                invoices, medications, payments, history, prescriptionItems, species, breeds,
                Clock.fixed(NOW, ZoneOffset.UTC));
        clinic = entity(new ClinicEntity("SW", "Synthetic Clinic", "Sample City",
                null, null, null, true), 1L);
        client = entity(new ClientEntity("SW", clinic, "Synthetic", "Owner",
                "owner@example.invalid", "555-0100", null, null, "EMAIL", NOW), 2L);
        speciesEntity = entity(new SpeciesEntity("SW", "DOG", "Dog"), 3L);
        vet = entity(new StaffEntity("SW", clinic, StaffEntity.StaffRole.VET,
                "Synthetic Vet", "LIC-1", true), 4L);
        patient = entity(new PatientEntity("SW", client, clinic, "Synthetic Patient", speciesEntity,
                null, PatientEntity.Sex.FEMALE, LocalDate.parse("2020-01-01"), "CHIP-1",
                false, new BigDecimal("12.50"), PatientEntity.PatientStatus.ACTIVE, null), 5L);
        visit = entity(new VisitEntity("SW", patient, clinic, vet, "CHECKUP",
                VisitEntity.VisitStatus.SCHEDULED, NOW, null, null, "Annual checkup", null, null), 6L);
        prescription = entity(new PrescriptionEntity("SW", patient, visit, vet,
                PrescriptionEntity.PrescriptionStatus.ACTIVE, LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-14"), "Synthetic"), 7L);
        invoice = entity(new InvoiceEntity("SW", clinic, client, visit, "INV-1",
                InvoiceEntity.InvoiceStatus.ISSUED, LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-14"), "EUR", new BigDecimal("100.00"),
                new BigDecimal("20.00")), 8L);
        payment = entity(new PaymentEntity("SW", invoice, NOW, new BigDecimal("20.00"),
                PaymentEntity.PaymentMethod.CARD), 9L);
        historyEntry = entity(new MedicalRecordEntryEntity("SW", patient, visit,
                MedicalRecordEntryEntity.EntryType.NOTE, "Synthetic note", NOW, "Synthetic Vet"), 10L);

        lenient().when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic));
        lenient().when(clients.findByTenantIdAndId("SW", 2L)).thenReturn(Optional.of(client));
        lenient().when(species.findByTenantIdAndId("SW", 3L)).thenReturn(Optional.of(speciesEntity));
        lenient().when(staff.findByTenantIdAndId("SW", 4L)).thenReturn(Optional.of(vet));
        lenient().when(patients.findByTenantIdAndId("SW", 5L)).thenReturn(Optional.of(patient));
        lenient().when(visits.findByTenantIdAndId("SW", 6L)).thenReturn(Optional.of(visit));
        lenient().when(prescriptions.findByTenantIdAndId("SW", 7L)).thenReturn(Optional.of(prescription));
        lenient().when(invoices.findByTenantIdAndId("SW", 8L)).thenReturn(Optional.of(invoice));
        lenient().when(payments.findByTenantIdAndId("SW", 9L)).thenReturn(Optional.of(payment));
        lenient().when(history.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(historyEntry));
    }

    @Test
    void givenAClinicWhenCreatedUpdatedAndFetchedThenItsTenantAndUpdatesAreMapped() {
        when(clinics.save(any())).thenAnswer(call -> entity(call.getArgument(0), 11L));
        when(clinics.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        ClinicRequest request = new ClinicRequest("Updated Clinic", "Sample City",
                "Synthetic address", "555-0101", "clinic@example.invalid", true);

        var created = service.createClinic("SW", request);
        var updated = service.updateClinic("SW", 1L, request);
        var fetched = service.clinic("SW", 1L);

        assertThat(created.id()).isEqualTo(11L);
        assertThat(updated.name()).isEqualTo("Updated Clinic");
        assertThat(fetched.city()).isEqualTo("Sample City");
        verify(clinics).save(any(ClinicEntity.class));
        verify(clinics).saveAndFlush(clinic);
    }

    @Test
    void givenStaffAtAClinicWhenManagedThenItsTenantAndClinicScopeArePreserved() {
        when(staff.findAllByTenantIdAndClinic_Id("SW", 1L)).thenReturn(List.of(vet));
        when(staff.findAllByTenantId("SW")).thenReturn(List.of(vet));
        when(staff.save(any())).thenAnswer(call -> entity(call.getArgument(0), 11L));
        when(staff.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        var created = service.createStaff("SW", 1L,
                new StaffRequest(1L, "vet", "Synthetic Vet Two", "LIC-2", true));
        var listed = service.staffForClinic("SW", 1L);
        var all = service.staff("SW");
        var found = service.staff("SW", 4L);
        var updated = service.updateStaff("SW", 4L,
                new StaffRequest(null, "nurse", "Synthetic Nurse", null, true));
        service.deleteStaff("SW", 4L);

        assertThat(created.role()).isEqualTo("VET");
        assertThat(listed).hasSize(1);
        assertThat(all).hasSize(1);
        assertThat(found.fullName()).isEqualTo("Synthetic Vet");
        assertThat(updated.role()).isEqualTo("NURSE");
        assertThat(vet.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenStaffWhenListedAndUpdatedThenClinicAssignmentAndSoftDeleteArePreserved() {
        when(staff.findAllByTenantId("SW")).thenReturn(List.of(vet));
        when(staff.findAllByTenantIdAndClinic_Id("SW", 1L)).thenReturn(List.of(vet));
        when(staff.save(any())).thenAnswer(call -> entity(call.getArgument(0), 11L));
        when(staff.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        StaffRequest request = new StaffRequest(null, "VET", "Synthetic Updated Vet", "LIC-2", true);

        var created = service.createStaff("SW", 1L, request);
        var all = service.staff("SW");
        var forClinic = service.staffForClinic("SW", 1L);
        var found = service.staff("SW", 4L);
        var updated = service.updateStaff("SW", 4L, request);
        service.deleteStaff("SW", 4L);

        assertThat(created.id()).isEqualTo(11L);
        assertThat(all).hasSize(1);
        assertThat(forClinic).extracting(view -> view.fullName()).containsExactly("Synthetic Vet");
        assertThat(found.role()).isEqualTo("VET");
        assertThat(updated.fullName()).isEqualTo("Synthetic Updated Vet");
        assertThat(vet.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAClientWhenSearchedUpdatedAndDeletedThenSearchIsCaseInsensitiveAndTenantScoped() {
        when(clients.findAllByTenantId("SW")).thenReturn(List.of(client));
        when(clients.save(any())).thenAnswer(call -> entity(call.getArgument(0), 12L));
        when(clients.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        ClientRequest request = new ClientRequest(1L, "Updated", "Owner",
                "updated@example.invalid", "555-0199", null, null, "PHONE", NOW);

        var matching = service.clients("SW", "  OWNER ");
        var byEmail = service.clients("SW", "example.invalid");
        var byPhone = service.clients("SW", "555-0100");
        var emptyQuery = service.clients("SW", " ");
        var found = service.client("SW", 2L);
        var created = service.createClient("SW", request);
        var updated = service.updateClient("SW", 2L, request);
        service.deleteClient("SW", 2L);

        assertThat(matching).hasSize(1);
        assertThat(byEmail).hasSize(1);
        assertThat(byPhone).hasSize(1);
        assertThat(emptyQuery).hasSize(1);
        assertThat(found.firstName()).isEqualTo("Synthetic");
        assertThat(created.firstName()).isEqualTo("Updated");
        assertThat(updated.preferredContact()).isEqualTo("PHONE");
        assertThat(client.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAPatientWhenCreatedUpdatedAndSearchedThenBreedAndMicrochipAreResolved() {
        BreedEntity breed = entity(new BreedEntity("SW", speciesEntity, "SYNTH", "Synthetic breed"), 13L);
        when(breeds.findByTenantIdAndId("SW", 13L)).thenReturn(Optional.of(breed));
        when(patients.findAllByTenantId("SW")).thenReturn(List.of(patient));
        when(patients.findByTenantIdAndMicrochipNo("SW", "CHIP-1")).thenReturn(Optional.of(patient));
        when(patients.findAllByTenantIdAndClient_Id("SW", 2L)).thenReturn(List.of(patient));
        when(patients.save(any())).thenAnswer(call -> entity(call.getArgument(0), 14L));
        when(patients.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        PatientRequest request = patientRequest(13L);

        var all = service.patients("SW", null);
        var byMicrochip = service.patients("SW", " CHIP-1 ");
        var byBlankMicrochip = service.patients("SW", " ");
        var forClient = service.patientsForClient("SW", 2L);
        var created = service.createPatientForClient("SW", 2L, request);
        var found = service.patient("SW", 5L);
        var updated = service.updatePatient("SW", 5L, request);
        service.deletePatient("SW", 5L);

        assertThat(all).hasSize(1);
        assertThat(byMicrochip).hasSize(1);
        assertThat(byBlankMicrochip).hasSize(1);
        assertThat(forClient).hasSize(1);
        assertThat(created.breedId()).isEqualTo(13L);
        assertThat(found.microchipNo()).isEqualTo("CHIP-1");
        assertThat(updated.name()).isEqualTo("Updated Patient");
        assertThat(patient.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAnActivePatientWithoutOpenVisitsWhenTransferredThenOnlyItsCurrentClinicChanges() {
        ClinicEntity destination = entity(new ClinicEntity("SW", "Destination Clinic", "Sample City",
                null, null, null, true), 25L);
        when(patients.findByTenantIdAndId("SW", 5L)).thenReturn(Optional.of(patient));
        when(clinics.findByTenantIdAndId("SW", 25L)).thenReturn(Optional.of(destination));
        when(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc("SW", 5L)).thenReturn(List.of());
        when(patients.saveAndFlush(patient)).thenReturn(patient);

        var transferred = service.transferPatientToClinic("SW", 5L, 25L);

        assertThat(transferred.clinicId()).isEqualTo(25L);
        assertThat(patient.getClinic().getId()).isEqualTo(25L);
        assertThat(visit.getClinic().getId()).isEqualTo(1L);
    }

    @Test
    void givenAPatientWithAnOpenVisitWhenTransferredThenTheTransferIsRejected() {
        ClinicEntity destination = entity(new ClinicEntity("SW", "Destination Clinic", "Sample City",
                null, null, null, true), 25L);
        when(patients.findByTenantIdAndId("SW", 5L)).thenReturn(Optional.of(patient));
        when(clinics.findByTenantIdAndId("SW", 25L)).thenReturn(Optional.of(destination));
        when(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc("SW", 5L)).thenReturn(List.of(visit));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.transferPatientToClinic("SW", 5L, 25L))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Patient cannot be transferred while scheduled or in-progress visits exist");

        verify(patients, never()).saveAndFlush(patient);
        assertThat(patient.getClinic().getId()).isEqualTo(1L);
    }

    @Test
    void givenAVisitWhenManagedThenPatientClinicAndVetLinksRemainConsistent() {
        when(visits.findAllByTenantId("SW")).thenReturn(List.of(visit));
        when(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc("SW", 5L))
                .thenReturn(List.of(visit));
        when(visits.save(any())).thenAnswer(call -> entity(call.getArgument(0), 15L));
        when(visits.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        VisitRequest request = visitRequest();

        var all = service.visits("SW");
        var found = service.visit("SW", 6L);
        var forPatient = service.visitsForPatient("SW", 5L);
        var created = service.createVisitForPatient("SW", 5L, request);
        var updated = service.updateVisit("SW", 6L, request);
        service.deleteVisit("SW", 6L);

        assertThat(all).hasSize(1);
        assertThat(found.patientId()).isEqualTo(5L);
        assertThat(forPatient).hasSize(1);
        assertThat(created.status()).isEqualTo("SCHEDULED");
        assertThat(updated.visitType()).isEqualTo("CHECKUP");
        assertThat(visit.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAScheduledVisitWhenStartedAndCompletedThenTransitionTimesAreRecorded() {
        when(visits.findByTenantIdAndId("SW", 6L)).thenReturn(Optional.of(visit));
        when(visits.saveAndFlush(visit)).thenReturn(visit);

        var started = service.transitionVisit("SW", 6L, "IN_PROGRESS");
        var completed = service.transitionVisit("SW", 6L, "COMPLETED");

        assertThat(started.status()).isEqualTo("IN_PROGRESS");
        assertThat(started.startedAt()).isEqualTo(NOW);
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.completedAt()).isEqualTo(NOW);
        verify(visits, org.mockito.Mockito.times(2)).saveAndFlush(visit);
    }

    @Test
    void givenAScheduledVisitWhenCompletedDirectlyThenTransitionIsRejected() {
        when(visits.findByTenantIdAndId("SW", 6L)).thenReturn(Optional.of(visit));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.transitionVisit("SW", 6L, "COMPLETED"))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Visit cannot transition from SCHEDULED to COMPLETED");

        verify(visits, never()).saveAndFlush(visit);
        assertThat(visit.getStatus()).isEqualTo(VisitEntity.VisitStatus.SCHEDULED);
    }

    @Test
    void givenAVisitUpdateWhenItChangesStatusThenTransitionActionIsRequired() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.updateVisit("SW", 6L, new VisitRequest(5L, 1L, 4L,
                                "CHECKUP", "COMPLETED", NOW, NOW, NOW,
                                "Annual checkup", null, null)))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Use the visit status transition action to change status");
    }

    @Test
    void givenAPrescriptionWhenManagedThenItsPatientVisitAndVetMustMatch() {
        when(prescriptions.findAllByTenantId("SW")).thenReturn(List.of(prescription));
        when(prescriptions.findAllByTenantIdAndVisit_Id("SW", 6L)).thenReturn(List.of(prescription));
        when(prescriptions.save(any())).thenAnswer(call -> entity(call.getArgument(0), 16L));
        when(prescriptions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        PrescriptionRequest request = prescriptionRequest();

        var all = service.prescriptions("SW");
        var found = service.prescription("SW", 7L);
        var forVisit = service.prescriptionsForVisit("SW", 6L);
        var created = service.createPrescriptionForVisit("SW", 6L, request);
        var updated = service.updatePrescription("SW", 7L, request);
        service.deletePrescription("SW", 7L);

        assertThat(all).hasSize(1);
        assertThat(found.patientId()).isEqualTo(5L);
        assertThat(forVisit).hasSize(1);
        assertThat(created.status()).isEqualTo("ACTIVE");
        assertThat(updated.validUntil()).isEqualTo(LocalDate.parse("2026-10-14"));
        assertThat(prescription.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAPrescriptionAndMedicationWhenAnItemIsAddedThenItIsScopedAndReturned() {
        MedicationEntity medication = entity(new MedicationEntity("SW", "SAMPLE-001",
                "Synthetic medicine", "tablet", "10 mg", false), 20L);
        PrescriptionItemEntity item = entity(new PrescriptionItemEntity("SW", prescription, medication,
                "1 tablet", "once daily", 7, new BigDecimal("7.000"), 0), 21L);
        when(prescriptions.findByTenantIdAndId("SW", 7L)).thenReturn(Optional.of(prescription));
        when(medications.findByTenantIdAndId("SW", 20L)).thenReturn(Optional.of(medication));
        when(prescriptionItems.save(any())).thenReturn(item);
        when(prescriptionItems.findAllByTenantIdAndPrescription_Id("SW", 7L)).thenReturn(List.of(item));

        var created = service.addPrescriptionItem("SW", 7L,
                new com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemRequest(
                        20L, "1 tablet", "once daily", 7, new BigDecimal("7.000"), 0));
        var listed = service.prescriptionItems("SW", 7L);

        assertThat(created.id()).isEqualTo(21L);
        assertThat(created.prescriptionId()).isEqualTo(7L);
        assertThat(created.medicationCode()).isEqualTo("SAMPLE-001");
        assertThat(created.durationDays()).isEqualTo(7);
        assertThat(listed).hasSize(1);
    }

    @Test
    void givenAControlledMedicationAndUnlicensedPrescriberWhenAddedThenItIsRejected() {
        MedicationEntity medication = entity(new MedicationEntity("SW", "CONTROLLED-001",
                "Synthetic controlled medicine", "tablet", "5 mg", true), 22L);
        StaffEntity nurse = entity(new StaffEntity("SW", clinic, StaffEntity.StaffRole.NURSE,
                "Synthetic Nurse", null, true), 23L);
        PrescriptionEntity unlicensedPrescription = entity(new PrescriptionEntity("SW", patient, visit, nurse,
                PrescriptionEntity.PrescriptionStatus.ACTIVE, LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-14"), "Synthetic"), 24L);
        when(prescriptions.findByTenantIdAndId("SW", 24L)).thenReturn(Optional.of(unlicensedPrescription));
        when(medications.findByTenantIdAndId("SW", 22L)).thenReturn(Optional.of(medication));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.addPrescriptionItem("SW", 24L,
                                new com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto
                                        .PrescriptionItemRequest(22L, "1 tablet", "once daily", 7,
                                        new BigDecimal("7.000"), 0)))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Controlled medication requires a licensed vet");

        verify(prescriptionItems, never()).save(any());
    }

    @Test
    void givenAnInvoiceWhenListedUpdatedAndItsPaymentsReadThenRelatedDataIsMapped() {
        when(invoices.findAllByTenantId("SW")).thenReturn(List.of(invoice));
        when(invoices.save(any())).thenAnswer(call -> call.getArgument(0));
        when(invoices.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(payments.findAllByTenantIdAndInvoice_IdOrderByPaidAtDesc("SW", 8L))
                .thenReturn(List.of(payment));
        when(payments.findByTenantIdAndId("SW", 9L)).thenReturn(Optional.of(payment));
        InvoiceRequest request = invoiceRequest("ISSUED", "100.00", "20.00");

        var all = service.invoices("SW", null);
        var issued = service.invoices("SW", "issued");
        var found = service.invoice("SW", 8L);
        var updated = service.updateInvoice("SW", 8L, request);
        var invoicePayments = service.paymentsForInvoice("SW", 8L);
        var paymentFound = service.paymentForInvoice("SW", 8L, 9L);
        service.deleteInvoice("SW", 8L);

        assertThat(all).hasSize(1);
        assertThat(issued).hasSize(1);
        assertThat(found.invoiceNo()).isEqualTo("INV-1");
        assertThat(updated.status()).isEqualTo("ISSUED");
        assertThat(invoicePayments).hasSize(1);
        assertThat(paymentFound.method()).isEqualTo("CARD");
        assertThat(invoice.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAPatientWhenHistoryIsAddedAndReadThenVisitOwnershipIsChecked() {
        when(history.findAllByTenantIdAndPatient_IdOrderByRecordedAtDesc("SW", 5L))
                .thenReturn(List.of(historyEntry));
        when(history.save(any())).thenAnswer(call -> entity(call.getArgument(0), 17L));
        HistoryRequest request = new HistoryRequest("NOTE", "Synthetic checkup note",
                6L, NOW, "Synthetic Vet");

        var added = service.addHistory("SW", 5L, request);
        var entries = service.history("SW", 5L);
        var found = service.historyEntry("SW", 5L, 10L);

        assertThat(added.description()).isEqualTo("Synthetic checkup note");
        assertThat(entries).hasSize(1);
        assertThat(found.patientId()).isEqualTo(5L);
    }

    @Test
    void givenAnInvalidBreedAndSpeciesPairWhenPatientIsCreatedThenItIsRejected() {
        SpeciesEntity otherSpecies = entity(new SpeciesEntity("SW", "CAT", "Cat"), 18L);
        BreedEntity breed = entity(new BreedEntity("SW", otherSpecies, "OTHER", "Other breed"), 19L);
        when(breeds.findByTenantIdAndId("SW", 19L)).thenReturn(Optional.of(breed));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.createPatient("SW", patientRequest(19L)))
                .isInstanceOf(PmsResourceNotFoundException.class)
                .hasMessage("Breed does not belong to species");
    }

    @Test
    void givenCatalogEntriesWhenLookedUpThenOnlyActiveTenantScopedValuesAreReturned() {
        BreedEntity breed = entity(new BreedEntity("SW", speciesEntity, "SYNTH", "Synthetic breed"), 26L);
        MedicationEntity medication = entity(new MedicationEntity("SW", "SAMPLE-001",
                "Synthetic medicine", "tablet", "10 mg", false), 27L);
        MedicationEntity deletedMedication = entity(new MedicationEntity("SW", "OLD-001",
                "Deleted medicine", "tablet", "1 mg", false), 28L);
        deletedMedication.setDeletedAt(NOW);
        when(species.findAllByTenantId("SW")).thenReturn(List.of(speciesEntity));
        when(species.findByTenantIdAndId("SW", 3L)).thenReturn(Optional.of(speciesEntity));
        when(breeds.findAllByTenantIdAndSpecies_Id("SW", 3L)).thenReturn(List.of(breed));
        when(medications.findAllByTenantId("SW")).thenReturn(List.of(medication, deletedMedication));

        assertThat(service.species("SW")).hasSize(1);
        assertThat(service.breeds("SW", 3L)).extracting(view -> view.code()).containsExactly("SYNTH");
        assertThat(service.medications("SW")).extracting(view -> view.code()).containsExactly("SAMPLE-001");
    }

    private PatientRequest patientRequest(Long breedId) {
        return new PatientRequest(2L, 1L, "Updated Patient", 3L, breedId,
                "female", LocalDate.parse("2020-01-01"), "CHIP-1", false,
                new BigDecimal("12.50"), "active", null);
    }

    private VisitRequest visitRequest() {
        return new VisitRequest(5L, 1L, 4L, "CHECKUP", "scheduled", NOW,
                null, null, "Annual checkup", null, null);
    }

    private PrescriptionRequest prescriptionRequest() {
        return new PrescriptionRequest(5L, 6L, 4L, "active",
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), "Synthetic");
    }

    private InvoiceRequest invoiceRequest(String status, String total, String paid) {
        return new InvoiceRequest(1L, 2L, 6L, "INV-1", status,
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), "EUR",
                new BigDecimal(total), new BigDecimal(paid));
    }

    private static <T> T entity(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
