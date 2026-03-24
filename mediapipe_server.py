import cv2
import mediapipe as mp
import sys

mp_hands = mp.solutions.hands
hands = mp_hands.Hands()

cap = cv2.VideoCapture(0)

while cap.isOpened():
    success, image = cap.read()
    if not success: break

    # Process the image
    results = hands.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))

    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            # Print X,Y of the tip of the index finger (Point 8)
            print(f"{hand_landmarks.landmark[8].x},{hand_landmarks.landmark[8].y}")
            sys.stdout.flush() # Forces Java to see the data immediately

cap.release()
