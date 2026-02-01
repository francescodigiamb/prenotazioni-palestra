package it.palestra.prenotazioni_palestra.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;

@Service
public class CleanupService {

    private final CorsoRepository corsoRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public CleanupService(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    // ====== MANUALE ADMIN ======
    // Elimina TUTTI i corsi archiviati (passati) + prenotazioni collegate
    @Transactional
    public int eliminaTuttiCorsiArchiviati() {

        List<Corso> tutti = corsoRepository.findAll();
        List<Corso> archiviati = new ArrayList<Corso>();

        for (Corso c : tutti) {
            if (c != null && isExpired(c)) {
                archiviati.add(c);
            }
        }

        return eliminaCorsi(archiviati);
    }

    // ====== AUTOMATICO ======
    // Elimina i corsi più vecchi di X giorni (es. 365) + prenotazioni collegate
    @Transactional
    public int eliminaCorsiPiuVecchiDiGiorni(int giorni) {

        LocalDate soglia = LocalDate.now().minusDays(giorni);

        List<Corso> tutti = corsoRepository.findAll();
        List<Corso> daEliminare = new ArrayList<Corso>();

        for (Corso c : tutti) {
            if (c == null || c.getData() == null) {
                continue;
            }

            // più vecchio della soglia
            if (c.getData().isBefore(soglia)) {
                daEliminare.add(c);
            }
        }

        return eliminaCorsi(daEliminare);
    }

    // Job notturno: cleanup dopo 365 giorni
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanupNotturno() {
        eliminaCorsiPiuVecchiDiGiorni(365);
    }

    // ====== METODI PRIVATI RIUSO ======

    private int eliminaCorsi(List<Corso> corsi) {

        int eliminati = 0;

        for (Corso c : corsi) {
            if (c == null || c.getId() == null) {
                continue;
            }

            // prima prenotazioni collegate
            prenotazioneRepository.deleteByCorso_Id(c.getId());

            // poi corso
            corsoRepository.deleteById(c.getId());

            eliminati++;
        }

        return eliminati;
    }

    private boolean isExpired(Corso c) {
        if (c == null || c.getData() == null || c.getOrario() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return c.getData().isBefore(today) || (c.getData().isEqual(today) && !c.getOrario().isAfter(now));
    }
}
