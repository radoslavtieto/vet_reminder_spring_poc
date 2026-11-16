package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.ClientEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.repository.ClientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientRequest;
import static com.vetsync.pms.adapter.in.web.dto.ClientDto.ClientView;
import static com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicRequest;
import static com.vetsync.pms.adapter.in.web.dto.ClinicDto.ClinicView;
import static com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffRequest;
import static com.vetsync.pms.adapter.in.web.dto.StaffDto.StaffView;
import static com.vetsync.pms.application.PmsApplicationSupport.*;

@Service
@Transactional
class PmsPracticeApplicationService {
    private final ClinicRepository clinics;
    private final StaffRepository staff;
    private final ClientRepository clients;
    private final Clock clock;

    PmsPracticeApplicationService(ClinicRepository clinics, StaffRepository staff,
                                  ClientRepository clients, Clock clock) {
        this.clinics = clinics;
        this.staff = staff;
        this.clients = clients;
        this.clock = clock;
    }

    // Clinics
    public List<ClinicView> clinics(String tenant) {
        return active(clinics.findAllByTenantId(tenant)).stream().map(PmsApplicationMapper::view).toList();
    }

    public ClinicView clinic(String tenant, long id) { return PmsApplicationMapper.view(clinicEntity(tenant, id)); }
    public ClinicView createClinic(String tenant, ClinicRequest request) {
        return PmsApplicationMapper.view(clinics.save(new ClinicEntity(tenant, request.name(),
                request.city(), request.address(), request.phone(), request.email(), request.active())));
    }

    public ClinicView updateClinic(String tenant, long id, ClinicRequest request) {
        ClinicEntity entity = clinicEntity(tenant, id);
        entity.update(request.name(), request.city(), request.address(), request.phone(), request.email(),
                request.active());
        return PmsApplicationMapper.view(clinics.saveAndFlush(entity));
    }

    public void deleteClinic(String tenant, long id) {
        PmsApplicationSupport.softDelete(clinicEntity(tenant, id), clock);
    }

    // Staff
    public List<StaffView> staffForClinic(String tenant, long clinicId) {
        clinicEntity(tenant, clinicId);
        return active(staff.findAllByTenantIdAndClinic_Id(tenant, clinicId)).stream()
                .map(PmsApplicationMapper::view).toList();
    }

    public List<StaffView> staff(String tenant) {
        return active(staff.findAllByTenantId(tenant)).stream().map(PmsApplicationMapper::view).toList();
    }

    public StaffView staff(String tenant, long id) { return PmsApplicationMapper.view(staffEntity(tenant, id)); }
    public StaffView createStaff(String tenant, long clinicId, StaffRequest request) {
        ClinicEntity clinic = clinicEntity(tenant, clinicId);
        if (request.clinicId() != null && request.clinicId() != clinicId) {
            throw badRequest("Clinic does not match path");
        }
        StaffEntity entity = new StaffEntity(tenant, clinic,
                enumValue(StaffEntity.StaffRole.class, request.role()), request.fullName(),
                request.licenseNo(), request.active());
        return PmsApplicationMapper.view(staff.save(entity));
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
        return PmsApplicationMapper.view(staff.saveAndFlush(entity));
    }

    public void deleteStaff(String tenant, long id) {
        PmsApplicationSupport.softDelete(staffEntity(tenant, id), clock);
    }

    // Clients
    public List<ClientView> clients(String tenant, String query) {
        String needle = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        return active(clients.findAllByTenantId(tenant)).stream()
                .filter(c -> needle == null || needle.isEmpty()
                        || (c.getFirstName() + " " + c.getLastName()).toLowerCase(Locale.ROOT).contains(needle)
                        || contains(c.getEmail(), needle) || contains(c.getPhone(), needle))
                .map(PmsApplicationMapper::view).toList();
    }

    public ClientView client(String tenant, long id) { return PmsApplicationMapper.view(clientEntity(tenant, id)); }
    public ClientView createClient(String tenant, ClientRequest request) {
        ClinicEntity clinic = clinicEntity(tenant, request.registeredClinicId());
        ClientEntity entity = new ClientEntity(tenant, clinic, request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), request.city(),
                request.preferredContact(), request.gdprConsentAt());
        return PmsApplicationMapper.view(clients.save(entity));
    }

    public ClientView updateClient(String tenant, long id, ClientRequest request) {
        ClientEntity entity = clientEntity(tenant, id);
        entity.update(clinicEntity(tenant, request.registeredClinicId()), request.firstName(), request.lastName(),
                request.email(), request.phone(), request.address(), request.city(), request.preferredContact(),
                request.gdprConsentAt());
        return PmsApplicationMapper.view(clients.saveAndFlush(entity));
    }

    public void deleteClient(String tenant, long id) {
        PmsApplicationSupport.softDelete(clientEntity(tenant, id), clock);
    }
    private ClinicEntity clinicEntity(String tenant, long id) { return find(clinics.findByTenantIdAndId(tenant, id)); }
    private StaffEntity staffEntity(String tenant, long id) { return find(staff.findByTenantIdAndId(tenant, id)); }
    private ClientEntity clientEntity(String tenant, long id) { return find(clients.findByTenantIdAndId(tenant, id)); }
}
