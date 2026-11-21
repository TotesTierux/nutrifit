# 🥑 NutriFit

Android-App für **Ernährung, Training, Intervallfasten und Gewohnheiten** – alles lokal auf dem Handy, ohne Konto.

## Funktionen
- **Essen erfassen:** KI-Fotoerkennung (Google Gemini, eigener API-Schlüssel), Barcode-Scanner, Online-Suche (Open Food Facts), eingebaute Lebensmittelliste
- **Training:** Trainingspläne, Sätze, Pausentimer, Rekorde, Level & Abzeichen – verbrannte Kalorien fließen ins Tagesbudget
- **Intervallfasten:** Timer mit schlauen Benachrichtigungen
- **Gewohnheiten:** Start/Stopp per Knopf, Kachel in den Schnelleinstellungen, Widget oder Benachrichtigung – mit Auswertung
- **Automatische Updates** aus diesem Repository

## Installation
Neueste APK unter [Releases](https://github.com/TotesTierux/nutrifit/releases/latest) herunterladen und öffnen. Spätere Updates meldet die App selbst.

## Ziffernblätter (Xiaomi Watch S5)
Die Bilder in [`watchfaces/`](watchfaces) aufs Handy laden und in **Mi Fitness → Gerät → Zifferblätter → Foto-Zifferblatt** als Hintergrund wählen.

## Neue Version veröffentlichen
1. `versionCode`/`versionName` in `android/AndroidManifest.xml` erhöhen und APK bauen (`android/build.sh`)
2. Release `vX.Y` mit der Datei `NutriFit.apk` anlegen
3. `update.json` anpassen – die App findet das Update automatisch
