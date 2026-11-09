package vn.codegym.clinicbook.controller.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.codegym.clinicbook.aop.AuditDetail;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.service.AdminDashboardService;
import vn.codegym.clinicbook.service.AdminScheduleService;
import vn.codegym.clinicbook.service.AuditLogService;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminDashboardService dashboardService;
    private final AdminScheduleService scheduleService;
    private final AuditLogService auditLogService;
    private final SpecialtyService specialtyService;

    public AdminController(AdminDashboardService dashboardService,
                           AdminScheduleService scheduleService,
                           AuditLogService auditLogService,
                           SpecialtyService specialtyService) {
        this.dashboardService = dashboardService;
        this.scheduleService = scheduleService;
        this.auditLogService = auditLogService;
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("o", dashboardService.overview());
        model.addAttribute("logs", auditLogService.findLatest());
        return "admin/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) Long specialty,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "1") int page,
                               Model model) {
        LocalDate today = LocalDate.now();
        LocalDate day = date == null ? today : date;
        AppointmentStatus filter = parseStatus(status);

        AdminScheduleService.History history = scheduleService.history(filter, specialty, keyword, page);
        Map<String, Object> keep = new LinkedHashMap<>();
        keep.put("date", date);
        keep.put("specialty", specialty);
        keep.put("status", filter);
        keep.put("keyword", keyword);
        history.getPage().link("/admin/appointments", keep);

        model.addAttribute("today", today);
        model.addAttribute("day", day);
        model.addAttribute("sched", scheduleService.day(day, specialty));
        model.addAttribute("history", history);
        model.addAttribute("specialties", specialtyService.findAll());
        model.addAttribute("specialty", specialty);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", filter == null ? "ALL" : filter.name());
        model.addAttribute("appointmentTotal", scheduleService.appointmentCount());
        model.addAttribute("auditTotal", auditLogService.total());
        return "admin/appointments";
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(defaultValue = "") String action,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(required = false) Long appointment,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        String search = appointment != null ? AuditDetail.keyOf(appointment) : keyword;
        AuditLogService.Search result = auditLogService.search(date, action, search, page);

        Map<String, Object> keep = new LinkedHashMap<>();
        keep.put("date", date);
        keep.put("action", action);
        keep.put("keyword", keyword);
        keep.put("appointment", appointment);
        result.getPage().link("/admin/audit", keep);

        model.addAttribute("result", result);
        model.addAttribute("summary", auditLogService.summary());
        model.addAttribute("actions", auditLogService.actions());
        model.addAttribute("date", date);
        model.addAttribute("action", action);
        model.addAttribute("keyword", keyword);
        model.addAttribute("appointmentFilter", appointment);
        model.addAttribute("appointmentTotal", scheduleService.appointmentCount());
        return "admin/audit";
    }

    private static AppointmentStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return AppointmentStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
