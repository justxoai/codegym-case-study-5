package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientDashboardTest {
    private static final String USERNAME = "dash_patient";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensurePatient() {
        if (userRepository.findByUsername(USERNAME).isPresent()) {
            return;
        }
        User u = new User();
        u.setUsername(USERNAME);
        u.setFullName("Nguyen Van An");
        u.setRole(Role.PATIENT);
        u.setPassword(passwordEncoder.encode("Secret123!"));
        u = userRepository.save(u);
        Patient p = new Patient();
        p.setUser(u);
        patientRepository.save(p);
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/patient/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void patientSeesDashboardEvenWithoutAppointments() throws Exception {
        mockMvc.perform(get("/patient/dashboard").with(user(USERNAME).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/dashboard"))
                .andExpect(model().attribute("firstName", "An"))
                .andExpect(model().attribute("totalCount", 0))
                .andExpect(content().string(containsString("Nguyen Van An")));
    }

    @Test
    void doctorsCannotOpenThePatientDashboard() throws Exception {
        mockMvc.perform(get("/patient/dashboard").with(user("someone").roles("DOCTOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?denied"));
    }

    @Test
    void patientLandsOnTheDashboardAfterLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user(USERNAME).password("Secret123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/dashboard"));
    }
}
