# DuckyA

**SMS, chat, samtaler og lydklipp — alt i én Android-app.**

DuckyA er en gratis, åpen Android-app skrevet i Kotlin + Jetpack Compose. Den sender og mottar ekte SMS via SIM-kortet ditt, lar deg chatte og ringe (lyd/video) med andre DuckyA-brukere via Firebase + WebRTC, og lar deg ta opp små lydklipp og legge dem ved meldinger.

---

## Funksjoner

- 📱 **Ekte SMS** — send og motta SMS direkte fra/til SIM-kortet ditt (Android `SmsManager`)
- 💬 **DuckyA-chat** — sanntidsmeldinger mellom DuckyA-brukere via Firebase Firestore
- 📞 **Lyd- og videosamtaler** — peer-to-peer mellom DuckyA-brukere (WebRTC + Google STUN)
- ☎️ **Ring vanlige numre** — åpner standard-dialer for utgående PSTN-samtaler
- 🎙️ **Lydklipp som vedlegg** — hold-for-å-snakke, slipp for å sende (DuckyA-chat)
- 🔔 **Push-varsler** — innkommende anrop og meldinger via Firebase Cloud Messaging
- 🔐 **Telefonpålogging** — Firebase Phone Auth med SMS-OTP

---

## Tech-stack

| Lag | Teknologi |
|-----|-----------|
| UI | Kotlin + Jetpack Compose (Material 3) |
| Min SDK / Target | 24 (Android 7.0) / 34 (Android 14) |
| Backend | Firebase (Auth, Firestore, Storage, FCM) — gratis Spark-tier |
| Samtaler app-til-app | WebRTC (`io.getstream:stream-webrtc-android`) |
| Signaling | Firestore real-time listeners |
| SMS | `android.telephony.SmsManager` + `BroadcastReceiver` |
| Lydopptak | `MediaRecorder` (AAC / .m4a) |
| DI | Hilt |
| Navigation | Navigation Compose |
| Lokal cache | Room |

---

## Installasjon

### Forutsetninger
- **Android Studio** Hedgehog (2023.1.1) eller nyere
- **JDK 17** (følger med Android Studio)
- En **Google-konto** for Firebase
- En **fysisk Android-telefon med SIM-kort** (emulator kan IKKE sende ekte SMS)

### Steg 1 — Klone repoet
```bash
git clone https://github.com/HollmanRivero/DuckyA.git
cd DuckyA
```

### Steg 2 — Opprett Firebase-prosjekt
1. Gå til <https://console.firebase.google.com>
2. Klikk **Add project** → kall det `DuckyA`
3. Klikk **Add app** → **Android**
4. Pakkenavn: `no.duckya.app`
5. Hent SHA-1 fra Android Studio: `Gradle → app → Tasks → android → signingReport` (eller `./gradlew signingReport`) og lim inn
6. Last ned `google-services.json` og legg den i `app/`

### Steg 3 — Aktiver Firebase-tjenester
I Firebase Console:
- **Authentication** → Sign-in method → aktiver **Phone**
- **Firestore Database** → Create database → start in **test mode** (innstram regler før produksjon)
- **Storage** → Get started → test mode
- **Cloud Messaging** — aktiveres automatisk

### Steg 4 — Bygg og kjør
```bash
# Windows
gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

Eller åpne mappa i Android Studio → **Sync Gradle** → koble til telefon med USB-debugging → trykk **Run**.

---

## Tillatelser

| Tillatelse | Hvorfor |
|------------|---------|
| `SEND_SMS` / `RECEIVE_SMS` / `READ_SMS` | Sende, motta og lese SMS |
| `CALL_PHONE` | Starte oppringning til vanlige numre |
| `READ_PHONE_STATE` | Identifisere telefonen og samtalestatus |
| `READ_CONTACTS` | Vise kontaktnavn i samtalelista |
| `RECORD_AUDIO` | Ta opp lydklipp og bruke mikrofon i samtaler |
| `CAMERA` | Videosamtaler |
| `MODIFY_AUDIO_SETTINGS` | Bytte mellom høyttaler og øreproppen |
| `POST_NOTIFICATIONS` | Vise meldings- og samtalevarsler |
| `FOREGROUND_SERVICE` | Holde innkommende samtaler i live |

Alle runtime-tillatelser spørres om første gang du bruker den aktuelle funksjonen.

---

## Begrensninger

- **Ingen MMS:** Lyd-/bildevedlegg fungerer kun i DuckyA-chat, ikke via SMS (MMS-stack er for kompleks)
- **Kan ikke ta opp PSTN-samtaler:** Android tillater ikke at vanlige apper tar opp telefonsamtaler
- **Krever fysisk SIM** for SMS-funksjoner — emulator støtter kun simulerte SMS-er innad
- **Firebase Spark-tier:** Gratis, men har kvoter (50k Firestore-leselinjer/dag, 5 GB Storage)

---

## Prosjektstruktur

```
DuckyA/
├── app/
│   ├── src/main/
│   │   ├── java/no/duckya/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── DuckyApp.kt
│   │   │   ├── ui/         (Compose-skjermer + komponenter)
│   │   │   ├── data/       (repositories: auth, sms, chat, call, audio)
│   │   │   ├── webrtc/     (PeerConnection + signaling)
│   │   │   ├── service/    (CallService, FCM)
│   │   │   └── di/         (Hilt-moduler)
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   ├── google-services.json   ← du legger denne her
│   └── build.gradle.kts
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── LICENSE
```

---

## Lisens

MIT License — se [LICENSE](LICENSE).

## Kontakt

**Hollman Rivero** — Salazar Rivero Smart Things
hollman.rivero@bygg-salazar.no
