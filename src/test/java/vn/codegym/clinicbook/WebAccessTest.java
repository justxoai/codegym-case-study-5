package vn.codegym.clinicbook;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.UserRepository;
import vn.codegym.clinicbook.util.RecentDoctorsCookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebAccessTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;

    private Doctor doctor;
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();
        specialty = new Specialty();
        specialty.setName("Tim mạch " + n);
        specialty.setNameEn("Cardiology " + n);
        specialty = specialtyRepository.save(specialty);

        User u = new User();
        u.setUsername("access_doc_" + n);
        u.setPassword("{noop}secret");
        u.setFullName("BS. Access " + n);
        u.setRole(Role.DOCTOR);
        u = userRepository.save(u);

        doctor = new Doctor();
        doctor.setUser(u);
        doctor.setFullName(u.getFullName());
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(6);
        doctor = doctorRepository.save(doctor);
    }

    @Test
    void logoutByPostGoesToTheHomePage() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()).with(user("someone").roles("PATIENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    void logoutByGetNeverShowsALogoutPage() throws Exception {
        mockMvc.perform(get("/logout").with(user("someone").roles("PATIENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    void homePageExplainsTheLogoutAndTheDeniedRedirect() throws Exception {
        mockMvc.perform(get("/").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cb-autoclose")));
        mockMvc.perform(get("/").param("denied", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cb-autoclose")));
        mockMvc.perform(get("/"))
                .andExpect(content().string(not(containsString("cb-autoclose"))));
    }

    @Test
    void forbiddenPagesSendTheVisitorHome() throws Exception {
        mockMvc.perform(get("/admin").with(user("p").roles("PATIENT"))).andExpect(redirectedUrl("/?denied"));
        mockMvc.perform(get("/admin/doctors").with(user("d").roles("DOCTOR"))).andExpect(redirectedUrl("/?denied"));
        mockMvc.perform(get("/doctor/dashboard").with(user("p").roles("PATIENT"))).andExpect(redirectedUrl("/?denied"));
        mockMvc.perform(get("/booking").with(user("d").roles("DOCTOR"))).andExpect(redirectedUrl("/?denied"));
        mockMvc.perform(get("/patient/dashboard").with(user("a").roles("ADMIN"))).andExpect(redirectedUrl("/?denied"));
    }

    private Cookie setCookie(MvcResult result, String name) {
        return result.getResponse().getCookie(name);
    }

    @Test
    void doctorViewIsRememberedOnlyAfterTheVisitorAcceptedCookies() throws Exception {
        MvcResult accepted = mockMvc.perform(get("/doctors/{id}", doctor.getId())
                        .cookie(new Cookie(RecentDoctorsCookie.CONSENT, RecentDoctorsCookie.CONSENT_ALL)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie remembered = setCookie(accepted, RecentDoctorsCookie.NAME);
        assertNotNull(remembered);
        assertEquals(String.valueOf(doctor.getId()), remembered.getValue());
        assertEquals(true, remembered.isHttpOnly());
    }

    @Test
    void withoutConsentNoRecentDoctorCookieIsWritten() throws Exception {
        MvcResult noChoice = mockMvc.perform(get("/doctors/{id}", doctor.getId()))
                .andExpect(status().isOk()).andReturn();
        assertNull(setCookie(noChoice, RecentDoctorsCookie.NAME));

        MvcResult declined = mockMvc.perform(get("/doctors/{id}", doctor.getId())
                        .cookie(new Cookie(RecentDoctorsCookie.CONSENT, "essential")))
                .andExpect(status().isOk()).andReturn();
        assertNull(setCookie(declined, RecentDoctorsCookie.NAME));
    }

    @Test
    void decliningCookiesDeletesTheOneAlreadyThere() throws Exception {
        MvcResult declined = mockMvc.perform(get("/doctors/{id}", doctor.getId())
                        .cookie(new Cookie(RecentDoctorsCookie.CONSENT, "essential"),
                                new Cookie(RecentDoctorsCookie.NAME, "1-2")))
                .andExpect(status().isOk()).andReturn();
        Cookie gone = setCookie(declined, RecentDoctorsCookie.NAME);
        assertNotNull(gone);
        assertEquals(0, gone.getMaxAge());
    }

    @Test
    void recentDoctorsOnTheHomePageNeedConsentToo() throws Exception {
        mockMvc.perform(get("/")
                        .cookie(new Cookie(RecentDoctorsCookie.NAME, String.valueOf(doctor.getId())),
                                new Cookie(RecentDoctorsCookie.CONSENT, RecentDoctorsCookie.CONSENT_ALL)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentDoctors", hasSize(1)));
        mockMvc.perform(get("/")
                        .cookie(new Cookie(RecentDoctorsCookie.NAME, String.valueOf(doctor.getId()))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentDoctors", empty()));
    }

    @Test
    void cookieBannerIsOnEveryPage() throws Exception {
        mockMvc.perform(get("/")).andExpect(content().string(containsString("cb-cookie")));
        mockMvc.perform(get("/login")).andExpect(content().string(containsString("cb-cookie")));
    }

    @Test
    void specialtyAndDoctorTitleFollowTheLanguage() throws Exception {
        String vi = specialty.getName();
        String en = specialty.getNameEn();
        String number = doctor.getFullName().replaceAll("\\D+", "");

        mockMvc.perform(get("/doctors").param("specialtyId", String.valueOf(specialty.getId())).param("lang", "vi"))
                .andExpect(content().string(containsString(vi)))
                .andExpect(content().string(containsString("BS. Access " + number)));
        mockMvc.perform(get("/doctors").param("specialtyId", String.valueOf(specialty.getId())).param("lang", "en"))
                .andExpect(content().string(containsString(en)))
                .andExpect(content().string(containsString("Dr. Access " + number)))
                .andExpect(content().string(not(containsString("BS. Access " + number))));
    }

    @Test
    void aSpecialtyWithoutAnEnglishNameKeepsItsName() {
        Specialty plain = new Specialty();
        plain.setName("Chỉ có tiếng Việt");
        assertEquals("Chỉ có tiếng Việt", plain.getLocalizedName());
    }
}
