package com.quizplatform.controller;

import com.quizplatform.service.PasswordResetService;
import com.quizplatform.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Data
    public static class RegisterForm {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @NotBlank
        private String email;
        @NotBlank
        private String fullName;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            return "redirect:" + (isAdmin ? "/admin/dashboard" : "/student/dashboard");
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm registerForm, Model model) {
        try {
            userService.registerStudent(
                    registerForm.getUsername().trim(),
                    registerForm.getPassword(),
                    registerForm.getEmail().trim(),
                    registerForm.getFullName().trim()
            );
            model.addAttribute("success", "Account created successfully! You can now log in.");
            model.addAttribute("registerForm", new RegisterForm());
            return "register";
        } catch (UserService.RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // ---------------- Forgot / reset password ----------------

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        passwordResetService.requestReset(email.trim());
        // Always show the same message, whether or not the email exists,
        // so the form can't be used to check which emails are registered.
        model.addAttribute("success",
                "If an account exists for that email, we've sent a password reset link. " +
                "Please check your inbox (and spam folder).");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        try {
            passwordResetService.validateToken(token);
            model.addAttribute("token", token);
        } catch (PasswordResetService.InvalidTokenException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            model.addAttribute("success", "Your password has been reset. You can now log in.");
        } catch (PasswordResetService.InvalidTokenException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
        }
        return "reset-password";
    }
}