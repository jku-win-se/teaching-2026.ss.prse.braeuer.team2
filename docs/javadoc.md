# Javadoc erstellen

Dieses Projekt verwendet Apache Maven. Die Javadoc-Dokumentation kann mit Maven aus dem Java-Quellcode erzeugt werden.

## Voraussetzungen

Java und Maven muessen installiert sein. Fuer dieses Projekt sollte Java 21 verwendet werden.

Installation pruefen:

```bash
java -version
mvn -version
```

## Maven auf macOS installieren

Der einfachste Weg ist die Installation ueber Homebrew.

Homebrew installieren, falls es noch nicht vorhanden ist:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Nach der Installation die von Homebrew angezeigten `Next steps` ausfuehren, zum Beispiel:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv zsh)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv zsh)"
```

Maven installieren:

```bash
brew install maven
```

Installation pruefen:

```bash
mvn -version
```

## Maven auf Windows installieren

Maven kann auf Windows zum Beispiel mit `winget` installiert werden:

```powershell
winget install Apache.Maven
```

Danach ein neues Terminal oeffnen und die Installation pruefen:

```powershell
mvn -version
```

Alternativ kann Maven manuell installiert werden:

1. Apache Maven von <https://maven.apache.org/download.cgi> herunterladen.
2. Die Binary-ZIP-Datei entpacken, zum Beispiel nach `C:\Program Files\Apache\Maven`.
3. Die Umgebungsvariable `MAVEN_HOME` auf den entpackten Maven-Ordner setzen.
4. `%MAVEN_HOME%\bin` zur `Path`-Variable hinzufuegen.
5. Ein neues Terminal oeffnen und `mvn -version` ausfuehren.

## Javadoc generieren

Im Projektordner ausfuehren:

```bash
mvn javadoc:javadoc
```

Nach erfolgreichem Build liegt die generierte Dokumentation unter:

```text
target/reports/apidocs/index.html
```

Auf macOS oeffnen:

```bash
open target/reports/apidocs/index.html
```

Auf Windows mit PowerShell oeffnen:

```powershell
Start-Process target/reports/apidocs/index.html
```

## Hinweis

Die generierte Javadoc-Dokumentation ist Build-Output und muss normalerweise nicht ins Repository committed werden. Sie kann jederzeit mit `mvn javadoc:javadoc` neu erzeugt werden.
