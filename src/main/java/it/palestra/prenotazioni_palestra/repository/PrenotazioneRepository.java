package it.palestra.prenotazioni_palestra.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Integer> {

  // --- Versione "ad oggetto" (quando hai già le entity a disposizione) ---
  List<Prenotazione> findByUtente(Utente utente);

  List<Prenotazione> findByCorso(Corso corso);

  // ordinato per più recente prima
  List<Prenotazione> findByCorsoOrderByCreatedAtDesc(Corso corso);

  int countByCorso(Corso corso);

  boolean existsByUtenteAndCorso(Utente utente, Corso corso);

  Optional<Prenotazione> findByUtenteAndCorso(Utente utente, Corso corso);

  // --- Versione "per id" (quando hai solo l'id a portata di mano) ---
  List<Prenotazione> findByUtente_Id(Integer utenteId);

  List<Prenotazione> findByCorso_Id(Integer corsoId);

  int countByCorso_Id(Integer corsoId);

  // Elenco prenotazioni dato l'email utente (navigazione dell'associazione)
  List<Prenotazione> findByUtente_Email(String email);

  void deleteByCorso(Corso corso);

  int countByCorsoAndRiservaFalse(Corso corso);

  int countByCorsoAndRiservaTrue(Corso corso);

  List<Prenotazione> findByCorsoAndRiservaFalse(Corso corso);

  List<Prenotazione> findByCorsoAndRiservaTrue(Corso corso);

  long countByCorso_IdAndRiservaFalse(Integer corsoId);

  long countByCorso_IdAndRiservaTrue(Integer corsoId);

  @Query("""
          SELECT p
          FROM Prenotazione p
          WHERE (:corsoId IS NULL OR p.corso.id = :corsoId)
            AND (:dataDa IS NULL OR p.corso.data >= :dataDa)
            AND (:dataA IS NULL OR p.corso.data <= :dataA)
          ORDER BY p.corso.data ASC, p.corso.orario ASC, p.id ASC
      """)
  List<Prenotazione> cercaPrenotazioniAdmin(
      @Param("corsoId") Integer corsoId,
      @Param("dataDa") LocalDate dataDa,
      @Param("dataA") LocalDate dataA);

}
