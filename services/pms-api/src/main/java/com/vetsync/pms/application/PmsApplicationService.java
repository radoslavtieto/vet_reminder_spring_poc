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
    private final PmsCatalogApplicationService catalog;
    private final PmsPracticeApplicationService practice;
    private final PmsPatientCareApplicationService patientCare;
    private final PmsPrescriptionApplicationService prescription;
    private final PmsBillingApplicationService billing;

    public PmsApplicationService(PmsCatalogApplicationService catalog,
                                 PmsPracticeApplicationService practice,
                                 PmsPatientCareApplicationService patientCare,
                                 PmsPrescriptionApplicationService prescription,
                                 PmsBillingApplicationService billing) {
        this.catalog = catalog;
        this.practice = practice;
        this.patientCare = patientCare;
        this.prescription = prescription;
        this.billing = billing;
    }

    public String tenant(String region) {
        return TenantResolver.resolve(region);
    }

    public List<SpeciesView> species(String tenant) {
        return catalog.species(tenant);
    }

    public List<BreedView> breeds(String tenant, long speciesId) {
        return catalog.breeds(tenant, speciesId);
    }

    public List<MedicationView> medications(String tenant) {
        return catalog.medications(tenant);
    }

    public List<ClinicView> clinics(String tenant) {
        return practice.clinics(tenant);
    }

    public ClinicView clinic(String tenant, long id) {
        return practice.clinic(tenant, id);
    }

    public ClinicView createClinic(String tenant, ClinicRequest request) {
        return practice.createClinic(tenant, request);
    }

    public ClinicView updateClinic(String tenant, long id, ClinicRequest request) {
        return practice.updateClinic(tenant, id, request);
    }

    public void deleteClinic(String tenant, long id) {
        practice.deleteClinic(tenant, id);
    }

    public List<StaffView> staffForClinic(String tenant, long clinicId) {
        return practice.staffForClinic(tenant, clinicId);
    }

    public List<StaffView> staff(String tenant) {
        return practice.staff(tenant);
    }

    public StaffView staff(String tenant, long id) {
        return practice.staff(tenant, id);
    }

    public StaffView createStaff(String tenant, long clinicId, StaffRequest request) {
        return practice.createStaff(tenant, clinicId, request);
    }

    public StaffView createStaff(String tenant, StaffRequest request) {
        return practice.createStaff(tenant, request);
    }

    public StaffView updateStaff(String tenant, long id, StaffRequest request) {
        return practice.updateStaff(tenant, id, request);
    }

    public void deleteStaff(String tenant, long id) {
        practice.deleteStaff(tenant, id);
    }

    public List<ClientView> clients(String tenant, String query) {
        return practice.clients(tenant, query);
    }

    public ClientView client(String tenant, long id) {
        return practice.client(tenant, id);
    }

    public ClientView createClient(String tenant, ClientRequest request) {
        return practice.createClient(tenant, request);
    }

    public ClientView updateClient(String tenant, long id, ClientRequest request) {
        return practice.updateClient(tenant, id, request);
    }

    public void deleteClient(String tenant, long id) {
        practice.deleteClient(tenant, id);
    }

    public List<PatientView> patients(String tenant, String microchip) {
        return patientCare.patients(tenant, microchip);
    }

    public PatientView patient(String tenant, long id) {
        return patientCare.patient(tenant, id);
    }

    public List<PatientView> patientsForClient(String tenant, long clientId) {
        return patientCare.patientsForClient(tenant, clientId);
    }

    public PatientView createPatientForClient(String tenant, long clientId, PatientRequest request) {
        return patientCare.createPatientForClient(tenant, clientId, request);
    }

    public PatientView createPatient(String tenant, PatientRequest request) {
        return patientCare.createPatient(tenant, request);
    }

    public PatientView updatePatient(String tenant, long id, PatientRequest request) {
        return patientCare.updatePatient(tenant, id, request);
    }

    public PatientView transferPatientToClinic(String tenant, long patientId, long clinicId) {
        return patientCare.transferPatientToClinic(tenant, patientId, clinicId);
    }

    public void deletePatient(String tenant, long id) {
        patientCare.deletePatient(tenant, id);
    }

    public List<HistoryView> history(String tenant, long patientId) {
        return patientCare.history(tenant, patientId);
    }

    public HistoryView addHistory(String tenant, long patientId, HistoryRequest request) {
        return patientCare.addHistory(tenant, patientId, request);
    }

    public HistoryView historyEntry(String tenant, long patientId, long entryId) {
        return patientCare.historyEntry(tenant, patientId, entryId);
    }

    public List<VisitView> visits(String tenant) {
        return patientCare.visits(tenant);
    }

    public VisitView visit(String tenant, long id) {
        return patientCare.visit(tenant, id);
    }

    public List<VisitView> visitsForPatient(String tenant, long patientId) {
        return patientCare.visitsForPatient(tenant, patientId);
    }

    public VisitView createVisitForPatient(String tenant, long patientId, VisitRequest request) {
        return patientCare.createVisitForPatient(tenant, patientId, request);
    }

    public VisitView createVisit(String tenant, VisitRequest request) {
        return patientCare.createVisit(tenant, request);
    }

    public VisitView updateVisit(String tenant, long id, VisitRequest request) {
        return patientCare.updateVisit(tenant, id, request);
    }

    public VisitView transitionVisit(String tenant, long id, String targetStatus) {
        return patientCare.transitionVisit(tenant, id, targetStatus);
    }

    public void deleteVisit(String tenant, long id) {
        patientCare.deleteVisit(tenant, id);
    }

    public List<PrescriptionView> prescriptions(String tenant) {
        return prescription.prescriptions(tenant);
    }

    public PrescriptionView prescription(String tenant, long id) {
        return prescription.prescription(tenant, id);
    }

    public List<PrescriptionView> prescriptionsForVisit(String tenant, long visitId) {
        return prescription.prescriptionsForVisit(tenant, visitId);
    }

    public PrescriptionView createPrescriptionForVisit(String tenant, long visitId, PrescriptionRequest request) {
        return prescription.createPrescriptionForVisit(tenant, visitId, request);
    }

    public PrescriptionView createPrescription(String tenant, PrescriptionRequest request) {
        return prescription.createPrescription(tenant, request);
    }

    public PrescriptionView updatePrescription(String tenant, long id, PrescriptionRequest request) {
        return prescription.updatePrescription(tenant, id, request);
    }

    public void deletePrescription(String tenant, long id) {
        prescription.deletePrescription(tenant, id);
    }

    public List<PrescriptionItemView> prescriptionItems(String tenant, long prescriptionId) {
        return prescription.prescriptionItems(tenant, prescriptionId);
    }

    public PrescriptionItemView addPrescriptionItem(String tenant, long prescriptionId,
                                                     PrescriptionItemRequest request) {
        return prescription.addPrescriptionItem(tenant, prescriptionId, request);
    }
    public List<InvoiceView> invoices(String tenant, String status) {
        return billing.invoices(tenant, status);
    }

    public InvoiceView invoice(String tenant, long id) {
        return billing.invoice(tenant, id);
    }

    public InvoiceView createInvoice(String tenant, InvoiceRequest request) {
        return billing.createInvoice(tenant, request);
    }

    public InvoiceView updateInvoice(String tenant, long id, InvoiceRequest request) {
        return billing.updateInvoice(tenant, id, request);
    }

    public InvoiceView issueInvoice(String tenant, long id) {
        return billing.issueInvoice(tenant, id);
    }

    public InvoiceView voidInvoice(String tenant, long id) {
        return billing.voidInvoice(tenant, id);
    }

    public void deleteInvoice(String tenant, long id) {
        billing.deleteInvoice(tenant, id);
    }

    public List<PaymentView> paymentsForInvoice(String tenant, long invoiceId) {
        return billing.paymentsForInvoice(tenant, invoiceId);
    }

    public PaymentView paymentForInvoice(String tenant, long invoiceId, long paymentId) {
        return billing.paymentForInvoice(tenant, invoiceId, paymentId);
    }

    public PaymentView addPayment(String tenant, long invoiceId, PaymentRequest request) {
        return billing.addPayment(tenant, invoiceId, request);
    }
}
