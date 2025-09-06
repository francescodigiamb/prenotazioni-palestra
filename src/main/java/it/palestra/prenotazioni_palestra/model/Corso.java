package it.palestra.prenotazioni_palestra.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "corsi")
public class Corso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Il nome del corso è obbligatorio")
    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @NotNull(message = "La data è obbligatoria")
    @Column(nullable = false)
    private LocalDate data;

    @NotNull(message = "L'orario è obbligatorio")
    @Column(nullable = false)
    private LocalTime orario;

    @Positive(message = "Il numero massimo di posti deve essere positivo")
    @Column(nullable = false)
    private int maxPosti;

    // Costruttore

    public Corso() {

    }

    public Corso(String nome, String descrizione, LocalDate data, LocalTime orario, int maxPosti) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.data = data;
        this.orario = orario;
        this.maxPosti = maxPosti;
    }

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

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getOrario() {
        return orario;
    }

    public void setOrario(LocalTime orario) {
        this.orario = orario;
    }

    public int getMaxPosti() {
        return maxPosti;
    }

    public void setMaxPosti(int maxPosti) {
        this.maxPosti = maxPosti;
    }

}
