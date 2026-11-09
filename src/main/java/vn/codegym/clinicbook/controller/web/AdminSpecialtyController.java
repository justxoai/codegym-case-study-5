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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegym.clinicbook.dto.SpecialtyRequest;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.service.AdminDashboardService;
import vn.codegym.clinicbook.service.AdminDashboardService.Overview;
import vn.codegym.clinicbook.service.AdminDashboardService.SpecialtyLoad;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.util.Comparator;

@Controller
@RequestMapping("/admin/specialties")
public class AdminSpecialtyController {
    private final SpecialtyService specialtyService;
    private final AdminDashboardService dashboardService;

    public AdminSpecialtyController(SpecialtyService specialtyService, AdminDashboardService dashboardService) {
        this.specialtyService = specialtyService;
        this.dashboardService = dashboardService;
    }

    private void fillList(Model model) {
        Overview overview = dashboardService.overview();
        model.addAttribute("o", overview);
        model.addAttribute("loads", overview.getSpecialties().stream()
                .sorted(Comparator.comparing((SpecialtyLoad l) -> l.getSpecialty().getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    @GetMapping
    public String list(Model model) {
        fillList(model);
        model.addAttribute("specialtyRequest", new SpecialtyRequest());
        model.addAttribute("editId", null);
        return "admin/specialties";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Specialty specialty = specialtyService.findById(id);
        SpecialtyRequest form = new SpecialtyRequest();
        form.setName(specialty.getName());
        form.setNameEn(specialty.getNameEn());
        form.setDescription(specialty.getDescription());
        fillList(model);
        model.addAttribute("specialtyRequest", form);
        model.addAttribute("editId", id);
        return "admin/specialties";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("specialtyRequest") SpecialtyRequest request,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        return save(null, request, binding, model, redirect);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("specialtyRequest") SpecialtyRequest request,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        return save(id, request, binding, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            specialtyService.delete(id);
            redirect.addFlashAttribute("successKey", "specialty.deleted");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("errorKey", e.getMessage());
        }
        return "redirect:/admin/specialties";
    }

    private String save(Long id, SpecialtyRequest request, BindingResult binding,
                        Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                if (id == null) {
                    specialtyService.create(request);
                } else {
                    specialtyService.update(id, request);
                }
                redirect.addFlashAttribute("successKey", "specialty.saved");
                return "redirect:/admin/specialties";
            } catch (BusinessException e) {
                binding.rejectValue("name", e.getMessage());
            }
        }
        fillList(model);
        model.addAttribute("editId", id);
        return "admin/specialties";
    }
}
