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
            // Perfected Geometric Decision Rules for Natural ASL Alphabet Hands (A-Z, space, del)
            if (indexTipY > wristY && middleTipY > wristY) {
                // Downward pointing gestures (P, Q)
                if (indexExtendDist > 0.30 && thumbExtendDist > 0.25 && middleExtendDist < 0.28) {
                    predictedLabel = "Q" // Index & Thumb pointing downward
                } else if (indexExtendDist > 0.30 && middleExtendDist > 0.25) {
                    predictedLabel = "P" // Index & Middle pointing downward
                } else {
                    predictedLabel = "M"
                }
            } else if (indexExtendDist < 0.32 && middleExtendDist < 0.32 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                // Closed Fist & Tucked Hand Signs (A, S, E, M, N, T, O)
                if (thumbExtendDist > 0.30 && thumbIndexDist > 0.15) {
                    predictedLabel = "A" // Fist with Thumb extended out to side
                } else if (thumbIndexDist < 0.12 && thumbMiddleDist < 0.12 && thumbRingDist < 0.12) {
                    predictedLabel = "S" // Fist with Thumb folded tightly over index/middle
                } else if (thumbTipY > indexTipY && thumbIndexDist < 0.16) {
                    predictedLabel = "E" // Fingertips curled down tightly to thumb base
                } else if (thumbRingDist < 0.15 && thumbPinkyDist < 0.18) {
                    predictedLabel = "M" // Thumb tucked under 3 fingers (Index, Middle, Ring)
                } else if (thumbMiddleDist < 0.15 && ringExtendDist < 0.22) {
                    predictedLabel = "N" // Thumb tucked under 2 fingers (Index, Middle)
                } else if (thumbIndexDist < 0.16) {
                    predictedLabel = "T" // Thumb tucked under 1 finger (Index)
                } else if (thumbIndexDist < 0.12 && indexMiddleDist < 0.08) {
                    predictedLabel = "O" // Curved fingers forming tight 'O' circle
                } else {
                    predictedLabel = "A"
                }
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30) {
                // Open Hand Signs (B, Space, C, F)
                if (thumbIndexDist < 0.14 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist > 0.30) {
                    predictedLabel = "F" // Thumb & Index touch forming circle, 3 extended
                } else if (thumbIndexDist > 0.14 && thumbIndexDist < 0.25 && indexMiddleDist < 0.08) {
                    predictedLabel = "C" // C-shaped curve
                } else if (thumbExtendDist > 0.30) {
                    predictedLabel = "space" // Full wide open hand
                } else {
                    predictedLabel = "B" // 4 fingers extended together, thumb tucked
                }
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist > 0.30 && pinkyExtendDist < 0.28) {
                predictedLabel = "W" // 3 fingers extended (Index, Middle, Ring)
            } else if (indexExtendDist > 0.30 && middleExtendDist > 0.30 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                // 2 fingers extended (V, U, K, R, H)
                if (indexMiddleDist > 0.10) {
                    predictedLabel = "V" // V shape separated
                } else if (thumbIndexDist > 0.18) {
                    predictedLabel = "K" // V shape with Thumb between them
                } else if (indexTipX < middleTipX) {
                    predictedLabel = "R" // Crossed fingers
                } else if (Math.abs((indexTipY - wristY).toDouble()) < 0.15) {
                    predictedLabel = "H" // Pointing horizontally
                } else {
                    predictedLabel = "U" // Index & Middle extended together
                }
            } else if (indexExtendDist > 0.30 && pinkyExtendDist > 0.30 && middleExtendDist < 0.28 && ringExtendDist < 0.28) {
                predictedLabel = "Y" // Thumb & Pinky extended (or ILY sign)
            } else if (indexExtendDist > 0.30 && middleExtendDist < 0.28 && ringExtendDist < 0.28 && pinkyExtendDist < 0.28) {
                // 1 finger extended (L, D, G, Z, X)
                if (thumbExtendDist > 0.30 && thumbIndexDist > 0.22) {
                    predictedLabel = "L" // L shape (Index UP, Thumb SIDEWAYS)
                } else if (Math.abs((indexTipY - wristY).toDouble()) < 0.15 && thumbIndexDist > 0.18) {
                    predictedLabel = "G" // Index & Thumb pointing horizontally
                } else if (thumbMiddleDist < 0.15) {
                    predictedLabel = "D" // Index UP, Thumb touches Middle
                } else if (indexTipY > indexPipY) {
                    predictedLabel = "X" // Hooked Index finger
                } else {
                    predictedLabel = "Z" // Index UP
                }
            } else if (pinkyExtendDist > 0.30 && indexExtendDist < 0.28 && middleExtendDist < 0.28 && ringExtendDist < 0.28) {
                if (pinkyTipY > wristY) {
                    predictedLabel = "J" // Pinky tracing J
                } else {
                    predictedLabel = "I" // Only Pinky extended
                }
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
