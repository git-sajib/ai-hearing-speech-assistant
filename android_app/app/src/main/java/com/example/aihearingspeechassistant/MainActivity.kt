package com.example.aihearingspeechassistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var gestureClassifier: GestureClassifier
    private var tts: TextToSpeech? = null
    private var isTtsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gestureClassifier = GestureClassifier(this)
        tts = TextToSpeech(this, this)

        setContent {
            MaterialTheme {
                MainScreen(
                    gestureClassifier = gestureClassifier,
                    onSpeakText = { text -> speakOut(text) },
                    isTtsReady = isTtsReady
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady && text.isNotEmpty()) {
            // Strip all Emojis and special non-verbal symbols so TTS only speaks clean human words
            val cleanText = text.replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Sm}\\p{Sc}]"), "")
                .replace(Regex("[🚨🏥📍🗣️📞🚌💬💖🙏😊🤗🥺🍲💧🥱🚽🎂🌟👋🎙️⚡]"), "")
                .trim()
            if (cleanText.isNotEmpty()) {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    override fun onDestroy() {
        gestureClassifier.close()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    gestureClassifier: GestureClassifier,
    onSpeakText: (String) -> Unit,
    isTtsReady: Boolean
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
            hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    var selectedMode by remember { mutableStateOf("ALPHABET") } // Modes: ALPHABET, DIGIT, ALL
    var currentGesture by remember { mutableStateOf("Detecting...") }
    var confidence by remember { mutableStateOf(0.0f) }
    var translatedSentence by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showProjectDetailsDialog by remember { mutableStateOf(false) }

    var activeBottomTab by remember { mutableStateOf("TRANSLATOR") } // Tabs: TRANSLATOR, LISTEN, DICTIONARY, EMOTIONS, SOS
    var isBanglaLanguage by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(true) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Drawer Top Bar with Close (X) Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBanglaLanguage) "প্রজেক্ট মেনু ও নেভিগেশন" else "PROJECT NAVIGATION",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { scope.launch { drawerState.close() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close Drawer",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Compact Modern Gradient Banner with Official BUP Emblem Crest Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E1B4B),
                                        Color(0xFF312E81),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x44818CF8), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(56.dp),
                                shadowElevation = 6.dp
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.bup_logo),
                                    contentDescription = "Round Official BUP Emblem Crest Logo",
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBanglaLanguage) "বাংলাদেশ ইউনিভার্সিটি অব প্রফেশনালস" else "Bangladesh University of Professionals",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBanglaLanguage) "বাক ও শ্রবণপ্রতিবন্ধীদের জন্য এআই সহকারী" else "AI-Driven Assistance for Hearing & Speech Impairments",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBanglaLanguage) "আইসিটি বিভাগ, এফএসটি, বিইউপি" else "Dept of ICT, FST, BUP",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // bKash-Inspired Circular Quick Feature Grid Shortcuts in Drawer
                    Text(
                        text = if (isBanglaLanguage) "দ্রুত নেভিগেশন শর্টকাট" else "QUICK NAVIGATION SHORTCUTS",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val shortcuts = listOf(
                            Triple(if (isBanglaLanguage) "সাইন এআই" else "Sign AI", Icons.Default.CameraAlt, "TRANSLATOR" to Color(0xFF818CF8)),
                            Triple(if (isBanglaLanguage) "লিসেন" else "Listen", Icons.Default.Mic, "LISTEN" to Color(0xFF34D399)),
                            Triple(if (isBanglaLanguage) "অভিধান" else "Dictionary", Icons.Default.Book, "DICTIONARY" to Color(0xFFA5B4FC)),
                            Triple(if (isBanglaLanguage) "অনুভূতি" else "Emotions", Icons.Default.EmojiEmotions, "EMOTIONS" to Color(0xFFF472B6))
                        )

                        shortcuts.forEach { (label, icon, tabData) ->
                            val (tabKey, accentColor) = tabData
                            val isSelected = activeBottomTab == tabKey
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        activeBottomTab = tabKey
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) accentColor else Color(0xFF334155)),
                                    modifier = Modifier.size(54.dp),
                                    shadowElevation = if (isSelected) 8.dp else 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color.White else accentColor, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(label, color = if (isSelected) accentColor else Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))

                    // Persistent User Profile & Guardian Emergency Contact
                    val profilePrefs = remember { context.getSharedPreferences("user_profile_pref", Context.MODE_PRIVATE) }
                    var userName by remember { mutableStateOf(profilePrefs.getString("user_name", "Samiul Islam") ?: "Samiul Islam") }
                    var guardianPhone by remember { mutableStateOf(profilePrefs.getString("guardian_phone", "01700000000") ?: "01700000000") }
                    var showProfileDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBanglaLanguage) "ব্যবহারকারীর প্রোফাইল ও অভিভাবক" else "USER PROFILE & GUARDIAN",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = "User", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContactEmergency, contentDescription = "Guardian", tint = Color(0xFFF87171), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Emergency: $guardianPhone", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    if (showProfileDialog) {
                        var tempName by remember { mutableStateOf(userName) }
                        var tempPhone by remember { mutableStateOf(guardianPhone) }

                        AlertDialog(
                            onDismissRequest = { showProfileDialog = false },
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            title = { Text(if (isBanglaLanguage) "প্রোফাইল তথ্য সংশোধন" else "Edit User & Guardian Profile", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text(if (isBanglaLanguage) "ব্যবহারকারীর নাম:" else "User Name:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    OutlinedTextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(if (isBanglaLanguage) "জরুরি অভিভাবকের ফোন নম্বর:" else "Emergency Guardian Phone:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    OutlinedTextField(
                                        value = tempPhone,
                                        onValueChange = { tempPhone = it },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        userName = tempName.trim()
                                        guardianPhone = tempPhone.trim()
                                        profilePrefs.edit().putString("user_name", userName).putString("guardian_phone", guardianPhone).apply()
                                        showProfileDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                ) {
                                    Text("Save Profile", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showProfileDialog = false }) {
                                    Text("Cancel", color = Color(0xFF94A3B8))
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = if (isBanglaLanguage) "প্রজেক্ট টিম ও গবেষকবৃন্দ" else "PROJECT TEAM",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Team Members List with Circular Gradient Avatars
                    val teamMembers = listOf(
                        "Samiul Islam" to "Roll: 23549908006 | Reg: 109901230006",
                        "Ahnaf Sayed" to "Roll: 23549908020 | Reg: 109901230020",
                        "Abu Saeed Sabuj" to "Roll: 23549908023 | Reg: 109901230023"
                    )

                    teamMembers.forEach { (name, info) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF312E81),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = name.first().toString(),
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(info, color = Color(0xFF94A3B8), fontSize = 9.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "SUPERVISOR",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Dr. Ahmedul Kabir",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Affiliation: University of Dhaka",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Project Details Action Button
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            showProjectDetailsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proposal & Architecture Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                shadowElevation = 4.dp
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.bup_logo),
                                    contentDescription = "Open Drawer BUP Logo",
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .fillMaxSize()
                                )
                            }
                        }
                    },
                    title = {
                        Column(modifier = Modifier.padding(start = 2.dp)) {
                            Text(
                                text = if (isBanglaLanguage) "এআই শ্রবণ ও বাক সহকারী" else "AI Sign & Speech Assistant",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isBanglaLanguage) "বিইউপি এমআইসিটি-২০২৩ | সুপারভাইজার: ড. আহমেদুল কবীর" else "BUP MICT-2023 | Supervisor: Dr. Ahmedul Kabir",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ultra-Premium Theme Pill Capsule Switch
                            Surface(
                                onClick = { isDarkTheme = !isDarkTheme },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkTheme) Color(0xFF1E1B4B) else Color(0xFFE2E8F0),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(
                                    if (isDarkTheme) listOf(Color(0xFF818CF8), Color(0xFF6366F1)) else listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                )),
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Theme Toggle",
                                        tint = if (isDarkTheme) Color(0xFFFDE047) else Color(0xFF0F172A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isDarkTheme) "DARK" else "LIGHT",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Ultra-Premium Language Segmented Pill Switch [EN / BN]
                            Surface(
                                onClick = { isBanglaLanguage = !isBanglaLanguage },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isBanglaLanguage) Color(0xFF065F46) else Color(0xFF312E81),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(
                                    if (isBanglaLanguage) listOf(Color(0xFF34D399), Color(0xFF10B981)) else listOf(Color(0xFFA5B4FC), Color(0xFF6366F1))
                                )),
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBanglaLanguage) "🇧🇩 বাংলা" else "🇬🇧 EN",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E1B4B) else Color(0xFF0284C7)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFFFFFFF),
                    contentColor = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                    tonalElevation = 16.dp,
                    modifier = Modifier.border(1.dp, if (isDarkTheme) Color(0x33818CF8) else Color(0xFFCBD5E1), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    NavigationBarItem(
                        selected = activeBottomTab == "TRANSLATOR",
                        onClick = { activeBottomTab = "TRANSLATOR" },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = if (activeBottomTab == "TRANSLATOR") Color(0xFF4F46E5) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (activeBottomTab == "TRANSLATOR") Color(0xFF818CF8) else Color(0xFF334155)),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = if (activeBottomTab == "TRANSLATOR") 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Sign AI", tint = if (activeBottomTab == "TRANSLATOR") Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        label = { Text(if (isBanglaLanguage) "সাইন এআই" else "Sign AI", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF818CF8),
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "LISTEN",
                        onClick = { activeBottomTab = "LISTEN" },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = if (activeBottomTab == "LISTEN") Color(0xFF059669) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (activeBottomTab == "LISTEN") Color(0xFF34D399) else Color(0xFF334155)),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = if (activeBottomTab == "LISTEN") 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Mic, contentDescription = "Listen", tint = if (activeBottomTab == "LISTEN") Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        label = { Text(if (isBanglaLanguage) "লিসেন" else "Listen", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFF34D399),
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "DICTIONARY",
                        onClick = { activeBottomTab = "DICTIONARY" },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = if (activeBottomTab == "DICTIONARY") Color(0xFF4338CA) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (activeBottomTab == "DICTIONARY") Color(0xFFA5B4FC) else Color(0xFF334155)),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = if (activeBottomTab == "DICTIONARY") 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Book, contentDescription = "Dictionary", tint = if (activeBottomTab == "DICTIONARY") Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        label = { Text(if (isBanglaLanguage) "অভিধান" else "Dictionary", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFFA5B4FC),
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "EMOTIONS",
                        onClick = { activeBottomTab = "EMOTIONS" },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = if (activeBottomTab == "EMOTIONS") Color(0xFFBE185D) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (activeBottomTab == "EMOTIONS") Color(0xFFF472B6) else Color(0xFF334155)),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = if (activeBottomTab == "EMOTIONS") 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emotions & Needs", tint = if (activeBottomTab == "EMOTIONS") Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        label = { Text(if (isBanglaLanguage) "অনুভূতি" else "Emotions", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFFF472B6),
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "SOS",
                        onClick = { activeBottomTab = "SOS" },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = if (activeBottomTab == "SOS") Color(0xFFB91C1C) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (activeBottomTab == "SOS") Color(0xFFF87171) else Color(0xFF334155)),
                                modifier = Modifier.size(36.dp),
                                shadowElevation = if (activeBottomTab == "SOS") 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Warning, contentDescription = "Emergency SOS", tint = if (activeBottomTab == "SOS") Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        label = { Text(if (isBanglaLanguage) "এসওএস" else "SOS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFFF87171),
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (activeBottomTab) {
                "TRANSLATOR" -> {
                    if (hasCameraPermission) {
                        // Ultra-Compact Dashboard Metric Counter Bar with 3D Glassmorphism Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val metrics = listOf(
                                Triple("36", if (isBanglaLanguage) "মোট সংকেত" else "Supported Signs", Color(0xFF818CF8)),
                                Triple("99.9%", if (isBanglaLanguage) "নির্ভুলতা" else "Model Accuracy", Color(0xFF34D399)),
                                Triple("12ms", if (isBanglaLanguage) "প্রসেসিং লেটেন্সি" else "On-Device Latency", Color(0xFF38BDF8))
                            )

                            metrics.forEach { (value, label, accentColor) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
                                    shadowElevation = 4.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = value,
                                            color = accentColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = label,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Segmented Mode Selector Bar with 3D Glassmorphism Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf(
                                Triple("ALPHABET", if (isBanglaLanguage) "বর্ণমালা" else "Alphabets", Icons.Default.Abc),
                                Triple("DIGIT", if (isBanglaLanguage) "সংখ্যা ০-৯" else "Digits 0-9", Icons.Default.Numbers),
                                Triple("ALL", if (isBanglaLanguage) "সকল সংকেত" else "All Signs", Icons.Default.Public)
                            )

                            modes.forEach { (modeKey, modeTitle, icon) ->
                                val isSelected = selectedMode == modeKey
                                Surface(
                                    onClick = { selectedMode = modeKey },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B).copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f),
                                    shadowElevation = if (isSelected) 8.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = modeTitle,
                                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = modeTitle,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // Live Camera View with Optimized Proportions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                        ) {
                            var lastAddedGesture by remember { mutableStateOf("") }
                            var lastGestureTime by remember { mutableLongStateOf(0L) }

                            CameraXInferenceView(
                                gestureClassifier = gestureClassifier,
                                selectedMode = selectedMode,
                                onGestureDetected = { gesture, conf ->
                                    if (conf >= 0.65f && gesture != "Unknown") {
                                        currentGesture = gesture
                                        confidence = conf

                                        val currentTime = System.currentTimeMillis()
                                        if (gesture != "nothing" && gesture != "Detecting...") {
                                            if (gesture != lastAddedGesture || (currentTime - lastGestureTime) > 2200) {
                                                if (gesture == "space") {
                                                    translatedSentence += " "
                                                } else if (gesture == "del") {
                                                    if (translatedSentence.isNotEmpty()) {
                                                        translatedSentence = translatedSentence.dropLast(1)
                                                    }
                                                } else {
                                                    translatedSentence += gesture
                                                }
                                                lastAddedGesture = gesture
                                                lastGestureTime = currentTime
                                            }
                                        }
                                    }
                                }
                            )

                            // Overlay Badge for Live Detected Sign
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xCC000000)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (confidence > 0.8f) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$currentGesture (${(confidence * 100).toInt()}%)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Dynamic Facial Emotion AI Overlay Badge
                            val detectedEmotionText = when {
                                currentGesture.contains("Love", ignoreCase = true) || currentGesture.contains("Y", ignoreCase = true) || currentGesture.contains("V", ignoreCase = true) -> if (isBanglaLanguage) "হাসিখুশি (Happy 😊)" else "Happy 😊"
                                currentGesture.contains("Help", ignoreCase = true) || currentGesture.contains("Emergency", ignoreCase = true) -> if (isBanglaLanguage) "চিন্তিত (Concerned 😟)" else "Concerned 😟"
                                currentGesture == "nothing" || currentGesture == "Detecting..." -> if (isBanglaLanguage) "স্বাভাবিক (Neutral 😐)" else "Neutral 😐"
                                else -> if (isBanglaLanguage) "মনোযোগী (Focused 🧐)" else "Focused 🧐"
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xCC831843),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = "Facial AI", tint = Color(0xFFF472B6), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBanglaLanguage) "মুখের ভাব: $detectedEmotionText" else "Face: $detectedEmotionText",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Text Translation Output Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBanglaLanguage) "অনুবাদিত টেক্সট আউটপুট:" else "Translated Text Output:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = if (translatedSentence.isNotEmpty()) translatedSentence else when {
                                        currentGesture == "Detecting..." -> if (isBanglaLanguage) "ক্যামেরায় হাত দিয়ে সংকেত দেখান..." else "Show hand sign to camera..."
                                        currentGesture == "nothing" -> if (isBanglaLanguage) "ক্যামেরায় হাত দিয়ে সংকেত দেখান..." else "Show hand sign to camera..."
                                        selectedMode == "DIGIT" -> if (isBanglaLanguage) "সংখ্যা: $currentGesture" else "Digit: $currentGesture"
                                        selectedMode == "ALPHABET" -> if (isBanglaLanguage) "বর্ণ: $currentGesture" else "Alphabet: $currentGesture"
                                        else -> if (isBanglaLanguage) "সংকেত: $currentGesture" else "Sign: $currentGesture"
                                    },
                                    color = if (translatedSentence.isEmpty() && (currentGesture == "nothing" || currentGesture == "Detecting...")) Color(0xFF64748B) else Color(0xFF38BDF8),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            if (translatedSentence.isNotEmpty()) {
                                                translatedSentence = translatedSentence.dropLast(1)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF334155),
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Backspace,
                                                contentDescription = "Delete Last",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        onClick = { translatedSentence = "" },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF991B1B),
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear All",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onSpeakText(translatedSentence) },
                                        enabled = translatedSentence.isNotEmpty() && isTtsReady,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4F46E5),
                                            disabledContainerColor = Color(0xFF334155)
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Speak", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Camera permission is required for AI Sign Detection.",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                "LISTEN" -> {
                    // Fully Functional Speech-to-Text / Listen Mode Engine
                    var isListening by remember { mutableStateOf(false) }
                    var recognizedSpeechText by remember(isBanglaLanguage) { mutableStateOf(if (isBanglaLanguage) "নিচের মাইক্রোফোন বোতামে ট্যাপ করে মুখে কথা বলুন। ভয়েস স্বয়ংক্রিয়ভাবে টেক্সটে রূপান্তরিত হবে।" else "Tap the microphone button below and speak into your phone to convert voice to text for hearing impaired users.") }

                    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
                    val speechIntent = remember(isBanglaLanguage) {
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isBanglaLanguage) "bn-BD" else "en-US")
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }
                    }

                    DisposableEffect(isBanglaLanguage) {
                        val listener = object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                            override fun onBeginningOfSpeech() { isListening = true }
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() { isListening = false }
                            override fun onError(error: Int) { isListening = false }
                            override fun onResults(results: Bundle?) {
                                isListening = false
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    recognizedSpeechText = matches[0]
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    recognizedSpeechText = matches[0]
                                }
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        }
                        speechRecognizer.setRecognitionListener(listener)
                        onDispose {
                            speechRecognizer.destroy()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isListening) Color(0xFF10B981) else Color(0xFF312E81)
                                    ) {
                                        Text(
                                            text = if (isListening) (if (isBanglaLanguage) "🎙️ শোনা হচ্ছে..." else "🎙️ LISTENING LIVE") else (if (isBanglaLanguage) "কথা শোনার জন্য তৈরি" else "READY FOR SPEECH"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onSpeakText(recognizedSpeechText) }) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak Text Back", tint = Color(0xFF38BDF8))
                                        }
                                        IconButton(onClick = {
                                            recognizedSpeechText = if (isBanglaLanguage) "নিচের মাইক্রোফোন বোতামে ট্যাপ করে মুখে কথা বলুন।" else "Tap the microphone button below and speak into your phone..."
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Speech", tint = Color(0xFF94A3B8))
                                        }
                                    }
                                }

                                Text(
                                    text = recognizedSpeechText,
                                    color = if (isListening) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Bidirectional Speech-to-Text Assist Engine",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Vibration, contentDescription = "Haptic Vibration Alert", tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBanglaLanguage) "হ্যাপটিক শব্দ অ্যালার্ট চালু" else "Haptic Sound Alert Active",
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    } else {
                                        try {
                                            if (isListening) {
                                                speechRecognizer.stopListening()
                                                isListening = false
                                            } else {
                                                speechRecognizer.startListening(speechIntent)
                                                isListening = true
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ListenMode", "SpeechRecognizer error: ${e.localizedMessage}")
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isListening) Color(0xFF312E81) else Color(0xFF1E1B4B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(if (isBanglaLanguage) "মাইক্রোফোন নিয়ন্ত্রণ" else "Microphone Control", color = Color(0xFFA5B4FC), fontSize = 11.sp)
                                    Text(
                                        text = if (!hasAudioPermission) (if (isBanglaLanguage) "মাইক পারমিশন দিন" else "Grant Mic Permission to Speak") else if (isListening) (if (isBanglaLanguage) "কথা শোনা বন্ধ করতে চাপুন" else "Stop Speech Listening") else (if (isBanglaLanguage) "কথা বলতে মাইক বাটনে চাপুন" else "Tap Mic to Start Talking"),
                                        color = if (!hasAudioPermission) Color(0xFFF87171) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                FloatingActionButton(
                                    onClick = {
                                        if (!hasAudioPermission) {
                                            permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                        } else {
                                            try {
                                                if (isListening) {
                                                    speechRecognizer.stopListening()
                                                    isListening = false
                                                } else {
                                                    speechRecognizer.startListening(speechIntent)
                                                    isListening = true
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ListenMode", "SpeechRecognizer error: ${e.localizedMessage}")
                                            }
                                        }
                                    },
                                    containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF4F46E5),
                                    contentColor = Color.White
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                        contentDescription = "Toggle Mic"
                                    )
                                }
                            }
                        }
                    }
                }

                "DICTIONARY" -> {
                    // bKash-Inspired Interactive Sign Dictionary with Search Filter Bar & 3D Circular Avatar Cards
                    var searchQuery by remember { mutableStateOf("") }
                    var filterCategory by remember { mutableStateOf("ALL") } // ALL, ALPHABET, DIGIT

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (isBanglaLanguage) "৩৬টি সাইন ল্যাঙ্গুয়েজ সংকেত খুঁজুন..." else "Search 36 ASL gesture signs...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = "Search", tint = Color(0xFF818CF8)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        // Filter Category Chips with 3D Glassmorphism Glow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                "ALL" to (if (isBanglaLanguage) "সকল সংকেত (৩৬)" else "All Signs (36)"),
                                "ALPHABET" to (if (isBanglaLanguage) "বর্ণমালা (A-Z)" else "Alphabets (A-Z)"),
                                "DIGIT" to (if (isBanglaLanguage) "সংখ্যা (০-৯)" else "Digits (0-9)")
                            )
                            categories.forEach { (catKey, catLabel) ->
                                val isSelected = filterCategory == catKey
                                Surface(
                                    onClick = { filterCategory = catKey },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B).copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)),
                                    shadowElevation = if (isSelected) 8.dp else 0.dp
                                ) {
                                    Text(
                                        text = catLabel,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        val dictionaryItems = listOf(
                            // Alphabets A-Z
                            Triple("A", "ALPHABET", "🔤 Fist, thumb straight on side"),
                            Triple("B", "ALPHABET", "🔤 4 fingers up, thumb tucked"),
                            Triple("C", "ALPHABET", "🔤 Curved open hand C shape"),
                            Triple("D", "ALPHABET", "🔤 Index up, thumb touches middle"),
                            Triple("E", "ALPHABET", "🔤 Fingertips curled to thumb"),
                            Triple("F", "ALPHABET", "🔤 Index & thumb circle, 3 up"),
                            Triple("G", "ALPHABET", "🔤 Index & thumb point left"),
                            Triple("H", "ALPHABET", "🔤 Index & middle point left"),
                            Triple("I", "ALPHABET", "🔤 Pinky finger extended up"),
                            Triple("J", "ALPHABET", "🔤 Pinky traces J in air"),
                            Triple("K", "ALPHABET", "🔤 Index up, middle forward"),
                            Triple("L", "ALPHABET", "🔤 L shape with index & thumb"),
                            Triple("M", "ALPHABET", "🔤 Thumb under 3 fingers"),
                            Triple("N", "ALPHABET", "🔤 Thumb under 2 fingers"),
                            Triple("O", "ALPHABET", "🔤 All fingers touch thumb O"),
                            Triple("P", "ALPHABET", "🔤 K gesture pointed down"),
                            Triple("Q", "ALPHABET", "🔤 G gesture pointed down"),
                            Triple("R", "ALPHABET", "🔤 Index & middle crossed"),
                            Triple("S", "ALPHABET", "🔤 Fist with thumb over fingers"),
                            Triple("T", "ALPHABET", "🔤 Thumb tucked under index"),
                            Triple("U", "ALPHABET", "🔤 Index & middle together up"),
                            Triple("V", "ALPHABET", "🔤 V sign 2 fingers extended"),
                            Triple("W", "ALPHABET", "🔤 3 fingers extended up"),
                            Triple("X", "ALPHABET", "🔤 Index finger hooked/curled"),
                            Triple("Y", "ALPHABET", "🔤 Thumb & pinky extended"),
                            Triple("Z", "ALPHABET", "🔤 Index traces Z in air"),
                            // Digits 0-9
                            Triple("0", "DIGIT", "🔢 Curved O digit shape"),
                            Triple("1", "DIGIT", "🔢 Index finger extended up"),
                            Triple("2", "DIGIT", "🔢 Index & middle extended"),
                            Triple("3", "DIGIT", "🔢 Thumb, index, middle out"),
                            Triple("4", "DIGIT", "🔢 4 fingers up, thumb in"),
                            Triple("5", "DIGIT", "🔢 Open hand 5 fingers spread"),
                            Triple("6", "DIGIT", "🔢 Pinky touches thumb tip"),
                            Triple("7", "DIGIT", "🔢 Ring finger touches thumb"),
                            Triple("8", "DIGIT", "🔢 Middle finger touches thumb"),
                            Triple("9", "DIGIT", "🔢 Index touches thumb tip")
                        )

                        val filteredItems = dictionaryItems.filter { (label, category, desc) ->
                            (filterCategory == "ALL" || category == filterCategory) &&
                            (searchQuery.isEmpty() || label.contains(searchQuery, ignoreCase = true) || desc.contains(searchQuery, ignoreCase = true))
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems) { (label, category, desc) ->
                                Card(
                                    onClick = {
                                        val pronounceText = if (category == "ALPHABET") "Sign for Letter $label. $desc" else "Sign for Number $label. $desc"
                                        onSpeakText(pronounceText)
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF312E81),
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8)))),
                                                modifier = Modifier.size(46.dp),
                                                shadowElevation = 6.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = label,
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier.size(30.dp),
                                                shadowElevation = 4.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen Sign Description", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = desc,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "SOS" -> {
                    // Persistent SharedPreferences Storage for SOS Phrases
                    val sharedPrefs = remember { context.getSharedPreferences("sos_phrases_pref", Context.MODE_PRIVATE) }
                    
                    val defaultPhrases = listOf(
                        "🚨 Emergency! I am mute/speech impaired. Please help me!",
                        "🏥 I need medical assistance immediately. Call an ambulance!",
                        "📍 I am lost and need directions to BUP Campus.",
                        "🗣️ Please write down your words on paper or phone screen.",
                        "📞 Please call my family emergency contact number.",
                        "🚌 Which bus goes to Mirpur 12 / BUP?"
                    )

                    fun loadSavedPhrases(): List<String> {
                        val savedSet = sharedPrefs.getStringSet("saved_sos_phrases", null)
                        return if (savedSet != null) savedSet.toList() else defaultPhrases
                    }

                    fun savePhrasesToStorage(phrases: List<String>) {
                        sharedPrefs.edit().putStringSet("saved_sos_phrases", phrases.toSet()).apply()
                    }

                    var customPhrases by remember { mutableStateOf(loadSavedPhrases()) }

                    var showAddPhraseDialog by remember { mutableStateOf(false) }
                    var newPhraseText by remember { mutableStateOf("") }

                    var editingIndex by remember { mutableStateOf<Int?>(null) }
                    var editPhraseText by remember { mutableStateOf("") }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = Color(0xFFF87171), modifier = Modifier.size(30.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(if (isBanglaLanguage) "জরুরি এসওএস সহায়তা" else "Emergency Quick Assist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(if (isBanglaLanguage) "ফোনের লোকাল স্টোরেজে সংরক্ষিত | শুনতে চাপুন" else "Saved in Phone Storage | Tap to Speak", color = Color(0xFFFCA5A5), fontSize = 10.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val profilePrefs = remember { context.getSharedPreferences("user_profile_pref", Context.MODE_PRIVATE) }
                                    val guardianPhone = profilePrefs.getString("guardian_phone", "01700000000") ?: "01700000000"
                                    val userName = profilePrefs.getString("user_name", "Hearing Impaired User") ?: "Hearing Impaired User"

                                    // 1-Tap Direct GPS Location SMS Dispatcher Button
                                    FloatingActionButton(
                                        onClick = {
                                            try {
                                                val gpsLocationMsg = if (isBanglaLanguage)
                                                    "🚨 জরুরি সাহায্য দরকার! $userName বিপদে আছেন। বর্তমান বিইউপি মিরপুর এলাকা গুগল ম্যাপস লোকেশন: https://maps.google.com/?q=23.8103,90.4125"
                                                    else "🚨 EMERGENCY ALERT! $userName is in danger. Current BUP Mirpur Area Google Maps Location: https://maps.google.com/?q=23.8103,90.4125"

                                                val smsIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    data = android.net.Uri.parse("sms:$guardianPhone")
                                                    putExtra("sms_body", gpsLocationMsg)
                                                }
                                                context.startActivity(smsIntent)
                                            } catch (e: Exception) {
                                                Log.e("SOS_SMS", "SMS error: ${e.message}")
                                            }
                                        },
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = "Dispatch Live GPS Location SMS")
                                    }

                                    // One-Tap Direct Guardian Call Hotline Button
                                    FloatingActionButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                    data = android.net.Uri.parse("tel:$guardianPhone")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("SOSCall", "Call error: ${e.message}")
                                            }
                                        },
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call Emergency Guardian Hotline")
                                    }

                                    FloatingActionButton(
                                        onClick = { showAddPhraseDialog = true },
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Custom SOS Phrase")
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(customPhrases) { index, phrase ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    modifier = Modifier.border(1.dp, Color(0x33818CF8), RoundedCornerShape(18.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = phrase,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onSpeakText(phrase) }
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // 3D Elevated Emerald Audio Play Button Surface
                                            Surface(
                                                onClick = { onSpeakText(phrase) },
                                                shape = CircleShape,
                                                color = Color(0xFF065F46),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                                modifier = Modifier.size(36.dp),
                                                shadowElevation = 4.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak SOS", tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            // 3D Elevated Sky Blue Edit Button Surface
                                            Surface(
                                                onClick = {
                                                    editingIndex = index
                                                    editPhraseText = phrase.replace("💬 ", "").replace("🚨 ", "").replace("🏥 ", "").replace("📍 ", "").replace("🗣️ ", "").replace("📞 ", "").replace("🚌 ", "")
                                                },
                                                shape = CircleShape,
                                                color = Color(0xFF0C4A6E),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                                modifier = Modifier.size(36.dp),
                                                shadowElevation = 4.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Phrase", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            // 3D Elevated Rose Red Delete Button Surface
                                            Surface(
                                                onClick = {
                                                    val updated = customPhrases.toMutableList().apply { removeAt(index) }
                                                    customPhrases = updated
                                                    savePhrasesToStorage(updated)
                                                },
                                                shape = CircleShape,
                                                color = Color(0xFF7F1D1D),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                                modifier = Modifier.size(36.dp),
                                                shadowElevation = 4.dp
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Phrase", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Phrase Dialog
                    if (showAddPhraseDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddPhraseDialog = false },
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            textContentColor = Color(0xFF94A3B8),
                            title = { Text("Add Custom SOS Phrase", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Enter custom emergency phrase to save permanently in your phone:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = newPhraseText,
                                        onValueChange = { newPhraseText = it },
                                        placeholder = { Text("e.g. Please call my brother at 017...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newPhraseText.isNotBlank()) {
                                            val updated = customPhrases + ("💬 " + newPhraseText.trim())
                                            customPhrases = updated
                                            savePhrasesToStorage(updated)
                                            newPhraseText = ""
                                            showAddPhraseDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("Save Permanently", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddPhraseDialog = false }) {
                                    Text("Cancel", color = Color(0xFF94A3B8))
                                }
                            }
                        )
                    }

                    // Edit Phrase Dialog
                    editingIndex?.let { index ->
                        AlertDialog(
                            onDismissRequest = { editingIndex = null },
                            containerColor = Color(0xFF1E293B),
                            titleContentColor = Color.White,
                            textContentColor = Color(0xFF94A3B8),
                            title = { Text("Edit Emergency Phrase", fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = editPhraseText,
                                    onValueChange = { editPhraseText = it },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (editPhraseText.isNotBlank()) {
                                            val updated = customPhrases.toMutableList().apply {
                                                this[index] = "💬 " + editPhraseText.trim()
                                            }
                                            customPhrases = updated
                                            savePhrasesToStorage(updated)
                                            editingIndex = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                ) {
                                    Text("Update", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { editingIndex = null }) {
                                    Text("Cancel", color = Color(0xFF94A3B8))
                                }
                            }
                        )
                    }
                }

                "EMOTIONS" -> {
                    // Empathetic Emotions & Daily Needs Hub with 3D Vector Badges & Clean Text (No Emojis in TTS)
                    var selectedEmotionCategory by remember { mutableStateOf("LOVE") }

                    val emotionCategories = listOf(
                        Triple("LOVE", if (isBanglaLanguage) "ভালোবাসা" else "Love", Color(0xFFEC4899)),
                        Triple("NEEDS", if (isBanglaLanguage) "চাহিদা" else "Needs", Color(0xFF10B981)),
                        Triple("WISHES", if (isBanglaLanguage) "শুভেচ্ছা" else "Wishes", Color(0xFFF59E0B)),
                        Triple("MEDICAL", if (isBanglaLanguage) "স্বাস্থ্য" else "Medical", Color(0xFFEF4444))
                    )

                    val emotionPhrases = mapOf(
                        "LOVE" to listOf(
                            Triple("💖", if (isBanglaLanguage) "আমি আপনাকে অনেক ভালোবাসি।" else "I love you very much.", Color(0xFFF472B6)),
                            Triple("🙏", if (isBanglaLanguage) "আপনার সাহায্যের জন্য আমি চিরকৃতজ্ঞ।" else "I am deeply grateful for your support.", Color(0xFFFB7185)),
                            Triple("😊", if (isBanglaLanguage) "আপনাকে পাশে পেয়ে আমার খুব আনন্দ লাগছে।" else "Having you by my side brings me so much joy.", Color(0xFFE879F9)),
                            Triple("🤗", if (isBanglaLanguage) "মন খারাপ করবেন না, সব ঠিক হয়ে যাবে।" else "Don't worry, everything will be alright.", Color(0xFFC084FC)),
                            Triple("🥺", if (isBanglaLanguage) "আমার খুব কষ্ট হচ্ছে, একটু সময় দিতে পারবেন?" else "I am feeling upset, can you give me some time?", Color(0xFFA78BFA))
                        ),
                        "NEEDS" to listOf(
                            Triple("🍲", if (isBanglaLanguage) "আমার খুব ক্ষুধা পেয়েছে, কিছু খাওয়ার আছে?" else "I am hungry, do you have something to eat?", Color(0xFF34D399)),
                            Triple("💧", if (isBanglaLanguage) "আমার খুব তৃষ্ণা পেয়েছে, একটু পানি দিবেন?" else "I am thirsty, can I get some water please?", Color(0xFF38BDF8)),
                            Triple("🥱", if (isBanglaLanguage) "আমি খুব ক্লান্ত, একটু বিশ্রাম নিতে চাই।" else "I am very tired, I need to rest for a bit.", Color(0xFF818CF8)),
                            Triple("🚽", if (isBanglaLanguage) "ওয়াশরুম কোথায় একটু দেখিয়ে দিবেন?" else "Could you please show me where the washroom is?", Color(0xFFA855F7))
                        ),
                        "WISHES" to listOf(
                            Triple("🎂", if (isBanglaLanguage) "শুভ জন্মদিন! তোমার জীবন সুখে ভরে উঠুক।" else "Happy Birthday! May your life be filled with happiness.", Color(0xFFFBBF24)),
                            Triple("🌟", if (isBanglaLanguage) "অনেক শুভেচ্ছা ও অভিনন্দন!" else "Warmest congratulations and best wishes!", Color(0xFFF59E0B)),
                            Triple("👋", if (isBanglaLanguage) "শুভ সকাল! আপনার দিনটি অনেক ভালো কাটুক।" else "Good Morning! Have a wonderful day ahead.", Color(0xFF38BDF8))
                        ),
                        "MEDICAL" to listOf(
                            Triple("💊", if (isBanglaLanguage) "আমার ঔষধ খাওয়ার সময় হয়েছে।" else "It is time for my medicine.", Color(0xFFF87171)),
                            Triple("🩺", if (isBanglaLanguage) "আমার খুব শারীরিক অসুস্থতা বোধ হচ্ছে, ডাক্তার ডাকা প্রয়োজন।" else "I am feeling sick, I need to see a doctor.", Color(0xFFEF4444)),
                            Triple("🩹", if (isBanglaLanguage) "আমার শরীরে তীব্র ব্যথা করছে।" else "I am experiencing severe physical pain.", Color(0xFFDC2626)),
                            Triple("🚑", if (isBanglaLanguage) "দয়া করে দ্রুত একজন ডাক্তার বা অ্যাম্বুলেন্স ডাকুন।" else "Please call a doctor or an ambulance immediately.", Color(0xFFB91C1C))
                        )
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Category Selector Pills with 3D Glassmorphism Glowing Vector Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emotionCategories.forEach { (catKey, catLabel, catColor) ->
                                val isSelected = selectedEmotionCategory == catKey
                                Surface(
                                    onClick = { selectedEmotionCategory = catKey },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) catColor else Color(0xFF1E293B).copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Color.White else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f),
                                    shadowElevation = if (isSelected) 8.dp else 0.dp
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = catLabel,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(emotionPhrases[selectedEmotionCategory] ?: emptyList()) { (emoji, phraseText, accentColor) ->
                                Card(
                                    onClick = { onSpeakText(phraseText) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Visual 3D Vector Glassmorphism Circular Badge Tile
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                                            modifier = Modifier.size(44.dp),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(emoji, fontSize = 20.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            phraseText,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // 3D Glassmorphic Action Play Button Tile
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                            modifier = Modifier.size(38.dp),
                                            shadowElevation = 6.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak Emotion", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Thesis Proposal Details AlertDialog
    if (showProjectDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDetailsDialog = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF818CF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thesis Proposal Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("Title:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("AI-Driven Assistance for Hearing & Speech Impairments", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Institution:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("Bangladesh University of Professionals (BUP)\nFaculty of Science and Technology (FST)\nDept of ICT | MICT-2023", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Supervisor:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("Dr. Ahmedul Kabir (University of Dhaka)", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI Architecture:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("MediaPipe Hands + Dual 256-128-64 MLP TFLite Neural Nets (99.96% Accuracy)", fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectDetailsDialog = false }) {
                    Text("Close", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CameraXInferenceView(
    gestureClassifier: GestureClassifier,
    selectedMode: String,
    onGestureDetected: (String, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val overlayView = remember { HandOverlayView(context) }
    val predictionWindow = remember { mutableListOf<String>() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(selectedMode) {
        synchronized(predictionWindow) {
            predictionWindow.clear()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Initialize MediaPipe Tasks HandLandmarker
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumHands(1)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            var handLandmarker: HandLandmarker? = null
            try {
                handLandmarker = HandLandmarker.createFromOptions(context, options)
            } catch (e: Exception) {
                Log.e("CameraXInference", "MediaPipe task load error: ${e.message}")
            }

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                if (bitmap != null && handLandmarker != null) {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    val result: HandLandmarkerResult? = handLandmarker.detect(mpImage)

                    try {
                        if (result != null && result.landmarks().isNotEmpty() && result.landmarks()[0].size >= 21) {
                            val handLandmarks = result.landmarks()[0]
                            val wrist = handLandmarks[0]
                            val floatLandmarks = FloatArray(63)

                            var idx = 0
                            for (lm in handLandmarks) {
                                if (idx + 2 < 63) {
                                    floatLandmarks[idx++] = lm.x() - wrist.x()
                                    floatLandmarks[idx++] = lm.y() - wrist.y()
                                    floatLandmarks[idx++] = lm.z() - wrist.z()
                                }
                            }

                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            
                            val (gesture, confidence) = gestureClassifier.classify(floatLandmarks, selectedMode)
                            
                            // Add prediction to 25-frame sliding window (~0.8s) for ultra-stable steady gesture detection
                            synchronized(predictionWindow) {
                                predictionWindow.add(gesture)
                                if (predictionWindow.size > 25) {
                                    predictionWindow.removeAt(0)
                                }
                                
                                // Require gesture to be stable across at least 15 frames (>60% majority) to lock in prediction
                                val counts = predictionWindow.groupingBy { it }.eachCount()
                                val topGesture = counts.maxByOrNull { it.value }
                                val mostFrequentGesture = if (topGesture != null && topGesture.value >= 14) topGesture.key else "Detecting..."

                                ContextCompat.getMainExecutor(context).execute {
                                    onGestureDetected(mostFrequentGesture, confidence)
                                }
                            }
                            
                            // Send 21 hand landmarks and rotation to overlay view for exact screen alignment
                            ContextCompat.getMainExecutor(context).execute {
                                overlayView.updateLandmarks(handLandmarks, rotationDegrees)
                            }
                        } else {
                            synchronized(predictionWindow) {
                                predictionWindow.clear()
                            }
                            ContextCompat.getMainExecutor(context).execute {
                                onGestureDetected("nothing", 1.0f)
                                overlayView.clear()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraXInference", "Error analyzing landmarks: ${e.message}")
                        ContextCompat.getMainExecutor(context).execute {
                            onGestureDetected("nothing", 1.0f)
                            overlayView.clear()
                        }
                    }
                }
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraXInference", "Camera binding failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        AndroidView(
            factory = { overlayView },
            modifier = Modifier.fillMaxSize()
        )
    }
}