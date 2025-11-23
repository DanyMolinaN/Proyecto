import os
import json
import torch
from PIL import Image
from facenet_pytorch import MTCNN, InceptionResnetV1

# Inicializar detector y modelo FaceNet
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print("Using device:", device)

mtcnn = MTCNN(image_size=160, margin=20, device=device)
model = InceptionResnetV1(pretrained="vggface2").eval().to(device)

# Directorio de dataset
DATASET_DIR = "dataset/faces"   # personas -> imagenes
OUTPUT_JSON = "face_embeddings.json"

# Función para procesar una imagen
def get_embedding(image_path):
    try:
        img = Image.open(image_path).convert("RGB")

        face = mtcnn(img)
        if face is None:
            print(f"[WARN] No face detected in {image_path}")
            return None

        face = face.unsqueeze(0).to(device)
        embedding = model(face).detach().cpu().numpy().flatten().tolist()
        return embedding

    except Exception as e:
        print(f"[ERROR] Failed processing {image_path}: {e}")
        return None

# Recorrer dataset y generar embeddings
def main():
    embeddings_dict = {}

    persons = os.listdir(DATASET_DIR)
    persons = [p for p in persons if os.path.isdir(os.path.join(DATASET_DIR, p))]

    print(f"Found persons: {persons}")

    for person_id in persons:
        person_path = os.path.join(DATASET_DIR, person_id)
        images = os.listdir(person_path)

        person_embeddings = []

        for img_name in images:
            img_path = os.path.join(person_path, img_name)

            emb = get_embedding(img_path)
            if emb is not None:
                person_embeddings.append(emb)

        if len(person_embeddings) == 0:
            print(f"[WARN] No usable images for {person_id}")
            continue

        # promedio de embeddings por persona
        avg_emb = torch.tensor(person_embeddings).mean(dim=0).tolist()
        embeddings_dict[person_id] = avg_emb

        print(f"[INFO] {person_id}: {len(person_embeddings)} embeddings added.")

    # Guardar JSON final
    with open(OUTPUT_JSON, "w") as f:
        json.dump(embeddings_dict, f, indent=2)

    print(f"\n[SAVED] Embeddings saved to {OUTPUT_JSON}")


if __name__ == "__main__":
    main()
