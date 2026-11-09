package vn.codegym.clinicbook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPagesTest {
    @Autowired MockMvc mockMvc;

    @Test
    void publicPagesRender() throws Exception {
        for (String url : new String[]{"/", "/login", "/register", "/doctors", "/news"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("ClinicBook")));
        }
    }

    @Test
    void newsListAndDetailAreOpenToEveryone() throws Exception {
        mockMvc.perform(get("/news").param("category", "HEART").param("keyword", "huyết áp"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/news").param("category", "UNKNOWN"))
                .andExpect(status().isOk());
    }

    @Test
    void i18n_defaultIsVietnamese_andLangParamSwitchesToEnglish() throws Exception {
        mockMvc.perform(get("/")).andExpect(content().string(containsString("Đặt lịch khám bác sĩ trực tuyến")));
        mockMvc.perform(get("/").param("lang", "en")).andExpect(content().string(containsString("Book a doctor online")));
    }

    @Test
    void protectedPagesRedirectAnonymousToLogin() throws Exception {
        for (String url : new String[]{"/admin", "/patient/appointments", "/patient/dashboard", "/doctor/schedule", "/doctor/dashboard", "/booking", "/booking/confirm", "/account/password"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }
}
