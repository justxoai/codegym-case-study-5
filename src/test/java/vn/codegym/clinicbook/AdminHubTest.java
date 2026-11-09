package vn.codegym.clinicbook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.util.DoctorNames;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import vn.codegym.clinicbook.constant.ErrorMessage;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminHubTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void anonymousAndNonAdminsAreKeptOut() throws Exception {
        for (String url : new String[]{"/admin/doctors", "/admin/doctors/new", "/admin/specialties"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
            mockMvc.perform(get(url).with(user("p").roles("PATIENT"))).andExpect(redirectedUrl("/?denied"));
            mockMvc.perform(get(url).with(user("d").roles("DOCTOR"))).andExpect(redirectedUrl("/?denied"));
        }
    }

    @Test
    void doctorListShowsDoctorsAndFiltersByNameAndSpecialty() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub list specialty " + n);
        Doctor doctor = newDoctor("hub_list_" + n, specialty);

        mockMvc.perform(get("/admin/doctors").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/doctors"))
                .andExpect(content().string(containsString(DoctorNames.strip(doctor.getFullName()))));

        mockMvc.perform(get("/admin/doctors").param("keyword", "hub_list_" + n).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("doctors", hasSize(1)));

        mockMvc.perform(get("/admin/doctors").param("keyword", "no-such-doctor-" + n).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("doctors", hasSize(0)));

        mockMvc.perform(get("/admin/doctors").param("specialty", String.valueOf(specialty.getId()))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("doctors", hasSize(1)));

        mockMvc.perform(get("/admin/doctors").param("specialty", "").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void createDoctorWithLoginAccount() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub create specialty " + n);
        String fullName = "Dr. Created " + n;

        mockMvc.perform(post("/admin/doctors").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("fullName", fullName)
                        .param("specialtyId", String.valueOf(specialty.getId()))
                        .param("yearsExperience", "6")
                        .param("bio", "Bio")
                        .param("username", "hub_created_" + n)
                        .param("password", "secret1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/doctors"))
                .andExpect(flash().attribute("successKey", "doctor.saved"));

        assertTrue(userRepository.existsByUsername("hub_created_" + n));
        assertFalse(doctorRepository.findByFullNameContainingIgnoreCaseOrderByFullName(fullName).isEmpty());
    }

    @Test
    void invalidDoctorFormStaysOnTheForm() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub invalid specialty " + n);

        mockMvc.perform(post("/admin/doctors").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("fullName", "")
                        .param("specialtyId", String.valueOf(specialty.getId()))
                        .param("yearsExperience", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/doctor-form"))
                .andExpect(model().attributeHasFieldErrors("doctorRequest", "fullName"));

        mockMvc.perform(post("/admin/doctors").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("fullName", "Dr. Short " + n)
                        .param("specialtyId", String.valueOf(specialty.getId()))
                        .param("yearsExperience", "2")
                        .param("username", "hub_short_" + n)
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/doctor-form"))
                .andExpect(model().attribute("errorKey", ErrorMessage.DOCTOR_ACCOUNT_REQUIRED));
        assertFalse(userRepository.existsByUsername("hub_short_" + n));
    }

    @Test
    void editAndDeleteDoctor() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub edit specialty " + n);
        Doctor doctor = newDoctor("hub_edit_" + n, specialty);

        mockMvc.perform(get("/admin/doctors/{id}/edit", doctor.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/doctor-form"))
                .andExpect(content().string(containsString(DoctorNames.strip(doctor.getFullName()))));

        mockMvc.perform(post("/admin/doctors/{id}", doctor.getId()).with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("fullName", "Dr. Renamed " + n)
                        .param("specialtyId", String.valueOf(specialty.getId()))
                        .param("yearsExperience", "9"))
                .andExpect(redirectedUrl("/admin/doctors"));
        assertTrue(doctorRepository.findById(doctor.getId()).orElseThrow().getFullName().startsWith("Dr. Renamed"));

        mockMvc.perform(post("/admin/doctors/{id}/delete", doctor.getId()).with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/doctors"))
                .andExpect(flash().attribute("successKey", "doctor.deleted"));
        assertFalse(doctorRepository.existsById(doctor.getId()));
    }

    @Test
    void specialtyCardsCreateEditAndDelete() throws Exception {
        long n = System.nanoTime();
        String name = "Hub card specialty " + n;

        mockMvc.perform(post("/admin/specialties").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("name", name).param("description", "Description"))
                .andExpect(redirectedUrl("/admin/specialties"))
                .andExpect(flash().attribute("successKey", "specialty.saved"));

        mockMvc.perform(get("/admin/specialties").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/specialties"))
                .andExpect(model().attributeExists("o", "loads"))
                .andExpect(content().string(containsString(name)));

        mockMvc.perform(post("/admin/specialties").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("name", name))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/specialties"))
                .andExpect(model().attributeHasFieldErrors("specialtyRequest", "name"));

        Specialty saved = specialtyRepository.findAllByOrderByNameAsc().stream()
                .filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
        mockMvc.perform(get("/admin/specialties/{id}/edit", saved.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("editId", saved.getId()));

        mockMvc.perform(post("/admin/specialties/{id}/delete", saved.getId()).with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/specialties"))
                .andExpect(flash().attribute("successKey", "specialty.deleted"));
        assertFalse(specialtyRepository.existsById(saved.getId()));
    }

    @Test
    void specialtyWithDoctorsCannotBeDeleted() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub in-use specialty " + n);
        newDoctor("hub_inuse_" + n, specialty);

        mockMvc.perform(post("/admin/specialties/{id}/delete", specialty.getId()).with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/specialties"))
                .andExpect(flash().attribute("errorKey", ErrorMessage.SPECIALTY_IN_USE));
        assertTrue(specialtyRepository.existsById(specialty.getId()));
    }

    @Test
    void doctorListHasTenRowsPerPage() throws Exception {
        long n = System.nanoTime();
        Specialty specialty = newSpecialty("Hub paging specialty " + n);
        for (int i = 0; i < 12; i++) {
            newDoctor(String.format("hub_page_%d_%02d", n, i), specialty);
        }
        String sp = String.valueOf(specialty.getId());

        mockMvc.perform(get("/admin/doctors").param("specialty", sp).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("doctors", hasSize(10)))
                .andExpect(model().attribute("pv", org.hamcrest.Matchers.hasProperty("totalPages", org.hamcrest.Matchers.equalTo(2))))
                .andExpect(content().string(containsString("specialty=" + sp + "&amp;page=2")));

        mockMvc.perform(get("/admin/doctors").param("specialty", sp).param("page", "2").with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("doctors", hasSize(2)));

        mockMvc.perform(get("/admin/doctors").param("specialty", sp).param("page", "99").with(user("admin").roles("ADMIN")))
                .andExpect(model().attribute("doctors", hasSize(2)));
    }

    private Specialty newSpecialty(String name) {
        Specialty specialty = new Specialty();
        specialty.setName(name);
        return specialtyRepository.save(specialty);
    }

    private Doctor newDoctor(String username, Specialty specialty) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("Secret123!"));
        u.setFullName("Dr. " + username);
        u.setRole(Role.DOCTOR);
        u = userRepository.save(u);

        Doctor doctor = new Doctor();
        doctor.setUser(u);
        doctor.setFullName("Dr. " + username);
        doctor.setSpecialty(specialty);
        doctor.setYearsExperience(5);
        return doctorRepository.save(doctor);
    }
}
