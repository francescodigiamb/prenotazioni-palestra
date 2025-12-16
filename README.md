# Prenotazioni Palestra (FitnessClub) - Work in Progress

Applicazione web per la gestione di **corsi** e **prenotazioni** con **capienza** e **lista d’attesa**.
Progetto personale in Java/Spring Boot.

## Stack
- Java, Spring Boot
- Spring MVC + Thymeleaf
- Spring Data JPA / Hibernate
- MySQL
- Maven

## Funzionalità principali
- CRUD Corsi
- Prenotazione corso con controllo posti disponibili
- Gestione lista d’attesa (riserve)
- Area admin per gestione corsi e prenotazioni

## Avvio in locale
1. Crea un database MySQL (es. `prenotazioni_palestra`)
2. Configura `application.properties`
3. Avvia:
   - `mvn spring-boot:run`
4. Apri: `http://localhost:8080`

## Roadmap
- migliorare la separazione ruoli (admin/utente)
- rifiniture UI e validazioni
