import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
import cv2

model_path = "pose_landmarker_full.task"

BaseOptions = mp.tasks.BaseOptions
PoseLandmarker = mp.tasks.vision.PoseLandmarker
PoseLandmarkerOptions = mp.tasks.vision.PoseLandmarkerOptions
PoseLandmarkerResult = mp.tasks.vision.PoseLandmarkerResult
VisionRunningMode = mp.tasks.vision.RunningMode

# Pose connections (pairs of landmark indices to draw lines between)
# This is purely for visualisation and debugging to make sure it's detecting positions accurately
POSE_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 7),
    (0, 4), (4, 5), (5, 6), (6, 8),
    (9, 10),
    (11, 12), (11, 13), (13, 15), (15, 17), (15, 19), (15, 21), (17, 19),
    (12, 14), (14, 16), (16, 18), (16, 20), (16, 22), (18, 20),
    (11, 23), (12, 24), (23, 24),
    (23, 25), (25, 27), (27, 29), (27, 31), (29, 31),
    (24, 26), (26, 28), (28, 30), (28, 32), (30, 32),
]

# Left side landmarks (drawn in green), right side in red
LEFT_LANDMARKS = {1, 2, 3, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31}
RIGHT_LANDMARKS = {4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32}

latest_result = None


def store_result(result: PoseLandmarkerResult, output_image: mp.Image, timestamp_ms: int):
    global latest_result
    latest_result = result


def draw_landmarks(frame, result: PoseLandmarkerResult):
    if not result or not result.pose_landmarks:
        return frame

    annotated = frame.copy()
    h, w = annotated.shape[:2]

    for pose_landmarks in result.pose_landmarks:
        # Convert normalised coords to pixel coords
        points = [
            (int(lm.x * w), int(lm.y * h))
            for lm in pose_landmarks
        ]

        # Draw connections
        for start_idx, end_idx in POSE_CONNECTIONS:
            if start_idx < len(points) and end_idx < len(points):
                cv2.line(annotated, points[start_idx], points[end_idx], (200, 200, 200), 2)

        # Draw landmark dots
        for idx, point in enumerate(points):
            if idx in LEFT_LANDMARKS:
                color = (0, 255, 0)    # Green for left
            elif idx in RIGHT_LANDMARKS:
                color = (0, 0, 255)    # Red for right
            else:
                color = (255, 255, 255)  # White for centre

            cv2.circle(annotated, point, 5, color, -1)
            cv2.circle(annotated, point, 6, (0, 0, 0), 1)  # Black outline

    return annotated


options = PoseLandmarkerOptions(
    base_options=BaseOptions(model_asset_path=model_path),
    running_mode=VisionRunningMode.LIVE_STREAM,
    result_callback=store_result
)

cap = cv2.VideoCapture(0)

with PoseLandmarker.create_from_options(options) as landmarker:
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            print("Failed to grab frame.")
            break

        frame_timestamp_ms = int(cap.get(cv2.CAP_PROP_POS_MSEC))

        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame)
        landmarker.detect_async(mp_image, frame_timestamp_ms)

        annotated_frame = draw_landmarks(frame, latest_result)

        cv2.imshow("Pose Landmarker", annotated_frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

cap.release()
cv2.destroyAllWindows()