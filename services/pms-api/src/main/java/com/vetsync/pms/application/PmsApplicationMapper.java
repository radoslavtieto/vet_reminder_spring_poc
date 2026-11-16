package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.InvoiceEntity;
import com.vetsync.pms.adapter.out.persistence.entity.MedicalRecordEntryEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PatientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PrescriptionItemEntity;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;

import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientView;
import static com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicView;
import static com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceView;
import static com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryView;
import static com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientView;
import static com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentView;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionDto.PrescriptionView;
import static com.vetsync.pms.adapter.in.web.dto.PrescriptionItemDto.PrescriptionItemView;
import static com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffView;
import static com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitView;
import static com.vetsync.pms.application.PmsApplicationSupport.name;

final class PmsApplicationMapper {
    private PmsApplicationMapper() { }

    static ClinicView view(ClinicEntity e) {
        return new ClinicView(e.getId(), e.getPublicId(), e.getTenantId(), e.getName(), e.getCity(),
                e.getAddress(), e.getPhone(), e.getEmail(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
    static StaffView view(StaffEntity e) {
        return new StaffView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClinic().getId(),
                e.getRole().name(), e.getFullName(), e.getLicenseNo(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
    static ClientView view(ClientEntity e) {
        return new ClientView(e.getId(), e.getPublicId(), e.getTenantId(), e.getRegisteredClinic().getId(),
                e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(), e.getAddress(), e.getCity(),
                e.getPreferredContact(), e.getGdprConsentAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
    static PatientView view(PatientEntity e) {
        return new PatientView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClient().getId(),
                e.getClinic().getId(), e.getName(), e.getSpecies().getId(),
                e.getBreed() == null ? null : e.getBreed().getId(), name(e.getSex()), e.getBirthDate(),
                e.getMicrochipNo(), e.isNeutered(), e.getWeightKg(), e.getStatus().name(), e.getDeceasedAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
    static VisitView view(VisitEntity e) {
        return new VisitView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getClinic().getId(), e.getVet().getId(), e.getVisitType(), e.getStatus().name(),
                e.getScheduledAt(), e.getStartedAt(), e.getCompletedAt(), e.getReason(), e.getDiagnosis(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
    static PrescriptionView view(PrescriptionEntity e) {
        return new PrescriptionView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getVisit().getId(), e.getVet().getId(), e.getStatus().name(), e.getIssuedOn(), e.getValidUntil(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
    static InvoiceView view(InvoiceEntity e) {
        return new InvoiceView(e.getId(), e.getPublicId(), e.getTenantId(), e.getClinic().getId(),
                e.getClient().getId(), e.getVisit() == null ? null : e.getVisit().getId(), e.getInvoiceNo(),
                e.getStatus().name(), e.getIssuedOn(), e.getDueOn(), e.getCurrency(), e.getTotalAmount(),
                e.getPaidAmount(), e.getCreatedAt(), e.getUpdatedAt());
    }
    static HistoryView view(MedicalRecordEntryEntity e) {
        return new HistoryView(e.getId(), e.getPublicId(), e.getTenantId(), e.getPatient().getId(),
                e.getVisit() == null ? null : e.getVisit().getId(), e.getEntryType().name(), e.getDescription(),
                e.getRecordedAt(), e.getRecordedBy());
    }
    static PaymentView view(PaymentEntity e) {
        return new PaymentView(e.getId(), e.getPublicId(), e.getTenantId(), e.getInvoice().getId(),
                e.getPaidAt(), e.getAmount(), e.getMethod().name());
    }
    static PrescriptionItemView view(PrescriptionItemEntity e) {
        return new PrescriptionItemView(e.getId(), e.getPublicId(), e.getTenantId(),
                e.getPrescription().getId(), e.getMedication().getId(), e.getMedication().getCode(),
                e.getMedication().getName(), e.getDosage(), e.getFrequency(), e.getDurationDays(),
                e.getQuantity(), e.getRefillsAllowed());
    }
}
