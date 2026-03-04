# Prenotazioni Palestra – FitnessClub

Applicazione web per la gestione di **corsi fitness** e **prenotazioni utenti**, con controllo automatico della **capienza dei corsi** e gestione della **lista d’attesa**.

Questo progetto è nato come **progetto personale** per consolidare le mie competenze in **Java**, **Spring Boot** e **MySQL**, ma è stato sviluppato pensando ad un utilizzo reale per una palestra locale.

## Demo Online

L'applicazione è disponibile online:

👉 https://fitnessclubchieti.up.railway.app/home

## Stack Tecnologico

- Java
- Spring Boot
- Spring MVC + Thymeleaf
- Spring Data JPA / Hibernate
- MySQL
- Maven
- Railway (deploy)

## Funzionalità principali

- Registrazione e login utenti
- Sistema di **verifica email** per attivazione account
- Visualizzazione corsi disponibili
- Prenotazione ai corsi con controllo dei posti disponibili
- Gestione automatica della **lista d’attesa** quando il corso è pieno
- Possibilità di **cancellare la prenotazione fino a un’ora prima dell’inizio del corso**
- Area amministrativa per gestione corsi e prenotazioni
- Visualizzazione iscritti e lista d’attesa

## Logica di prenotazione

Il sistema gestisce automaticamente la disponibilità dei posti:

- se il corso ha posti disponibili → l’utente viene prenotato
- se il corso è pieno → l’utente entra in **lista d’attesa**
- se un utente cancella la prenotazione → il primo utente in lista d’attesa viene promosso automaticamente

## Avvio in locale

1. Crea un database MySQL (es. `prenotazioni_palestra`)

2. Configura il file `application.properties` con i dati di connessione al database

3. Avvia l’applicazione

mvn spring-boot:run

4. Apri il browser su

http://localhost:8080

## Obiettivo del progetto

L'obiettivo di questo progetto è stato sviluppare un'applicazione completa utilizzando **Java e Spring Boot**, simulando un sistema reale di gestione corsi per una palestra.

Il progetto mi ha permesso di lavorare su:

- gestione delle prenotazioni
- logica di business (posti disponibili e lista d’attesa)
- gestione utenti e autenticazione
- integrazione database con JPA/Hibernate
- deploy dell'applicazione online

## Stato del progetto

L'applicazione è **online e funzionante**.

Sono previste possibili migliorie future su:

- miglioramento dell'interfaccia grafica
- ulteriori validazioni lato server
- miglior gestione dei ruoli utente
