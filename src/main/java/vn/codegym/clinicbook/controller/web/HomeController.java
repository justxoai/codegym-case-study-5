package vn.codegym.clinicbook.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.service.ArticleService;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SpecialtyService;
import vn.codegym.clinicbook.util.RecentDoctorsCookie;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class HomeController {
    private final SpecialtyService specialtyService;
    private final DoctorService doctorService;
    private final ArticleService articleService;

    public HomeController(SpecialtyService specialtyService, DoctorService doctorService,
                          ArticleService articleService) {
        this.specialtyService = specialtyService;
        this.doctorService = doctorService;
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(@CookieValue(name = RecentDoctorsCookie.NAME, required = false) String recent,
                       @CookieValue(name = RecentDoctorsCookie.CONSENT, required = false) String consent,
                       Model model) {
        List<Specialty> specialties = specialtyService.findAll();
        List<Doctor> doctors = doctorService.findAll(null, "");

        Map<Long, Long> doctorCounts = new HashMap<>();
        for (Doctor d : doctors) {
            doctorCounts.merge(d.getSpecialty().getId(), 1L, Long::sum);
        }
        Map<Long, String> specialtyIcons = new HashMap<>();
        for (Specialty s : specialties) {
            specialtyIcons.put(s.getId(), iconFor(s.getName()));
        }

        model.addAttribute("specialties", specialties);
        model.addAttribute("doctorCounts", doctorCounts);
        model.addAttribute("specialtyIcons", specialtyIcons);
        model.addAttribute("totalDoctors", doctors.size());
        model.addAttribute("latestNews", articleService.latest(3));
        model.addAttribute("featuredDoctors", doctors.stream().limit(3).toList());
        model.addAttribute("recentDoctors", doctorService.findByIds(
                RecentDoctorsCookie.allowed(consent) ? RecentDoctorsCookie.parse(recent) : List.of()));
        return "index";
    }

    static String iconFor(String name) {
        String n = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('\u0111', 'd').toLowerCase(Locale.ROOT);
        if (n.contains("tim") || n.contains("cardio")) return "bi-heart-pulse";
        if (n.contains("nhi") || n.contains("pediatric")) return "bi-emoji-smile";
        if (n.contains("da lieu") || n.contains("derma")) return "bi-droplet-half";
        if (n.contains("rang") || n.contains("dental") || n.contains("dent")) return "bi-emoji-laughing";
        if (n.contains("mat") || n.contains("ophthal")) return "bi-eye";
        if (n.contains("than kinh") || n.contains("tam than") || n.contains("neuro") || n.contains("mental")) return "bi-activity";
        if (n.contains("xuong") || n.contains("khop") || n.contains("ortho")) return "bi-bandaid";
        if (n.contains("noi") || n.contains("general")) return "bi-clipboard2-pulse";
        return "bi-hospital";
    }
}
