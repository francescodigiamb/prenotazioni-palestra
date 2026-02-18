package it.palestra.prenotazioni_palestra.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.palestra.prenotazioni_palestra.model.PasswordResetToken;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.PasswordResetTokenRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UtenteRepository utenteRepo;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo,
            UtenteRepository utenteRepo,
            PasswordEncoder passwordEncoder,
            BrevoEmailService brevoEmailService) {
        this.tokenRepo = tokenRepo;
        this.utenteRepo = utenteRepo;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;
    }

    /**
     * Invia email reset password se l'utente esiste.
     * (Non rivela mai se l'email è presente o no: sicurezza)
     */
    @Transactional
    public void requestReset(String email) {

        // pulizia token scaduti (leggera)
        tokenRepo.deleteByExpiresAtBefore(LocalDateTime.now());

        Optional<Utente> opt = utenteRepo.findByEmail(email);
        if (opt.isEmpty()) {
            // Non rivelare nulla
            return;
        }

        Utente utente = opt.get();

        // crea token valido 30 minuti
        PasswordResetToken token = PasswordResetToken.of(utente, 30);
        tokenRepo.save(token);

        String link = baseUrl + "/reset-password?token=" + token.getToken();
        String subject = "Reimposta la password – FitnessClub Chieti";

        String html = """
                <p>Ciao,</p>
                <p>Abbiamo ricevuto una richiesta di reimpostazione password per il tuo account.</p>
                <p>Clicca qui per impostare una nuova password (valido 30 minuti):</p>
                <p><a href="%s">Reimposta password</a></p>
                <p>Se non sei stato tu, ignora questa email.</p>
                <p>A presto,<br>FitnessClub Chieti</p>
                """.formatted(link);

        brevoEmailService.sendVerificationEmail(mailFrom, utente.getEmail(), subject, html);
        // (il metodo si chiama ancora sendVerificationEmail, ma lo usiamo come
        // "sendEmail".
        // Se vuoi, dopo lo rinominiamo in un nome più generico.)
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String token) {
        PasswordResetToken t = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token non valido"));

        if (t.isUsed()) {
            throw new IllegalArgumentException("Token già utilizzato");
        }
        if (t.isExpired()) {
            throw new IllegalArgumentException("Token scaduto");
        }
        return t;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken t = validateToken(token);

        Utente utente = t.getUtente();
        utente.setPassword(passwordEncoder.encode(newPassword));
        // se hai un metodo diverso o un campo diverso, lo adattiamo

        t.markUsed();

        // grazie a @Transactional, salva tutto
        utenteRepo.save(utente);
        tokenRepo.save(t);
    }
}
