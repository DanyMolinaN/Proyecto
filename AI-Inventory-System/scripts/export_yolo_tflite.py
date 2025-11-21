# scripts/export_yolo_tflite.py
from ultralytics import YOLO
import tensorflow as tf
import numpy as np
import os

# 1) Export using ultralytics
yolo = YOLO('runs/train/exp/weights/best.pt')
yolo.export(format='onnx', imgsz=640)  # produce best.onnx in runs/export/...

onnx_path = 'runs/export/best.onnx'
saved_tf = 'runs/export/tf_saved_model'

# 2) Convert ONNX -> SavedModel (optional) using onnx-tf or directly use TF converter.
# Simpler: let ultralytics export to tflite directly if supported:
yolo.export(format='tflite', imgsz=640, dynamic=False)  # uses tflite export, check ultralytics version

# If manual conversion needed: ONNX -> TFLite pipeline (requires onnxruntime, tf2onnx, etc.)
# 3) Quantize to INT8 with representative dataset
tflite_path = 'runs/export/best.tflite'

# Representative dataset generator
def representative_data_gen():
    # iterate over a set of representative images preprocessed to model input
    img_dir = 'dataset/images/representative'
    for fname in os.listdir(img_dir):
        path = os.path.join(img_dir, fname)
        img = tf.io.read_file(path)
        img = tf.image.decode_image(img, channels=3)
        img = tf.image.resize(img, [640, 640])
        img = tf.cast(img, tf.uint8)
        img = tf.expand_dims(img, 0)
        yield [img.numpy()]

# Example quantization using TFLiteConverter from SavedModel
converter = tf.lite.TFLiteConverter.from_saved_model(saved_tf)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8
tflite_quant = converter.convert()
open('yolov8n_int8.tflite', 'wb').write(tflite_quant)
print("Saved yolov8n_int8.tflite")
