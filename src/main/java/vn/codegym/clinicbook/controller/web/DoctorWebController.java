package vn.codegym.clinicbook.controller.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SlotService;
import vn.codegym.clinicbook.service.SpecialtyService;
import vn.codegym.clinicbook.util.RecentDoctorsCookie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/doctors")
public class DoctorWebController {
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final SlotService slotService;

    public DoctorWebController(DoctorService doctorService,
                               SpecialtyService specialtyService,
                               SlotService slotService) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.slotService = slotService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long specialtyId,
                       @RequestParam(required = false, defaultValue = "") String keyword,
                       @RequestParam(required = false, defaultValue = "name") String sort,
                       Model model) {
        List<Doctor> doctors = new ArrayList<>(doctorService.findAll(specialtyId, keyword));
        if ("experience".equals(sort)) {
            doctors.sort(Comparator.comparingInt(Doctor::getYearsExperience).reversed()
                    .thenComparing(Doctor::getFullName));
        } else {
            sort = "name";
        }

        Map<Long, List<TimeSlot>> nextSlots = new HashMap<>();
        Map<Long, Integer> freeSlotCounts = new HashMap<>();
        for (Doctor d : doctors) {
            List<TimeSlot> free = slotService.findAvailable(d.getId());
            freeSlotCounts.put(d.getId(), free.size());
            nextSlots.put(d.getId(), free.stream().limit(3).toList());
        }

        Map<Long, Long> specialtyCounts = new HashMap<>();
        for (Doctor d : doctorService.findAll(null, "")) {
            specialtyCounts.merge(d.getSpecialty().getId(), 1L, Long::sum);
        }

        model.addAttribute("doctors", doctors);
        model.addAttribute("nextSlots", nextSlots);
        model.addAttribute("freeSlotCounts", freeSlotCounts);
        model.addAttribute("specialtyCounts", specialtyCounts);
        model.addAttribute("specialties", specialtyService.findAll());
        model.addAttribute("specialtyId", specialtyId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        return "doctors/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @CookieValue(name = RecentDoctorsCookie.NAME, required = false) String recent,
                         @CookieValue(name = RecentDoctorsCookie.CONSENT, required = false) String consent,
                         HttpServletResponse response,
                         Model model) {
        model.addAttribute("doctor", doctorService.findById(id));

        List<TimeSlot> slots = slotService.findAvailable(id);
        Map<LocalDate, List<TimeSlot>> slotsByDate = new TreeMap<>();
        for (TimeSlot slot : slots) {
            slotsByDate.computeIfAbsent(slot.getSlotDate(), d -> new ArrayList<>()).add(slot);
        }
        model.addAttribute("slotsByDate", slotsByDate);

        if (RecentDoctorsCookie.allowed(consent)) {
            RecentDoctorsCookie.remember(recent, id, response);
        } else if (recent != null) {
            RecentDoctorsCookie.forget(response);
        }
        return "doctors/detail";
    }
}
