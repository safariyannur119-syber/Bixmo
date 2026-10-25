# BIXMO Android — starter project

Starter aplikasi komunitas MAX Owners Banjarmasin. Identitas visual: hitam, kuning neon, aksen merah dapat ditambahkan pada iterasi desain berikutnya.

## Status saat ini
- UI Android Kotlin + Jetpack Compose.
- Firebase Email/Password login dan pendaftaran.
- Profil anggota dibuat dengan status `pending` dan role `member`.
- Halaman menunggu persetujuan dan pemeriksaan status.
- Kerangka halaman Beranda, Agenda, Member, dan Info.
- Firestore security rules awal di `firebase/firestore.rules`.

**Belum produksi:** halaman agenda/info saat ini sebagian masih contoh statis. Panel admin, CRUD agenda/pengumuman, absensi yang terhubung database, notifikasi, reset kata sandi, dan pengujian keamanan masih perlu dikerjakan. Tidak disertakan `google-services.json`, karena file tersebut harus berasal dari proyek Firebase milik pengelola BIXMO.

## Yang diperlukan
- Android Studio versi stabil terbaru.
- JDK 17.
- Akun Google untuk membuat proyek Firebase.

## Setup Firebase
1. Buka Firebase Console: https://console.firebase.google.com/ dan buat proyek bernama `BIXMO`.
2. Tambahkan aplikasi Android dengan package name persis `com.bixmo.app`.
3. Unduh `google-services.json` dan letakkan di folder `app/` (sejajar dengan `build.gradle.kts`). Jangan bagikan file konfigurasi atau kredensial admin secara publik.
4. Firebase Console → Authentication → Sign-in method → aktifkan **Email/Password**.
5. Firebase Console → Firestore Database → buat database.
6. Buka Rules dan salin isi `firebase/firestore.rules`, lalu Publish. Uji rules sebelum dipakai sungguhan.
7. Buka folder proyek ini di Android Studio, tunggu Gradle Sync, lalu jalankan ke emulator atau HP Android dengan USB debugging.

## Menyetujui anggota pertama / menjadikan pengurus
Untuk keamanan, pendaftaran dari aplikasi tidak bisa memilih role admin atau status approved. Setelah membuat akun pertama, gunakan Firebase Console (Firestore → `users` → dokumen dengan ID UID akun tersebut) untuk mengubah:
- `status`: `approved`
- `role`: `admin`

Lakukan ini hanya untuk akun pengurus yang benar-benar dipercaya. Setelah itu admin pertama dapat dijadikan dasar pembangunan panel persetujuan anggota. Jangan membuat tombol admin publik.

## Skema Firestore awal
`users/{uid}`:
```json
{
  "uid": "UID_AUTH",
  "name": "Nama anggota",
  "email": "anggota@example.com",
  "bike": "NMAX",
  "role": "member",
  "status": "pending",
  "createdAt": "server timestamp"
}
```
`events/{eventId}` dan `announcements/{announcementId}` akan digunakan pada tahap berikutnya. Aturan database membatasi penulisan data acara/pengumuman kepada admin.

## Catatan penting
- Rules adalah lapisan keamanan yang penting, tetapi wajib ditinjau dan diuji sebelum produksi.
- Data sensitif anggota sebaiknya dibatasi, kumpulkan hanya data yang memang diperlukan.
- APK belum dibangun dari lingkungan ini. Setelah konfigurasi Firebase dipasang, Android Studio dapat melakukan build dan menghasilkan APK.


## Build APK online using GitHub Actions (phone-friendly)
1. Create a GitHub account and a new repository.
2. Upload the contents of this project folder to the repository (the `.github/workflows/build-apk.yml` file must be included).
3. Open the repository's **Actions** tab and select **Build BIXMO APK**.
4. Tap **Run workflow** if it has not started automatically. Wait for the run to finish successfully.
5. Open the completed run and download the artifact named **BIXMO-debug-APK**. Extract it to get `app-debug.apk`.

This is a debug/test APK. It is not yet connected to Firebase or real member accounts, and the SOS screen is not a production emergency alert system.
