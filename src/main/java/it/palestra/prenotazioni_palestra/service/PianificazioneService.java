package it.palestra.prenotazioni_palestra.service;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.ModelloCorso;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.ModelloCorsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class PianificazioneService {

    private final ModelloCorsoRepository modelloRepo;
    private final CorsoRepository corsoRepo;

    public PianificazioneService(ModelloCorsoRepository modelloRepo, CorsoRepository corsoRepo) {
        this.modelloRepo = modelloRepo;
        this.corsoRepo = corsoRepo;
    }

    @Transactional
    public int generaOccorrenze(Integer modelloId, LocalDate dal, LocalDate al) {
        ModelloCorso m = modelloRepo.findById(modelloId)
                .orElseThrow(() -> new IllegalArgumentException("Modello non trovato"));

        int creati = 0;

        creati += generaPerGiorno(m, m.getLunedi(), m.getOrarioLunedi(), DayOfWeek.MONDAY, dal, al);
        creati += generaPerGiorno(m, m.getMartedi(), m.getOrarioMartedi(), DayOfWeek.TUESDAY, dal, al);
        creati += generaPerGiorno(m, m.getMercoledi(), m.getOrarioMercoledi(), DayOfWeek.WEDNESDAY, dal, al);
        creati += generaPerGiorno(m, m.getGiovedi(), m.getOrarioGiovedi(), DayOfWeek.THURSDAY, dal, al);
        creati += generaPerGiorno(m, m.getVenerdi(), m.getOrarioVenerdi(), DayOfWeek.FRIDAY, dal, al);
        creati += generaPerGiorno(m, m.getSabato(), m.getOrarioSabato(), DayOfWeek.SATURDAY, dal, al);
        creati += generaPerGiorno(m, m.getDomenica(), m.getOrarioDomenica(), DayOfWeek.SUNDAY, dal, al);

        return creati;
    }

    private int generaPerGiorno(ModelloCorso m,
            Boolean abilitato,
            LocalTime orario,
            DayOfWeek giorno,
            LocalDate dal,
            LocalDate al) {

        if (abilitato == null || !abilitato || orario == null) {
            return 0;
        }

        int creati = 0;

        // trova la prima data nel range con quel giorno della settimana
        LocalDate d = dal;
        while (d.getDayOfWeek() != giorno) {
            d = d.plusDays(1);
            if (d.isAfter(al)) {
                return 0;
            }
        }

        // loop ogni 7 giorni
        for (; !d.isAfter(al); d = d.plusWeeks(1)) {

            boolean esiste = corsoRepo.existsByDataAndOrarioAndModello(d, orario, m);
            if (esiste) {
                continue;
            }

            Corso c = new Corso();
            c.setData(d);
            c.setOrario(orario);
            c.setMaxPosti(m.getMaxPostiDefault());
            c.setNome(m.getNome());
            c.setDescrizione(m.getDescrizione());
            // se hai flag tipo chiuso/archiviato, setta qui i default
            // c.setChiuso(false);
            c.setModello(m);

            corsoRepo.save(c);
            creati++;
        }

        return creati;
    }
}
