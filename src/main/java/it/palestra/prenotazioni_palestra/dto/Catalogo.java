package it.palestra.prenotazioni_palestra.dto;

import java.util.List;

public class Catalogo {

    private String nome;
    private String descrizione;

    // es: ["Lunedì 18:00", "Mercoledì 20:30"]
    private List<String> giorniOrari;

    private int posti;
    private int listaAttesa; // (ex riserve)
    private String durata; // es: "50 min"

    public Catalogo(String nome, String descrizione, List<String> giorniOrari,
            int posti, int listaAttesa, String durata) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.giorniOrari = giorniOrari;
        this.posti = posti;
        this.listaAttesa = listaAttesa;
        this.durata = durata;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public List<String> getGiorniOrari() {
        return giorniOrari;
    }

    public int getPosti() {
        return posti;
    }

    public int getListaAttesa() {
        return listaAttesa;
    }

    public String getDurata() {
        return durata;
    }
}
