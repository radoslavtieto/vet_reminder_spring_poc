package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.BreedEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.MedicalRecordEntryEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PatientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.SpeciesEntity;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.entity.VisitEntity;
import com.vetsync.pms.adapter.out.persistence.repository.BreedRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicalRecordEntryRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PatientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.SpeciesRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import com.vetsync.pms.adapter.out.persistence.repository.VisitRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryRequest;
import static com.vetsync.pms.adapter.in.web.dto.MedicalRecordEntryDto.HistoryView;
import static com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientRequest;
import static com.vetsync.pms.adapter.in.web.dto.PatientDto.PatientView;
import static com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitRequest;
import static com.vetsync.pms.adapter.in.web.dto.VisitDto.VisitView;
import static com.vetsync.pms.application.PmsApplicationSupport.*;

@Service
@Transactional
class PmsPatientCareApplicationService {
    private final ClinicRepository clinics;
    private final StaffRepository staff;
    private final ClientRepository clients;
    private final PatientRepository patients;
    private final VisitRepository visits;
    private final MedicalRecordEntryRepository history;
    private final SpeciesRepository species;
    private final BreedRepository breeds;
    private final Clock clock;

    PmsPatientCareApplicationService(ClinicRepository clinics, StaffRepository staff,
                                     ClientRepository clients, PatientRepository patients,
                                     VisitRepository visits, MedicalRecordEntryRepository history,
                                     SpeciesRepository species, BreedRepository breeds, Clock clock) {
        this.clinics = clinics;
        this.staff = staff;
        this.clients = clients;
        this.patients = patients;
        this.visits = visits;
        this.history = history;
        this.species = species;
        this.breeds = breeds;
        this.clock = clock;
    }

    // Patients
    public List<PatientView> patients(String tenant, String microchip) {
        List<PatientEntity> result = microchip == null || microchip.isBlank()
                ? patients.findAllByTenantId(tenant)
                : patients.findByTenantIdAndMicrochipNo(tenant, microchip.trim()).stream().toList();
        return active(result).stream().map(PmsApplicationMapper::view).toList();
    }

    public PatientView patient(String tenant, long id) { return PmsApplicationMapper.view(patientEntity(tenant, id)); }
    public List<PatientView> patientsForClient(String tenant, long clientId) {
        clientEntity(tenant, clientId);
        return active(patients.findAllByTenantIdAndClient_Id(tenant, clientId)).stream()
                .map(PmsApplicationMapper::view).toList();
    }

    public PatientView createPatientForClient(String tenant, long clientId, PatientRequest request) {
        clientEntity(tenant, clientId);
        if (request.clientId() != null && request.clientId() != clientId) {
            throw badRequest("Client does not match path");
        }
        PatientRequest bound = new PatientRequest(clientId, request.clinicId(), request.name(), request.speciesId(),
                request.breedId(), request.sex(), request.birthDate(), request.microchipNo(), request.neutered(),
                request.weightKg(), request.status(), request.deceasedAt());
        return createPatient(tenant, bound);
    }

    public PatientView createPatient(String tenant, PatientRequest request) {
        if (request.clientId() == null) throw badRequest("clientId is required");
        return PmsApplicationMapper.view(patients.save(patientFrom(tenant, request)));
    }

    public PatientView updatePatient(String tenant, long id, PatientRequest request) {
        if (request.clientId() == null) throw badRequest("clientId is required");
        PatientEntity entity = patientEntity(tenant, id);
        patientFrom(tenant, request, entity);
        return PmsApplicationMapper.view(patients.saveAndFlush(entity));
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
        return PmsApplicationMapper.view(patients.saveAndFlush(patient));
    }

    public void deletePatient(String tenant, long id) {
        PmsApplicationSupport.softDelete(patientEntity(tenant, id), clock);
    }

    // Medical record entries
    public List<HistoryView> history(String tenant, long patientId) {
        patientEntity(tenant, patientId);
        return active(history.findAllByTenantIdAndPatient_IdOrderByRecordedAtDesc(tenant, patientId))
                .stream().map(PmsApplicationMapper::view).toList();
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
        return PmsApplicationMapper.view(history.save(entity));
    }

    public HistoryView historyEntry(String tenant, long patientId, long entryId) {
        MedicalRecordEntryEntity entity = find(history.findByTenantIdAndId(tenant, entryId));
        if (!Objects.equals(entity.getPatient().getId(), patientId)) {
            throw missing("History entry does not belong to patient");
        }
        patientEntity(tenant, patientId);
        return PmsApplicationMapper.view(entity);
    }

    // Visits
    public List<VisitView> visits(String tenant) {
        return active(visits.findAllByTenantId(tenant)).stream().map(PmsApplicationMapper::view).toList();
    }

    public VisitView visit(String tenant, long id) { return PmsApplicationMapper.view(visitEntity(tenant, id)); }
    public List<VisitView> visitsForPatient(String tenant, long patientId) {
        patientEntity(tenant, patientId);
        return active(visits.findAllByTenantIdAndPatient_IdOrderByScheduledAtDesc(tenant, patientId))
                .stream().map(PmsApplicationMapper::view).toList();
    }

    public VisitView createVisitForPatient(String tenant, long patientId, VisitRequest request) {
        patientEntity(tenant, patientId);
        if (request.patientId() != null && request.patientId() != patientId) {
            throw badRequest("Patient does not match path");
        }
        VisitRequest bound = new VisitRequest(patientId, request.clinicId(), request.vetId(), request.visitType(),
                request.status(), request.scheduledAt(), request.startedAt(), request.completedAt(), request.reason(),
                request.diagnosis(), request.notes());
        return createVisit(tenant, bound);
    }

    public VisitView createVisit(String tenant, VisitRequest request) {
        if (request.patientId() == null) throw badRequest("patientId is required");
        return PmsApplicationMapper.view(visits.save(visitFrom(tenant, request)));
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
        return PmsApplicationMapper.view(visits.saveAndFlush(entity));
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
        return PmsApplicationMapper.view(visits.saveAndFlush(entity));
    }

    public void deleteVisit(String tenant, long id) {
        PmsApplicationSupport.softDelete(visitEntity(tenant, id), clock);
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

    private void verifyVisitLinks(PatientEntity patient, ClinicEntity clinic, StaffEntity vet) {
        if (!Objects.equals(patient.getClinic().getId(), clinic.getId())
                || !Objects.equals(vet.getClinic().getId(), clinic.getId())) {
            throw missing("Patient, clinic and vet must belong to the same clinic");
        }
    }

    private ClinicEntity clinicEntity(String tenant, long id) { return find(clinics.findByTenantIdAndId(tenant, id)); }
    private StaffEntity staffEntity(String tenant, long id) { return find(staff.findByTenantIdAndId(tenant, id)); }
    private ClientEntity clientEntity(String tenant, long id) { return find(clients.findByTenantIdAndId(tenant, id)); }
    private PatientEntity patientEntity(String tenant, long id) {
        return find(patients.findByTenantIdAndId(tenant, id));
    }
    private VisitEntity visitEntity(String tenant, long id) { return find(visits.findByTenantIdAndId(tenant, id)); }
    private SpeciesEntity speciesEntity(String tenant, long id) {
        return find(species.findByTenantIdAndId(tenant, id));
    }
    private BreedEntity breedEntity(String tenant, Long id, long speciesId) {
        if (id == null) return null;
        BreedEntity breed = find(breeds.findByTenantIdAndId(tenant, id));
        if (!Objects.equals(breed.getSpecies().getId(), speciesId)) throw missing("Breed does not belong to species");
        return breed;
    }
}
