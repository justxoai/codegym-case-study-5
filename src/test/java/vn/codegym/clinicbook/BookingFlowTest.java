package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
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
import vn.codegym.clinicbook.util.DoctorNames;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class BookingFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired TimeSlotRepository slotRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private String patientName;
    private Doctor doctor;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Flow specialty " + n);
        specialty = specialtyRepository.save(specialty);

        doctor = new Doctor();
        doctor.setUser(newUser("flow_doc_" + n, Role.DOCTOR));
        doctor.setFullName("Dr. Flow " + n);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(7);
        doctor = doctorRepository.save(doctor);

        slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);

        patientName = "flow_pat_" + n;
        Patient patient = new Patient();
        patient.setUser(newUser(patientName, Role.PATIENT));
        patientRepository.save(patient);
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/booking"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void pageStartsWithTheSpecialtyAndListsNoDoctorYet() throws Exception {
        mockMvc.perform(get("/booking").with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/select"))
                .andExpect(model().attributeExists("specialties"))
                .andExpect(model().attribute("doctors", org.hamcrest.Matchers.empty()))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString(DoctorNames.strip(doctor.getFullName())))));
    }

    @Test
    void chosenSpecialtyListsOnlyItsDoctors() throws Exception {
        Long specialtyId = doctor.getSpecialty().getId();
        mockMvc.perform(get("/booking").param("specialtyId", String.valueOf(specialtyId))
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("specialtyId", specialtyId))
                .andExpect(model().attribute("doctors", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(containsString(DoctorNames.strip(doctor.getFullName()))));
    }

    @Test
    void changingTheSpecialtyDropsTheDoctorOfTheOldOne() throws Exception {
        Specialty other = new Specialty();
        other.setName("Other flow specialty " + System.nanoTime());
        other = specialtyRepository.save(other);

        mockMvc.perform(get("/booking")
                        .param("specialtyId", String.valueOf(other.getId()))
                        .param("doctorId", String.valueOf(doctor.getId()))
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("doctorId", org.hamcrest.Matchers.nullValue()))
                .andExpect(model().attributeDoesNotExist("doctor"))
                .andExpect(model().attribute("doctors", org.hamcrest.Matchers.empty()));
    }

    @Test
    void pickingADoctorAloneFillsInItsSpecialty() throws Exception {
        mockMvc.perform(get("/booking").param("doctorId", String.valueOf(doctor.getId()))
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("specialtyId", doctor.getSpecialty().getId()))
                .andExpect(model().attributeExists("doctor"));
    }

    @Test
    void pageListsTheFreeSlotsOfTheChosenDoctor() throws Exception {
        mockMvc.perform(get("/booking").param("doctorId", String.valueOf(doctor.getId()))
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("doctor", "dates", "periods"))
                .andExpect(content().string(containsString("09:00")));
    }

    @Test
    void slotLinkPreselectsDoctorAndTime() throws Exception {
        mockMvc.perform(get("/booking").param("slotId", String.valueOf(slot.getId()))
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("selectedSlot"));
    }

    @Test
    void fullFlowCreatesAPendingAppointment() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/booking/select").session(session).with(csrf())
                        .with(user(patientName).roles("PATIENT"))
                        .param("slotId", String.valueOf(slot.getId()))
                        .param("reasonKey", "symptom")
                        .param("note", "Dau dau 3 ngay"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking/confirm"));

        mockMvc.perform(get("/booking/confirm").session(session).with(user(patientName).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/confirm"))
                .andExpect(content().string(containsString("Dau dau 3 ngay")));

        mockMvc.perform(post("/booking/confirm").session(session).with(csrf())
                        .with(user(patientName).roles("PATIENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/dashboard"));

        List<Appointment> mine = appointmentRepository.findByPatientIdOrderByCreatedAtDesc(
                patientRepository.findByUserUsername(patientName).orElseThrow().getId());
        assertEquals(1, mine.size());
        assertEquals(AppointmentStatus.PENDING, mine.get(0).getStatus());
        assertTrue(mine.get(0).getReason().contains("Dau dau 3 ngay"));
        assertEquals(SlotStatus.BOOKED, slotRepository.findById(slot.getId()).orElseThrow().getStatus());
    }

    @Test
    void selectWithoutASlotGoesBackToThePage() throws Exception {
        mockMvc.perform(post("/booking/select").with(csrf()).with(user(patientName).roles("PATIENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking"));
    }

    private User newUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("{noop}unused");
        u.setFullName("Flow " + username);
        u.setRole(role);
        return userRepository.save(u);
    }
}
