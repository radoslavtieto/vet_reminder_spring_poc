package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.InvoiceEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PaymentEntity;
import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;
import com.vetsync.pms.adapter.out.persistence.repository.ClientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import com.vetsync.pms.adapter.out.persistence.repository.InvoiceRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PaymentRepository;
import com.vetsync.pms.adapter.out.persistence.repository.VisitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceRequest;
import static com.vetsync.pms.adapter.in.web.dto.InvoiceDto.InvoiceView;
import static com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentRequest;
import static com.vetsync.pms.adapter.in.web.dto.PaymentDto.PaymentView;
import static com.vetsync.pms.application.PmsApplicationSupport.*;

@Service
@Transactional
class PmsBillingApplicationService {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ClinicRepository clinics;
    private final ClientRepository clients;
    private final VisitRepository visits;
    private final Clock clock;

    PmsBillingApplicationService(InvoiceRepository invoices, PaymentRepository payments,
                                 ClinicRepository clinics, ClientRepository clients,
                                 VisitRepository visits, Clock clock) {
        this.invoices = invoices;
        this.payments = payments;
        this.clinics = clinics;
        this.clients = clients;
        this.visits = visits;
        this.clock = clock;
    }

    // Invoices
    public List<InvoiceView> invoices(String tenant, String status) {
        if (status != null && !status.isBlank()) enumValue(InvoiceEntity.InvoiceStatus.class, status);
        return active(invoices.findAllByTenantId(tenant)).stream()
                .filter(invoice -> status == null || status.isBlank()
                        || invoice.getStatus().name().equalsIgnoreCase(status))
                .map(PmsApplicationMapper::view).toList();
    }

    public InvoiceView invoice(String tenant, long id) { return PmsApplicationMapper.view(invoiceEntity(tenant, id)); }
    public InvoiceView createInvoice(String tenant, InvoiceRequest request) {
        return PmsApplicationMapper.view(invoices.save(invoiceFrom(tenant, request)));
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
        return PmsApplicationMapper.view(invoices.saveAndFlush(entity));
    }

    public InvoiceView issueInvoice(String tenant, long id) {
        InvoiceEntity invoice = invoiceEntity(tenant, id);
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.ISSUED) {
            return PmsApplicationMapper.view(invoice);
        }
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.DRAFT
                || invoice.getPaidAmount().signum() != 0) {
            throw badRequest("Only unpaid draft invoices can be issued");
        }
        invoice.transitionTo(InvoiceEntity.InvoiceStatus.ISSUED);
        return PmsApplicationMapper.view(invoices.saveAndFlush(invoice));
    }

    public InvoiceView voidInvoice(String tenant, long id) {
        InvoiceEntity invoice = invoiceEntity(tenant, id);
        if (invoice.getStatus() == InvoiceEntity.InvoiceStatus.VOID) {
            return PmsApplicationMapper.view(invoice);
        }
        if (invoice.getPaidAmount().signum() != 0) {
            throw badRequest("An invoice with payments cannot be voided");
        }
        if (invoice.getStatus() != InvoiceEntity.InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceEntity.InvoiceStatus.ISSUED) {
            throw badRequest("Only draft or unpaid issued invoices can be voided");
        }
        invoice.transitionTo(InvoiceEntity.InvoiceStatus.VOID);
        return PmsApplicationMapper.view(invoices.saveAndFlush(invoice));
    }

    public void deleteInvoice(String tenant, long id) {
        PmsApplicationSupport.softDelete(invoiceEntity(tenant, id), clock);
    }

    // Payments
    public List<PaymentView> paymentsForInvoice(String tenant, long invoiceId) {
        invoiceEntity(tenant, invoiceId);
        return active(payments.findAllByTenantIdAndInvoice_IdOrderByPaidAtDesc(tenant, invoiceId))
                .stream().map(PmsApplicationMapper::view).toList();
    }

    public PaymentView paymentForInvoice(String tenant, long invoiceId, long paymentId) {
        PaymentEntity entity = find(payments.findByTenantIdAndId(tenant, paymentId));
        if (!Objects.equals(entity.getInvoice().getId(), invoiceId)) {
            throw missing("Payment does not belong to invoice");
        }
        invoiceEntity(tenant, invoiceId);
        return PmsApplicationMapper.view(entity);
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
        return PmsApplicationMapper.view(payment);
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
    private ClientEntity clientEntity(String tenant, long id) { return find(clients.findByTenantIdAndId(tenant, id)); }
    private VisitEntity visitEntity(String tenant, long id) { return find(visits.findByTenantIdAndId(tenant, id)); }
    private InvoiceEntity invoiceEntity(String tenant, long id) {
        return find(invoices.findByTenantIdAndId(tenant, id));
    }
}
