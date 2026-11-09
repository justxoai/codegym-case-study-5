package vn.codegym.clinicbook.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.security.CurrentUser;
import vn.codegym.clinicbook.service.AppointmentService;
import vn.codegym.clinicbook.service.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class PatientWebController {
    private final AppointmentService appointmentService;
    private final AuthService authService;
    private final CurrentUser currentUser;

    public PatientWebController(AppointmentService appointmentService, AuthService authService, CurrentUser currentUser) {
        this.appointmentService = appointmentService;
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User profile = authService.getProfile(currentUser.username());
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> all = appointmentService.findForCurrentUser();

        Map<Boolean, List<Appointment>> parts = all.stream()
                .collect(Collectors.partitioningBy(a -> isUpcoming(a, now)));
        List<Appointment> upcoming = parts.get(true).stream()
                .sorted(Comparator.comparing(a -> a.getSlot().startAt()))
                .toList();
        List<Appointment> history = parts.get(false).stream()
                .sorted(Comparator.comparing((Appointment a) -> a.getSlot().startAt()).reversed())
                .toList();

        Appointment next = upcoming.isEmpty() ? null : upcoming.get(0);
        Map<Long, String> icons = new HashMap<>();
        for (Appointment a : all) {
            icons.put(a.getId(), HomeController.iconFor(a.getDoctor().getSpecialty().getName()));
        }

        model.addAttribute("profile", profile);
        model.addAttribute("firstName", firstName(profile.getFullName()));
        model.addAttribute("next", next);
        model.addAttribute("daysUntilNext", next == null ? 0
                : ChronoUnit.DAYS.between(LocalDate.now(), next.getSlot().getSlotDate()));
        model.addAttribute("later", upcoming.size() > 1 ? upcoming.subList(1, upcoming.size()) : List.of());
        model.addAttribute("history", history);
        model.addAttribute("icons", icons);
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("completedCount", all.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count());
        model.addAttribute("totalCount", all.size());
        return "patient/dashboard";
    }

    @GetMapping("/appointments")
    public String myAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.findForCurrentUser());
        return "patient/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String from,
                         RedirectAttributes redirect) {
        try {
            appointmentService.cancel(id);
            redirect.addFlashAttribute("successKey", "appointment.cancelled");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
        }
        return "dashboard".equals(from) ? "redirect:/patient/dashboard" : "redirect:/patient/appointments";
    }

    private static boolean isUpcoming(Appointment a, LocalDateTime now) {
        boolean active = a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED;
        return active && !a.getSlot().startAt().isBefore(now);
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}
