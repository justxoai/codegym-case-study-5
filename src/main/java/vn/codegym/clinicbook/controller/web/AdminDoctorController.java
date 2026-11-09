package vn.codegym.clinicbook.controller.web;

import jakarta.validation.Valid;
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
import vn.codegym.clinicbook.dto.DoctorRequest;
import vn.codegym.clinicbook.dto.PageView;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.service.AdminDashboardService;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/doctors")
public class AdminDoctorController {
    private final DoctorService doctorService;
    private final SpecialtyService specialtyService;
    private final AdminDashboardService dashboardService;

    public AdminDoctorController(DoctorService doctorService,
                                 SpecialtyService specialtyService,
                                 AdminDashboardService dashboardService) {
        this.doctorService = doctorService;
        this.specialtyService = specialtyService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long specialty,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        List<Doctor> all = doctorService.findAll(specialty, keyword);
        Map<String, Object> keep = new LinkedHashMap<>();
        keep.put("specialty", specialty);
        keep.put("keyword", keyword);
        PageView<Doctor> view = PageView.of(all, page, PageView.DEFAULT_SIZE).link("/admin/doctors", keep);

        model.addAttribute("o", dashboardService.overview());
        model.addAttribute("doctors", view.getContent());
        model.addAttribute("pv", view);
        model.addAttribute("specialties", specialtyService.findAll());
        model.addAttribute("specialty", specialty);
        model.addAttribute("keyword", keyword);
        return "admin/doctors";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        DoctorRequest form = new DoctorRequest();
        form.setYearsExperience(0);
        model.addAttribute("doctorRequest", form);
        model.addAttribute("editId", null);
        model.addAttribute("specialties", specialtyService.findAll());
        return "admin/doctor-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.findById(id);
        DoctorRequest form = new DoctorRequest();
        form.setFullName(doctor.getFullName());
        form.setSpecialtyId(doctor.getSpecialty().getId());
        form.setYearsExperience(doctor.getYearsExperience());
        form.setBio(doctor.getBio());
        model.addAttribute("doctorRequest", form);
        model.addAttribute("editId", id);
        model.addAttribute("specialties", specialtyService.findAll());
        return "admin/doctor-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("doctorRequest") DoctorRequest request,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        return save(null, request, binding, model, redirect);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("doctorRequest") DoctorRequest request,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        return save(id, request, binding, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            doctorService.delete(id);
            redirect.addFlashAttribute("successKey", "doctor.deleted");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    private String save(Long id, DoctorRequest request, BindingResult binding,
                        Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                if (id == null) {
                    doctorService.create(request);
                } else {
                    doctorService.update(id, request);
                }
                redirect.addFlashAttribute("successKey", "doctor.saved");
                return "redirect:/admin/doctors";
            } catch (BusinessException e) {
                model.addAttribute("errorKey", e.getMessage());
            }
        }
        model.addAttribute("editId", id);
        model.addAttribute("specialties", specialtyService.findAll());
        return "admin/doctor-form";
    }
}
