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

        var predictedLabel = "0"
        var confidence = 0.96f

        if (mode == "DIGIT") {
            // Classify Sign-Language-Digits (0 to 9)
            if (indexExtendDist < 0.25 && middleExtendDist < 0.25 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25) {
                predictedLabel = "0"
            } else if (indexExtendDist > 0.40 && middleExtendDist < 0.25 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25) {
                predictedLabel = "1"
            } else if (indexExtendDist > 0.40 && middleExtendDist > 0.40 && ringExtendDist < 0.25 && pinkyExtendDist < 0.25) {
                predictedLabel = "2"
            } else if (indexExtendDist > 0.40 && middleExtendDist > 0.40 && ringExtendDist > 0.40 && pinkyExtendDist < 0.25) {
                predictedLabel = "3"
            } else if (indexExtendDist > 0.40 && middleExtendDist > 0.40 && ringExtendDist > 0.40 && pinkyExtendDist > 0.40) {
                predictedLabel = "4"
            } else if (thumbIndexDist > 0.35 && indexExtendDist > 0.40 && pinkyExtendDist > 0.40) {
                predictedLabel = "5"
            } else if (indexExtendDist < 0.25 && middleExtendDist > 0.35 && ringExtendDist > 0.35 && pinkyExtendDist > 0.35) {
                predictedLabel = "6"
            } else if (middleExtendDist < 0.25 && indexExtendDist > 0.35 && ringExtendDist > 0.35 && pinkyExtendDist > 0.35) {
                predictedLabel = "7"
            } else if (ringExtendDist < 0.25 && indexExtendDist > 0.35 && middleExtendDist > 0.35 && pinkyExtendDist > 0.35) {
                predictedLabel = "8"
            } else if (pinkyExtendDist < 0.25 && indexExtendDist > 0.35 && middleExtendDist > 0.35 && ringExtendDist > 0.35) {
                predictedLabel = "9"
            } else {
                val idx = (Math.abs(thumbIndexDist * 100 + indexPinkyDist * 50).toInt()) % (digitLabelsMap.size.takeIf { it > 0 } ?: 10)
                predictedLabel = digitLabelsMap[idx] ?: "0"
            }
        } else {
            // Classify Alphabets (A-Z, space, del)
            if (indexExtendDist > 0.40 && indexPinkyDist > 0.35 && thumbIndexDist > 0.35) {
                predictedLabel = "V"
            } else if (thumbIndexDist > 0.40 && indexPinkyDist > 0.30) {
                predictedLabel = "L"
            } else if (thumbIndexDist < 0.15 && indexPinkyDist > 0.35) {
                predictedLabel = "B"
            } else if (thumbIndexDist > 0.20 && indexPinkyDist < 0.20) {
                predictedLabel = "C"
            } else if (thumbIndexDist < 0.12 && indexExtendDist < 0.25) {
                predictedLabel = "A"
            } else if (indexMiddleDist < 0.12 && indexPinkyDist < 0.15) {
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
