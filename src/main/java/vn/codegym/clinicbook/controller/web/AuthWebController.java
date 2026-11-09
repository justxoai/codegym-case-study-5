package vn.codegym.clinicbook.controller.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import vn.codegym.clinicbook.dto.RegisterRequest;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.service.AuthService;

@Controller
public class AuthWebController {
    private final AuthService authService;

    public AuthWebController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.registerPatient(request);
        } catch (BusinessException e) {
            binding.rejectValue("username", e.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
