package it.palestra.prenotazioni_palestra.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.palestra.prenotazioni_palestra.service.EmailService;

@Profile("dev")
@Controller
@RequestMapping("/admin")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/email-test")
    public String emailTest() {
        return "admin-email-test";
    }

    @PostMapping("/email-test")
    public String inviaEmailTest(@RequestParam("to") String to, RedirectAttributes ra) {
        emailService.inviaEmailTest(to.trim());
        ra.addFlashAttribute("success", "Email inviata a " + to);
        return "redirect:/admin/email-test";
    }
}
