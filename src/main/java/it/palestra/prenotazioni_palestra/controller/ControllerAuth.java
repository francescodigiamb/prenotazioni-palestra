package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.dto.RegistrationForm;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import it.palestra.prenotazioni_palestra.service.EmailService;
import it.palestra.prenotazioni_palestra.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControllerAuth {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final EmailService emailService;

    public ControllerAuth(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder,
            VerificationService verificationService, EmailService emailService) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult binding,
            RedirectAttributes ra) {

        // validazioni custom
        if (!form.getPassword().equals(form.getConfermaPassword())) {
            binding.rejectValue("confermaPassword", "match", "Le password non coincidono");
        }
        if (utenteRepository.findByEmail(form.getEmail().trim()).isPresent()) {
            binding.rejectValue("email", "unique", "Email già registrata");
        }
        if (binding.hasErrors()) {
            return "register";
        }

        // crea utente
        Utente u = new Utente();
        u.setNome(form.getNome().trim());
        u.setCognome(form.getCognome().trim());
        u.setEmail(form.getEmail().trim());
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setRuolo("UTENTE");
        utenteRepository.save(u);

        // ✅ EMAIL conferma registrazione (NO conferma prenotazione, solo registrazione)
        emailService.inviaConfermaRegistrazione(u.getEmail(), u.getNome());

        // email verifica
        verificationService.sendVerification(u);

        ra.addFlashAttribute("success",
                "Registrazione quasi completata! Controlla la tua email (Controlla anche in Indesiderata) e clicca sul link di verifica per attivare l’account.");
        return "redirect:/login";

    }

}
