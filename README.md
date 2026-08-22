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
[ Google MediaPipe Tasks API ] ─── (Extract 3D Hand & Face Landmarks)
           │
           ▼
[ TensorFlow Lite Engine ]    ─── (On-Device Model Inference & Classification)
           │
           ▼
[ Android Native TTS ]        ─── (Real-Time Text & Voice Synthesis Output)
```

---

## 🛠 Tech Stack

- **Mobile Application**: Android Native (Kotlin + Jetpack Compose)
- **Camera Processing**: Android CameraX API
- **Feature Extraction**: Google MediaPipe (Hands & Face Mesh)
- **Machine Learning Runtime**: TensorFlow Lite (TFLite) with GPU Delegate
- **Speech Synthesis**: Android Native `TextToSpeech` (TTS) Engine
- **ML Training Stack**: Python 3.11, TensorFlow 2.x, PyTorch, OpenCV, NumPy, Scikit-Learn

---

## 📁 Repository Structure

```directory
ai-hearing-speech-assistant/
├── ml_models/               # Python scripts, data preprocessing, MediaPipe pipelines & TFLite model training
│   └── test_pipeline.py     # Sanity check script for MediaPipe tracking
├── android_app/             # Android application source code (Kotlin / Jetpack Compose)
├── .gitignore               # Excludes large binaries, virtualenvs, and IDE configurations
└── README.md                # Project documentation and roadmap
```

---

## 🎯 Key Datasets Used

1. **[Google Isolated Sign Language Recognition (GISLR)](https://www.kaggle.com/competitions/asl-signs)**: Hand landmarks and 3D gesture trajectory dataset for sign language recognition.
2. **[FER-2013 Facial Expression Dataset](https://www.kaggle.com/datasets/msambare/fer2013)**: Benchmark dataset for facial expression and emotion classification.

---

## 🚀 Getting Started

### Prerequisites

- Python 3.11+ installed
- Android Studio (Ladybug or newer)
- Android device or Emulator running Android 8.0 (API 26) or higher

### Local Setup & Pipeline Verification

1. Clone the repository:
   ```bash
   git clone https://github.com/git-sajib/ai-hearing-speech-assistant.git
   cd ai-hearing-speech-assistant
   ```

2. Test the MediaPipe Pipeline:
   ```bash
   cd ml_models
   python test_pipeline.py
   ```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
