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
import vn.codegym.clinicbook.entity.AuditLog;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.util.DoctorNames;
import vn.codegym.clinicbook.repository.AuditLogRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class AdminOpsTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Specialty specialty;
    private Doctor doctor;
    private String patientName;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        specialty = new Specialty();
        specialty.setName("Ops specialty " + n);
        specialty = specialtyRepository.save(specialty);

        doctor = new Doctor();
        doctor.setUser(newUser("ops_doc_" + n, Role.DOCTOR));
        doctor.setFullName("Dr. Ops " + n);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(4);
        doctor = doctorRepository.save(doctor);

        User patientUser = newUser("ops_pat_" + n, Role.PATIENT);
        patientName = patientUser.getFullName();
        Patient patient = new Patient();
        patient.setUser(patientUser);
        patient = patientRepository.save(patient);

        TimeSlot booked = newSlot(9, SlotStatus.BOOKED);
        newSlot(10, SlotStatus.AVAILABLE);

        appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(booked);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReason("Ops headache");
        appointment = appointmentRepository.save(appointment);
    }

    @Test
    void onlyAdminsCanOpenThePages() throws Exception {
        for (String url : new String[]{"/admin/appointments", "/admin/audit"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
            mockMvc.perform(get(url).with(user("p").roles("PATIENT"))).andExpect(redirectedUrl("/?denied"));
            mockMvc.perform(get(url).with(user("d").roles("DOCTOR"))).andExpect(redirectedUrl("/?denied"));
        }
    }

    @Test
    void scheduleGridAndHistoryShowTheAppointment() throws Exception {
        mockMvc.perform(get("/admin/appointments").param("specialty", String.valueOf(specialty.getId()))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/appointments"))
                .andExpect(model().attribute("sched", hasProperty("total", equalTo(1L))))
                .andExpect(model().attribute("sched", hasProperty("slotTotal", equalTo(2L))))
                .andExpect(model().attribute("sched", hasProperty("occupancyPercent", equalTo(50))))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(1L))))
                .andExpect(content().string(containsString(patientName)))
                .andExpect(content().string(containsString(DoctorNames.strip(doctor.getFullName()))))
                .andExpect(content().string(containsString("/admin/audit?appointment=" + appointment.getId())));
    }

    @Test
    void anotherDayHasNoSlots() throws Exception {
        mockMvc.perform(get("/admin/appointments")
                        .param("date", LocalDate.now().plusDays(400).toString())
                        .param("specialty", String.valueOf(specialty.getId()))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sched", hasProperty("slotTotal", equalTo(0L))));
    }

    @Test
    void historyFiltersByStatusAndKeyword() throws Exception {
        String sp = String.valueOf(specialty.getId());

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("status", "PENDING")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("status", "COMPLETED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(0L))));

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("status", "bogus")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("status", "ALL"))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("keyword", "ops headache")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("keyword", "#" + appointment.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/appointments").param("specialty", sp).param("keyword", "nobody-called-this")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("history", hasProperty("matching", equalTo(0L))));
    }

    @Test
    void adminCannotChangeAppointmentsFromTheHistory() throws Exception {
        mockMvc.perform(post("/admin/appointments/{id}/status", appointment.getId()).with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("status", "CONFIRMED"))
                .andExpect(status().is4xxClientError());
        assertEquals(AppointmentStatus.PENDING,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    @Test
    void auditLogFiltersByKeywordActionAndDay() throws Exception {
        long n = System.nanoTime();
        String actor = "ops_actor_" + n;
        AuditLog log = new AuditLog();
        log.setAction("APPOINTMENT_BOOKED");
        log.setUsername(actor);
        log.setDetail("book[1, x] -> appointment #" + n + " [PENDING]");
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);

        mockMvc.perform(get("/admin/audit").param("keyword", actor).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit"))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(1L))))
                .andExpect(model().attribute("summary", hasProperty("today", greaterThanOrEqualTo(1L))))
                .andExpect(content().string(containsString(actor)));

        mockMvc.perform(get("/admin/audit").param("keyword", "appointment #" + n + " [")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/audit").param("keyword", actor).param("action", "APPOINTMENT_BOOKED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/audit").param("keyword", actor).param("action", "APPOINTMENT_CANCELLED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(0L))));

        mockMvc.perform(get("/admin/audit").param("keyword", actor).param("date", LocalDate.now().toString())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(1L))));

        mockMvc.perform(get("/admin/audit").param("keyword", actor)
                        .param("date", LocalDate.now().minusDays(1).toString())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("result", hasProperty("matching", equalTo(0L))));
    }

    @Test
    void auditLogWithoutFiltersStillRenders() throws Exception {
        mockMvc.perform(get("/admin/audit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("result", "summary", "actions"));
    }

    private TimeSlot newSlot(int hour, SlotStatus slotStatus) {
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now());
        slot.setStartTime(LocalTime.of(hour, 0));
        slot.setEndTime(LocalTime.of(hour + 1, 0));
        slot.setStatus(slotStatus);
        return slotRepository.save(slot);
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
