package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DoctorDashboardTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String doctorName;
    private String patientFullName;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Queue specialty " + n);
        specialty = specialtyRepository.save(specialty);

        doctorName = "queue_doc_" + n;
        Doctor doctor = new Doctor();
        doctor.setUser(newUser(doctorName, Role.DOCTOR));
        doctor.setFullName("Dr. Queue " + n);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(4);
        doctor = doctorRepository.save(doctor);

        User patientUser = newUser("queue_pat_" + n, Role.PATIENT);
        patientFullName = patientUser.getFullName();
        Patient patient = new Patient();
        patient.setUser(patientUser);
        patient = patientRepository.save(patient);

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now());
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.BOOKED);
        slot = slotRepository.save(slot);

        appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReason("Headache");
        appointment = appointmentRepository.save(appointment);
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/doctor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void patientsCannotOpenTheDoctorDashboard() throws Exception {
        mockMvc.perform(get("/doctor/dashboard").with(user("someone").roles("PATIENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?denied"));
    }

    @Test
    void doctorSeesTodaysQueue() throws Exception {
        mockMvc.perform(get("/doctor/dashboard").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/dashboard"))
                .andExpect(model().attribute("dayTotal", 1))
                .andExpect(content().string(containsString(patientFullName)));
    }

    @Test
    void statusFilterAndSearchNarrowTheQueue() throws Exception {
        mockMvc.perform(get("/doctor/dashboard").param("status", "COMPLETED").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()));
        mockMvc.perform(get("/doctor/dashboard").param("keyword", "no-such-patient").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()));
        mockMvc.perform(get("/doctor/dashboard").param("status", "bogus").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", "ALL"));
    }

    @Test
    void confirmingFromTheDashboardComesBackToIt() throws Exception {
        LocalDate today = LocalDate.now();
        mockMvc.perform(post("/doctor/appointments/{id}/status", appointment.getId()).with(csrf())
                        .with(user(doctorName).roles("DOCTOR"))
                        .param("status", "CONFIRMED")
                        .param("from", "dashboard")
                        .param("date", today.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/dashboard?date=" + today));

        assertEquals(AppointmentStatus.CONFIRMED,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    @Test
    void unknownFromValueFallsBackToTheScheduleAndNeverRedirectsElsewhere() throws Exception {
        mockMvc.perform(post("/doctor/appointments/{id}/status", appointment.getId()).with(csrf())
                        .with(user(doctorName).roles("DOCTOR"))
                        .param("status", "CONFIRMED")
                        .param("from", "https://evil.example"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/schedule"));
    }

    @Test
    void doctorLandsOnTheDashboardAfterLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user(doctorName).password("Secret123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/dashboard"));
    }

    private User newUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("Secret123!"));
        u.setFullName("Name " + username);
        u.setRole(role);
        return userRepository.save(u);
    }
}
