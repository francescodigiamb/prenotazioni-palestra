package it.palestra.prenotazioni_palestra.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Integer> {

    // --- Versione "ad oggetto" (quando hai già le entity a disposizione) ---
    List<Prenotazione> findByUtente(Utente utente);

    List<Prenotazione> findByCorso(Corso corso);

    int countByCorso(Corso corso);

    boolean existsByUtenteAndCorso(Utente utente, Corso corso);

    Optional<Prenotazione> findByUtenteAndCorso(Utente utente, Corso corso);

    // --- Versione "per id" (quando hai solo l'id a portata di mano) ---
    List<Prenotazione> findByUtente_Id(Integer utenteId);

    List<Prenotazione> findByCorso_Id(Integer corsoId);

    int countByCorso_Id(Integer corsoId);
}
