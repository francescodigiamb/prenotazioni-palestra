package it.palestra.prenotazioni_palestra.service;

import java.time.LocalDate;
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

    // ogni giorno alle 03:30
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void eliminaCorsiArchiviatiDopoUnAnno() {

        int giorni = 365;
        LocalDate soglia = LocalDate.now().minusDays(giorni);

        List<Corso> tutti = corsoRepository.findAll();
        List<Corso> daEliminare = new ArrayList<Corso>();

        for (Corso c : tutti) {
            if (c == null || c.getData() == null) {
                continue;
            }

            // “passato da oltre 365 giorni”
            if (c.getData().isBefore(soglia)) {
                daEliminare.add(c);
            }
        }

        for (Corso c : daEliminare) {
            prenotazioneRepository.deleteByCorso_Id(c.getId());
            corsoRepository.deleteById(c.getId());
        }
    }
}
