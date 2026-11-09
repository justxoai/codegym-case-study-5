package vn.codegym.clinicbook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
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
import vn.codegym.clinicbook.service.AdminDashboardService;
import vn.codegym.clinicbook.service.AdminDashboardService.Overview;
import vn.codegym.clinicbook.service.AdminDashboardService.SpecialtyLoad;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class AdminDashboardTest {
    @Autowired MockMvc mockMvc;
    @Autowired AdminDashboardService dashboardService;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void onlyAdminsCanOpenIt() throws Exception {
        mockMvc.perform(get("/admin").with(user("p").roles("PATIENT")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?denied"));
        mockMvc.perform(get("/admin").with(user("d").roles("DOCTOR")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/?denied"));
    }

    @Test
    void adminSeesThePageInBothLanguages() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("o", "logs"))
                .andExpect(content().string(containsString("Tổng quan vận hành phòng khám")));
        mockMvc.perform(get("/admin").param("lang", "en").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Clinic operations overview")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void figuresFollowTheDatabase() throws Exception {
        Overview before = dashboardService.overview();

        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Dash specialty " + n);
        specialty = specialtyRepository.save(specialty);

        Doctor busy = newDoctor("dash_busy_" + n, specialty);
        newDoctor("dash_idle_" + n, specialty);
        Patient patient = newPatient("dash_pat_" + n);

        LocalDate soon = LocalDate.now().plusDays(2);
        TimeSlot bookedSlot = newSlot(busy, soon, 9, SlotStatus.BOOKED);
        newSlot(busy, soon, 10, SlotStatus.AVAILABLE);
        TimeSlot pastSlot = newSlot(busy, LocalDate.now().minusDays(1), 9, SlotStatus.BOOKED);

        newAppointment(patient, busy, bookedSlot, AppointmentStatus.PENDING);
        newAppointment(patient, busy, pastSlot, AppointmentStatus.PENDING);

        Overview after = dashboardService.overview();

        assertEquals(before.getDoctorCount() + 2, after.getDoctorCount());
        assertEquals(before.getSpecialtyCount() + 1, after.getSpecialtyCount());
        assertEquals(before.getPatientCount() + 1, after.getPatientCount());
        assertEquals(before.getAppointmentCount() + 2, after.getAppointmentCount());
        assertEquals(before.getSlotTotal() + 2, after.getSlotTotal());
        assertEquals(before.getSlotBooked() + 1, after.getSlotBooked());
        assertEquals(before.getPendingTotal() + 2, after.getPendingTotal());
        assertEquals(before.getOverdue() + 1, after.getOverdue());
        assertEquals(before.getIdleDoctorCount() + 1, after.getIdleDoctorCount());
        assertEquals(before.getDoctorsWithFreeSlots() + 1, after.getDoctorsWithFreeSlots());

        String name = specialty.getName();
        SpecialtyLoad load = after.getSpecialties().stream()
                .filter(l -> l.getSpecialty().getName().equals(name))
                .findFirst().orElse(null);
        assertNotNull(load);
        assertEquals(2, load.getDoctors());
        assertEquals(2, load.getTotal());
        assertEquals(1, load.getBooked());
        assertEquals(50, load.getPercent());
        assertEquals("normal", load.getLevel());
    }

    @Test
    void workloadAndAlertsAreShownOnThePage() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Dash page specialty " + n);
        specialty = specialtyRepository.save(specialty);
        Doctor doctor = newDoctor("dash_page_doc_" + n, specialty);
        newSlot(doctor, LocalDate.now().plusDays(1), 9, SlotStatus.AVAILABLE);

        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(specialty.getName())));
    }

    @Test
    void adminCannotChangeAnAppointmentStatus() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Dash status specialty " + n);
        specialty = specialtyRepository.save(specialty);
        Doctor doctor = newDoctor("dash_status_doc_" + n, specialty);
        Patient patient = newPatient("dash_status_pat_" + n);
        TimeSlot slot = newSlot(doctor, LocalDate.now().plusDays(3), 9, SlotStatus.BOOKED);
        Appointment appointment = newAppointment(patient, doctor, slot, AppointmentStatus.PENDING);

        mockMvc.perform(post("/admin/appointments/{id}/status", appointment.getId()).with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("status", "CONFIRMED"))
                .andExpect(status().is4xxClientError());
        assertEquals(AppointmentStatus.PENDING,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    private Doctor newDoctor(String username, Specialty specialty) {
        Doctor doctor = new Doctor();
        doctor.setUser(newUser(username, Role.DOCTOR));
        doctor.setFullName("Dr. " + username);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(3);
        return doctorRepository.save(doctor);
    }

    private Patient newPatient(String username) {
        Patient patient = new Patient();
        patient.setUser(newUser(username, Role.PATIENT));
        return patientRepository.save(patient);
    }

    private TimeSlot newSlot(Doctor doctor, LocalDate date, int hour, SlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(date);
        slot.setStartTime(LocalTime.of(hour, 0));
        slot.setEndTime(LocalTime.of(hour + 1, 0));
        slot.setStatus(status);
        return slotRepository.save(slot);
    }

    private Appointment newAppointment(Patient patient, Doctor doctor, TimeSlot slot, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
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
