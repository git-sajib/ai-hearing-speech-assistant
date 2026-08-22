import os
import cv2
import glob
import pandas as pd
import mediapipe as mp

def extract_landmarks():
    alphabet_dataset_dir = r"C:\Projects\ai-hearing-speech-assistant\ml_models\datasets\asl_alphabet\asl_alphabet_train"
    if os.path.exists(os.path.join(alphabet_dataset_dir, "asl_alphabet_train")):
        alphabet_dataset_dir = os.path.join(alphabet_dataset_dir, "asl_alphabet_train")

    digit_dataset_dir = r"C:\Projects\ai-hearing-speech-assistant\ml_models\datasets\Sign-Language-Digits\Dataset"

    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=True,
        max_num_hands=1,
        min_detection_confidence=0.3
    )

    col_names = [f"{coord}{i}" for i in range(21) for coord in ("x", "y", "z")] + ["label"]

    # 1. Extract Alphabet Landmarks
    if os.path.exists(alphabet_dataset_dir):
        print("=== 1. Extracting ALPHABET Landmarks ===", flush=True)
        alphabet_rows = []
        alphabet_classes = [d for d in os.listdir(alphabet_dataset_dir) if os.path.isdir(os.path.join(alphabet_dataset_dir, d))]
        alphabet_classes.sort()

        for label in alphabet_classes:
            class_dir = os.path.join(alphabet_dataset_dir, label)
            image_paths = glob.glob(os.path.join(class_dir, "*.jpg")) + glob.glob(os.path.join(class_dir, "*.png"))
            image_paths = image_paths[:400]

            count = 0
            for img_path in image_paths:
                img = cv2.imread(img_path)
                if img is None:
                    continue
                img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                results = hands.process(img_rgb)
                if results.multi_hand_landmarks:
                    for hand_landmarks in results.multi_hand_landmarks:
                        landmarks = []
                        wrist = hand_landmarks.landmark[0]
                        for lm in hand_landmarks.landmark:
                            landmarks.extend([lm.x - wrist.x, lm.y - wrist.y, lm.z - wrist.z])
                        landmarks.append(label)
                        alphabet_rows.append(landmarks)
                        count += 1
            print(f"Alphabet '{label}': {count} samples.", flush=True)

        df_alphabet = pd.DataFrame(alphabet_rows, columns=col_names)
        alphabet_csv = r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_alphabet_landmarks.csv"
        df_alphabet.to_csv(alphabet_csv, index=False)
        print(f"[SUCCESS] Saved ALPHABET CSV ({len(df_alphabet)} rows) to: {alphabet_csv}\n", flush=True)

    # 2. Extract Digit Landmarks from C:\Projects\ai-hearing-speech-assistant\ml_models\datasets\Sign-Language-Digits\Dataset
    if os.path.exists(digit_dataset_dir):
        print("=== 2. Extracting DIGIT Landmarks ===", flush=True)
        digit_rows = []
        digit_classes = [d for d in os.listdir(digit_dataset_dir) if os.path.isdir(os.path.join(digit_dataset_dir, d))]
        digit_classes.sort()

        for label in digit_classes:
            class_dir = os.path.join(digit_dataset_dir, label)
            image_paths = glob.glob(os.path.join(class_dir, "*.JPG")) + glob.glob(os.path.join(class_dir, "*.jpg")) + glob.glob(os.path.join(class_dir, "*.png"))

            count = 0
            for img_path in image_paths:
                img = cv2.imread(img_path)
                if img is None:
                    continue
                img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                results = hands.process(img_rgb)
                if results.multi_hand_landmarks:
                    for hand_landmarks in results.multi_hand_landmarks:
                        landmarks = []
                        wrist = hand_landmarks.landmark[0]
                        for lm in hand_landmarks.landmark:
                            landmarks.extend([lm.x - wrist.x, lm.y - wrist.y, lm.z - wrist.z])
                        landmarks.append(label)
                        digit_rows.append(landmarks)
                        count += 1
            print(f"Digit '{label}': {count} samples.", flush=True)

        df_digit = pd.DataFrame(digit_rows, columns=col_names)
        digit_csv = r"C:\Projects\ai-hearing-speech-assistant\ml_models\asl_digit_landmarks.csv"
        df_digit.to_csv(digit_csv, index=False)
        print(f"[SUCCESS] Saved DIGIT CSV ({len(df_digit)} rows) to: {digit_csv}\n", flush=True)

    hands.close()

if __name__ == "__main__":
    extract_landmarks()
