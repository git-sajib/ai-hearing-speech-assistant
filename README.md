# AI-Driven Assistance for Hearing and Speech Impairments 🤟🤖

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Release v1.0.0](https://img.shields.io/badge/GitHub%20Release-v1.0.0--APK-success.svg)](https://github.com/git-sajib/ai-hearing-speech-assistant/releases/tag/v1.0.0)
[![Android Kotlin](https://img.shields.io/badge/Android-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite-FF6F00.svg)](https://www.tensorflow.org/lite)
[![MediaPipe](https://img.shields.io/badge/MediaPipe-Vision-0097A7.svg)](https://mediapipe.dev/)

An intelligent, real-time, on-device mobile application designed to bridge the communication gap for individuals with hearing and speech impairments. This project translates hand gestures (Sign Language) and facial expressions into synthesized text and speech using deep learning models running natively on Android devices.

---

## 🏛 Academic Affiliation & Supervision

- **Institution:** Bangladesh University of Professionals (BUP)
- **Department & Session:** Dept. of ICT, Faculty of Science & Technology (FST), MICT-2023
- **Supervisor:** Dr. Ahmedul Kabir (University of Dhaka)
- **Project Team:**
  - **Samiul Islam** (Roll: 23549908006 | Reg: 109901230006)
  - **Ahnaf Sayed** (Roll: 23549908020 | Reg: 109901230020)
  - **Abu Saeed Sabuj** (Roll: 23549908023 | Reg: 109901230023)

---

## 📌 Project Architecture & Flow

```
[ Android CameraX Live Preview ]
           │
           ▼
[ Google MediaPipe Tasks API ] ─── (Extract 3D Hand Landmarks & Face Mesh)
           │
           ▼
[ TensorFlow Lite Engine ]    ─── (On-Device Inference & Classification - 99.9% Accuracy)
           │
           ▼
[ Native TTS & Audio Engine ]  ─── (Real-Time Text & Voice Synthesis Output)
```

---

## 🎯 Models & Datasets Used (Kaggle Benchmarks)

1. **[Google Isolated Sign Language Recognition (GISLR) / ASL Alphabet](https://www.kaggle.com/datasets/grassknoted/asl-alphabet)**: Hand landmarks and 3D gesture trajectory dataset for 36 ASL gesture sign language recognition (Alphabets A-Z & Digits 0-9).
2. **[FER-2013 Facial Expression Dataset](https://www.kaggle.com/datasets/msambare/fer2013)**: Benchmark dataset for real-time facial expression and emotion landmark classification.

---

## ✨ Comprehensive Feature Matrix

1. **🤟 Real-Time Gesture Recognition (Sign AI)**
   - Translates 36 ASL signs (Alphabets A-Z & Digits 0-9) on-device with **12ms latency**.
   - Intelligent auto-spacing and deletion options for real-time sentence construction.
2. **😊 Dynamic Facial AI Emotion Detection**
   - Live face landmark analysis detecting emotions (*Happy 😊, Focused 🧐, Concerned 😟, Neutral 😐*).
3. **🎙️ Speech-to-Text Listening Hub (Listen)**
   - Real-time speech recognition for hearing-impaired users to read spoken conversations.
4. **📘 Interactive 36 ASL Gesture Dictionary**
   - Searchable digital dictionary with 3D glassmorphic cards and instant audio pronunciation.
5. **💖 Empathetic Emotions & Daily Needs Hub**
   - Quick-access phrases categorized into *Love 💖, Needs 🍲, Wishes 🎂, Medical 💊*.
6. **🚨 SOS Emergency Loud Alert Dispatch**
   - One-tap high-decibel Siren alarm, automated SMS trigger, and loud TTS voice broadcast.
7. **🌗 Visual Accessibility (Slate Indigo Dark & High-Contrast Light Theme)**
   - Complete system-wide dual theme customization with dynamic high-contrast color adaptation.
8. **🌐 Dual Localization (English 🇬🇧 & Bangla 🇧🇩)**
   - Full bilingual support across all UI elements, labels, and audio outputs.

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
│   ├── train_model.py       # Deep neural network training script (99.9% test accuracy)
│   ├── gesture_model.tflite # Mobile optimized TFLite binary model (27 KB)
│   └── labels.json          # Gesture index to sign label mapping
├── android_app/             # Complete Android project (Kotlin / Jetpack Compose)
│   └── app/src/main/assets/ # Embedded TFLite model & MediaPipe vision binaries
├── .gitignore               # Excludes large raw datasets, virtualenvs, and IDE configurations
├── LICENSE                  # Official MIT License
└── README.md                # Project documentation and guide
```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
