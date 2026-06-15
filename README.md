# HomeE

HomeE ist eine JavaFX-basierte Smarthome-Anwendung für die Verwaltung eines privaten Haushalts. Benutzerinnen und Benutzer können Räume anlegen, Geräte hinzufügen, Geräte manuell steuern, Automatisierungen erstellen, Zeitpläne definieren, Szenen ausführen, den Energieverbrauch einsehen und Aktivitäten nachvollziehen. Die Anwendung richtet sich an Haushalte, die typische Smarthome-Abläufe übersichtlich in einer Desktop-Anwendung simulieren und verwalten möchten.

Testabdeckung:
![Model Test Coverage](https://raw.githubusercontent.com/jku-win-se/teaching-2026.ss.prse.braeuer.team2/gh-pages/badges/jacoco-model.svg)


# Umgesetzte Anforderungen

Alle im Projekt vorgesehenen Anforderungen wurden umgesetzt. Die folgende Tabelle dokumentiert die Anforderungen aus den GitHub-Issues.

| ID | Anforderung | Umsetzung in HomeE | Status |
|---|---|---|---|
| FR-01 | Benutzerregistrierung | Benutzer können ein Konto mit E-Mail-Adresse und Passwort erstellen. Passwörter werden nicht im Klartext gespeichert. | umgesetzt |
| FR-02 | Benutzer-Login | Registrierte Benutzer können sich anmelden und erhalten Zugriff auf ihren Haushalt. | umgesetzt |
| FR-03 | Raumverwaltung | Eigentümer können Räume erstellen, umbenennen und löschen. | umgesetzt |
| FR-04 | Gerät zu Raum hinzufügen | Geräte können einem bestehenden Raum zugeordnet werden. | umgesetzt |
| FR-05 | Gerät entfernen oder umbenennen | Geräte können umbenannt und entfernt werden. Abhängige Regeln, Zeitpläne und Szenen werden berücksichtigt. | umgesetzt |
| FR-06 | Gerät manuell steuern | Geräte können abhängig vom Gerätetyp manuell geschaltet oder über Werte gesteuert werden. | umgesetzt |
| FR-07 | Gerätestatus anzeigen | Der aktuelle Zustand jedes Geräts wird im Dashboard angezeigt. | umgesetzt |
| FR-08 | Aktivitätslog für Zustandsänderungen | Zustandsänderungen werden protokolliert und in einer eigenen Ansicht angezeigt. | umgesetzt |
| FR-09 | Rollen- und Berechtigungskonzept | Eigentümer besitzen Verwaltungsrechte; Mitglieder können Geräte bedienen, aber keine administrativen Funktionen ausführen. | umgesetzt |
| FR-10 | Persistenz | Benutzer-, Raum-, Geräte- und Automatisierungsdaten werden dauerhaft in SQLite gespeichert. | umgesetzt |
| FR-11 | Energieübersicht | Die Anwendung zeigt geschätzte Energieverbräuche nach Gerät und Raum an. | umgesetzt |
| FR-12 | Zeitpläne | Eigentümer können wiederkehrende Zeitpläne für Geräte erstellen, bearbeiten und löschen. | umgesetzt |
| FR-13 | Automatisierungsregeln | Eigentümer können Regeln mit Auslösern und Aktionen definieren. | umgesetzt |
| FR-14 | Schwellenwertregeln | Sensorwerte können als Auslöser für Regeln verwendet werden. | umgesetzt |
| FR-15 | Zeitbasierte Regeln | Regeln können zu bestimmten Uhrzeiten ausgeführt werden. | umgesetzt |
| FR-16 | Konflikterkennung | Widersprüchliche Regeln und Zeitpläne werden erkannt und verhindert. | umgesetzt |
| FR-17 | Szenen-Funktion | Mehrere Gerätezustände können als Szene gespeichert, aktiviert und deaktiviert werden. | umgesetzt |
| FR-18 | Optionale IoT-Integrationsschicht | Eine optionale Integrationsschicht für externe IoT-Protokolle ist vorgesehen. | umgesetzt |
| FR-19 | Tages-Simulation im Zeitraffer | Ein Tagesablauf kann simuliert werden, um Regeln und Zeitpläne nachvollziehbar zu testen. | umgesetzt |
| FR-20 | Mitglieder einladen und Zugang widerrufen | Eigentümer können Mitglieder einladen und deren Zugriff wieder entziehen. | umgesetzt |
| FR-21 | Urlaubsmodus | Für Abwesenheiten können eigene Urlaubszeitpläne aktiviert werden. | umgesetzt |


# Überblick über die Applikation aus Benutzersicht

[Link zu Benutzerdokumentation](./docs/user-handbook.md)

Kurzüberblick:

- **Registrierung und Login:** Benutzer erstellen ein Konto und melden sich mit E-Mail-Adresse und Passwort an.
- **Dashboard:** Räume und Geräte werden übersichtlich dargestellt. Geräte können direkt gesteuert werden.
- **Räume und Geräte:** Eigentümer können Räume und Geräte anlegen, umbenennen und löschen.
- **Mitglieder:** Eigentümer können weitere Benutzer als Haushaltsmitglieder einladen oder entfernen.
- **Regeln:** Automatisierungen können anhand von Gerätezuständen, Sensorwerten oder Uhrzeiten ausgelöst werden.
- **Zeitpläne:** Geräte können zu bestimmten Uhrzeiten und Wochentagen automatisch gesteuert werden.
- **Szenen:** Mehrere Gerätezustände können gebündelt aktiviert und wieder zurückgesetzt werden.
- **Energie:** Die Anwendung zeigt geschätzte Verbrauchswerte pro Gerät und Raum.
- **Aktivitäten:** Zustandsänderungen werden nachvollziehbar protokolliert.
- **Simulation:** Ein Tag kann simuliert werden, um Auswirkungen von Zeitplänen und Regeln zu prüfen.
- **Urlaubsmodus:** Für Abwesenheiten können spezielle Zeitpläne aktiv geschaltet werden.



# Überblick über die Applikation aus Entwicklersicht

[Link zu Systemarchitektur-Dokumentation](./docs/system-architecture.md)

HomeE ist als Java-Desktop-Anwendung mit JavaFX umgesetzt. Die Anwendung ist grob in folgende Bereiche gegliedert:

- **UI/Controller:** JavaFX-Controller verwalten die Ansichten, Navigation und Benutzereingaben.
- **Domänenmodell:** Klassen wie `SmartHomeSystem`, `Room`, `Device`, `Rule`, `Schedule`, `Scene` und `User` bilden die zentrale Fachlogik ab.
- **Repository-Schicht:** Persistenz wird über Repository-Klassen gekapselt. Für produktive Nutzung wird SQLite verwendet; für Tests existieren In-Memory-Implementierungen.
- **Hilfsklassen:** Utility-Klassen übernehmen Querschnittsaufgaben wie Passwort-Hashing.
- **IoT-Integration:** Eine optionale Integrationsschicht erlaubt spätere Anbindung externer Geräteprotokolle.

Wichtige Designentscheidungen:

- **Zentrale Domänenlogik in `SmartHomeSystem`:** Dadurch bleiben Regeln, Zeitpläne, Szenen, Benutzerrechte und Persistenzaufrufe fachlich gebündelt.
- **Trennung von UI und Modell:** Controller rufen Fachmethoden auf, enthalten aber nicht die vollständige Geschäftslogik.
- **Repository-Pattern:** SQLite- und In-Memory-Persistenz können getrennt verwendet werden, was Tests erleichtert.
- **Rollenmodell:** Owner und Member werden getrennt behandelt. Kritische Verwaltungsfunktionen sind Ownern vorbehalten.
- **Konfliktprüfung:** Regeln und Zeitpläne werden vor dem Speichern auf widersprüchliche Ausführungen geprüft.

Build und Qualitätssicherung:

- **Build-Tool:** Maven
- **Sprache/Runtime:** Java 21
- **UI:** JavaFX 21
- **Tests:** JUnit 4
- **Coverage:** JaCoCo, Fokus auf Modellklassen
- **Statische Analyse:** PMD
- **Persistenz:** SQLite über `sqlite-jdbc`
- **Logging:** Log4j

Wichtige Testbereiche:

- Benutzerregistrierung, Login und Rollenrechte
- Raum- und Geräteverwaltung
- Gerätesteuerung und Aktivitätslog
- Regel- und Zeitplanlogik inklusive Konflikterkennung
- Szenen, Simulation und Urlaubsmodus
- Persistenz über Repository-Klassen



# JavaDoc für wichtige Klassen, Interfaces und Methoden

[Anleitung zum Erstellen der JavaDoc-Dokumentation](./docs/javadoc.md)
