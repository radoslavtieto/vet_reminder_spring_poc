package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto;
import com.vetsync.pms.adapter.in.web.dto.PatientDto;
import com.vetsync.pms.adapter.in.web.dto.VisitDto;
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
public class PatientController {

    private final PmsApplicationService service;

    public PatientController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping("/patients")
    public List<PatientDto.PatientView> patients(@PathVariable String region,
                                           @RequestParam(required = false) String microchip) {
        return service.patients(service.tenant(region), microchip);
    }

    @PostMapping("/patients")
    public ResponseEntity<PatientDto.PatientView> createPatient(@PathVariable String region,
                                                                  @Valid @RequestBody
                                                                  PatientDto.PatientRequest request) {
        PatientDto.PatientView created = service.createPatient(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/patients/" + created.id(), created);
    }

    @GetMapping("/patients/{id}")
    public PatientDto.PatientView patient(@PathVariable String region, @PathVariable long id) {
        return service.patient(service.tenant(region), id);
    }

    @PutMapping("/patients/{id}")
    public PatientDto.PatientView updatePatient(@PathVariable String region, @PathVariable long id,
                                                @Valid @RequestBody PatientDto.PatientRequest request) {
        return service.updatePatient(service.tenant(region), id, request);
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable String region, @PathVariable long id) {
        service.deletePatient(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients/{clientId}/patients")
    public List<PatientDto.PatientView> clientPatients(@PathVariable String region,
                                                        @PathVariable long clientId) {
        return service.patientsForClient(service.tenant(region), clientId);
    }

    @PostMapping("/clients/{clientId}/patients")
    public ResponseEntity<PatientDto.PatientView> createClientPatient(@PathVariable String region,
                                                                       @PathVariable long clientId,
                                                                       @Valid @RequestBody
                                                                       PatientDto.PatientRequest request) {
        PatientDto.PatientView created = service.createPatientForClient(service.tenant(region), clientId, request);
        return ControllerResponses.created("/api/v1/" + region + "/patients/" + created.id(), created);
    }

    @GetMapping("/patients/{patientId}/history")
    public List<MedicalRecordEntryDto.HistoryView> patientHistory(@PathVariable String region,
                                                                   @PathVariable long patientId) {
        return service.history(service.tenant(region), patientId);
    }

    @GetMapping("/patients/{patientId}/history/{entryId}")
    public MedicalRecordEntryDto.HistoryView patientHistoryEntry(@PathVariable String region,
                                                                  @PathVariable long patientId,
                                                                  @PathVariable long entryId) {
        return service.historyEntry(service.tenant(region), patientId, entryId);
    }

    @PostMapping("/patients/{patientId}/history")
    public ResponseEntity<MedicalRecordEntryDto.HistoryView> addPatientHistory(@PathVariable String region,
                                                                                @PathVariable long patientId,
                                                                                @Valid @RequestBody
                                                                                MedicalRecordEntryDto.HistoryRequest request) {
        MedicalRecordEntryDto.HistoryView created = service.addHistory(service.tenant(region), patientId, request);
        return ControllerResponses.created("/api/v1/" + region + "/patients/" + patientId
                + "/history/" + created.id(), created);
    }

    @GetMapping("/patients/{patientId}/visits")
    public List<VisitDto.VisitView> patientVisits(@PathVariable String region, @PathVariable long patientId) {
        return service.visitsForPatient(service.tenant(region), patientId);
    }

    @PostMapping("/patients/{patientId}/visits")
    public ResponseEntity<VisitDto.VisitView> createPatientVisit(@PathVariable String region,
                                                                  @PathVariable long patientId,
                                                                  @Valid @RequestBody VisitDto.VisitRequest request) {
        VisitDto.VisitView created = service.createVisitForPatient(service.tenant(region), patientId, request);
        return ControllerResponses.created("/api/v1/" + region + "/visits/" + created.id(), created);
    }
}
