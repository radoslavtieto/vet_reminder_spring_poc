package com.vetsync.pms.application;

import com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceRequest;
import com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentRequest;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.InvoiceEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("bdd")
class PmsApplicationServiceBehaviorTests {

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

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PmsApplicationService(
                new PmsCatalogApplicationService(species, breeds, medications),
                new PmsPracticeApplicationService(clinics, staff, clients, clock),
                new PmsPatientCareApplicationService(clinics, staff, clients, patients, visits,
                        history, species, breeds, clock),
                new PmsPrescriptionApplicationService(prescriptions, prescriptionItems, medications,
                        patients, visits, staff, clock),
                new PmsBillingApplicationService(invoices, payments, clinics, clients, visits, clock));
    }

    @Test
    void givenAnIssuedInvoiceWhenPaymentsSettleItsBalanceThenItsStatusTracksTheBalance() {
        ClinicEntity clinic = clinic(1L);
        ClientEntity client = new ClientEntity("SW", clinic, "Synthetic", "Owner",
                null, null, null, null, null, null);
        InvoiceEntity invoice = new InvoiceEntity("SW", clinic, client, null, "INV-TEST",
                InvoiceEntity.InvoiceStatus.ISSUED, LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-14"), "EUR", new BigDecimal("100.00"),
                new BigDecimal("0.00"));
        ReflectionTestUtils.setField(invoice, "id", 10L);
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));
        when(payments.save(any())).thenAnswer(invocation -> {
            var payment = invocation.<com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity>getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 20L);
            return payment;
        });

        var partial = service.addPayment("SW", 10L,
                new PaymentRequest(NOW, new BigDecimal("35.00"), "card"));
        var settled = service.addPayment("SW", 10L,
                new PaymentRequest(NOW, new BigDecimal("65.00"), "BANK_TRANSFER"));

        assertThat(partial.amount()).isEqualByComparingTo("35.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceEntity.InvoiceStatus.PAID);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(settled.method()).isEqualTo("BANK_TRANSFER");
        verify(invoices, org.mockito.Mockito.times(2)).save(invoice);
    }

    @Test
    void givenASettledBalanceWhenAnotherPaymentWouldOverpayThenTheRequestIsRejected() {
        InvoiceEntity invoice = invoice(10L, InvoiceEntity.InvoiceStatus.PARTIALLY_PAID,
                "100.00", "90.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.addPayment("SW", 10L,
                new PaymentRequest(NOW, new BigDecimal("10.01"), "CARD")))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Payment exceeds invoice balance");

        verify(payments, never()).save(any());
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void givenAnInvoiceWithAnInvalidDateRangeWhenCreatedThenItIsRejectedBeforePersistence() {
        when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic(1L)));
        when(clients.findByTenantIdAndId("SW", 2L)).thenReturn(Optional.of(
                new ClientEntity("SW", clinic(1L), "Synthetic", "Owner",
                        null, null, null, null, null, null)));
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "DRAFT",
                LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01"), "EUR",
                new BigDecimal("10.00"), BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createInvoice("SW", request))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("dueOn must not precede issuedOn");

        verify(invoices, never()).save(any());
    }

    @Test
    void givenADraftInvoiceWhenIssuedThenStatusChangesWithoutChangingItsAmountsOrDates() {
        InvoiceEntity invoice = invoice(10L, InvoiceEntity.InvoiceStatus.DRAFT, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));
        when(invoices.saveAndFlush(invoice)).thenReturn(invoice);

        var issued = service.issueInvoice("SW", 10L);

        assertThat(issued.status()).isEqualTo("ISSUED");
        assertThat(issued.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(issued.paidAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceEntity.InvoiceStatus.ISSUED);
    }

    @Test
    void givenAnInvoiceWhenUpdatedWithAStatusChangeThenTheRequestIsRejected() {
        InvoiceEntity invoice = invoice(10L, InvoiceEntity.InvoiceStatus.DRAFT, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "ISSUED",
                invoice.getIssuedOn(), invoice.getDueOn(), "EUR",
                new BigDecimal("100.00"), BigDecimal.ZERO);

        assertThatThrownBy(() -> service.updateInvoice("SW", 10L, request))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Invoice status can only be changed through issue, void, and payment actions");

        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void givenAPartiallyPaidInvoiceWhenVoidedThenTheRequestIsRejected() {
        InvoiceEntity invoice = invoice(10L, InvoiceEntity.InvoiceStatus.PARTIALLY_PAID, "100.00", "20.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.voidInvoice("SW", 10L))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("An invoice with payments cannot be voided");

        verify(invoices, never()).saveAndFlush(any());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceEntity.InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void givenAnInvoiceWithNoPaymentsWhenUpdatedWithAPaidAmountThenTheRequestIsRejected() {
        InvoiceEntity invoice = invoice(10L, InvoiceEntity.InvoiceStatus.DRAFT, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(invoice));
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "DRAFT",
                invoice.getIssuedOn(), invoice.getDueOn(), "EUR",
                new BigDecimal("100.00"), new BigDecimal("1.00"));

        assertThatThrownBy(() -> service.updateInvoice("SW", 10L, request))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("paidAmount is managed through payment registration");

        verify(invoices, never()).saveAndFlush(any());
    }

    @Test
    void givenAnInvoiceWhenIssuedAgainOrIssuedFromAnInvalidStateThenOnlyTheExistingIssueIsReturned() {
        InvoiceEntity issued = invoice(10L, InvoiceEntity.InvoiceStatus.ISSUED, "100.00", "0.00");
        InvoiceEntity voided = invoice(11L, InvoiceEntity.InvoiceStatus.VOID, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(issued));
        when(invoices.findByTenantIdAndId("SW", 11L)).thenReturn(Optional.of(voided));

        assertThat(service.issueInvoice("SW", 10L).status()).isEqualTo("ISSUED");
        assertThatThrownBy(() -> service.issueInvoice("SW", 11L))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Only unpaid draft invoices can be issued");

        verify(invoices, never()).saveAndFlush(issued);
    }

    @Test
    void givenADraftInvoiceWhenVoidedThenRepeatedVoidIsIdempotent() {
        InvoiceEntity draft = invoice(10L, InvoiceEntity.InvoiceStatus.DRAFT, "100.00", "0.00");
        InvoiceEntity invalidPaid = invoice(11L, InvoiceEntity.InvoiceStatus.PAID, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(draft));
        when(invoices.findByTenantIdAndId("SW", 11L)).thenReturn(Optional.of(invalidPaid));
        when(invoices.saveAndFlush(draft)).thenReturn(draft);

        assertThat(service.voidInvoice("SW", 10L).status()).isEqualTo("VOID");
        assertThat(service.voidInvoice("SW", 10L).status()).isEqualTo("VOID");
        assertThatThrownBy(() -> service.voidInvoice("SW", 11L))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Only draft or unpaid issued invoices can be voided");

        verify(invoices).saveAndFlush(draft);
    }

    @Test
    void givenAPaymentFromAnotherInvoiceWhenFetchedThenItIsNotReturned() {
        InvoiceEntity otherInvoice = invoice(11L, InvoiceEntity.InvoiceStatus.ISSUED, "100.00", "0.00");
        PaymentEntity payment = new PaymentEntity("SW", otherInvoice, NOW,
                new BigDecimal("10.00"), PaymentEntity.PaymentMethod.CARD);
        ReflectionTestUtils.setField(payment, "id", 30L);
        when(payments.findByTenantIdAndId("SW", 30L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.paymentForInvoice("SW", 10L, 30L))
                .isInstanceOf(PmsResourceNotFoundException.class)
                .hasMessage("Payment does not belong to invoice");
    }

    @Test
    void givenADraftInvoiceWhenPaymentIsAddedThenTheRequestIsRejected() {
        InvoiceEntity draft = invoice(10L, InvoiceEntity.InvoiceStatus.DRAFT, "100.00", "0.00");
        when(invoices.findByTenantIdAndId("SW", 10L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.addPayment("SW", 10L,
                new PaymentRequest(NOW, new BigDecimal("10.00"), "CARD")))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Payments can only be registered for issued invoices");

        verify(payments, never()).save(any());
    }

    @Test
    void givenAValidInvoiceRequestWhenCreatedThenItIsPersisted() {
        ClinicEntity clinic = clinic(1L);
        ClientEntity client = new ClientEntity("SW", clinic, "Synthetic", "Owner",
                null, null, null, null, null, null);
        ReflectionTestUtils.setField(client, "id", 2L);
        when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic));
        when(clients.findByTenantIdAndId("SW", 2L)).thenReturn(Optional.of(client));
        when(invoices.save(any())).thenAnswer(invocation -> {
            InvoiceEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "DRAFT",
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), null,
                new BigDecimal("100.00"), BigDecimal.ZERO);

        var created = service.createInvoice("SW", request);

        assertThat(created.id()).isEqualTo(12L);
        assertThat(created.currency()).isEqualTo("EUR");
    }

    @Test
    void givenAnInvoiceWithPaidAmountAboveItsTotalWhenCreatedThenItIsRejected() {
        ClinicEntity clinic = clinic(1L);
        ClientEntity client = new ClientEntity("SW", clinic, "Synthetic", "Owner",
                null, null, null, null, null, null);
        when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic));
        when(clients.findByTenantIdAndId("SW", 2L)).thenReturn(Optional.of(client));
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "DRAFT",
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), "EUR",
                new BigDecimal("100.00"), new BigDecimal("101.00"));

        assertThatThrownBy(() -> service.createInvoice("SW", request))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("paidAmount must not exceed totalAmount");

        verify(invoices, never()).save(any());
    }

    @Test
    void givenAnUnsettledPaidInvoiceWhenCreatedThenItIsRejected() {
        ClinicEntity clinic = clinic(1L);
        ClientEntity client = new ClientEntity("SW", clinic, "Synthetic", "Owner",
                null, null, null, null, null, null);
        when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic));
        when(clients.findByTenantIdAndId("SW", 2L)).thenReturn(Optional.of(client));
        InvoiceRequest request = new InvoiceRequest(1L, 2L, null, "INV-TEST", "PAID",
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), "EUR",
                new BigDecimal("100.00"), new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.createInvoice("SW", request))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("A paid invoice must be fully settled");

        verify(invoices, never()).save(any());
    }

    @Test
    void givenARegionCodeWhenMappedThenOnlySupportedTenantCodesAreReturned() {
        assertThat(service.tenant("sw")).isEqualTo("SW");
        assertThat(service.tenant("se")).isEqualTo("SE");
        assertThat(service.tenant("sc")).isEqualTo("SC");
        assertThat(service.tenant("ne")).isEqualTo("NE");
        assertThat(service.tenant("nw")).isEqualTo("NW");
        assertThat(service.tenant("nc")).isEqualTo("NC");
        assertThatThrownBy(() -> service.tenant("xx"))
                .isInstanceOf(PmsResourceNotFoundException.class)
                .hasMessage("Unknown region");
    }

    @Test
    void givenClinicsIncludingSoftDeletedWhenListedThenOnlyActiveClinicsAreReturned() {
        ClinicEntity visible = clinic(1L);
        visible.update("Visible", "Sample City", null, null, null, true);
        ClinicEntity deleted = clinic(2L);
        deleted.setDeletedAt(NOW);
        when(clinics.findAllByTenantId("SW")).thenReturn(List.of(visible, deleted));

        assertThat(service.clinics("SW"))
                .extracting(view -> view.name())
                .containsExactly("Visible");
    }

    @Test
    void givenAnExistingClinicWhenDeletedThenTheInjectedClockSetsItsDeletionTime() {
        ClinicEntity clinic = clinic(1L);
        when(clinics.findByTenantIdAndId("SW", 1L)).thenReturn(Optional.of(clinic));

        service.deleteClinic("SW", 1L);

        assertThat(clinic.getDeletedAt()).isEqualTo(NOW);
    }

    private static ClinicEntity clinic(long id) {
        ClinicEntity clinic = new ClinicEntity("SW", "Synthetic Clinic", "Sample City",
                null, null, null, true);
        ReflectionTestUtils.setField(clinic, "id", id);
        return clinic;
    }

    private static InvoiceEntity invoice(long id, InvoiceEntity.InvoiceStatus status,
                                         String total, String paid) {
        ClinicEntity clinic = clinic(1L);
        ClientEntity client = new ClientEntity("SW", clinic, "Synthetic", "Owner",
                null, null, null, null, null, null);
        ReflectionTestUtils.setField(client, "id", 2L);
        InvoiceEntity invoice = new InvoiceEntity("SW", clinic, client, null, "INV-TEST", status,
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-14"), "EUR",
                new BigDecimal(total), new BigDecimal(paid));
        ReflectionTestUtils.setField(invoice, "id", id);
        return invoice;
    }
}
