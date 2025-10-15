package it.palestra.prenotazioni_palestra.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "prenotazioni", uniqueConstraints = @UniqueConstraint(name = "uk_prenotazione_utente_corso", columnNames = {
        "utente_id", "corso_id" }))
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relazione con Utente
    @ManyToOne
    @JoinColumn(name = "utente_id", nullable = false)
    @NotNull(message = "L'utente è obbligatorio")
    private Utente utente;

    // Relazione con Corso
    @NotNull(message = "Il corso è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "corso_id", nullable = false)
    private Corso corso;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }

    // Costruttori
    public Prenotazione() {
    }

    public Prenotazione(Utente utente, Corso corso) {
        this.utente = utente;
        this.corso = corso;
    }

    // Getter e Setter
    public Integer getId() {
        return id;
    }

    public Utente getUtente() {
        return utente;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
    }

    public Corso getCorso() {
        return corso;
    }

    public void setCorso(Corso corso) {
        this.corso = corso;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
