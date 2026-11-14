package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.repository.BreedRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClinicRepository;
import com.vetsync.pms.adapter.out.persistence.repository.ClientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.InvoiceRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicationRepository;
import com.vetsync.pms.adapter.out.persistence.repository.MedicalRecordEntryRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PatientRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PaymentRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionItemRepository;
import com.vetsync.pms.adapter.out.persistence.repository.PrescriptionRepository;
import com.vetsync.pms.adapter.out.persistence.repository.SpeciesRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import com.vetsync.pms.adapter.out.persistence.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PmsApplicationServiceWiringTests {

    @Test
    void givenTheWorkflowRepositoriesWhenTheApplicationServicesAreRegisteredThenTheFacadeIsWired() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ClinicRepository.class, () -> mock(ClinicRepository.class));
            context.registerBean(StaffRepository.class, () -> mock(StaffRepository.class));
            context.registerBean(ClientRepository.class, () -> mock(ClientRepository.class));
            context.registerBean(PatientRepository.class, () -> mock(PatientRepository.class));
            context.registerBean(VisitRepository.class, () -> mock(VisitRepository.class));
            context.registerBean(PrescriptionRepository.class, () -> mock(PrescriptionRepository.class));
            context.registerBean(InvoiceRepository.class, () -> mock(InvoiceRepository.class));
            context.registerBean(MedicationRepository.class, () -> mock(MedicationRepository.class));
            context.registerBean(PaymentRepository.class, () -> mock(PaymentRepository.class));
            context.registerBean(MedicalRecordEntryRepository.class, () -> mock(MedicalRecordEntryRepository.class));
            context.registerBean(PrescriptionItemRepository.class, () -> mock(PrescriptionItemRepository.class));
            context.registerBean(SpeciesRepository.class, () -> mock(SpeciesRepository.class));
            context.registerBean(BreedRepository.class, () -> mock(BreedRepository.class));
            context.registerBean(Clock.class, Clock::systemUTC);
            context.register(PmsCatalogApplicationService.class, PmsPracticeApplicationService.class,
                    PmsPatientCareApplicationService.class, PmsPrescriptionApplicationService.class,
                    PmsBillingApplicationService.class, PmsApplicationService.class);
            context.refresh();

            assertThat(context.getBean(PmsApplicationService.class)).isNotNull();
        }
    }
}
