package it.palestra.prenotazioni_palestra.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
    }

    public static PasswordResetToken of(Utente utente, int minutesValid) {
        PasswordResetToken t = new PasswordResetToken();
        t.utente = utente;
        t.token = UUID.randomUUID().toString().replace("-", "");
        t.expiresAt = LocalDateTime.now().plusMinutes(minutesValid);
        t.used = false;
        return t;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void markUsed() {
        this.used = true;
    }

    // getter/setter

    public Integer getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Utente getUtente() {
        return utente;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
