package com.bixmo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val Ink = Color(0xFF101114)
private val Panel = Color(0xFF202126)
private val Neon = Color(0xFFE5FF00)
private val Soft = Color(0xFFB9BBC3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Neon, onPrimary = Ink, background = Ink, surface = Panel, onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    var ready by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        try { FirebaseApp.initializeApp(this@MainActivity) } catch (_: Exception) { }
                        ready = true
                    }
                    if (ready) BixmoRoot() else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Neon) }
                }
            }
        }
    }
}

@Composable
private fun BixmoRoot() {
    val auth = remember { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    var user by remember { mutableStateOf(auth?.currentUser) }
    var screen by remember { mutableStateOf("Beranda") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            runCatching { FirebaseFirestore.getInstance().collection("users").document(user!!.uid).get().addOnSuccessListener { status = it.getString("status") ?: "pending" } }
        }
    }

    if (auth == null) {
        InfoScreen("Firebase belum dikonfigurasi", "Tambahkan file app/google-services.json dari proyek Firebase. Lihat README.md untuk langkah setup.")
        return
    }

    if (user == null) {
        AuthScreen(
            onLogin = { email, password ->
                auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                    if (task.isSuccessful) { user = auth.currentUser; error = null }
                    else error = task.exception?.localizedMessage ?: "Login gagal. Periksa email dan kata sandi."
                }
            },
            onRegister = { name, email, password, bike ->
                auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val current = auth.currentUser
                        if (current == null) { error = "Akun dibuat, tetapi sesi login tidak ditemukan."; return@addOnCompleteListener }
                        val profile = hashMapOf("uid" to current.uid, "name" to name.trim(), "email" to email.trim(), "bike" to bike, "role" to "member", "status" to "pending", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                        FirebaseFirestore.getInstance().collection("users").document(current.uid).set(profile)
                            .addOnSuccessListener { user = current; status = "pending"; error = null }
                            .addOnFailureListener { error = "Akun dibuat, tetapi profil gagal disimpan. Periksa konfigurasi Firestore." }
                    } else error = task.exception?.localizedMessage ?: "Pendaftaran gagal."
                }
            }, error = error
        )
    } else if (status != "approved") {
        PendingScreen(status ?: "pending", onRefresh = {
            FirebaseFirestore.getInstance().collection("users").document(user!!.uid).get()
                .addOnSuccessListener { status = it.getString("status") ?: "pending" }
                .addOnFailureListener { error = "Tidak dapat memuat status. Periksa koneksi." }
        }, onLogout = { auth.signOut(); user = null; status = null }, message = error)
    } else {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(Neon, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("⚡", color = Ink, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("BIXMO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("MAX OWNERS BANJARMASIN", color = Neon, style = MaterialTheme.typography.labelSmall) }
                    TextButton(onClick = { auth.signOut(); user = null; status = null }) { Text("Keluar", color = Neon) }
                }
                when (screen) {
                    "Beranda" -> HomeScreen(user?.email.orEmpty())
                    "Agenda" -> AgendaScreen()
                    "Member" -> MemberScreen(user?.email.orEmpty())
                    else -> AnnouncementScreen()
                }
            }
            NavigationBar(containerColor = Panel) {
                listOf("Beranda", "Agenda", "Member", "Info").forEach { item ->
                    val icon = when (item) { "Beranda" -> Icons.Default.Home; "Agenda" -> Icons.Default.CalendarMonth; "Member" -> Icons.Default.Groups; else -> Icons.Default.Campaign }
                    NavigationBarItem(selected = screen == item, onClick = { screen = item }, icon = { Icon(icon, contentDescription = item) }, label = { Text(item) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Neon, selectedTextColor = Neon, indicatorColor = Ink, unselectedIconColor = Soft, unselectedTextColor = Soft))
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(onLogin: (String, String) -> Unit, onRegister: (String, String, String, String) -> Unit, error: String?) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bike by remember { mutableStateOf("NMAX") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(64.dp).background(Neon, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text("⚡", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(18.dp)); Text("BIXMO", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
        Text("MAX OWNERS BANJARMASIN", color = Neon, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp)); Text(if (register) "Daftar anggota" else "Selamat datang kembali", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (register) {
            Spacer(Modifier.height(12.dp)); OutlinedTextField(name, { name = it }, label = { Text("Nama lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        Spacer(Modifier.height(10.dp)); OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, label = { Text("Kata sandi (min. 6 karakter)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        if (register) {
            Spacer(Modifier.height(10.dp)); Text("Jenis motor", color = Soft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("NMAX", "XMAX", "TMAX").forEach { item -> FilterChip(selected = bike == item, onClick = { bike = item }, label = { Text(item) }) } }
            Text("Pendaftaran akan menunggu persetujuan pengurus.", color = Soft, style = MaterialTheme.typography.bodySmall)
        }
        if (error != null) { Spacer(Modifier.height(8.dp)); Text(error, color = Color(0xFFFF7777), style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { if (register) onRegister(name, email, password, bike) else onLogin(email, password) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Ink)) { Text(if (register) "Daftar" else "Masuk", fontWeight = FontWeight.Bold) }
        TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar", color = Neon) }
    }
}

@Composable
private fun PendingScreen(status: String, onRefresh: () -> Unit, onLogout: () -> Unit, message: String?) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Groups, contentDescription = null, tint = Neon, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp)); Text(if (status == "rejected") "Pendaftaran belum disetujui" else "Menunggu persetujuan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text(if (status == "rejected") "Silakan hubungi pengurus BIXMO untuk informasi lebih lanjut." else "Akunmu sudah terdaftar. Akses anggota akan terbuka setelah pengurus menyetujui pendaftaran.", color = Soft)
        if (message != null) { Spacer(Modifier.height(8.dp)); Text(message, color = Color(0xFFFF7777)) }
        Spacer(Modifier.height(22.dp)); Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Ink)) { Text("Periksa status") }
        TextButton(onClick = onLogout) { Text("Keluar", color = Neon) }
    }
}

@Composable
private fun HomeScreen(email: String) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("HALO, MEMBER BIXMO!", color = Neon, style = MaterialTheme.typography.labelLarge)
        Text("Satu komunitas. Satu perjalanan.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Informasi komunitas dalam satu aplikasi.", color = Soft)
        FeatureCard("Agenda kopdar", "Jadwal, lokasi, dan konfirmasi kehadiran", Icons.Default.CalendarMonth)
        FeatureCard("Data anggota", "Direktori anggota terverifikasi", Icons.Default.Groups)
        FeatureCard("Informasi komunitas", "Pengumuman dari pengurus BIXMO", Icons.Default.Campaign)
        Text("Login sebagai $email", color = Soft, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AgendaScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Agenda kegiatan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Agenda resmi akan muncul di sini setelah pengurus menambahkannya.", color = Soft)
        FeatureCard("Kopdar BIXMO", "Gas Coffe, Banjarmasin • tanggal menunggu konfirmasi", Icons.Default.CalendarMonth)
        Text("Ini masih contoh tampilan; data belum diambil dari Firestore.", color = Soft, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MemberScreen(email: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profil anggota", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FeatureCard("Akun aktif", email, Icons.Default.Groups)
        FeatureCard("Data motor", "Data profil akan ditampilkan dari database pada tahap berikutnya", Icons.Default.DirectionsBike)
    }
}

@Composable
private fun AnnouncementScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Info BIXMO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FeatureCard("Pengumuman komunitas", "Informasi PDH, jaket windbreaker IMO, Portal Tumpres, dan kopdar Kalsel.", Icons.Default.Campaign)
        Text("Konten ini contoh awal. Fitur admin dan penyimpanan pengumuman perlu diselesaikan berikutnya.", color = Soft, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FeatureCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Neon, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, color = Color.White); Spacer(Modifier.height(4.dp)); Text(description, color = Soft, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun InfoScreen(title: String, detail: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Neon)
        Spacer(Modifier.height(12.dp)); Text(detail, color = Soft)
    }
}
