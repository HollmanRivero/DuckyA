---
layout: default
title: DuckyA Privacy Policy
permalink: /privacy.html
---

# DuckyA — Privacy Policy

**Last updated:** 2026-05-26
**Effective date:** 2026-05-26
**App package:** `no.duckya.app`
**Developer:** Hollman Rivero (Bygg Salazar)
**Contact:** hollman.rivero@bygg-salazar.no

---

## 1. Summary (plain language)

DuckyA is a Norwegian-built Android messaging app that combines real SMS,
in-app chat, and voice/video calls. To do that, it needs access to your
phone number, SMS messages, contacts, microphone, and camera.

**Three things we want you to know up front:**

- **We do not sell your data.** Ever. No advertising networks. No data brokers.
- **We do not read your SMS messages on our servers.** SMS stays on your phone.
- **You can delete your account and all server-side data at any time** from
  inside the app: **Settings → Slett konto**.

---

## 2. Data we collect

### 2.1 Data you provide

| Data | Purpose | Stored where | Retention |
|---|---|---|---|
| Phone number (E.164 format) | Account identity, sign-in via SMS code | Firebase Authentication + your Firestore user document | Until you delete your account |
| Google account email & display name (only if you sign in with Google) | Account identity | Firebase Authentication | Until you delete your account |
| Chat messages you send through DuckyA-to-DuckyA chat | Deliver the message to the recipient | Firebase Firestore (Google Cloud, EU region) | Until you or the recipient delete the conversation |
| Audio clip attachments you record and send | Deliver the audio to the recipient | Firebase Storage (Google Cloud, EU region) | Until the conversation is deleted |

### 2.2 Data generated automatically

| Data | Purpose | Stored where |
|---|---|---|
| Firebase user ID (UID) | Internal account identifier | Firebase |
| Firebase Cloud Messaging (FCM) push token | Deliver incoming-message and incoming-call notifications | Your Firestore user document |
| Last-seen timestamp | Show whether you're online | Firestore |
| Crash reports & basic analytics events | Diagnose bugs (Firebase Analytics, anonymous, aggregated) | Firebase Analytics |

### 2.3 Data we DO NOT collect

- Your SMS message contents (these stay on your phone — DuckyA reads them locally on the device only to show your conversation list and never uploads them anywhere).
- Your contact list (read locally only, never uploaded).
- Your call recordings (voice/video calls are peer-to-peer via WebRTC; we see only the signaling metadata needed to connect the call, not its audio or video).
- Your precise location.
- Your device's photos, videos, or other files outside what you explicitly attach to a DuckyA message.

---

## 3. Permissions DuckyA requests and why

Android requires us to ask for each of these. You can deny any of them — the relevant feature will be unavailable but the rest of the app keeps working.

### 3.1 Sensitive permissions (Google Play restricted)

| Permission | Why we ask | What we do with it | What we do NOT do |
|---|---|---|---|
| `READ_SMS` | Show your existing SMS conversation list inside DuckyA's unified inbox | Read SMS thread metadata (sender, snippet, timestamp) **on-device only**; render a list | Never upload SMS content to any server; never share with third parties |
| `RECEIVE_SMS` | Detect incoming SMS in real time so the inbox updates without re-opening the app | Trigger a UI refresh **on-device only** | Never forward incoming SMS |
| `SEND_SMS` | Let you send SMS from inside DuckyA via Android's built-in `SmsManager` | Hand the message to your carrier; the SMS is sent **directly from your phone** | Never relay through our servers |
| `READ_CONTACTS` | Show contact names next to phone numbers in the inbox | Resolve phone numbers to names **on-device only** | Never upload your address book |
| `READ_PHONE_STATE`, `READ_PHONE_NUMBERS` | Detect your phone's own number to pre-fill sign-in; pause calls during a regular phone call | Read identifiers **on-device only** | Never transmit |
| `CALL_PHONE` | Let you dial a contact directly from a conversation | Use Android's dialer | Never auto-dial |
| `RECORD_AUDIO`, `CAMERA` | Voice and video calls; voice-clip attachments | Stream peer-to-peer via WebRTC during an active call; record audio clips you choose to attach | No background recording; no upload of raw camera/microphone feed |

### 3.2 Standard permissions

- `INTERNET`, `ACCESS_NETWORK_STATE`: connect to Firebase + WebRTC peers.
- `POST_NOTIFICATIONS`: show incoming-message and incoming-call alerts.
- `FOREGROUND_SERVICE` (+ `_PHONE_CALL`, `_MICROPHONE`, `_CAMERA`): keep voice/video calls running when the screen turns off.
- `WAKE_LOCK`, `VIBRATE`, `USE_FULL_SCREEN_INTENT`: standard call-app behavior.
- `BLUETOOTH`, `BLUETOOTH_CONNECT`, `MODIFY_AUDIO_SETTINGS`: route call audio to a Bluetooth headset if connected.

---

## 4. Third-party services

DuckyA uses Google Firebase (operated by Google LLC) for backend services.
Firebase has its own privacy practices: [firebase.google.com/support/privacy](https://firebase.google.com/support/privacy)

Specifically:

- **Firebase Authentication** — phone-number and Google sign-in
- **Cloud Firestore** — chat messages, user profiles, signaling
- **Firebase Storage** — audio attachments
- **Firebase Cloud Messaging (FCM)** — push notifications
- **Firebase Analytics** — anonymous app-usage stats and crash diagnostics
- **Google Sign-In (via Credential Manager)** — optional alternative sign-in

Data is stored in Google Cloud's **europe-west1** region (Belgium) when
possible. Some Firebase services may transit through US infrastructure for
delivery; Google's Standard Contractual Clauses govern this transfer.

We do **not** use advertising SDKs, tracking pixels, or analytics tools other
than Firebase Analytics.

---

## 5. Your rights (GDPR, applicable across the EU/EEA and UK)

You have the right to:

- **Access** the data we hold about you (request a copy)
- **Correct** any inaccurate data
- **Delete** your account and all server-side data — use **Settings → Slett konto** in the app. This permanently removes your Firestore user document and your Firebase Authentication record.
- **Restrict** or **object to** processing
- **Data portability** — request your data in a machine-readable format
- **Withdraw consent** at any time by uninstalling the app and deleting your account

To exercise any right not covered by the in-app delete button, email
**hollman.rivero@bygg-salazar.no** and we will respond within 30 days.

You also have the right to lodge a complaint with the Norwegian Data
Protection Authority (**Datatilsynet**): [datatilsynet.no](https://www.datatilsynet.no)

---

## 6. Data retention

| Data | Kept for |
|---|---|
| Your Firebase Authentication record | Until you delete your account |
| Your Firestore user document (phone, FCM token, last-seen) | Until you delete your account |
| Chat messages | Until you or the recipient delete the conversation |
| Audio attachments in Firebase Storage | Until the parent conversation is deleted |
| Anonymous Firebase Analytics events | Up to 14 months (Firebase default), then aggregated |
| Server logs | 30 days |

When you delete your account in **Settings → Slett konto**, your Firestore
document is deleted immediately and your Firebase Authentication record is
deleted within seconds.

---

## 7. Security

- All traffic between the app and Firebase uses TLS 1.2+.
- WebRTC media streams between callers are encrypted end-to-end using DTLS-SRTP. We (the developer) cannot see or hear your calls.
- Firebase Storage objects are protected by Firestore security rules tied to your Firebase Authentication UID.
- We do not have any "master password" or backdoor that lets us read your account.

If we ever experience a data breach affecting your data, we will notify you
through the app and via email (where available) within 72 hours, as required
by GDPR.

---

## 8. Children

DuckyA is not directed at children under 13 (or 16 in some EU/EEA countries).
We do not knowingly collect data from children. If you believe a child has
created an account, email us and we will delete it.

---

## 9. Changes to this policy

We may update this policy. Material changes will be announced in-app and the
"Last updated" date at the top will change. Continued use after a change
constitutes acceptance.

---

## 10. Contact

**Developer:** Hollman Rivero
**Company:** Bygg Salazar
**Email:** hollman.rivero@bygg-salazar.no
**WhatsApp:** [wa.me/48904267](https://wa.me/48904267)
**App:** DuckyA — `no.duckya.app`

For privacy-specific questions, please put "DuckyA privacy" in the subject line.
