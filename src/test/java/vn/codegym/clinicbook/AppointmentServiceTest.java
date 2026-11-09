package vn.codegym.clinicbook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.SpecialtyRequest;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.repository.AuditLogRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;
import vn.codegym.clinicbook.service.AppointmentService;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import vn.codegym.clinicbook.constant.ErrorMessage;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentServiceTest {
    @Autowired AppointmentService appointmentService;
    @Autowired SpecialtyService specialtyService;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        Specialty specialty = new Specialty();
        specialty.setName("Test specialty " + System.nanoTime());
        specialty = specialtyRepository.save(specialty);

        User doctorUser = user("doc_" + System.nanoTime(), Role.DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setFullName("Dr. Test");
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(5);
        doctor = doctorRepository.save(doctor);

        slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patientCanBookFreeSlot_slotBecomesBooked_andAuditLogIsWritten() {
        String patientName = newPatient();
        loginAs(patientName, "PATIENT");
        long auditBefore = auditLogRepository.count();

        Appointment appointment = appointmentService.book(slot.getId(), "Headache");

        assertEquals(AppointmentStatus.PENDING, appointment.getStatus());
        assertEquals(SlotStatus.BOOKED, slotRepository.findById(slot.getId()).orElseThrow().getStatus());
        assertEquals(auditBefore + 1, auditLogRepository.count());
    }

    @Test
    void secondPatientCannotBookTheSameSlot() {
        loginAs(newPatient(), "PATIENT");
        appointmentService.book(slot.getId(), "first");

        loginAs(newPatient(), "PATIENT");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.book(slot.getId(), "second"));
        assertEquals(ErrorMessage.SLOT_UNAVAILABLE, ex.getMessage());
    }

    @Test
    void cancelFreesTheSlotAgain() {
        loginAs(newPatient(), "PATIENT");
        Appointment appointment = appointmentService.book(slot.getId(), "x");

        appointmentService.cancel(appointment.getId());

        assertEquals(SlotStatus.AVAILABLE, slotRepository.findById(slot.getId()).orElseThrow().getStatus());
        assertEquals(AppointmentStatus.CANCELLED,
                appointmentService.findById(appointment.getId()).getStatus());
    }

    @Test
    void methodSecurity_patientCannotCreateSpecialty() {
        loginAs(newPatient(), "PATIENT");
        SpecialtyRequest request = new SpecialtyRequest();
        request.setName("Hacked");
        assertThrows(AccessDeniedException.class, () -> specialtyService.create(request));
    }

    @Test
    void methodSecurity_adminCanCreateSpecialty() {
        loginAs("admin_test", "ADMIN");
        SpecialtyRequest request = new SpecialtyRequest();
        request.setName("Allowed " + System.nanoTime());
        Specialty created = specialtyService.create(request);
        assertTrue(created.getId() != null);
    }

    @Test
    void methodSecurity_adminOnlyWatchesAppointments() {
        loginAs(newPatient(), "PATIENT");
        Appointment appointment = appointmentService.book(slot.getId(), "x");

        loginAs("admin_test", "ADMIN");
        assertThrows(AccessDeniedException.class,
                () -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.CONFIRMED));
        assertThrows(AccessDeniedException.class, () -> appointmentService.cancel(appointment.getId()));
        assertThrows(AccessDeniedException.class, () -> appointmentService.book(slot.getId(), "admin"));
        assertEquals(AppointmentStatus.PENDING, appointmentService.findById(appointment.getId()).getStatus());
    }

    @Test
    void methodSecurity_patientCannotConfirmHisOwnAppointment() {
        loginAs(newPatient(), "PATIENT");
        Appointment appointment = appointmentService.book(slot.getId(), "x");
        assertThrows(AccessDeniedException.class,
                () -> appointmentService.updateStatus(appointment.getId(), AppointmentStatus.CONFIRMED));
    }

    @Test
    void auditRowCarriesTheFactsOfTheAppointment() {
        loginAs(newPatient(), "PATIENT");
        Appointment appointment = appointmentService.book(slot.getId(), "x");

        var newest = auditLogRepository.findAll().stream()
                .filter(l -> l.getDetail() != null
                        && l.getDetail().contains(vn.codegym.clinicbook.aop.AuditDetail.keyOf(appointment.getId())))
                .findFirst().orElseThrow();
        assertEquals("APPOINTMENT_BOOKED", newest.getAction());
        assertTrue(newest.getDetail().contains("status=PENDING"), newest.getDetail());
        assertTrue(newest.getDetail().contains("doctor=Dr. Test"), newest.getDetail());
    }

    private String newPatient() {
        User u = user("pat_" + System.nanoTime(), Role.PATIENT);
        Patient p = new Patient();
        p.setUser(u);
        patientRepository.save(p);
        return u.getUsername();
    }

    private User user(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("{noop}secret");
        u.setFullName(username);
        u.setRole(role);
        return userRepository.save(u);
    }

    private void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
