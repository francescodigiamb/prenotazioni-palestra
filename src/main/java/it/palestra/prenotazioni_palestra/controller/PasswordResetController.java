package it.palestra.prenotazioni_palestra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import it.palestra.prenotazioni_palestra.service.PasswordResetService;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // --- FORM: "Password dimenticata" ---
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam("email") String email, Model model) {

        // Non riveliamo mai se l'email esiste o no
        passwordResetService.requestReset(email);

        model.addAttribute("success",
                "Se l'email è corretta, riceverai un link per reimpostare la password entro pochi minuti.");
        return "forgot-password";
    }

    // --- FORM: reset con token ---
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token, Model model) {

        try {
            passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            return "auth/reset-password";
        } catch (Exception ex) {
            model.addAttribute("error", "Link non valido o scaduto. Richiedi un nuovo reset password.");
            return "reset-password-error";
        }
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "La password deve contenere almeno 6 caratteri.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Le password non coincidono.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(token, password);
            model.addAttribute("success", "Password aggiornata con successo. Ora puoi effettuare il login.");
            return "login";
        } catch (Exception ex) {
            model.addAttribute("error", "Impossibile aggiornare la password. Il link potrebbe essere scaduto.");
            return "reset-password-error";
        }
    }
}
