import mediapipe as mp
import cv2
import json
import numpy as np
import socket
import struct
import threading
import time

# run from the scripts/ directory, or adjust this path
model_path = "../src/main/resources/assets/pose_landmarker_heavy.task"

BaseOptions = mp.tasks.BaseOptions
PoseLandmarker = mp.tasks.vision.PoseLandmarker
PoseLandmarkerOptions = mp.tasks.vision.PoseLandmarkerOptions
PoseLandmarkerResult = mp.tasks.vision.PoseLandmarkerResult
VisionRunningMode = mp.tasks.vision.RunningMode

HOST = "127.0.0.1"
PORT = 5001

latest_result = None
result_lock = threading.Lock()

'''
Landmark index reference (left/right from user's perspective):

0  - nose
1  - left eye (inner)    4  - right eye (inner)
2  - left eye            5  - right eye
3  - left eye (outer)    6  - right eye (outer)
7  - left ear            8  - right ear
9  - mouth (left)        10 - mouth (right)
11 - left shoulder       12 - right shoulder
13 - left elbow          14 - right elbow
15 - left wrist          16 - right wrist
17 - left pinky          18 - right pinky
19 - left index          20 - right index
21 - left thumb          22 - right thumb
23 - left hip            24 - right hip
25 - left knee           26 - right knee
27 - left ankle          28 - right ankle
29 - left heel           30 - right heel
31 - left foot index     32 - right foot index
'''


def store_result(result: PoseLandmarkerResult, output_image: mp.Image, timestamp_ms: int):
    global latest_result
    with result_lock:
        latest_result = result

    # print all landmark coords to console so you can verify detection is working
    if result and result.pose_landmarks:
        for pose in result.pose_landmarks:
            for i, lm in enumerate(pose):
                print(f"  [{i:2d}] x={lm.x:.4f}  y={lm.y:.4f}  z={lm.z:.4f}  vis={lm.visibility:.4f}")
        print()


def landmarks_to_json(result):
    # returns a JSON array of landmark objects, or "null" if nothing detected
    if not result or not result.pose_landmarks:
        return "null"
    # only the first detected pose for now
    pose = result.pose_landmarks[0]
    lms = [
        {"x": lm.x, "y": lm.y, "z": lm.z, "visibility": lm.visibility}
        for lm in pose
    ]
    # compact separators are required - the Java regex expects no spaces around : or ,
    return json.dumps(lms, separators=(',', ':'))


def run_server(landmarker):
    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_sock.bind((HOST, PORT))
    server_sock.listen(1)
    print(f"[pose] server ready on {HOST}:{PORT} - waiting for Java app...")

    conn, addr = server_sock.accept()
    print(f"[pose] connected: {addr}")

    try:
        while True:
            # read the 4-byte big-endian length prefix
            header = b""
            while len(header) < 4:
                chunk = conn.recv(4 - len(header))
                if not chunk:
                    return
                header += chunk
            msg_len = struct.unpack(">I", header)[0]

            # read exactly msg_len bytes for the JPEG frame
            data = b""
            while len(data) < msg_len:
                chunk = conn.recv(min(65536, msg_len - len(data)))
                if not chunk:
                    return
                data += chunk

            # decode JPEG and run async detection
            np_arr = np.frombuffer(data, dtype=np.uint8)
            frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            if frame is not None:
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame_rgb)
                # timestamp just needs to be strictly increasing for live stream mode
                timestamp_ms = int(time.monotonic() * 1000)
                landmarker.detect_async(mp_image, timestamp_ms)

            # send back the most recent result - may lag one frame, that's fine
            with result_lock:
                response = landmarks_to_json(latest_result)

            response_bytes = response.encode("utf-8")
            conn.sendall(struct.pack(">I", len(response_bytes)) + response_bytes)

    except Exception as e:
        print(f"[pose] error: {e}")
    finally:
        conn.close()
        server_sock.close()
        print("[pose] server closed")


options = PoseLandmarkerOptions(
    base_options=BaseOptions(model_asset_path=model_path),
    running_mode=VisionRunningMode.LIVE_STREAM,
    result_callback=store_result
)

with PoseLandmarker.create_from_options(options) as landmarker:
    run_server(landmarker)