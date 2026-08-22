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
    private var rotationDegrees: Int = 270

    // Outer Glow / Ring Paint for Cyberpunk / Modern AI Look
    private val outerGlowPaint = Paint().apply {
        color = Color.parseColor("#4000E5FF") // Translucent Cyan Glow
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Modern Neon Cyan Keypoint Paint
    private val pointPaint = Paint().apply {
        color = Color.parseColor("#00E5FF") // Vibrant Cyan/Electric Blue
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Fingertip Glowing Accent Paint
    private val tipPointPaint = Paint().apply {
        color = Color.parseColor("#FF4081") // Neon Pink/Rose Accent for Fingertips
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // High Precision Skeleton Line Paint
    private val linePaint = Paint().apply {
        color = Color.parseColor("#00E5FF") // Cyan Line
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val lineGlowPaint = Paint().apply {
        color = Color.parseColor("#6000E5FF") // Soft Line Glow
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // MediaPipe Hand 21 Landmark Connections
    private val connections = listOf(
        // Wrist to fingers
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),        // Thumb
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),        // Index
        Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),    // Middle
        Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),  // Ring
        Pair(13, 17), Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20) // Pinky
    )

    private val fingerTips = setOf(4, 8, 12, 16, 20)

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>, rotation: Int = 270) {
        this.landmarks = newLandmarks
        this.rotationDegrees = rotation
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

        // Helper function to map MediaPipe normalized coordinates (rotated by CameraX) to screen X, Y
        fun transformCoords(normX: Float, normY: Float): Pair<Float, Float> {
            return when (rotationDegrees) {
                90 -> {
                    // Front camera portrait 90 deg rotation
                    val screenX = normY * viewWidth
                    val screenY = normX * viewHeight
                    Pair(screenX, screenY)
                }
                270 -> {
                    // Front camera portrait 270 deg rotation (Correct Orientation)
                    val screenX = (1.0f - normY) * viewWidth
                    val screenY = (1.0f - normX) * viewHeight
                    Pair(screenX, screenY)
                }
                else -> {
                    val screenX = (1.0f - normX) * viewWidth
                    val screenY = normY * viewHeight
                    Pair(screenX, screenY)
                }
            }
        }

        // 1. Draw Glowing Under-Lines & Main Lines
        for (connection in connections) {
            val startIdx = connection.first
            val endIdx = connection.second

            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val (startX, startY) = transformCoords(landmarks[startIdx].x(), landmarks[startIdx].y())
                val (endX, endY) = transformCoords(landmarks[endIdx].x(), landmarks[endIdx].y())

                // Draw soft glow first
                canvas.drawLine(startX, startY, endX, endY, lineGlowPaint)
                // Draw sharp line
                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }

        // 2. Draw Keypoint Dots (Outer Glow + Center Point)
        for (i in landmarks.indices) {
            val lm = landmarks[i]
            val (cx, cy) = transformCoords(lm.x(), lm.y())

            val isTip = fingerTips.contains(i)
            val baseRadius = if (isTip) 14f else 10f

            // Outer translucence glow
            canvas.drawCircle(cx, cy, baseRadius * 1.8f, outerGlowPaint)

            // Inner solid keypoint (Pink for tips, Cyan for joints)
            if (isTip) {
                canvas.drawCircle(cx, cy, baseRadius, tipPointPaint)
            } else {
                canvas.drawCircle(cx, cy, baseRadius, pointPaint)
            }
        }
    }
}
