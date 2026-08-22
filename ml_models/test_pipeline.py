import cv2
import mediapipe as mp
import numpy as np

def test_mediapipe_landmarks():
    print("Testing MediaPipe Hands & Face Detection setup...")
    mp_hands = mp.solutions.hands
    mp_face_mesh = mp.solutions.face_mesh

    hands = mp_hands.Hands(static_image_mode=True, max_num_hands=2)
    face_mesh = mp_face_mesh.FaceMesh(static_image_mode=True)

    # Create a blank dummy RGB image (300x300 pixels)
    blank_image = np.zeros((300, 300, 3), dtype=np.uint8)

    # Process through pipeline
    hand_results = hands.process(blank_image)
    face_results = face_mesh.process(blank_image)

    print("[SUCCESS] MediaPipe initialized successfully!")
    print("Hand Landmarks Pipeline: READY")
    print("Face Landmarks Pipeline: READY")

if __name__ == "__main__":
    test_mediapipe_landmarks()
