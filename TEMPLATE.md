# PRSE Project Template (SmartHome)

Dieses Repository enthält die SmartHome-Orchestrator-Anwendung des PRSE-Praktikums.

## Voraussetzungen

- Java 21 (JDK)
- Maven 3.9+

## Projektstruktur

```
src/
├── main/
│   ├── java/at/jku/se/smarthome/         # Applikationscode
│   └── resources/at/jku/se/smarthome/    # FXML und Ressourcen
└── test/
    └── java/at/jku/se/smarthome/         # Unit-Tests
```

## Übliche Befehle

| Command | Beschreibung |
|---|---|
| `mvn compile` | Kompilieren |
| `mvn test` | Tests ausführen + JaCoCo Report erzeugen |
| `mvn package -DskipTests` | JAR bauen |
| `mvn pmd:pmd pmd:cpd` | Statische Analyse |

## CI

Der Workflow unter `.github/workflows/Continuous Integration.yaml` führt Compile, Tests, PMD und Packaging für `main` und Pull Requests aus.
