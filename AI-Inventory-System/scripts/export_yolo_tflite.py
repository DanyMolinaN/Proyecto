from ultralytics import YOLO
import tensorflow as tf
import numpy as np
import os

def export_yolo_all():
    model_path = "runs/detect/train/weights/best.pt"
    model = YOLO(model_path)

    print("\n=== EXPORTANDO YOLO a TFLite (FP32, FP16, INT8) ===")

    # ============================================================
    # 1) EXPORTAR FP32 y FP16 con Ultralytics
    # ============================================================
    print("\n➡ Exportando TFLite FP32...")
    fp32_path = model.export(format="tflite")  # Float32
    print(f"✔ Modelo FP32 guardado en: {fp32_path}")

    print("\n➡ Exportando TFLite FP16...")
    fp16_path = model.export(format="tflite", half=True)
    print(f"✔ Modelo FP16 guardado en: {fp16_path}")

    # ============================================================
    # 2) Crear firma en SavedModel para INT8
    # ============================================================
    saved_model_dir = "runs/detect/train/weights/best_saved_model"
    model_tf = tf.saved_model.load(saved_model_dir)

    @tf.function(input_signature=[tf.TensorSpec(shape=[1, 640, 640, 3], dtype=tf.float32)])
    def serve_fn(input_tensor):
        outputs = model_tf(input_tensor)
        return {"outputs": outputs}

    signed_model_dir = "runs/detect/train/weights/best_saved_model_signed"
    tf.saved_model.save(model_tf, signed_model_dir, signatures={"serving_default": serve_fn})
    print(f"✔ Nuevo SavedModel con firma guardado en: {signed_model_dir}")

    # ============================================================
    # 3) Preparar dataset representativo
    # ============================================================
    rep_data_dir = "./AI-Inventory-System/dataset/images/representative"
    rep_imgs = [
        os.path.join(rep_data_dir, f)
        for f in os.listdir(rep_data_dir)
        if f.lower().endswith((".jpg", ".jpeg", ".png"))
    ]

    def representative_dataset():
        for img_path in rep_imgs:
            img = tf.io.read_file(img_path)
            img = tf.io.decode_image(img, channels=3)
            img = tf.image.resize(img, (640, 640))
            img = tf.cast(img, tf.float32) / 255.0
            img = tf.expand_dims(img, 0)
            yield [img]

    print(f"✔ {len(rep_imgs)} imágenes representativas cargadas")

    # ============================================================
    # 4) Convertir a INT8
    # ============================================================
    print("\n➡ Convirtiendo a TFLite INT8...")
    converter = tf.lite.TFLiteConverter.from_saved_model(signed_model_dir)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = representative_dataset
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8

    int8_tflite = converter.convert()
    out_path = "runs/detect/train/weights/best_int8.tflite"
    with open(out_path, "wb") as f:
        f.write(int8_tflite)

    print(f"✔ Modelo INT8 guardado en: {out_path}")
    print("\n=== EXPORTACIÓN COMPLETA ===")

if __name__ == "__main__":
    export_yolo_all()