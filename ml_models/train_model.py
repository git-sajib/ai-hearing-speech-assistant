import os
import json
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

def train_and_convert():
    csv_path = r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_landmarks.csv"
    if not os.path.exists(csv_path):
        print(f"Error: {csv_path} does not exist.")
        return

    print("Loading extracted landmark dataset...", flush=True)
    df = pd.read_csv(csv_path)

    X = df.drop(columns=["label"]).values
    y_raw = df["label"].values

    # Encode string labels (A-Z, del, nothing, space) to integers
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y_raw)
    num_classes = len(label_encoder.classes_)

    # Save class label mapping to JSON for Android app
    label_mapping = {int(idx): str(label) for idx, label in enumerate(label_encoder.classes_)}
    labels_file = r"C:\Projects\ai-hearing-speech-assistant\ml_models\labels.json"
    with open(labels_file, "w") as f:
        json.dump(label_mapping, f, indent=4)
    print(f"Saved {num_classes} class labels mapping to: {labels_file}", flush=True)

    X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.15, random_state=42, stratify=y_encoded)

    print(f"Training shape: {X_train.shape}, Test shape: {X_test.shape}", flush=True)

    # Lightweight Multi-Layer Perceptron (MLP) Deep Learning Architecture
    model = keras.Sequential([
        keras.layers.Input(shape=(63,)), # 21 keypoints * (x, y, z)
        keras.layers.Dense(128, activation='relu'),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.3),
        keras.layers.Dense(64, activation='relu'),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.2),
        keras.layers.Dense(32, activation='relu'),
        keras.layers.Dense(num_classes, activation='softmax')
    ])

    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    print("Training Neural Network Model...", flush=True)
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=40,
        batch_size=32,
        verbose=1
    )

    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"\n[RESULTS] Model Test Accuracy: {accuracy * 100:.2f}%", flush=True)

    # Save Keras Model
    keras_model_path = r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_gesture_model.h5"
    model.save(keras_model_path)
    print(f"Saved Keras model to: {keras_model_path}", flush=True)

    # Convert to TensorFlow Lite (.tflite) for Android
    print("Converting Model to TensorFlow Lite (.tflite)...", flush=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT] # FP16/INT8 Mobile Optimization
    tflite_model = converter.convert()

    tflite_path = r"C:\Projects\ai-hearing-speech-assistant\ml_models\gesture_model.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)

    tflite_size_kb = os.path.getsize(tflite_path) / 1024
    print(f"[SUCCESS] Converted mobile-ready TFLite model ({tflite_size_kb:.2f} KB) saved to: {tflite_path}", flush=True)

if __name__ == "__main__":
    train_and_convert()
