# scripts/train_yolo.py
from ultralytics import YOLO

def main():
    # Carga pesos base (yolov8n es el más rápido)
    model = YOLO("yolov8n.pt")

    # Entrenamiento
    model.train(
        data="./AI-Inventory-System/data.yaml",      # Ruta al archivo de configuración del dataset
        epochs=100,
        imgsz=640,
        batch=16,
        workers=4,
        device="cpu",              # GPU = 0, CPU = "cpu"
        patience=20,           # Early stopping
        cos_lr=True,           # Cosine learning rate
        pretrained=True
    )

    print("Entrenamiento terminado. Revisa 'runs/detect/train/' para los resultados.")


if __name__ == "__main__":
    main()