import os
import cv2
import glob
import numpy as np
import pandas as pd
import mediapipe as mp

def extract_landmarks():
    base_dataset = r"C:\Projects\ai-hearing-speech-assistant\ml_models\datasets\asl_alphabet\asl_alphabet_train"
    dataset_dir = os.path.join(base_dataset, "asl_alphabet_train")
    if not os.path.exists(dataset_dir):
        dataset_dir = base_dataset

    print(f"Loading images from dataset path: {dataset_dir}", flush=True)

    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=True,
        max_num_hands=1,
        min_detection_confidence=0.3
    )

    data_rows = []
    # Extract all 29 dataset classes (A-Z, del, space, nothing)
    classes = [d for d in os.listdir(dataset_dir) if os.path.isdir(os.path.join(dataset_dir, d))]
    classes.sort()

    print(f"Found {len(classes)} classes in dataset: {classes}", flush=True)

    # Extract 500 high-quality images per class across all 26 A-Z classes for rock solid dataset generalization
    SAMPLES_PER_CLASS = 500

    for label in classes:
        class_dir = os.path.join(dataset_dir, label)
        image_paths = glob.glob(os.path.join(class_dir, "*.jpg")) + glob.glob(os.path.join(class_dir, "*.png"))
        image_paths = image_paths[:SAMPLES_PER_CLASS]

        processed_count = 0
        for img_path in image_paths:
            img = cv2.imread(img_path)
            if img is None:
                continue

            h, w, c = img.shape
            img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            results = hands.process(img_rgb)

            if results.multi_hand_landmarks:
                for hand_landmarks in results.multi_hand_landmarks:
                    landmarks = []
                    wrist = hand_landmarks.landmark[0]
                    for lm in hand_landmarks.landmark:
                        landmarks.extend([lm.x - wrist.x, lm.y - wrist.y, lm.z - wrist.z])
                    
                    landmarks.append(label)
                    data_rows.append(landmarks)
                    processed_count += 1

        print(f"Class '{label}': extracted {processed_count} landmark features.", flush=True)

    hands.close()

    col_names = []
    for i in range(21):
        col_names.extend([f"x{i}", f"y{i}", f"z{i}"])
    col_names.append("label")

    df = pd.DataFrame(data_rows, columns=col_names)
    output_csv = r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_landmarks.csv"
    df.to_csv(output_csv, index=False)
    print(f"[SUCCESS] Extracted {len(df)} total hand landmark samples and saved to: {output_csv}", flush=True)

if __name__ == "__main__":
    extract_landmarks()
