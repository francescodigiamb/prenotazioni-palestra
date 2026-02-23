package it.palestra.prenotazioni_palestra.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "modelli_corso")
public class ModelloCorso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome; // es. "Pilates"

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column(nullable = false)
    private int maxPostiDefault = 12;

    @Column(nullable = false)
    private boolean attivo = true;

    // ====== Programmazione settimanale ======

    private Boolean lunedi;
    private LocalTime orarioLunedi;
    private LocalTime orarioLunedi2;

    private Boolean martedi;
    private LocalTime orarioMartedi;
    private LocalTime orarioMartedi2;

    private Boolean mercoledi;
    private LocalTime orarioMercoledi;
    private LocalTime orarioMercoledi2;

    private Boolean giovedi;
    private LocalTime orarioGiovedi;
    private LocalTime orarioGiovedi2;

    private Boolean venerdi;
    private LocalTime orarioVenerdi;
    private LocalTime orarioVenerdi2;

    private Boolean sabato;
    private LocalTime orarioSabato;
    private LocalTime orarioSabato2;

    public LocalTime getOrarioLunedi2() {
        return orarioLunedi2;
    }

    public void setOrarioLunedi2(LocalTime orarioLunedi2) {
        this.orarioLunedi2 = orarioLunedi2;
    }

    public LocalTime getOrarioMartedi2() {
        return orarioMartedi2;
    }

    public void setOrarioMartedi2(LocalTime orarioMartedi2) {
        this.orarioMartedi2 = orarioMartedi2;
    }

    public LocalTime getOrarioMercoledi2() {
        return orarioMercoledi2;
    }

    public void setOrarioMercoledi2(LocalTime orarioMercoledi2) {
        this.orarioMercoledi2 = orarioMercoledi2;
    }

    public LocalTime getOrarioGiovedi2() {
        return orarioGiovedi2;
    }

    public void setOrarioGiovedi2(LocalTime orarioGiovedi2) {
        this.orarioGiovedi2 = orarioGiovedi2;
    }

    public LocalTime getOrarioVenerdi2() {
        return orarioVenerdi2;
    }

    public void setOrarioVenerdi2(LocalTime orarioVenerdi2) {
        this.orarioVenerdi2 = orarioVenerdi2;
    }

    public LocalTime getOrarioSabato2() {
        return orarioSabato2;
    }

    public void setOrarioSabato2(LocalTime orarioSabato2) {
        this.orarioSabato2 = orarioSabato2;
    }

    public LocalTime getOrarioDomenica2() {
        return orarioDomenica2;
    }

    public void setOrarioDomenica2(LocalTime orarioDomenica2) {
        this.orarioDomenica2 = orarioDomenica2;
    }

    private Boolean domenica;
    private LocalTime orarioDomenica;
    private LocalTime orarioDomenica2;

    // ====== Getter / Setter ======

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

    public int getMaxPostiDefault() {
        return maxPostiDefault;
    }

    public void setMaxPostiDefault(int maxPostiDefault) {
        this.maxPostiDefault = maxPostiDefault;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public Boolean getLunedi() {
        return lunedi;
    }

    public void setLunedi(Boolean lunedi) {
        this.lunedi = lunedi;
    }

    public LocalTime getOrarioLunedi() {
        return orarioLunedi;
    }

    public void setOrarioLunedi(LocalTime orarioLunedi) {
        this.orarioLunedi = orarioLunedi;
    }

    public Boolean getMartedi() {
        return martedi;
    }

    public void setMartedi(Boolean martedi) {
        this.martedi = martedi;
    }

    public LocalTime getOrarioMartedi() {
        return orarioMartedi;
    }

    public void setOrarioMartedi(LocalTime orarioMartedi) {
        this.orarioMartedi = orarioMartedi;
    }

    public Boolean getMercoledi() {
        return mercoledi;
    }

    public void setMercoledi(Boolean mercoledi) {
        this.mercoledi = mercoledi;
    }

    public LocalTime getOrarioMercoledi() {
        return orarioMercoledi;
    }

    public void setOrarioMercoledi(LocalTime orarioMercoledi) {
        this.orarioMercoledi = orarioMercoledi;
    }

    public Boolean getGiovedi() {
        return giovedi;
    }

    public void setGiovedi(Boolean giovedi) {
        this.giovedi = giovedi;
    }

    public LocalTime getOrarioGiovedi() {
        return orarioGiovedi;
    }

    public void setOrarioGiovedi(LocalTime orarioGiovedi) {
        this.orarioGiovedi = orarioGiovedi;
    }

    public Boolean getVenerdi() {
        return venerdi;
    }

    public void setVenerdi(Boolean venerdi) {
        this.venerdi = venerdi;
    }

    public LocalTime getOrarioVenerdi() {
        return orarioVenerdi;
    }

    public void setOrarioVenerdi(LocalTime orarioVenerdi) {
        this.orarioVenerdi = orarioVenerdi;
    }

    public Boolean getSabato() {
        return sabato;
    }

    public void setSabato(Boolean sabato) {
        this.sabato = sabato;
    }

    public LocalTime getOrarioSabato() {
        return orarioSabato;
    }

    public void setOrarioSabato(LocalTime orarioSabato) {
        this.orarioSabato = orarioSabato;
    }

    public Boolean getDomenica() {
        return domenica;
    }

    public void setDomenica(Boolean domenica) {
        this.domenica = domenica;
    }

    public LocalTime getOrarioDomenica() {
        return orarioDomenica;
    }

    public void setOrarioDomenica(LocalTime orarioDomenica) {
        this.orarioDomenica = orarioDomenica;
    }
}
