package vn.codegym.clinicbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordTest {
    private static final String USERNAME = "pw_tester";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetUser() {
        User u = userRepository.findByUsername(USERNAME).orElseGet(User::new);
        u.setUsername(USERNAME);
        u.setFullName("Password Tester");
        u.setRole(Role.PATIENT);
        u.setPassword(passwordEncoder.encode("OldPass1!"));
        userRepository.save(u);
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/account/password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void signedInUserSeesTheForm() throws Exception {
        mockMvc.perform(get("/account/password").with(user(USERNAME).roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("account/password"));
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        mockMvc.perform(submit("WrongPass1!", "NewPass1!", "NewPass1!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "currentPassword"));
    }

    @Test
    void newPasswordMustDifferFromCurrent() throws Exception {
        mockMvc.perform(submit("OldPass1!", "OldPass1!", "OldPass1!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "newPassword"));
    }

    @Test
    void confirmationMustMatch() throws Exception {
        mockMvc.perform(submit("OldPass1!", "NewPass1!", "Different1!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "confirmPassword"));
    }

    @Test
    void validChangeIsSavedHashed() throws Exception {
        mockMvc.perform(submit("OldPass1!", "NewPass1!", "NewPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/password"));

        User saved = userRepository.findByUsername(USERNAME).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPass1!", saved.getPassword()));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submit(
            String current, String newPassword, String confirm) {
        return post("/account/password")
                .with(user(USERNAME).roles("PATIENT"))
                .with(csrf())
                .param("currentPassword", current)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirm);
    }
}
