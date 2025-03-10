from flask import Flask, request, jsonify
import tensorflow as tf
import numpy as np
import cv2
import librosa
from tensorflow.keras.preprocessing.image import img_to_array
from tensorflow.keras.models import load_model
import os

app = Flask(__name__)

# Load pre-trained models
image_model = load_model('xception_deepfake_detection.h5')
video_model = load_model('deepfake_detector_video4.h5')
audio_model = load_model('deepfake_audio_detection_model.h5')

# Function to extract MFCC features from an audio file
def extract_audio_features(file_name):
    audio, sample_rate = librosa.load(file_name, sr=22050)
    mfccs = librosa.feature.mfcc(y=audio, sr=sample_rate, n_mfcc=40)
    mfccs = np.mean(mfccs.T, axis=0)
    return mfccs

# Image deepfake prediction
@app.route('/predict/image', methods=['POST'])
def predict_image():
    image = request.files['file'].read()
    npimg = np.fromstring(image, np.uint8)
    img = cv2.imdecode(npimg, cv2.IMREAD_COLOR)
    img = cv2.resize(img, (299, 299))
    img = img_to_array(img) / 255.0
    img = np.expand_dims(img, axis=0)

    prediction = image_model.predict(img)[0][0]
    result = 'Fake' if prediction > 0.5 else 'Real'
    return jsonify({'prediction': result})

# Video deepfake prediction
def extract_frames(video_path, sequence_length=20):
    cap = cv2.VideoCapture(video_path)
    frames = []
    while len(frames) < sequence_length:
        ret, frame = cap.read()
        if not ret:
            break
        frame = cv2.resize(frame, (160, 160))
        frames.append(img_to_array(frame) / 255.0)
    cap.release()
    return np.array(frames)

@app.route('/predict/video', methods=['POST'])
def predict_video():
    video = request.files['file']
    video_path = 'temp_video.mp4'
    video.save(video_path)

    frames = extract_frames(video_path)
    frames = np.expand_dims(frames, axis=0)

    prediction = video_model.predict(frames)[0][0]
    result = 'Fake' if prediction > 0.5 else 'Real'
    os.remove(video_path)
    return jsonify({'prediction': result})

# Audio deepfake prediction
@app.route('/predict/audio', methods=['POST'])
def predict_audio():
    audio_file = request.files['file']
    audio_path = 'temp_audio.wav'
    audio_file.save(audio_path)

    features = extract_audio_features(audio_path)
    features = features.reshape(1, features.shape[0], 1)  # Reshape to match input shape
    prediction = audio_model.predict(features)[0][0]
    result = 'Fake' if prediction > 0.5 else 'Real'
    os.remove(audio_path)
    return jsonify({'prediction': result})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
