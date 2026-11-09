package vn.codegym.clinicbook.controller.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegym.clinicbook.dto.ChangePasswordRequest;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.security.CurrentUser;
import vn.codegym.clinicbook.service.AuthService;
import vn.codegym.clinicbook.constant.ErrorMessage;

@Controller
@RequestMapping("/account")
public class AccountWebController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AccountWebController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        model.addAttribute("profile", authService.getProfile(currentUser.username()));
        return "account/password";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult binding,
                                 Model model,
                                 RedirectAttributes redirect) {
        String username = currentUser.username();
        if (!binding.hasFieldErrors("newPassword") && !binding.hasFieldErrors("confirmPassword")
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", ErrorMessage.PASSWORD_MISMATCH);
        }
        if (!binding.hasErrors()) {
            try {
                authService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
                redirect.addFlashAttribute("successKey", "password.changed");
                return "redirect:/account/password";
            } catch (BusinessException e) {
                binding.rejectValue(e.getMessage().equals(ErrorMessage.PASSWORD_SAME_AS_OLD) ? "newPassword" : "currentPassword",
                        e.getMessage());
            }
        }
        model.addAttribute("profile", authService.getProfile(username));
        return "account/password";
    }
}
