package vn.codegym.clinicbook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
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
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;
import vn.codegym.clinicbook.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import vn.codegym.clinicbook.constant.ErrorMessage;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {
    private static final int PATIENTS = 5;

    @Autowired AppointmentService appointmentService;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private Specialty specialty;
    private Doctor doctor;
    private TimeSlot slot;
    private final List<String> patientNames = new ArrayList<>();
    private final List<Patient> patients = new ArrayList<>();

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        specialty = new Specialty();
        specialty.setName("Race specialty " + n);
        specialty = specialtyRepository.save(specialty);

        doctor = new Doctor();
        doctor.setUser(user("race_doc_" + n, Role.DOCTOR));
        doctor.setFullName("Dr. Race " + n);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(5);
        doctor = doctorRepository.save(doctor);

        slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now().plusDays(3));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);

        for (int i = 0; i < PATIENTS; i++) {
            Patient patient = new Patient();
            patient.setUser(user("race_pat_" + n + "_" + i, Role.PATIENT));
            patients.add(patientRepository.save(patient));
            patientNames.add(patient.getUser().getUsername());
        }
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        appointmentRepository.findAll().stream()
                .filter(a -> a.getSlot().getId().equals(slot.getId()))
                .forEach(appointmentRepository::delete);
        slotRepository.deleteById(slot.getId());
        patients.forEach(p -> {
            patientRepository.delete(p);
            userRepository.delete(p.getUser());
        });
        doctorRepository.delete(doctor);
        userRepository.delete(doctor.getUser());
        specialtyRepository.delete(specialty);
    }

    @Test
    void onlyOneOfManySimultaneousBookingsWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PATIENTS);
        CountDownLatch ready = new CountDownLatch(PATIENTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (String name : patientNames) {
            Callable<Boolean> task = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(name, null,
                                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))));
                try {
                    ready.countDown();
                    go.await();
                    appointmentService.book(slot.getId(), "race " + name);
                    return true;
                } catch (BusinessException e) {
                    assertEquals(ErrorMessage.SLOT_UNAVAILABLE, e.getMessage());
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            results.add(pool.submit(task));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();

        int winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        pool.shutdown();

        assertEquals(1, winners, "exactly one patient gets the slot");
        List<Appointment> kept = appointmentRepository.findAll().stream()
                .filter(a -> a.getSlot().getId().equals(slot.getId()))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .toList();
        assertEquals(1, kept.size(), "and only one live appointment exists for it");
        assertEquals(SlotStatus.BOOKED, slotRepository.findById(slot.getId()).orElseThrow().getStatus());
    }

    private User user(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("{noop}secret");
        u.setFullName(username);
        u.setRole(role);
        return userRepository.save(u);
    }
}
