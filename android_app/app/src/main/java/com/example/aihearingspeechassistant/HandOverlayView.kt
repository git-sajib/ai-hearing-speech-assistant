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

    private var faceLandmarks: List<NormalizedLandmark> = emptyList()

    // Futuristic Sci-Fi Glowing Emerald Face Mesh Paint
    private val facePointPaint = Paint().apply {
        color = Color.parseColor("#10B981") // Vibrant Emerald Green
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val faceGlowPaint = Paint().apply {
        color = Color.parseColor("#4010B981") // Soft Translucent Emerald Halo
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val faceMeshLinePaint = Paint().apply {
        color = Color.parseColor("#6034D399") // Translucent Neon Mint Mesh Wireframe
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Key MediaPipe 468 Face Mesh Wireframe Connections (Face Oval, Lips, Eyes, Eyebrows)
    private val faceConnections = listOf(
        // Lip Outline Connections
        Pair(61, 185), Pair(185, 40), Pair(40, 39), Pair(39, 37), Pair(37, 0), Pair(0, 267), Pair(267, 269), Pair(269, 270), Pair(270, 409), Pair(409, 291), // Upper Lip Outer
        Pair(61, 146), Pair(146, 91), Pair(91, 181), Pair(181, 84), Pair(84, 17), Pair(17, 314), Pair(314, 405), Pair(405, 321), Pair(321, 375), Pair(375, 291), // Lower Lip Outer
        Pair(78, 95), Pair(95, 88), Pair(88, 178), Pair(178, 87), Pair(87, 14), Pair(14, 317), Pair(317, 402), Pair(402, 318), Pair(318, 324), Pair(324, 308), // Inner Lip
        // Eyebrows
        Pair(70, 63), Pair(63, 105), Pair(105, 66), Pair(66, 107), Pair(55, 65), Pair(65, 52), Pair(52, 53), Pair(53, 46), // Left Eyebrow
        Pair(300, 293), Pair(293, 334), Pair(334, 296), Pair(296, 336), Pair(285, 295), Pair(295, 282), Pair(282, 283), Pair(283, 276), // Right Eyebrow
        // Eyes
        Pair(33, 160), Pair(160, 158), Pair(158, 133), Pair(133, 153), Pair(153, 144), Pair(144, 33), // Left Eye
        Pair(362, 385), Pair(385, 387), Pair(387, 263), Pair(263, 373), Pair(373, 380), Pair(380, 362)  // Right Eye
    )

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>, rotation: Int = 270, newFaceLandmarks: List<NormalizedLandmark> = emptyList()) {
        this.landmarks = newLandmarks
        this.faceLandmarks = newFaceLandmarks
        this.rotationDegrees = rotation
        invalidate() // Trigger redraw on main UI thread
    }

    fun clear() {
        this.landmarks = emptyList()
        this.faceLandmarks = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty() && faceLandmarks.isEmpty()) return

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

        // 1. Draw Futuristic Sci-Fi Face Mesh Wireframe Lines & Glowing Emerald Dots
        if (faceLandmarks.isNotEmpty()) {
            // Draw Wireframe Lines between face keypoints
            for (conn in faceConnections) {
                val startIdx = conn.first
                val endIdx = conn.second
                if (startIdx < faceLandmarks.size && endIdx < faceLandmarks.size) {
                    val (startX, startY) = transformCoords(faceLandmarks[startIdx].x(), faceLandmarks[startIdx].y())
                    val (endX, endY) = transformCoords(faceLandmarks[endIdx].x(), faceLandmarks[endIdx].y())
                    canvas.drawLine(startX, startY, endX, endY, faceMeshLinePaint)
                }
            }

            // Draw glowing emerald dots for all 468 landmarks
            for (lm in faceLandmarks) {
                val (cx, cy) = transformCoords(lm.x(), lm.y())
                canvas.drawCircle(cx, cy, 5f, faceGlowPaint)
                canvas.drawCircle(cx, cy, 2.5f, facePointPaint)
            }
        }

        // 2. Draw Glowing Under-Lines & Main Lines for Hand
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

        // 3. Draw Hand Keypoint Dots (Outer Glow + Center Point)
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
