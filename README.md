# Cubeside Voter

Android-App, die nacheinander alle Vote-Seiten für cubeside.de öffnet,
deinen Minecraft-Namen einträgt und nach dem Vote automatisch zur nächsten Seite springt.

## APK bauen (GitHub Actions)
1. Neues Repo auf GitHub anlegen, z. B. `CubesideVoter`.
2. Alle Dateien aus der ZIP hochladen – die Ordnerstruktur muss gleich bleiben
   (`.github/workflows/build.yml`, `src/main/...`, `build.gradle`, `settings.gradle`,
   `gradle.properties`, `debug.keystore`).
3. Reiter **Actions** → „APK bauen“ läuft automatisch (oder „Run workflow“).
4. Nach ca. 3–5 Minuten unten bei **Artifacts** `CubesideVoter-APK` laden, entpacken, installieren.

## Bedienung
- Namen eintragen → **Jetzt voten**.
- Oben siehst du, auf welcher Seite du bist. Wenn ein Captcha erscheint, tippst du es an –
  danach klickt die App selbst auf „Voten“ und geht weiter.
- **Weiter ⏭** überspringt eine Seite.
- mc-liste.de hat eine kleine Rechenaufgabe: die tippst du selbst ein.
- Am Ende öffnet sich deine Vote-Statistik auf stats.cubeside.de.
- Vote-Seite stimmt nicht mehr? In der Liste lange drücken → Adresse ändern.

## Seiten anpassen
Alle Seiten mit Adresse, Erfolgstexten und Feldern stehen in `src/main/assets/sites.json`.
