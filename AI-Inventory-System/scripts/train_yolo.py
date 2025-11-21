# scripts/train_yolo.py
from ultralytics import YOLO

# Configura rutas dataset en formato YOLO (images, labels) y datos YAML
# data.yaml ejemplo:
# train: ../dataset/images/train
# val: ../dataset/images/val
# names: ['product1', 'product2', ...]

model = YOLO('yolov8n.pt')  # base weights; también puedes iniciar desde yolov8n.yaml
model.train(data='data.yaml', epochs=100, imgsz=640, batch=16, imgsz_t=640)
# El mejor .pt quedará en runs/train/exp...
