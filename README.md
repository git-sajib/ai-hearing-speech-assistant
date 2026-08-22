# AI-Driven Assistance for Hearing and Speech Impairments 🤟🤖

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Python 3.11](https://img.shields.io/badge/Python-3.11-blue.svg)](https://www.python.org/)
[![Android Kotlin](https://img.shields.io/badge/Android-Kotlin-purple.svg)](https://kotlinlang.org/)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite-FF6F00.svg)](https://www.tensorflow.org/lite)
[![MediaPipe](https://img.shields.io/badge/MediaPipe-Vision-0097A7.svg)](https://mediapipe.dev/)

An intelligent, real-time, on-device mobile application designed to bridge the communication gap for individuals with hearing and speech impairments. This project translates hand gestures (Sign Language) and facial expressions into synthesized text and speech using deep learning models running natively on Android devices.

---

## 📌 Project Architecture & Flow

```
[ Android CameraX Preview ]
           │
           ▼
[ Google MediaPipe Tasks API ] ─── (Extract 3D Hand Landmarks)
           │
           ▼
[ TensorFlow Lite Engine ]    ─── (On-Device Model Inference & Classification - 98.43% Accuracy)
           │
           ▼
[ Android Native TTS ]        ─── (Real-Time Text & Voice Synthesis Output)
```

---

## 🛠 Tech Stack

- **Mobile Application**: Android Native (Kotlin + Jetpack Compose)
- **Camera Processing**: Android CameraX API
- **Feature Extraction**: Google MediaPipe (Hands & Face Mesh)
- **Machine Learning Runtime**: TensorFlow Lite (TFLite) Engine
- **Speech Synthesis**: Android Native `TextToSpeech` (TTS) Engine
- **ML Training Stack**: Python 3.11, TensorFlow 2.x, PyTorch, OpenCV, NumPy, Scikit-Learn

---

## 📁 Repository Structure

```directory
ai-hearing-speech-assistant/
├── ml_models/               # Python scripts, feature extraction & TFLite model training
│   ├── extract_landmarks.py # Extract 3D Hand Landmarks via MediaPipe
│   ├── train_model.py       # Deep neural network training script (98.43% test accuracy)
│   ├── gesture_model.tflite # Mobile optimized TFLite binary model (27 KB)
│   └── labels.json          # Gesture index to sign label mapping
├── android_app/             # Complete Android project (Kotlin / Jetpack Compose)
│   └── app/src/main/assets/ # Embedded TFLite model & MediaPipe vision binaries
├── .gitignore               # Excludes large raw datasets, virtualenvs, and IDE configurations
├── LICENSE                  # Official MIT License
└── README.md                # Project documentation and guide
```

---

## 🎯 Models & Datasets Used

1. **[Google Isolated Sign Language Recognition (GISLR) / ASL Alphabet](https://www.kaggle.com/datasets/grassknoted/asl-alphabet)**: Hand landmarks and 3D gesture trajectory dataset for sign language recognition.
2. **[FER-2013 Facial Expression Dataset](https://www.kaggle.com/datasets/msambare/fer2013)**: Benchmark dataset for facial expression and emotion classification.

---

## 🚀 Mobile Deployment & Wi-Fi Debugging

1. Open `android_app` project directory.
2. Connect mobile device via fixed Wi-Fi port (`192.168.0.109:5555`).
3. App builds and deploys directly to device over local network without requiring Android Studio UI.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
