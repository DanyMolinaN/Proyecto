import json
import torch
from PIL import Image
from facenet_pytorch import MTCNN, InceptionResnetV1

# Cargar embeddings entrenados
with open("face_embeddings.json", "r") as f:
    embeddings_dict = json.load(f)

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
mtcnn = MTCNN(image_size=160, margin=20, device=device)
model = InceptionResnetV1(pretrained="vggface2").eval().to(device)

def recognize_face(image_path, threshold=0.9):
    img = Image.open(image_path).convert("RGB")
    face = mtcnn(img)
    if face is None:
        print("No face detected")
        return None

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

    print(f"Reconocido como: {identity} (distancia={min_dist:.4f})")
    return identity

# Ejemplo de uso
recognize_face("test_face.jpeg")