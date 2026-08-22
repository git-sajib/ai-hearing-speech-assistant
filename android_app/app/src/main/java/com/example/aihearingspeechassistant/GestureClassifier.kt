package com.example.aihearingspeechassistant

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class GestureClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val labelsMap = mutableMapOf<Int, String>()

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd("gesture_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            Log.d("GestureClassifier", "TFLite Model loaded successfully.")
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading TFLite model: ${e.message}")
        }
    }

    private fun loadLabels() {
        try {
            val jsonString = context.assets.open("labels.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                labelsMap[key.toInt()] = jsonObject.getString(key)
            }
            Log.d("GestureClassifier", "Labels mapping loaded: ${labelsMap.size} classes.")
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error loading labels.json: ${e.message}")
        }
    }

    fun classify(landmarks: FloatArray): Pair<String, Float> {
        if (interpreter == null || landmarks.size != 63) {
            return Pair("Unknown", 0.0f)
        }

        // Input tensor shape: [1, 63] float32
        val inputBuffer = ByteBuffer.allocateDirect(1 * 63 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (valValue in landmarks) {
                putFloat(valValue)
            }
        }

        // Output tensor shape: [1, 28] float32
        val outputArray = Array(1) { FloatArray(labelsMap.size) }
        interpreter?.run(inputBuffer, outputArray)

        val probabilities = outputArray[0]
        var maxIdx = -1
        var maxProb = -1.0f

        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        val predictedLabel = labelsMap[maxIdx] ?: "Unknown"
        return Pair(predictedLabel, maxProb)
    }

    fun close() {
        interpreter?.close()
    }
}
