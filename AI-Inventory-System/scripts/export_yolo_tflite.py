import os
import tensorflow as tf
from ultralytics import YOLO

def export_yolo_to_tflite():

    # 1) Exportar YOLOv8 → TensorFlow SavedModel
    model_path = "runs/detect/train4/weights/best.pt"  # Ajusta este path si tu carpeta exp cambió
    model = YOLO(model_path)

    print("\n=== EXPORTANDO YOLO a TensorFlow SavedModel ===")
    model.export(format="tf")   # → genera carpeta: runs/detect/train4/weights/best_saved_model/
    
    saved_tf = model.exported_model  # ruta generada por ultralytics

    # 2) Convertir TF → TFLite (FP32)
    print("\n=== EXPORTANDO TFLite FP32 ===")

    converter = tf.lite.TFLiteConverter.from_saved_model(saved_tf)
    tflite_fp32 = converter.convert()

    os.makedirs("runs/export", exist_ok=True)
    fp32_path = "runs/export/yolov8_fp32.tflite"

    with open(fp32_path, "wb") as f:
        f.write(tflite_fp32)

    print(f"Guardado: {fp32_path}")

    # 3) Quantize to INT8 with representative dataset
    print("\n=== GENERANDO TFLite INT8 ===")

    def representative_data_gen():
        img_dir = "dataset/images/representative"

        for fname in os.listdir(img_dir):
            path = os.path.join(img_dir, fname)

            if not fname.lower().endswith(("jpg", "jpeg", "png", "bmp", "webp")):
                continue

            img = tf.io.read_file(path)
            img = tf.image.decode_image(img, channels=3)
            img = tf.image.resize(img, [640, 640])
            img = tf.cast(img, tf.uint8)
            img = tf.expand_dims(img, 0)

            yield [img.numpy()]

    converter = tf.lite.TFLiteConverter.from_saved_model(saved_tf)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = representative_data_gen

    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8

    tflite_int8 = converter.convert()

    int8_path = "runs/export/yolov8_int8.tflite"

    with open(int8_path, "wb") as f:
        f.write(tflite_int8)

    print(f"Guardado: {int8_path}")
    print("\n=== EXPORTACIÓN COMPLETA ===")

if __name__ == "__main__":
    export_yolo_to_tflite()