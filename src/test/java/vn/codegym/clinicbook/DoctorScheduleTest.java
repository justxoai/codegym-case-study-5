package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DoctorScheduleTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired TimeSlotRepository slotRepository;

    private String doctorName;
    private LocalDate slotDay;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Timetable specialty " + n);
        specialty = specialtyRepository.save(specialty);

        User u = new User();
        doctorName = "tt_doc_" + n;
        u.setUsername(doctorName);
        u.setPassword("{noop}unused");
        u.setFullName("Dr. Timetable " + n);
        u.setRole(Role.DOCTOR);
        u = userRepository.save(u);

        Doctor doctor = new Doctor();
        doctor.setUser(u);
        doctor.setFullName(u.getFullName());
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(3);
        doctor = doctorRepository.save(doctor);

        slotDay = LocalDate.now().plusDays(10);
        slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(slotDay);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot = slotRepository.save(slot);
    }

    @Test
    void currentWeekRendersEvenWithoutSlots() throws Exception {
        mockMvc.perform(get("/doctor/schedule").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/schedule"))
                .andExpect(model().attributeExists("days", "rows", "monday", "sunday"));
    }

    @Test
    void weekWithASlotShowsItInTheGrid() throws Exception {
        mockMvc.perform(get("/doctor/schedule").param("week", slotDay.toString())
                        .with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("slotCount", 1))
                .andExpect(content().string(containsString("09:00 - 10:00")));
    }

    @Test
    void anyDayOfTheWeekGivesTheSameMonday() throws Exception {
        LocalDate monday = slotDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        mockMvc.perform(get("/doctor/schedule").param("week", slotDay.toString())
                        .with(user(doctorName).roles("DOCTOR")))
                .andExpect(model().attribute("monday", monday));
    }

    @Test
    void addingASlotOpensTheWeekOfThatSlot() throws Exception {
        LocalDate day = LocalDate.now().plusDays(20);
        mockMvc.perform(post("/doctor/slots").with(csrf()).with(user(doctorName).roles("DOCTOR"))
                        .param("slotDate", day.toString())
                        .param("startTime", "14:00")
                        .param("endTime", "15:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/schedule?week=" + day));
    }

    @Test
    void invalidSlotFormShowsThePageAgain() throws Exception {
        mockMvc.perform(post("/doctor/slots").with(csrf()).with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/schedule"));
    }

    @Test
    void deletingAFreeSlotKeepsTheWeek() throws Exception {
        mockMvc.perform(post("/doctor/slots/{id}/delete", slot.getId()).with(csrf())
                        .with(user(doctorName).roles("DOCTOR"))
                        .param("week", slotDay.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/schedule?week=" + slotDay));
        assertFalse(slotRepository.findById(slot.getId()).isPresent());
    }

    @Test
    void aBookedSlotCannotBeDeleted() throws Exception {
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);
        mockMvc.perform(post("/doctor/slots/{id}/delete", slot.getId()).with(csrf())
                        .with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/schedule"));
        assertTrue(slotRepository.findById(slot.getId()).isPresent());
    }
}
