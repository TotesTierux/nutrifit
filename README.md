# 🥑 NutriFit

Android-App für **Ernährung, Training, Intervallfasten und Gewohnheiten** – alles lokal auf dem Handy, ohne Konto.

## Funktionen
- **Planer:** Wochenplan, Einkaufsliste (nach Kategorien) und Rezepte mit Vorschlägen – auf Wunsch mit der Familie geteilt
- **Konto & Familie:** Login per E-Mail, Synchronisierung über Firebase, Familie per Einladungscode
- **Essen erfassen:** KI-Fotoerkennung (Google Gemini, eigener API-Schlüssel), Barcode-Scanner, Online-Suche (Open Food Facts), eingebaute Lebensmittelliste
- **Training:** Trainingspläne, Sätze, Pausentimer, Rekorde, Level & Abzeichen – verbrannte Kalorien fließen ins Tagesbudget
- **Intervallfasten:** Timer mit schlauen Benachrichtigungen
- **Gewohnheiten:** Start/Stopp per Knopf, Kachel in den Schnelleinstellungen, Widget oder Benachrichtigung – mit Auswertung
- **Automatische Updates** aus diesem Repository

## Installation
Neueste APK herunterladen: [NutriFit.apk](https://github.com/TotesTierux/nutrifit/raw/main/apk/NutriFit.apk) – öffnen und installieren. Spätere Updates meldet die App selbst.

## Ziffernblätter (Xiaomi Watch S5)
Die Bilder in [`watchfaces/`](watchfaces) aufs Handy laden und in **Mi Fitness → Gerät → Zifferblätter → Foto-Zifferblatt** als Hintergrund wählen.

## Neue Version veröffentlichen
1. `versionCode`/`versionName` in `android/AndroidManifest.xml` erhöhen und APK bauen (`android/build.sh`)
2. APK als `apk/NutriFit-X.Y.apk` und `apk/NutriFit.apk` ablegen
3. `update.json` anpassen – die App findet das Update automatisch

## Firebase (Sync & Familie)
Eigenes Firebase-Projekt: Authentication → E-Mail/Passwort aktivieren, Firestore-Datenbank anlegen und die Regeln aus [`firebase/firestore.rules`](firebase/firestore.rules) eintragen. Die `firebaseConfig` einmalig in der App unter ⚙️ → Konto → Einrichten einfügen.
