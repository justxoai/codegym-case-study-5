package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import vn.codegym.clinicbook.constant.ErrorMessage;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SlotRulesTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired TimeSlotRepository slotRepository;

    private String doctorName;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        Specialty specialty = new Specialty();
        specialty.setName("Rules specialty " + n);
        specialty = specialtyRepository.save(specialty);

        User u = new User();
        doctorName = "rules_doc_" + n;
        u.setUsername(doctorName);
        u.setPassword("{noop}secret");
        u.setFullName("Dr. Rules " + n);
        u.setRole(Role.DOCTOR);
        u = userRepository.save(u);

        doctor = new Doctor();
        doctor.setUser(u);
        doctor.setFullName(u.getFullName());
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(4);
        doctor = doctorRepository.save(doctor);
    }

    private ResultActions add(LocalDate day, String start, String end) throws Exception {
        return mockMvc.perform(post("/doctor/slots").with(csrf()).with(user(doctorName).roles("DOCTOR"))
                .param("slotDate", day.toString())
                .param("startTime", start)
                .param("endTime", end));
    }

    private void assertRejected(ResultActions result, String errorKey) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(view().name("doctor/schedule"))
                .andExpect(model().attribute("errorKey", errorKey));
        assertEquals(0, slotRepository.findAll().stream()
                .filter(s -> s.getDoctor().getId().equals(doctor.getId())).count());
    }

    @Test
    void theFormOpensWithTheDayAlreadyFilledIn() throws Exception {
        mockMvc.perform(get("/doctor/schedule").with(user(doctorName).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("slotRequest",
                        org.hamcrest.Matchers.hasProperty("slotDate", org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void aMissingDayIsReportedOnTheForm() throws Exception {
        mockMvc.perform(post("/doctor/slots").with(csrf()).with(user(doctorName).roles("DOCTOR"))
                        .param("startTime", "09:00").param("endTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/schedule"))
                .andExpect(model().attributeHasFieldErrors("slotRequest", "slotDate"));
    }

    @Test
    void endMustBeAfterStart() throws Exception {
        assertRejected(add(LocalDate.now().plusDays(5), "10:00", "09:00"), ErrorMessage.SLOT_TIME_ORDER);
        assertRejected(add(LocalDate.now().plusDays(5), "10:00", "10:00"), ErrorMessage.SLOT_TIME_ORDER);
    }

    @Test
    void nothingBeforeSixOrAfterTenInTheEvening() throws Exception {
        assertRejected(add(LocalDate.now().plusDays(5), "05:00", "06:30"), ErrorMessage.SLOT_OUTSIDE_HOURS);
        assertRejected(add(LocalDate.now().plusDays(5), "21:30", "22:30"), ErrorMessage.SLOT_OUTSIDE_HOURS);
    }

    @Test
    void aSlotIsBetweenFifteenMinutesAndFourHours() throws Exception {
        assertRejected(add(LocalDate.now().plusDays(5), "09:00", "09:10"), ErrorMessage.SLOT_DURATION);
        assertRejected(add(LocalDate.now().plusDays(5), "08:00", "13:00"), ErrorMessage.SLOT_DURATION);
    }

    @Test
    void notMoreThanNinetyDaysAhead() throws Exception {
        assertRejected(add(LocalDate.now().plusDays(120), "09:00", "10:00"), ErrorMessage.SLOT_TOO_FAR);
    }

    @Test
    void aDayInThePastIsRejected() throws Exception {
        assertRejected(add(LocalDate.now().minusDays(1), "09:00", "10:00"), ErrorMessage.SLOT_IN_PAST);
    }

    @Test
    void validSlotIsSavedAndOverlapIsRejected() throws Exception {
        LocalDate day = LocalDate.now().plusDays(7);
        add(day, "09:00", "10:00")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/schedule?week=" + day));
        assertEquals(1, slotRepository.findAll().stream()
                .filter(s -> s.getDoctor().getId().equals(doctor.getId())).count());

        add(day, "09:30", "10:30")
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorKey", ErrorMessage.SLOT_OVERLAP));
        add(day, "10:00", "11:00").andExpect(status().is3xxRedirection());
    }
}
