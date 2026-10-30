package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.InvoiceDto;
import com.vetsync.pms.adapter.in.web.dto.PaymentDto;
import com.vetsync.pms.application.PmsApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}")
public class InvoiceController {

    private final PmsApplicationService service;

    public InvoiceController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping("/invoices")
    public List<InvoiceDto.InvoiceView> invoices(@PathVariable String region,
                                           @RequestParam(required = false) String status) {
        return service.invoices(service.tenant(region), status);
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDto.InvoiceView> createInvoice(@PathVariable String region,
                                                                  @Valid @RequestBody
                                                                  InvoiceDto.InvoiceRequest request) {
        InvoiceDto.InvoiceView created = service.createInvoice(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/invoices/" + created.id(), created);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceDto.InvoiceView invoice(@PathVariable String region, @PathVariable long id) {
        return service.invoice(service.tenant(region), id);
    }

    @PostMapping("/invoices/{id}/issue")
    public InvoiceDto.InvoiceView issueInvoice(@PathVariable String region, @PathVariable long id) {
        return service.issueInvoice(service.tenant(region), id);
    }

    @PostMapping("/invoices/{id}/void")
    public InvoiceDto.InvoiceView voidInvoice(@PathVariable String region, @PathVariable long id) {
        return service.voidInvoice(service.tenant(region), id);
    }

    @PutMapping("/invoices/{id}")
    public InvoiceDto.InvoiceView updateInvoice(@PathVariable String region, @PathVariable long id,
                                                @Valid @RequestBody InvoiceDto.InvoiceRequest request) {
        return service.updateInvoice(service.tenant(region), id, request);
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable String region, @PathVariable long id) {
        service.deleteInvoice(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    public List<PaymentDto.PaymentView> invoicePayments(@PathVariable String region,
                                                         @PathVariable long invoiceId) {
        return service.paymentsForInvoice(service.tenant(region), invoiceId);
    }

    @GetMapping("/invoices/{invoiceId}/payments/{paymentId}")
    public PaymentDto.PaymentView invoicePayment(@PathVariable String region, @PathVariable long invoiceId,
                                                  @PathVariable long paymentId) {
        return service.paymentForInvoice(service.tenant(region), invoiceId, paymentId);
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    public ResponseEntity<PaymentDto.PaymentView> addInvoicePayment(@PathVariable String region,
                                                                    @PathVariable long invoiceId,
                                                                    @Valid @RequestBody
                                                                    PaymentDto.PaymentRequest request) {
        PaymentDto.PaymentView created = service.addPayment(service.tenant(region), invoiceId, request);
        return ControllerResponses.created("/api/v1/" + region + "/invoices/" + invoiceId
                + "/payments/" + created.id(), created);
    }
}
