package vn.codegym.clinicbook.controller.web;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegym.clinicbook.dto.SlotRequest;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.service.AppointmentService;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SlotService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/doctor")
public class DoctorPortalController {
    private final DoctorService doctorService;
    private final SlotService slotService;
    private final AppointmentService appointmentService;

    public DoctorPortalController(DoctorService doctorService,
                                  SlotService slotService,
                                  AppointmentService appointmentService) {
        this.doctorService = doctorService;
        this.slotService = slotService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false, defaultValue = "ALL") String status,
                            @RequestParam(required = false, defaultValue = "") String keyword,
                            Authentication authentication,
                            Model model) {
        Doctor doctor = doctorService.findByUsername(authentication.getName());
        LocalDate today = LocalDate.now();
        LocalDate day = date == null ? today : date;
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> all = appointmentService.findForCurrentUser();
        List<Appointment> ofDay = all.stream().filter(a -> a.getSlot().getSlotDate().equals(day)).toList();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", (long) ofDay.size());
        for (AppointmentStatus st : AppointmentStatus.values()) {
            counts.put(st.name(), ofDay.stream().filter(a -> a.getStatus() == st).count());
        }

        AppointmentStatus filter = parseStatus(status);
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        List<Appointment> queue = ofDay.stream()
                .filter(a -> filter == null || a.getStatus() == filter)
                .filter(a -> needle.isEmpty()
                        || a.getPatient().getUser().getFullName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();

        Appointment next = ofDay.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(a -> !day.equals(today) || a.getSlot().getSlotDate().atTime(a.getSlot().getEndTime()).isAfter(now))
                .findFirst().orElse(null);

        List<Appointment> pendingAll = all.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING && !a.getSlot().startAt().isBefore(now))
                .toList();

        Map<LocalDate, Long> week = new LinkedHashMap<>();
        long weekMax = 1;
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            long n = all.stream()
                    .filter(a -> a.getSlot().getSlotDate().equals(d) && a.getStatus() != AppointmentStatus.CANCELLED)
                    .count();
            week.put(d, n);
            weekMax = Math.max(weekMax, n);
        }

        long freeSlotsOnDay = slotService.findAvailable(doctor.getId()).stream()
                .filter(s -> s.getSlotDate().equals(day)).count();

        model.addAttribute("doctor", doctor);
        model.addAttribute("day", day);
        model.addAttribute("today", today);
        model.addAttribute("status", filter == null ? "ALL" : filter.name());
        model.addAttribute("keyword", keyword);
        model.addAttribute("counts", counts);
        model.addAttribute("queue", queue);
        model.addAttribute("dayTotal", ofDay.size());
        model.addAttribute("next", next);
        model.addAttribute("pending", pendingAll.stream().limit(5).toList());
        model.addAttribute("pendingTotal", pendingAll.size());
        model.addAttribute("week", week);
        model.addAttribute("weekMax", weekMax);
        model.addAttribute("freeSlotsOnDay", freeSlotsOnDay);
        return "doctor/dashboard";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                           Authentication authentication, Model model) {
        fill(authentication, week, model);
        if (!model.containsAttribute("slotRequest")) {
            SlotRequest form = new SlotRequest();
            LocalDate today = LocalDate.now();
            form.setSlotDate(LocalTime.now().isAfter(LocalTime.of(20, 0)) ? today.plusDays(1) : today);
            model.addAttribute("slotRequest", form);
        }
        return "doctor/schedule";
    }

    @PostMapping("/slots")
    public String createSlot(@Valid @ModelAttribute("slotRequest") SlotRequest request,
                             BindingResult binding,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            fill(authentication, null, model);
            return "doctor/schedule";
        }
        try {
            slotService.create(request);
            redirect.addFlashAttribute("successKey", "slot.created");
        } catch (BusinessException e) {
            fill(authentication, request.getSlotDate(), model);
            model.addAttribute("errorKey", e.getMessage());
            return "doctor/schedule";
        }
        return "redirect:" + scheduleUrl(request.getSlotDate());
    }

    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@PathVariable Long id,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                             RedirectAttributes redirect) {
        try {
            slotService.delete(id);
            redirect.addFlashAttribute("successKey", "slot.deleted");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
        }
        return "redirect:" + scheduleUrl(week);
    }

    @PostMapping("/appointments/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam AppointmentStatus status,
                               @RequestParam(required = false) String from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                               RedirectAttributes redirect) {
        try {
            appointmentService.updateStatus(id, status);
            redirect.addFlashAttribute("successKey", "appointment.updated");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
        }
        if ("dashboard".equals(from)) {
            return "redirect:/doctor/dashboard" + (date == null ? "" : "?date=" + date);
        }
        return "redirect:" + scheduleUrl(week);
    }

    private static String scheduleUrl(LocalDate week) {
        return "/doctor/schedule" + (week == null ? "" : "?week=" + week);
    }

    private static AppointmentStatus parseStatus(String value) {
        try {
            return AppointmentStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void fill(Authentication authentication, LocalDate weekDate, Model model) {
        Doctor doctor = doctorService.findByUsername(authentication.getName());
        LocalDate today = LocalDate.now();
        LocalDate monday = (weekDate == null ? today : weekDate).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(monday.plusDays(i));
        }

        List<TimeSlot> slots = slotService.findBetween(doctor.getId(), monday, sunday);
        List<Appointment> all = appointmentService.findForCurrentUser();

        Map<Long, Appointment> bySlot = new HashMap<>();
        for (Appointment a : all) {
            if (a.getStatus() != AppointmentStatus.CANCELLED) {
                bySlot.put(a.getSlot().getId(), a);
            }
        }

        List<TimetableRow> rows = new ArrayList<>();
        if (!slots.isEmpty()) {
            int first = slots.stream().mapToInt(s -> s.getStartTime().getHour()).min().orElse(8);
            int last = slots.stream().mapToInt(s -> s.getStartTime().getHour()).max().orElse(first);
            for (int hour = first; hour <= last; hour++) {
                List<List<TimeSlot>> cells = new ArrayList<>();
                for (LocalDate d : days) {
                    final int h = hour;
                    cells.add(slots.stream()
                            .filter(s -> s.getSlotDate().equals(d) && s.getStartTime().getHour() == h)
                            .toList());
                }
                rows.add(new TimetableRow(hour, cells));
            }
        }

        long booked = slots.stream().filter(s -> s.getStatus() == SlotStatus.BOOKED).count();
        long visits = slots.stream().filter(s -> bySlot.containsKey(s.getId())).count();
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> pending = all.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING && !a.getSlot().startAt().isBefore(now))
                .toList();
        Appointment next = all.stream()
                .filter(a -> (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                        && !a.getSlot().startAt().isBefore(now))
                .findFirst().orElse(null);

        model.addAttribute("doctor", doctor);
        model.addAttribute("today", today);
        model.addAttribute("monday", monday);
        model.addAttribute("sunday", sunday);
        model.addAttribute("prevWeek", monday.minusWeeks(1));
        model.addAttribute("nextWeek", monday.plusWeeks(1));
        model.addAttribute("days", days);
        model.addAttribute("rows", rows);
        model.addAttribute("bySlot", bySlot);
        model.addAttribute("slotCount", slots.size());
        model.addAttribute("capacity", slots.isEmpty() ? 0 : Math.round(booked * 100.0 / slots.size()));
        model.addAttribute("visits", visits);
        model.addAttribute("next", next);
        model.addAttribute("pending", pending.stream().limit(5).toList());
        model.addAttribute("pendingTotal", pending.size());
    }

    public static class TimetableRow {
        private final int hour;
        private final List<List<TimeSlot>> cells;

        public TimetableRow(int hour, List<List<TimeSlot>> cells) {
            this.hour = hour;
            this.cells = cells;
        }

        public int getHour() {
            return hour;
        }

        public List<List<TimeSlot>> getCells() {
            return cells;
        }
    }
}
