import os
import json
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

def train_individual_model(csv_path, model_name_prefix):
    if not os.path.exists(csv_path):
        print(f"Error: {csv_path} does not exist.")
        return

    print(f"\n==========================================", flush=True)
    print(f"   Training {model_name_prefix.upper()} Neural Network Model", flush=True)
    print(f"==========================================", flush=True)

    df = pd.read_csv(csv_path)
    X = df.drop(columns=["label"]).values
    y_raw = df["label"].values

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y_raw)
    num_classes = len(label_encoder.classes_)

    label_mapping = {int(idx): str(label) for idx, label in enumerate(label_encoder.classes_)}
    labels_json_path = f"C:\\Projects\\ai-hearing-speech-assistant\\ml_models\\{model_name_prefix}_labels.json"
    with open(labels_json_path, "w") as f:
        json.dump(label_mapping, f, indent=4)
    print(f"Saved {num_classes} classes to: {labels_json_path}", flush=True)

    X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.15, random_state=42, stratify=y_encoded)

    model = keras.Sequential([
        keras.layers.Input(shape=(63,)),
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

    model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=35,
        batch_size=32,
        verbose=1
    )

    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"\n[ACCURACY] {model_name_prefix.upper()} Test Accuracy: {accuracy * 100:.2f}%\n", flush=True)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    tflite_path = f"C:\\Projects\\ai-hearing-speech-assistant\\ml_models\\{model_name_prefix}_model.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"[SUCCESS] Converted mobile-ready TFLite saved to: {tflite_path}", flush=True)

if __name__ == "__main__":
    train_individual_model(r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_landmarks.csv", "alphabet")
    train_individual_model(r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_digit_landmarks.csv", "digit")
