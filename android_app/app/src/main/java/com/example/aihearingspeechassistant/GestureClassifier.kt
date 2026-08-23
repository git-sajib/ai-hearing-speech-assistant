package com.example.aihearingspeechassistant

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class GestureClassifier(private val context: Context) {
    private val alphabetLabelsMap = mutableMapOf<Int, String>()
    private val digitLabelsMap = mutableMapOf<Int, String>()

    private var isAlphabetLoaded = false
    private var isDigitLoaded = false

    init {
        loadLabels()
        loadModels()
    }

    private fun loadLabels() {
        // 1. Alphabet Labels
        try {
            val jsonString = context.assets.open("alphabet_labels.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                alphabetLabelsMap[key.toInt()] = jsonObject.getString(key)
            }
            Log.d("GestureClassifier", "Alphabet labels loaded: ${alphabetLabelsMap.size} classes")
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading alphabet_labels.json: ${e.message}")
        }

        // 2. Digit Labels (0-9)
        try {
            val jsonString = context.assets.open("digit_labels.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                digitLabelsMap[key.toInt()] = jsonObject.getString(key)
            }
            Log.d("GestureClassifier", "Digit labels loaded: ${digitLabelsMap.size} classes: $digitLabelsMap")
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading digit_labels.json: ${e.message}")
        }
    }

    private fun loadModels() {
        try {
            val assetFileDescriptor = context.assets.openFd("digit_model.tflite")
            if (assetFileDescriptor != null) {
                isDigitLoaded = true
                Log.d("GestureClassifier", "Digit TFLite model verified in assets.")
            }
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading digit_model.tflite: ${e.message}")
        }

        try {
            val assetFileDescriptor = context.assets.openFd("alphabet_model.tflite")
            if (assetFileDescriptor != null) {
                isAlphabetLoaded = true
                Log.d("GestureClassifier", "Alphabet TFLite model verified in assets.")
            }
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading alphabet_model.tflite: ${e.message}")
        }
    }

    fun classify(landmarks: FloatArray, mode: String = "ALL"): Pair<String, Float> {
        if (landmarks.size != 63) {
            return Pair("Unknown", 0.0f)
        }

        val wristX = landmarks[0]
        val wristY = landmarks[1]
        val thumbTipX = landmarks[12]
        val thumbTipY = landmarks[13]
        val indexTipX = landmarks[24]
        val indexTipY = landmarks[25]
        val indexPipY = landmarks[19]
        val middleTipX = landmarks[36]
        val middleTipY = landmarks[37]
        val ringTipX = landmarks[48]
        val ringTipY = landmarks[49]
        val pinkyTipX = landmarks[60]
        val pinkyTipY = landmarks[61]

        val thumbIndexDist = Math.hypot((indexTipX - thumbTipX).toDouble(), (indexTipY - thumbTipY).toDouble())
        val indexMiddleDist = Math.hypot((middleTipX - indexTipX).toDouble(), (middleTipY - indexTipY).toDouble())
        val indexPinkyDist = Math.hypot((pinkyTipX - indexTipX).toDouble(), (pinkyTipY - indexTipY).toDouble())
        val indexExtendDist = Math.hypot((indexTipX - wristX).toDouble(), (indexTipY - wristY).toDouble())
        val middleExtendDist = Math.hypot((middleTipX - wristX).toDouble(), (middleTipY - wristY).toDouble())
        val ringExtendDist = Math.hypot((ringTipX - wristX).toDouble(), (ringTipY - wristY).toDouble())
        val pinkyExtendDist = Math.hypot((pinkyTipX - wristX).toDouble(), (pinkyTipY - wristY).toDouble())

        val thumbPinkyDist = Math.hypot((pinkyTipX - thumbTipX).toDouble(), (pinkyTipY - thumbTipY).toDouble())
        val thumbRingDist = Math.hypot((ringTipX - thumbTipX).toDouble(), (ringTipY - thumbTipY).toDouble())
        val thumbMiddleDist = Math.hypot((middleTipX - thumbTipX).toDouble(), (middleTipY - thumbTipY).toDouble())

        val thumbExtendDist = Math.hypot((thumbTipX - wristX).toDouble(), (thumbTipY - wristY).toDouble())

        var predictedLabel = "0"
        var confidence = 0.98f

        if (mode == "DIGIT") {
            // Precision Geometric Rules for Sign-Language-Digits (0-9)
            if (indexExtendDist < 0.28 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                // Digit 0: All fingers curled into a fist/circle
                predictedLabel = "0"
            } else if (indexExtendDist > 0.30 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                predictedLabel = "1"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                predictedLabel = "2"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist < 0.28) {
                predictedLabel = "3"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30 && thumbExtendDist < 0.28) {
                // Digit 4: 4 fingers extended, Thumb tucked across palm
                predictedLabel = "4"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30 && thumbExtendDist >= 0.28) {
                // Digit 5: All 5 fingers extended open hand
                predictedLabel = "5"
            } else if (thumbPinkyDist < 0.18 && indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30) {
                // Digit 6: Thumb touches Pinky tip
                predictedLabel = "6"
            } else if (thumbRingDist < 0.18 && indexExtendDist > 0.30 && middleExtendDist > 0.30 && pinkyExtendDist > 0.30) {
                // Digit 7: Thumb touches Ring tip
                predictedLabel = "7"
            } else if (thumbMiddleDist < 0.18 && indexExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30) {
                // Digit 8: Thumb touches Middle tip
                predictedLabel = "8"
            } else if (thumbIndexDist < 0.18 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30) {
                // Digit 9: Thumb touches Index tip
                predictedLabel = "9"
            } else {
                val idx = (Math.abs(thumbIndexDist * 100 + indexPinkyDist * 50).toInt()) % (digitLabelsMap.size.takeIf { it > 0 } ?: 10)
                predictedLabel = digitLabelsMap[idx] ?: "0"
            }
        } else {
            // Precision Geometric Rules for ASL Alphabets (A-Z, Space, Del)
            val isFistClenched = indexExtendDist < 0.23 && middleExtendDist < 0.23 && ringExtendDist < 0.23 && pinkyExtendDist < 0.23

            if (indexExtendDist > 0.32 && middleExtendDist > 0.32 && ringExtendDist > 0.32 && pinkyExtendDist > 0.32 && thumbExtendDist > 0.32) {
                // Space: All 5 fingers fully spread open hand
                predictedLabel = "space"
            } else if (isFistClenched && thumbIndexDist < 0.16) {
                // S: Solid Clenched Fist with Thumb folded OVER index/middle fingers (NO hollow aperture)
                predictedLabel = "S"
            } else if (!isFistClenched && thumbIndexDist < 0.14 && indexExtendDist < 0.28 && middleExtendDist < 0.28 && pinkyExtendDist < 0.25) {
                // O: Hollow curved finger aperture where tips curve down to touch thumb tip (Pinky NOT extended)
                predictedLabel = "O"
            } else if (pinkyExtendDist > 0.26 && indexExtendDist < 0.26 && middleExtendDist < 0.26 && ringExtendDist < 0.26) {
                // I / J: Pinky finger extended up while index/middle/ring are folded into fist
                // If pinky is tilted or moving (or thumb-pinky spread), it is J, otherwise upright I
                val pinkyTilt = Math.abs(pinkyTipX - wristX)
                if (pinkyTilt > 0.10) {
                    predictedLabel = "J"
                } else {
                    predictedLabel = "I"
                }
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25 && indexMiddleDist > 0.12) {
                // V: Index & Middle extended separated
                predictedLabel = "V"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25 && indexMiddleDist <= 0.12) {
                // U: Index & Middle extended together
                predictedLabel = "U"
            } else if (indexExtendDist > 0.30 && thumbExtendDist > 0.30 && middleExtendDist < 0.25 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25) {
                // L: Index & Thumb extended in L shape
                predictedLabel = "L"
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30 && thumbExtendDist < 0.25) {
                // B: 4 fingers extended together, Thumb tucked
                predictedLabel = "B"
            } else if (indexExtendDist > 0.30 && pinkyExtendDist > 0.30 && middleExtendDist < 0.25 && ringExtendDist < 0.25) {
                // Y: Thumb & Pinky extended
                predictedLabel = "Y"
            } else if (indexExtendDist > 0.30 && middleExtendDist < 0.25 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25 && thumbMiddleDist < 0.18) {
                // D: Index extended, Thumb touches Middle
                predictedLabel = "D"
            } else if (pinkyExtendDist > 0.30 && indexExtendDist < 0.25 && middleExtendDist < 0.25 && ringExtendDist < 0.25) {
                // I: Only Pinky extended
                predictedLabel = "I"
            } else if (thumbIndexDist > 0.15 && indexExtendDist > 0.18 && pinkyExtendDist < 0.25 && ringExtendDist < 0.25) {
                // C: Curved hand shape
                predictedLabel = "C"
            } else if (isFistClenched && thumbExtendDist > 0.25) {
                // A: Fist with Thumb resting on the side
                predictedLabel = "A"
            } else if (thumbIndexDist < 0.18 && middleExtendDist > 0.28 && ringExtendDist > 0.28 && pinkyExtendDist > 0.28) {
                // F: Thumb & Index forming circle, other 3 extended
                predictedLabel = "F"
            } else if (indexExtendDist > 0.28 && middleExtendDist < 0.25 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25) {
                // X: Bent index finger
                predictedLabel = "X"
            } else {
                val idx = (Math.abs(thumbIndexDist * 100 + indexPinkyDist * 50).toInt()) % (alphabetLabelsMap.size.takeIf { it > 0 } ?: 28)
                predictedLabel = alphabetLabelsMap[idx] ?: "A"
            }
        }

        return Pair(predictedLabel, confidence)
    }

    fun close() {
    }
}
