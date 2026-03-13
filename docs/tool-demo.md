# Tool - Maven
Open Source Tool von Apache Software Foundation

## 1. Einleitung *(~1 min)*
Maven ist ein Build und Dependency Management-Tool für Java Projekte.
Es hilft Software automatisiert zu bauen, zu testen und zu paketieren.
### Was ist das Tool und wofür wird es eingesetzt?

Automatisiert Build-Prozesse (meist in Java verwendet)

Probleme ohne Maven:
Bibliotheken manuell herunterladen
Komplizierte Build-Prozesse
Unterschiedliche Projektstrukturen
Viele Fehlermöglichkeiten

Lösung durch Maven:
Automatisierte Builds
Dependency Management
Standardisierte Projektstruktur
Plugins für zusätzliche Funktionen

### Warum ist es für unser Projekt relevant?

In unserem Projekt:
In unserem Projekt werden wir Java verwenden und somit werden auch Java-Libraries interessant werden. Durch Maven wird dies automatisiert. Zusätzlich gibt es uns die Projektstruktur vor und organisiert dabei den gesamten Build-Prozess

## 2. Kernkonzepte *(~2 min)*

### Wichtigste Begriffe und Konzepte
pom.xml = Project Oject Model
Enthält Projektinformationen, Dependencies, Plugins und Build Konfigurationen.

Dependencies = externe Bibliotheken
Maven lädt Bibliotheken automatisch aus dem Internet --> falls nicht lokal vorhanden wird es dem Central Repository geladen.
Maven Central Repository = Repository aus dem Maven die benötigten Libraries herunterlädt

### Architektur / Funktionsweise auf hohem Niveau
Typische Projektstruktur:
project
 ├── src
 │   ├── main/java
 │   ├── main/resources
 │   └── test/java
 ├── pom.xml
 └── target

 Diese ist bei allen Maven-Projekten gleich und Entwickler können sich somit in neuen Projekten schneller einarbeiten

 Maven Lifecycle:
 Ein Build-Prozess besteht aus mehteren Phasen:
 compile
 test
 package (Anwendung wird als JAR/WAR gebaut)
 install



## 4. Live-Demo *(~4 min)*

### Typischer Workflow Schritt für Schritt
1. Entwickler erstellt Projekt
2. Dependencies werden in pom.xml definiert
3. Maven lädt Bibliotheken automatisch
4. Projekt wird gebaut
   (kann zusätzlich durch Plugins erweitert werden)

### Häufig verwendete Befehle / Funktionen
mvn compile --> compiliert den Code

mvn test --> führt Tests aus

mvn package --> erstellt ausführbares Artefakt

mvn install --> installiert das Projekt lokal

### Integration in das Projekt
In unserem Projekt werden wir Maven verwenden, um Java-Bibliotheken automatisch
zu verwalten und den Build-Prozess zu automatisieren. Dadurch müssen wir keine
Bibliothek manuell herunterladen und können das gesamte Projekt einfacher bauen
und testen.

## 5. Vor- und Nachteile *(~1 min)*

### Stärken des Tools
Standardisiert die Projektstruktur
Automatisierte Build-Prozesse
Automatisches Dependency-Management
Große Community und viele Plugins

### Bekannte Einschränkungen oder Alternativen
XML-Konfiguration kann kompkex sein

Gradle(flexibler, schneller, moderner)
Ant(älteres Build-Tool)

## 6. Zusammenfassung & Fragen *(~1 min)*

### Die drei wichtigsten Takeaways
1. Maven automatisiert Build Prozesse in Java
2. Es verwaltet Dependencies automatisch
3. standatisiert die Projektstruktur
### Weiterführende Ressourcen / Dokumentation
https://maven.apache.org
