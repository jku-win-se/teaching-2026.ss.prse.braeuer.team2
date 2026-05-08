# Systemdokumentation

## Überblick
Das System ist eine JavaFX-basierte Smart-Home-Anwendung zur Verwaltung von Geräten, Räume, Zeitplänen, Regeln, Energieverbrauch und Benutzerollen.


## Architektur

- UI: JavaFX mit FXML-Views und Controllern
- Domänenlogik: Klassen im Package 'model'
- Startpunkt: `main.java`

## Wichtige Designentscheidungen

Die Anwendung ist schichtenorientierung aufgebaut.
UI, Logik und Datenzugriff sind getrennt, damit Funktionen leichter getestet, erweitert und gewartet werden können.

## Erweiterungspunkte

- Neue Gerätentypen ergänzen, z. B. Kamera, Heizung, oder Lautsprecher
- Neue Regeln hinzufügen
- Weiter Auswertungen ergänzen, z. B. Energieverbrauch pro Monat
- Zusätzliche Speicherarten einbauen, z. B. Datenbank statt In-Memory-Speicherung
- Weitere Tests ergänzen, wenn neue Funktionen dazukommen


## Build und Qualität

- Build-Tool: Maven
- Tests: JUnit
- Statische Analyse: PMD

## Testfallbeschreibung und Testabdeckung

- Die Tests prüfen zentrale Funktionen wie Benutzerregistrierung und Login, Rollenrechte, Geräte- und Raumverwaltung, Zeitpläne, Regeln, Konflikterkennung, Energieauswertung und CSV-Export.
- Aktuelle Testabdeckung: 84,2 % (ohne UI-Klassen)





