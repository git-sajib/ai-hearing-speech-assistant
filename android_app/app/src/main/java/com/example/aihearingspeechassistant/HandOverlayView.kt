package com.example.aihearingspeechassistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class HandOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var landmarks: List<NormalizedLandmark> = emptyList()

    private val pointPaint = Paint().apply {
        color = Color.parseColor("#00FF88") // Vibrant neon green for hand keypoints
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#38BDF8") // Sky blue connections for finger skeleton
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // MediaPipe Hand 21 Landmark Connections
    private val connections = listOf(
        // Wrist to fingers
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),      // Thumb
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),      // Index
        Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),  // Middle
        Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),// Ring
        Pair(13, 17), Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20) // Pinky
    )

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>) {
        this.landmarks = newLandmarks
        invalidate() // Trigger redraw on main UI thread
    }

    fun clear() {
        this.landmarks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Draw Skeleton Lines between Joints
        for (connection in connections) {
            val startIdx = connection.first
            val endIdx = connection.second

            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val startLm = landmarks[startIdx]
                val endLm = landmarks[endIdx]

                // Mirror X for front camera view
                val startX = (1.0f - startLm.x()) * viewWidth
                val startY = startLm.y() * viewHeight
                val endX = (1.0f - endLm.x()) * viewWidth
                val endY = endLm.y() * viewHeight

                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }

        // Draw 21 Landmark Keypoint Dots
        for (lm in landmarks) {
            val cx = (1.0f - lm.x()) * viewWidth
            val cy = lm.y() * viewHeight
            canvas.drawCircle(cx, cy, 10f, pointPaint)
        }
    }
}
