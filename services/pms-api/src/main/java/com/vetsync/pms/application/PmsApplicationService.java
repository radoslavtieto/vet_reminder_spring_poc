package com.vetsync.pms.application;

import java.util.List;
import org.springframework.stereotype.Service;

import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.BreedView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.MedicationView;
import static com.vetsync.pms.adapter.in.web.dto.CatalogDto.SpeciesView;
import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientRequest;
import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientView;
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

@Service
public class PmsApplicationService {
    private final PmsCatalogApplicationService catalogService;
    private final PmsPracticeApplicationService practiceService;
    private final PmsPatientCareApplicationService patientCareService;
    private final PmsPrescriptionApplicationService prescriptionService;
    private final PmsBillingApplicationService billingService;

    public PmsApplicationService(PmsCatalogApplicationService catalogService,
                                 PmsPracticeApplicationService practiceService,
                                 PmsPatientCareApplicationService patientCareService,
                                 PmsPrescriptionApplicationService prescriptionService,
                                 PmsBillingApplicationService billingService) {
        this.catalogService = catalogService;
        this.practiceService = practiceService;
        this.patientCareService = patientCareService;
        this.prescriptionService = prescriptionService;
        this.billingService = billingService;
    }

    public String tenant(String region) {
        return TenantResolver.resolve(region);
    }

    public List<SpeciesView> species(String tenant) {
        return catalogService.species(tenant);
    }

    public List<BreedView> breeds(String tenant, long speciesId) {
        return catalogService.breeds(tenant, speciesId);
    }

    public List<MedicationView> medications(String tenant) {
        return catalogService.medications(tenant);
    }

    public List<ClinicView> clinics(String tenant) {
        return practiceService.clinics(tenant);
    }

    public ClinicView clinic(String tenant, long id) {
        return practiceService.clinic(tenant, id);
    }

    public ClinicView createClinic(String tenant, ClinicRequest request) {
        return practiceService.createClinic(tenant, request);
    }

    public ClinicView updateClinic(String tenant, long id, ClinicRequest request) {
        return practiceService.updateClinic(tenant, id, request);
    }

    public void deleteClinic(String tenant, long id) {
        practiceService.deleteClinic(tenant, id);
    }

    public List<StaffView> staffForClinic(String tenant, long clinicId) {
        return practiceService.staffForClinic(tenant, clinicId);
    }

    public List<StaffView> staff(String tenant) {
        return practiceService.staff(tenant);
    }

    public StaffView staff(String tenant, long id) {
        return practiceService.staff(tenant, id);
    }

    public StaffView createStaff(String tenant, long clinicId, StaffRequest request) {
        return practiceService.createStaff(tenant, clinicId, request);
    }

    public StaffView createStaff(String tenant, StaffRequest request) {
        return practiceService.createStaff(tenant, request);
    }

    public StaffView updateStaff(String tenant, long id, StaffRequest request) {
        return practiceService.updateStaff(tenant, id, request);
    }

    public void deleteStaff(String tenant, long id) {
        practiceService.deleteStaff(tenant, id);
    }

    public List<ClientView> clients(String tenant, String query) {
        return practiceService.clients(tenant, query);
    }

    public ClientView client(String tenant, long id) {
        return practiceService.client(tenant, id);
    }

    public ClientView createClient(String tenant, ClientRequest request) {
        return practiceService.createClient(tenant, request);
    }

    public ClientView updateClient(String tenant, long id, ClientRequest request) {
        return practiceService.updateClient(tenant, id, request);
    }

    public void deleteClient(String tenant, long id) {
        practiceService.deleteClient(tenant, id);
    }

    public List<PatientView> patients(String tenant, String microchip) {
        return patientCareService.patients(tenant, microchip);
    }

    public PatientView patient(String tenant, long id) {
        return patientCareService.patient(tenant, id);
    }

    public List<PatientView> patientsForClient(String tenant, long clientId) {
        return patientCareService.patientsForClient(tenant, clientId);
    }

    public PatientView createPatientForClient(String tenant, long clientId, PatientRequest request) {
        return patientCareService.createPatientForClient(tenant, clientId, request);
    }

    public PatientView createPatient(String tenant, PatientRequest request) {
        return patientCareService.createPatient(tenant, request);
    }

    public PatientView updatePatient(String tenant, long id, PatientRequest request) {
        return patientCareService.updatePatient(tenant, id, request);
    }

    public PatientView transferPatientToClinic(String tenant, long patientId, long clinicId) {
        return patientCareService.transferPatientToClinic(tenant, patientId, clinicId);
    }

    public void deletePatient(String tenant, long id) {
        patientCareService.deletePatient(tenant, id);
    }

    public List<HistoryView> history(String tenant, long patientId) {
        return patientCareService.history(tenant, patientId);
    }

    public HistoryView addHistory(String tenant, long patientId, HistoryRequest request) {
        return patientCareService.addHistory(tenant, patientId, request);
    }

    public HistoryView historyEntry(String tenant, long patientId, long entryId) {
        return patientCareService.historyEntry(tenant, patientId, entryId);
    }

    public List<VisitView> visits(String tenant) {
        return patientCareService.visits(tenant);
    }

    public VisitView visit(String tenant, long id) {
        return patientCareService.visit(tenant, id);
    }

    public List<VisitView> visitsForPatient(String tenant, long patientId) {
        return patientCareService.visitsForPatient(tenant, patientId);
    }

    public VisitView createVisitForPatient(String tenant, long patientId, VisitRequest request) {
        return patientCareService.createVisitForPatient(tenant, patientId, request);
    }

    public VisitView createVisit(String tenant, VisitRequest request) {
        return patientCareService.createVisit(tenant, request);
    }

    public VisitView updateVisit(String tenant, long id, VisitRequest request) {
        return patientCareService.updateVisit(tenant, id, request);
    }

    public VisitView transitionVisit(String tenant, long id, String targetStatus) {
        return patientCareService.transitionVisit(tenant, id, targetStatus);
    }

    public void deleteVisit(String tenant, long id) {
        patientCareService.deleteVisit(tenant, id);
    }

    public List<PrescriptionView> prescriptions(String tenant) {
        return prescriptionService.prescriptions(tenant);
    }

    public PrescriptionView prescription(String tenant, long id) {
        return prescriptionService.prescription(tenant, id);
    }

    public List<PrescriptionView> prescriptionsForVisit(String tenant, long visitId) {
        return prescriptionService.prescriptionsForVisit(tenant, visitId);
    }

    public PrescriptionView createPrescriptionForVisit(String tenant, long visitId, PrescriptionRequest request) {
        return prescriptionService.createPrescriptionForVisit(tenant, visitId, request);
    }

    public PrescriptionView createPrescription(String tenant, PrescriptionRequest request) {
        return prescriptionService.createPrescription(tenant, request);
    }

    public PrescriptionView updatePrescription(String tenant, long id, PrescriptionRequest request) {
        return prescriptionService.updatePrescription(tenant, id, request);
    }

    public void deletePrescription(String tenant, long id) {
        prescriptionService.deletePrescription(tenant, id);
    }

    public List<PrescriptionItemView> prescriptionItems(String tenant, long prescriptionId) {
        return prescriptionService.prescriptionItems(tenant, prescriptionId);
    }

    public PrescriptionItemView addPrescriptionItem(String tenant, long prescriptionId,
                                                     PrescriptionItemRequest request) {
        return prescriptionService.addPrescriptionItem(tenant, prescriptionId, request);
    }
    public List<InvoiceView> invoices(String tenant, String status) {
        return billingService.invoices(tenant, status);
    }

    public InvoiceView invoice(String tenant, long id) {
        return billingService.invoice(tenant, id);
    }

    public InvoiceView createInvoice(String tenant, InvoiceRequest request) {
        return billingService.createInvoice(tenant, request);
    }

    public InvoiceView updateInvoice(String tenant, long id, InvoiceRequest request) {
        return billingService.updateInvoice(tenant, id, request);
    }

    public InvoiceView issueInvoice(String tenant, long id) {
        return billingService.issueInvoice(tenant, id);
    }

    public InvoiceView voidInvoice(String tenant, long id) {
        return billingService.voidInvoice(tenant, id);
    }

    public void deleteInvoice(String tenant, long id) {
        billingService.deleteInvoice(tenant, id);
    }

    public List<PaymentView> paymentsForInvoice(String tenant, long invoiceId) {
        return billingService.paymentsForInvoice(tenant, invoiceId);
    }

    public PaymentView paymentForInvoice(String tenant, long invoiceId, long paymentId) {
        return billingService.paymentForInvoice(tenant, invoiceId, paymentId);
    }

    public PaymentView addPayment(String tenant, long invoiceId, PaymentRequest request) {
        return billingService.addPayment(tenant, invoiceId, request);
    }
}
