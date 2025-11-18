package it.palestra.prenotazioni_palestra.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "utenti")

public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Il nome è obbligatorio") // non può essere vuota o solo spazi
    @Column(nullable = false) // obbligatorio
    private String nome;

    @Column(unique = true)
    @NotBlank(message = "L'email è obbligatoria") // non può essere vuota o solo spazi
    @Email(message = "Formato email non valido") // deve avere un formato corretto
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 6, message = "La password deve avere almeno 6 caratteri")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Il ruolo è obbligatorio")
    @Column(nullable = false)
    private String ruolo; // UTENTE o ISTRUTTORE

    // flag per bloccare chi non ha verificato
    @Column(nullable = false)
    private boolean enabled = false; // per i nuovi utenti da registrazione

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Costruttori
    public Utente() {
    }

    public Utente(String nome, String email, String password, String ruolo) {
        this.nome = nome;
        this.email = email;
        this.password = password;
        this.ruolo = ruolo;
    }

    // Getter e Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

}
