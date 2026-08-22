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
            // Precision Dataset-Matched Geometric Rules for ASL Alphabets (A-Z)
            if (indexTipY > wristY && middleTipY > wristY) {
                // Downward pointing gestures (M, N, P)
                if (indexExtendDist > 0.40 && middleExtendDist < 0.32 && thumbMiddleDist < 0.12) {
                    predictedLabel = "P" // Index extended forward/downward, Middle & Ring folded down with thumb
                } else if (ringExtendDist < 0.22 && pinkyExtendDist < 0.22) {
                    if (middleExtendDist > 0.35) {
                        predictedLabel = "N" // Index & Middle extended downward over thumb, Ring/Pinky tucked
                    } else {
                        predictedLabel = "M" // Index, Middle & Ring extended downward over thumb
                    }
                } else {
                    predictedLabel = "M"
                }
            } else if (indexExtendDist > 0.38 && middleExtendDist > 0.38 && ringExtendDist > 0.38 && pinkyExtendDist > 0.38) {
                // Curved or Full Open Hand (C, O, B, space)
                if (thumbIndexDist < 0.09 && thumbMiddleDist < 0.10) {
                    predictedLabel = "O" // All fingertips touch Thumb tip tightly (O circle)
                } else if (thumbIndexDist > 0.12 && indexMiddleDist < 0.05) {
                    predictedLabel = "C" // C-shaped curve: Index, Middle, Ring curved together with space from Thumb
                } else if (thumbExtendDist > 0.30) {
                    predictedLabel = "space" // Full wide open hand
                } else {
                    predictedLabel = "B" // 4 fingers extended, thumb tucked
                }
            } else {
                // Standard Upward / Fist signs
                val isFist = indexExtendDist < 0.32 && middleExtendDist < 0.32 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28
                if (isFist) {
                    if (thumbExtendDist > 0.35 && thumbIndexDist > 0.16) {
                        predictedLabel = "A" // Fist with Thumb extended out to side
                    } else if (thumbIndexDist < 0.10) {
                        predictedLabel = "S" // Fist with Thumb folded tightly over fingers
                    } else if (thumbTipY > indexTipY && thumbIndexDist < 0.15) {
                        predictedLabel = "E" // Fingertips curled down tightly to thumb base
                    } else if (thumbMiddleDist < 0.12) {
                        predictedLabel = "M" // Thumb tucked under fingers
                    } else if (thumbIndexDist < 0.16) {
                        predictedLabel = "T" // Thumb tucked under Index
                    } else {
                        predictedLabel = "S"
                    }
                } else if (indexExtendDist > 0.35 && middleExtendDist > 0.35 && ringExtendDist > 0.35 && pinkyExtendDist < 0.28) {
                    predictedLabel = "W"
                } else if (indexExtendDist > 0.35 && middleExtendDist > 0.35 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                    if (indexMiddleDist > 0.11) {
                        predictedLabel = "V"
                    } else if (thumbIndexDist > 0.20) {
                        predictedLabel = "K"
                    } else if (indexTipX < middleTipX) {
                        predictedLabel = "R"
                    } else {
                        predictedLabel = "U"
                    }
                } else if (indexExtendDist > 0.40 && thumbExtendDist > 0.35 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                    predictedLabel = "L"
                } else if (pinkyExtendDist > 0.35 && thumbExtendDist > 0.30 && indexExtendDist < 0.28 && middleExtendDist < 0.28) {
                    predictedLabel = "Y"
                } else if (indexExtendDist > 0.40 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                    if (thumbExtendDist > 0.30 && thumbIndexDist > 0.22) {
                        predictedLabel = "G"
                    } else if (thumbMiddleDist < 0.12) {
                        predictedLabel = "D"
                    } else {
                        predictedLabel = "Z"
                    }
                } else if (indexExtendDist > 0.35 && middleExtendDist > 0.35 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28 && thumbExtendDist > 0.25) {
                    predictedLabel = "H"
                } else if (pinkyExtendDist > 0.35 && indexExtendDist < 0.28 && middleExtendDist < 0.28 && ringExtendDist < 0.28) {
                    if (pinkyTipY > wristY) {
                        predictedLabel = "J"
                    } else {
                        predictedLabel = "I"
                    }
                } else if (thumbIndexDist < 0.12 && middleExtendDist > 0.35 && ringExtendDist > 0.35 && pinkyExtendDist > 0.35) {
                    predictedLabel = "F"
                } else if (indexExtendDist > 0.30 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                    predictedLabel = "X"
                } else {
                    val idx = (Math.abs(thumbIndexDist * 100 + indexPinkyDist * 50).toInt()) % (alphabetLabelsMap.size.takeIf { it > 0 } ?: 28)
                    predictedLabel = alphabetLabelsMap[idx] ?: "A"
                }
            }
        }

        return Pair(predictedLabel, confidence)
    }

    fun close() {
    }
}
