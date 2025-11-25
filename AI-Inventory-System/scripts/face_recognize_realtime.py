import cv2
import json
import torch
import numpy as np
from facenet_pytorch import MTCNN, InceptionResnetV1
from PIL import Image

# Cargar embeddings entrenados
with open("face_embeddings.json", "r") as f:
    embeddings_dict = json.load(f)

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
mtcnn = MTCNN(image_size=160, margin=20, device=device)
model = InceptionResnetV1(pretrained="vggface2").eval().to(device)

def recognize_face(frame, threshold=0.9):
    # Convertir frame de OpenCV (BGR) a PIL (RGB)
    img = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    face = mtcnn(img)
    if face is None:
        return None, None

    face = face.unsqueeze(0).to(device)
    emb = model(face).detach().cpu().numpy().flatten()

    min_dist = float("inf")
    identity = "Desconocido"

    for person, ref_emb in embeddings_dict.items():
        dist = torch.dist(torch.tensor(emb), torch.tensor(ref_emb)).item()
        if dist < min_dist:
            min_dist = dist
            identity = person

    if min_dist > threshold:
        identity = "Desconocido"

    return identity, min_dist

def main():
    cap = cv2.VideoCapture(0)  # abrir cámara
    print("[INFO] Presiona 'q' para salir")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        identity, dist = recognize_face(frame)
        if identity is not None:
            text = f"{identity} ({dist:.2f})"
            cv2.putText(frame, text, (20, 40),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

        cv2.imshow("Face Recognition", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()
