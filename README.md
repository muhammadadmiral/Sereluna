# 🌙 Sereluna - Your Mental Health Companion

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-33.1.0-orange.svg)](https://firebase.google.com/)
[![Material Design](https://img.shields.io/badge/Material--Design-3.0-blue.svg)](https://material.io/)

**Sereluna** adalah aplikasi pendamping kesehatan mental yang dirancang untuk membantu Anda memahami, melacak, dan meningkatkan kesejahteraan emosional Anda. Dengan perpaduan teknologi AI dan metodologi psikologi yang teruji, Sereluna hadir sebagai teman yang selalu ada di genggaman Anda.

---

## ✨ Fitur Utama

### 🤖 Diary Chatbot (AI)
Berceritalah tanpa ragu. AI Chatbot kami siap mendengarkan keluh kesah Anda kapan saja dan membantu Anda menuangkan perasaan ke dalam catatan diary digital yang terorganisir.

### 📊 Skrining Kesehatan Mental (DASS-21)
Pantau kondisi kesehatan mental Anda secara berkala melalui tes **DASS-21** (Depression, Anxiety, and Stress Scale) yang tervalidasi secara klinis.

### 😴 Sleep Tracking
Kualitas tidur sangat berpengaruh pada mood. Lacak pola tidur harian Anda untuk melihat korelasinya dengan kesehatan mental Anda.

### 📖 Artikel Edukasi
Perluas wawasan Anda mengenai kesehatan mental melalui berbagai artikel informatif yang kurasi untuk membantu Anda menghadapi tantangan sehari-hari.

### 🗓️ Kalender & Riwayat
Lihat perkembangan diri Anda dari waktu ke waktu melalui kalender interaktif yang mencatat mood, diary, dan hasil skrining Anda.

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Material Design Components](https://material.io/components) & View Binding
- **Networking:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Database & Cloud:** [Firebase](https://firebase.google.com/) (Auth, Firestore, Cloud Messaging, Functions)
- **Dependency Injection:** Manual/Repository Pattern
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/) & [Picasso](https://square.github.io/picasso/)
- **Architecture:** MVVM (Model-View-ViewModel)

---

## 📸 Screenshots

| Onboarding | Login | Dashboard | Chatbot |
|---|---|---|---|
| <img src="https://via.placeholder.com/200x400?text=Onboarding" width="200"> | <img src="https://via.placeholder.com/200x400?text=Login" width="200"> | <img src="https://via.placeholder.com/200x400?text=Dashboard" width="200"> | <img src="https://via.placeholder.com/200x400?text=Chatbot" width="200"> |

---

## 🚀 Memulai (Getting Started)

### Prasyarat
- Android Studio Iguana atau versi yang lebih baru.
- JDK 17 atau yang lebih baru.
- Perangkat Android dengan minimal API 24 (Nougat).

### Instalasi
1. Clone repositori ini:
   ```bash
   git clone https://github.com/username/sereluna.git
   ```
2. Buka project di Android Studio.
3. Tambahkan file `google-services.json` (dapatkan dari console Firebase) ke dalam folder `app/`.
4. Tambahkan API Key dan URL Backend di `local.properties`:
   ```properties
   GUARDIAN_API_KEY=your_api_key_here
   SERELUNA_BASE_URL=https://your-api-url.com/
   ```
5. Sync project dengan file Gradle.
6. Jalankan aplikasi di Emulator atau Perangkat Fisik.

---

## 📂 Struktur Folder
```text
app/src/main/java/com/android/capstone/sereluna/
├── data/           # Data Layer (API, Models, Repositories)
├── service/        # Services (Firebase Messaging, etc.)
├── ui/             # UI Layer (Activities, Fragments, ViewModels)
└── util/           # Utility Classes
```

---

## 🤝 Kontribusi
Kami sangat terbuka bagi siapa pun yang ingin berkontribusi untuk mengembangkan Sereluna lebih jauh. Silakan fork repositori ini dan kirimkan Pull Request.

---

## 📄 Lisensi
Project ini berada di bawah lisensi MIT - lihat file [LICENSE](LICENSE) untuk detail lebih lanjut.

---

<p align="center">
  Dibuat dengan ❤️ oleh <b>Tim Sereluna</b>
</p>
