package vn.codegym.clinicbook.controller.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegym.clinicbook.dto.BookingDraft;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.service.AppointmentService;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SlotService;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import vn.codegym.clinicbook.constant.ErrorMessage;

@Controller
@RequestMapping("/booking")
public class BookingController {
    static final List<String> REASONS = List.of("checkup", "symptom", "followup", "prescription");
    private static final int MAX_DATES = 12;
    private static final int MAX_NOTE = 300;

    private final SlotService slotService;
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final AppointmentService appointmentService;
    private final MessageSource messageSource;

    public BookingController(SlotService slotService,
                             DoctorService doctorService,
                             SpecialtyService specialtyService,
                             AppointmentService appointmentService,
                             MessageSource messageSource) {
        this.slotService = slotService;
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.appointmentService = appointmentService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String page(@RequestParam(required = false) Long specialtyId,
                       @RequestParam(required = false) Long doctorId,
                       @RequestParam(required = false) Long slotId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model,
                       RedirectAttributes redirect) {
        try {
            Long selectedSlotId = null;
            if (slotId != null) {
                TimeSlot slot = slotService.findById(slotId);
                if (slot.getStatus() == SlotStatus.AVAILABLE) {
                    selectedSlotId = slotId;
                    doctorId = slot.getDoctor().getId();
                    date = slot.getSlotDate();
                }
            }

            Doctor doctor = doctorId == null ? null : doctorService.findById(doctorId);
            if (doctor != null && specialtyId != null && !doctor.getSpecialty().getId().equals(specialtyId)) {
                doctor = null;
                doctorId = null;
            }
            if (doctor != null) {
                specialtyId = doctor.getSpecialty().getId();
            }
            model.addAttribute("specialties", specialtyService.findAll());
            model.addAttribute("specialtyId", specialtyId);
            model.addAttribute("doctors", specialtyId == null ? List.of() : doctorService.findAll(specialtyId, ""));
            model.addAttribute("reasons", REASONS);
            model.addAttribute("doctorId", doctorId);

            if (doctor != null) {
                List<TimeSlot> free = slotService.findAvailable(doctorId);

                Map<LocalDate, Integer> dates = new LinkedHashMap<>();
                for (TimeSlot s : free) {
                    if (dates.size() >= MAX_DATES && !dates.containsKey(s.getSlotDate())) {
                        break;
                    }
                    dates.merge(s.getSlotDate(), 1, Integer::sum);
                }
                LocalDate selectedDate = (date != null && dates.containsKey(date)) ? date
                        : dates.isEmpty() ? null : dates.keySet().iterator().next();

                Map<String, List<TimeSlot>> periods = new LinkedHashMap<>();
                periods.put("morning", new ArrayList<>());
                periods.put("afternoon", new ArrayList<>());
                periods.put("evening", new ArrayList<>());
                for (TimeSlot s : free) {
                    if (s.getSlotDate().equals(selectedDate)) {
                        int hour = s.getStartTime().getHour();
                        periods.get(hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening").add(s);
                    }
                }
                periods.values().removeIf(List::isEmpty);

                model.addAttribute("doctor", doctor);
                model.addAttribute("dates", dates);
                model.addAttribute("selectedDate", selectedDate);
                model.addAttribute("periods", periods);
                model.addAttribute("selectedSlot", selectedSlotId == null ? null : slotService.findById(selectedSlotId));
            }
            return "booking/select";
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
            return "redirect:/booking";
        }
    }

    @PostMapping("/select")
    public String select(@RequestParam(required = false) Long slotId,
                         @RequestParam(required = false, defaultValue = "checkup") String reasonKey,
                         @RequestParam(required = false) String note,
                         HttpSession session,
                         RedirectAttributes redirect) {
        if (slotId == null) {
            redirect.addFlashAttribute("errorKey", "booking.pickSlot");
            return "redirect:/booking";
        }
        try {
            TimeSlot slot = slotService.findById(slotId);
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new BusinessException(ErrorMessage.SLOT_UNAVAILABLE);
            }
            session.setAttribute(BookingDraft.SESSION_KEY,
                    new BookingDraft(slot.getDoctor().getId(), slotId, buildReason(reasonKey, note)));
            return "redirect:/booking/confirm";
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
            return "redirect:/booking";
        }
    }

    @GetMapping("/confirm")
    public String confirmForm(HttpSession session, Model model, RedirectAttributes redirect) {
        BookingDraft draft = (BookingDraft) session.getAttribute(BookingDraft.SESSION_KEY);
        if (draft == null) {
            return "redirect:/booking";
        }
        try {
            model.addAttribute("slot", slotService.findById(draft.getSlotId()));
            model.addAttribute("reason", draft.getReason());
            return "booking/confirm";
        } catch (ResourceNotFoundException e) {
            session.removeAttribute(BookingDraft.SESSION_KEY);
            redirect.addFlashAttribute("errorKey", e.getMessage());
            return "redirect:/booking";
        }
    }

    @PostMapping("/confirm")
    public String confirm(HttpSession session, RedirectAttributes redirect) {
        BookingDraft draft = (BookingDraft) session.getAttribute(BookingDraft.SESSION_KEY);
        if (draft == null) {
            return "redirect:/booking";
        }
        try {
            appointmentService.book(draft.getSlotId(), draft.getReason());
            session.removeAttribute(BookingDraft.SESSION_KEY);
            redirect.addFlashAttribute("successKey", "booking.success");
            return "redirect:/patient/dashboard";
        } catch (BusinessException | ResourceNotFoundException e) {
            session.removeAttribute(BookingDraft.SESSION_KEY);
            redirect.addFlashAttribute("errorKey", e.getMessage());
            return "redirect:/booking?doctorId=" + draft.getDoctorId();
        }
    }

    @PostMapping("/cancel")
    public String cancel(HttpSession session) {
        BookingDraft draft = (BookingDraft) session.getAttribute(BookingDraft.SESSION_KEY);
        session.removeAttribute(BookingDraft.SESSION_KEY);
        return draft == null ? "redirect:/booking" : "redirect:/booking?doctorId=" + draft.getDoctorId();
    }

    private String buildReason(String reasonKey, String note) {
        String key = REASONS.contains(reasonKey) ? reasonKey : REASONS.get(0);
        Locale locale = LocaleContextHolder.getLocale();
        String reason = messageSource.getMessage("booking.reason." + key, null, locale);
        if (note != null && !note.isBlank()) {
            String trimmed = note.trim();
            reason += ": " + (trimmed.length() > MAX_NOTE ? trimmed.substring(0, MAX_NOTE) : trimmed);
        }
        return reason;
    }
}
