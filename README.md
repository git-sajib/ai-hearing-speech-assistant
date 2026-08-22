# AI-Driven Assistance for Hearing and Speech Impairments

A real-time, on-device mobile application for translating hand gestures and facial expressions into synthesized text and speech for individuals with hearing and speech impairments.

## Project Structure
- `ml_models/`: Python scripts, MediaPipe feature extraction, model training, and TensorFlow Lite conversion.
- `android_app/`: Android application code developed in Kotlin with Jetpack Compose, CameraX, and TFLite integration.

## Key Features
- **Landmark Tracking**: MediaPipe Hands and Face Mesh.
- **On-Device Inference**: TensorFlow Lite with GPU acceleration.
- **Speech Output**: Native Android TextToSpeech (TTS) integration.
