# Benutzerdokumentation

## Zielgruppe

HomeE richtet sich an Personen, die ein Smarthome in einer Desktop-Anwendung verwalten oder simulieren möchten. Die Anwendung eignet sich besonders für Haushalte, in denen Räume, Geräte, automatische Regeln, Zeitpläne und Szenen übersichtlich gesteuert werden sollen.

Es gibt zwei Benutzerrollen:

- **Owner/Eigentümer:** besitzt volle Rechte und kann Räume, Geräte, Regeln, Zeitpläne, Szenen, Urlaubsmodus und Mitglieder verwalten.
- **Member/Mitglied:** kann den Haushalt verwenden und Geräte steuern, hat aber keinen Zugriff auf administrative Verwaltungsfunktionen wie Regeln oder Mitgliederverwaltung.

## Installation und Start

1. Java 21 und Maven installieren.
2. Projekt im Terminal öffnen.
3. Mit `mvn clean package` bauen.
4. Anwendung starten.

## Erste Schritte

1. Anwendung starten.
2. Im Startbildschirm auf Registrierung wechseln.
3. E-Mail-Adresse und Passwort eingeben.
4. Konto erstellen.
5. Danach anmelden.
6. Im Dashboard einen ersten Raum anlegen.
7. Ein Gerät zu diesem Raum hinzufügen.
8. Gerät über den angezeigten Button oder Regler steuern.

Nach dem ersten Start wird eine lokale SQLite-Datenbank verwendet, damit Benutzer, Räume, Geräte und Automatisierungen gespeichert bleiben.

## Überblick über die Oberfläche

### Login und Registrierung

Beim Start wird zunächst die Authentifizierungsansicht geöffnet. Dort kann ein Benutzer entweder ein neues Konto erstellen oder sich mit bestehenden Zugangsdaten anmelden.

### Dashboard

Das Dashboard ist die zentrale Ansicht von HomeE. Es zeigt Räume, darin enthaltene Geräte und aktuelle Gerätezustände. Über das Dashboard können Eigentümer neue Räume und Geräte erstellen. Geräte können direkt geschaltet oder über Regler verändert werden.

Unterstützte Gerätetypen:

- **Switch:** Ein/Aus-Schalter
- **Dimmer:** Prozentwert von 0 bis 100
- **Thermostat:** Temperaturwert
- **Sensor:** frei setzbarer Messwert
- **Jalousie:** offen/geschlossen beziehungsweise Positionswert

### Aktivitätslog

Das Aktivitätslog zeigt Zustandsänderungen im System. Dadurch kann nachvollzogen werden, ob ein Gerät manuell, durch eine Regel, durch einen Zeitplan oder durch eine Szene verändert wurde.

### Energieübersicht

Die Energieansicht zeigt geschätzte Verbrauchswerte. Die Daten werden aus den vorhandenen Geräten und Aktivitätsinformationen berechnet und helfen dabei, den Verbrauch einzelner Geräte oder Räume besser einzuschätzen.

### Regeln

In der Regelansicht können Eigentümer Automatisierungen erstellen. Eine Regel besteht aus einem Auslöser und einer Aktion.

Mögliche Auslöser:

- Gerätezustand
- Sensor-Schwellwert
- Uhrzeit

Mögliche Aktionen:

- Gerät umschalten
- Gerät einschalten oder ausschalten
- Zielwert setzen, zum Beispiel Dimmerwert oder Temperatur

HomeE prüft beim Erstellen und Bearbeiten, ob Regeln mit bestehenden Regeln oder Zeitplänen kollidieren.

### Zeitpläne

Zeitpläne führen Aktionen zu bestimmten Uhrzeiten und an ausgewählten Wochentagen aus. Sie eignen sich zum Beispiel, um Lampen morgens automatisch einzuschalten oder die Heizung abends zu reduzieren.

### Szenen

Szenen speichern mehrere Gerätezustände unter einem Namen. Eine Szene kann aktiviert werden, um alle hinterlegten Zustände gleichzeitig anzuwenden. Beim Deaktivieren wird versucht, die vorherigen Gerätezustände wiederherzustellen.

Beispiele:

- „Filmabend“: Licht dimmen, Jalousie schließen, Temperatur anpassen.
- „Guten Morgen“: Jalousie öffnen, Licht einschalten, Thermostat erhöhen.

### Simulation

Die Tages-Simulation zeigt, welche Zustandsänderungen bei einem simulierten Tagesablauf auftreten würden. Das ist hilfreich, um Regeln und Zeitpläne vorab zu prüfen.

### Urlaubsmodus

Im Urlaubsmodus können spezielle Urlaubszeitpläne aktiviert werden. Diese Zeitpläne gelten während eines definierten Abwesenheitszeitraums und können normale Zeitpläne übersteuern.

### Mitgliederverwaltung

Eigentümer können weitere Personen per E-Mail als Mitglieder einladen. Eingeladene Mitglieder können nach ihrer Registrierung auf den Haushalt zugreifen. Der Zugriff kann vom Eigentümer wieder widerrufen werden.

## Nutzungsszenarien

### Szenario 1: Neuen Raum erstellen

1. Als Eigentümer anmelden.
2. Im Dashboard auf **Create Room** klicken.
3. Namen des Raums eingeben, zum Beispiel `Wohnzimmer`.
4. Bestätigen.
5. Der Raum erscheint im Dashboard.

### Szenario 2: Neues Gerät hinzufügen

1. Im Dashboard auf **Create Device** oder im gewünschten Raum auf **+ Device** klicken.
2. Raum auswählen, falls mehrere Räume vorhanden sind.
3. Gerätenamen eingeben, zum Beispiel `Stehlampe`.
4. Gerätetyp auswählen, zum Beispiel `Switch` oder `Dimmer`.
5. Bestätigen.
6. Das Gerät erscheint im ausgewählten Raum.

### Szenario 3: Gerät manuell steuern

1. Im Dashboard den Raum mit dem Gerät öffnen beziehungsweise suchen.
2. Beim gewünschten Gerät den Button oder Regler verwenden.
3. HomeE aktualisiert den Gerätestatus.
4. Die Änderung wird im Aktivitätslog gespeichert.

### Szenario 4: Regel erstellen

1. Als Eigentümer anmelden.
2. In die Ansicht **Rules** wechseln.
3. Neue Regel erstellen.
4. Namen vergeben, zum Beispiel `Licht bei Bewegung`.
5. Auslöser auswählen, zum Beispiel Sensorwert oder Uhrzeit.
6. Zielgerät und Aktion auswählen.
7. Speichern.
8. Falls ein Konflikt besteht, zeigt HomeE eine Fehlermeldung und speichert die Regel nicht.

### Szenario 5: Zeitplan erstellen

1. In die Ansicht **Schedules** wechseln.
2. Neuen Zeitplan anlegen.
3. Gerät auswählen.
4. Aktion festlegen, zum Beispiel Einschalten oder Zielwert setzen.
5. Uhrzeit und Wochentage auswählen.
6. Speichern.
7. Der Zeitplan wird automatisch ausgeführt, sobald er fällig ist.

### Szenario 6: Szene erstellen und aktivieren

1. In die Ansicht **Scenes** wechseln.
2. Neue Szene erstellen.
3. Namen vergeben, zum Beispiel `Filmabend`.
4. Geräte und Zielzustände auswählen.
5. Szene speichern.
6. Szene aktivieren.
7. HomeE setzt alle gespeicherten Gerätezustände.
8. Szene bei Bedarf wieder deaktivieren.

### Szenario 7: Urlaubsmodus verwenden

1. Zuerst einen oder mehrere Urlaubszeitpläne erstellen.
2. In die Ansicht **Vacation Mode** wechseln.
3. Start- und Endzeitpunkt der Abwesenheit definieren.
4. Gewünschte Urlaubszeitpläne auswählen.
5. Urlaubsmodus aktivieren.
6. Während des Zeitraums führt HomeE die ausgewählten Urlaubszeitpläne aus.

### Szenario 8: Mitglied einladen

1. Als Eigentümer anmelden.
2. Im Dashboard die Mitgliederverwaltung öffnen.
3. **Invite member** auswählen.
4. E-Mail-Adresse des Mitglieds eingeben.
5. Bestätigen.
6. Das Mitglied kann nach Registrierung mit derselben E-Mail-Adresse auf den Haushalt zugreifen.

## Bekannte Einschränkungen

- HomeE ist eine Desktop-Anwendung und keine mobile App.
- Die Anwendung simuliert Smarthome-Geräte primär innerhalb der Software. Eine reale Geräteintegration ist nur über die optionale IoT-Schicht vorgesehen.
- Die Energieübersicht basiert auf geschätzten beziehungsweise modellierten Verbrauchsdaten und ersetzt keine reale Strommessung.
- Zeitpläne werden in der laufenden Anwendung regelmäßig geprüft. Die Anwendung muss dafür gestartet sein.
- Die Mitgliedereinladung erfolgt über E-Mail-Adressen innerhalb der Anwendung, aber ohne echten E-Mail-Versand.
- Die Daten werden lokal gespeichert. Eine Cloud-Synchronisation ist nicht Bestandteil der Anwendung.

## Fehlerbehebung

### Anwendung startet nicht

Prüfen, ob Java 21 installiert ist:

```bash
java -version
```

Prüfen, ob Maven installiert ist:

```bash
mvn -version
```

Danach erneut starten:

```bash
mvn javafx:run
```

### Anmeldung schlägt fehl

- E-Mail-Adresse auf Tippfehler prüfen.
- Passwort erneut eingeben.
- Falls noch kein Konto existiert, zuerst registrieren.

### Gerät kann nicht erstellt werden

- Zuerst muss mindestens ein Raum vorhanden sein.
- Nur Eigentümer können Geräte erstellen.

### Regel oder Zeitplan kann nicht gespeichert werden

- Prüfen, ob Zielgerät und Aktion gültig sind.
- Prüfen, ob bereits eine widersprüchliche Regel oder ein widersprüchlicher Zeitplan existiert.
- Bei Konflikten bestehende Automatisierung bearbeiten oder entfernen.

### Mitglied hat keinen Zugriff

- Prüfen, ob die Einladung mit exakt derselben E-Mail-Adresse erfolgt ist, mit der sich das Mitglied registriert.
- Prüfen, ob der Zugriff nicht widerrufen wurde.

## FAQ

### Muss ich echte Smarthome-Geräte besitzen?

Nein. HomeE kann auch ohne reale Geräte verwendet werden, da Geräte in der Anwendung angelegt und simuliert werden.

### Wer darf Regeln und Zeitpläne erstellen?

Nur Eigentümer. Mitglieder dürfen Geräte bedienen, aber keine administrativen Automatisierungen verwalten.

### Bleiben meine Daten nach dem Schließen erhalten?

Ja, die Anwendung verwendet eine lokale SQLite-Datenbank.

### Warum wird eine Regel nicht ausgeführt?

Mögliche Gründe sind ein nicht erfüllter Auslöser, ein deaktivierter beziehungsweise nicht passender Zeitplan oder ein Konflikt, der bereits beim Speichern verhindert wurde.

### Kann ich den Urlaubsmodus automatisch beenden lassen?

Ja. Beim Aktivieren wird ein Zeitraum definiert. Nach Ablauf ist der Urlaubsmodus nicht mehr aktiv.
