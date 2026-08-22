package com.example.aihearingspeechassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.aihearingspeechassistant.ui.theme.AIHearingSpeechAssistantTheme
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady by mutableStateOf(false)
    private lateinit var gestureClassifier: GestureClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        gestureClassifier = GestureClassifier(this)

        setContent {
            AIHearingSpeechAssistantTheme {
                MainAppScreen(
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
        if (isTtsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        gestureClassifier.close()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentGesture by remember { mutableStateOf("Detecting...") }
    var confidence by remember { mutableStateOf(0.0f) }
    var translatedSentence by remember { mutableStateOf("") }
    var lastAddedChar by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Sign Language Assistant",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1B4B)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasCameraPermission) {
                // Live Camera View with MediaPipe & TFLite
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    CameraXInferenceView(
                        gestureClassifier = gestureClassifier,
                        onGestureDetected = { gesture, conf ->
                            if (conf >= 0.70f && gesture != "Unknown") {
                                currentGesture = gesture
                                confidence = conf
                            }
                        }
                    )

                    // Overlay Badge for Live Detected Sign
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xAA000000)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (confidence > 0.8f) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$currentGesture (${(confidence * 100).toInt()}%)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Text Translation Output Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Translated Text Output:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = if (currentGesture == "nothing" || currentGesture == "Detecting...") "Show hand sign to camera..." else "Detected Alphabet: $currentGesture",
                            color = if (currentGesture == "nothing" || currentGesture == "Detecting...") Color(0xFF64748B) else Color(0xFF38BDF8),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Action Buttons: Speak Text & Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (translatedSentence.isNotEmpty()) {
                                        translatedSentence = translatedSentence.dropLast(1)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete Last", tint = Color(0xFF94A3B8))
                            }

                            IconButton(
                                onClick = {
                                    translatedSentence = ""
                                    lastAddedChar = ""
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear All", tint = Color(0xFFEF4444))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onSpeakText(translatedSentence) },
                                enabled = translatedSentence.isNotEmpty() && isTtsReady,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speak", fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun CameraXInferenceView(
    gestureClassifier: GestureClassifier,
    onGestureDetected: (String, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
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

                    if (result != null && result.landmarks().isNotEmpty()) {
                        val handLandmarks = result.landmarks()[0]
                        val wrist = handLandmarks[0]
                        val floatLandmarks = FloatArray(63)

                        var idx = 0
                        for (lm in handLandmarks) {
                            floatLandmarks[idx++] = lm.x() - wrist.x()
                            floatLandmarks[idx++] = lm.y() - wrist.y()
                            floatLandmarks[idx++] = lm.z() - wrist.z()
                        }

                        val (gesture, confidence) = gestureClassifier.classify(floatLandmarks)
                        onGestureDetected(gesture, confidence)
                    } else {
                        onGestureDetected("nothing", 1.0f)
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

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}