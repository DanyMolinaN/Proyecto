# neuroshelf_full.py
import tkinter as tk
import ttkbootstrap as ttk
from ttkbootstrap.constants import *
from tkinter import filedialog, messagebox, simpledialog
import os
import shutil
import subprocess
import threading
import sys
import time
import platform

# =======================
# RUTAS PRINCIPALES
# =======================
BASE_DIR = r"C:\Users\HP VICTUS\Documents\Inteligencia Artificial\Proyecto\AI-Inventory-System"
SCRIPTS_DIR = os.path.join(BASE_DIR, "scripts")

# YOLO scripts (asegúrate que los nombres coincidan)
SCRIPT_TRAIN_YOLO = os.path.join(SCRIPTS_DIR, "train_yolo.py")
SCRIPT_EXPORT_YOLO = os.path.join(SCRIPTS_DIR, "export_yolo_tflite.py")

# Face scripts
SCRIPT_FACE_TRAIN = os.path.join(SCRIPTS_DIR, "face_train_embeddings.py")
SCRIPT_FACE_REALTIME = os.path.join(SCRIPTS_DIR, "face_recognize_realtime.py")
SCRIPT_FACE_IMAGES = os.path.join(SCRIPTS_DIR, "face_recognize.py")

# Dataset paths
DATASET_TRAIN_DIR = os.path.join(BASE_DIR, "dataset", "images", "train")
REPRESENTATIVE_DIR = os.path.join(BASE_DIR, "dataset", "images", "representative")
FACE_DATASET_DIR = os.path.join(BASE_DIR, "dataset", "faces")

# Asegurar estructura
os.makedirs(DATASET_TRAIN_DIR, exist_ok=True)
os.makedirs(REPRESENTATIVE_DIR, exist_ok=True)
os.makedirs(FACE_DATASET_DIR, exist_ok=True)


# =======================
# UTIL: abrir carpeta en SO
# =======================
def open_folder_in_explorer(path):
    path = os.path.abspath(path)
    if not os.path.exists(path):
        messagebox.showwarning("Carpeta no existe", f"No existe la carpeta:\n{path}")
        return
    try:
        if platform.system() == "Windows":
            os.startfile(path)
        elif platform.system() == "Darwin":
            subprocess.Popen(["open", path])
        else:
            subprocess.Popen(["xdg-open", path])
    except Exception as e:
        messagebox.showerror("Error", f"No se pudo abrir la carpeta:\n{e}")


# =======================
# VENTANA: OBJET DETECTION / YOLO
# =======================
class ObjectDetectionWindow:
    def __init__(self, parent):
        self.win = ttk.Toplevel(parent)
        self.win.title("Entrenamiento y Exportación YOLO - NeuroShelf")
        self.win.geometry("840x620")
        self.win.resizable(False, False)

        ttk.Label(self.win, text="YOLO — Entrenamiento y Exportación", font=("Segoe UI Black", 18), bootstyle="info").pack(pady=10)

        # Acciones (arriba)
        actions = ttk.Frame(self.win, padding=10)
        actions.pack(fill="x")

        ttk.Button(actions, text="📥 Agregar imágenes (múltiples)", bootstyle="warning",
                   command=self.add_images).grid(row=0, column=0, padx=6, pady=6)

        ttk.Button(actions, text="📂 Abrir carpeta dataset", bootstyle="secondary-outline",
                   command=lambda: open_folder_in_explorer(DATASET_TRAIN_DIR)).grid(row=0, column=1, padx=6, pady=6)

        ttk.Button(actions, text="🧾 Abrir carpeta representativa", bootstyle="secondary-outline",
                   command=lambda: open_folder_in_explorer(REPRESENTATIVE_DIR)).grid(row=0, column=2, padx=6, pady=6)

        # Contador
        self.count_var = tk.StringVar()
        self.update_dataset_count()
        ttk.Label(actions, textvariable=self.count_var).grid(row=0, column=3, padx=12)

        ttk.Separator(self.win, orient="horizontal").pack(fill="x", pady=8)

        # Botones entrenar/exportar
        ops = ttk.Frame(self.win, padding=10)
        ops.pack(fill="x")

        ttk.Button(ops, text="🧠 Iniciar Entrenamiento (train_yolo.py)", bootstyle="success",
                   command=self.start_training).grid(row=0, column=0, padx=8, pady=8)

        ttk.Button(ops, text="📦 Exportar TFLite (export_yolo_tflite.py)", bootstyle="primary",
                   command=self.start_export).grid(row=0, column=1, padx=8, pady=8)

        ttk.Button(ops, text="🔍 Detección con Cámara (mostrar con OpenCV)", bootstyle="info-outline",
                   command=self.start_camera_detection).grid(row=0, column=2, padx=8, pady=8)

        ttk.Separator(self.win, orient="horizontal").pack(fill="x", pady=8)

        # Consola logs
        console_frame = ttk.Frame(self.win, padding=10)
        console_frame.pack(fill="both", expand=True)

        self.log_box = tk.Text(console_frame, height=22, width=100, bg="#0f1720", fg="#d0ffe0")
        self.log_box.pack(side="left", fill="both", expand=True)

        scrollbar = ttk.Scrollbar(console_frame, command=self.log_box.yview)
        scrollbar.pack(side="right", fill="y")
        self.log_box.config(yscrollcommand=scrollbar.set)

    # agregar varias imágenes al dataset train
    def add_images(self):
        files = filedialog.askopenfilenames(title="Seleccionar imágenes", filetypes=[("Imágenes", "*.jpg *.jpeg *.png *.bmp *.webp")])
        if not files:
            return

        def task():
            added = 0
            for f in files:
                try:
                    fname = os.path.basename(f)
                    dest = os.path.join(DATASET_TRAIN_DIR, fname)
                    if os.path.exists(dest):
                        base, ext = os.path.splitext(fname)
                        dest = os.path.join(DATASET_TRAIN_DIR, f"{base}_{int(time.time())}{ext}")
                    shutil.copy2(f, dest)
                    self.log(f"✔ Copiado: {fname}")
                    added += 1
                except Exception as e:
                    self.log(f"✖ Error copiando {f}: {e}")
            self.log(f"\n[OK] {added} imágenes agregadas al dataset.")
            self.update_dataset_count()

        threading.Thread(target=task, daemon=True).start()

    def update_dataset_count(self):
        try:
            files = [f for f in os.listdir(DATASET_TRAIN_DIR) if f.lower().endswith((".jpg", ".jpeg", ".png", ".bmp", ".webp"))]
            self.count_var.set(f"Imágenes en dataset: {len(files)}")
        except Exception:
            self.count_var.set("Imágenes en dataset: (error)")

    # ejecutar script de entrenamiento
    def start_training(self):
        if not os.path.exists(SCRIPT_TRAIN_YOLO):
            messagebox.showerror("Script no encontrado", f"No encontré: {SCRIPT_TRAIN_YOLO}")
            return
        self.log("\n🚀 Iniciando entrenamiento (train_yolo.py)...")
        threading.Thread(target=self._run_script_capture, args=(SCRIPT_TRAIN_YOLO,), daemon=True).start()

    # ejecutar script de export
    def start_export(self):
        if not os.path.exists(SCRIPT_EXPORT_YOLO):
            messagebox.showerror("Script no encontrado", f"No encontré: {SCRIPT_EXPORT_YOLO}")
            return
        self.log("\n📦 Iniciando exportación (export_yolo_tflite.py)...")
        threading.Thread(target=self._run_script_capture, args=(SCRIPT_EXPORT_YOLO,), daemon=True).start()

    # iniciar detección con cámara (simple wrapper que ejecuta un script o implementa captura local)
    def start_camera_detection(self):
        # Si prefieres ejecutar un script externo para la detección en tiempo real,
        # crea un script y cámbialo aquí. Mientras tanto, intentamos abrir OpenCV con YOLO si existe.
        # Para mantenerlo simple: llamamos al script de entrenamiento si existiera un script de demo.
        self.log("\n🎥 Iniciando detección con cámara (llamando script si existe)...")
        # Example: if you had a script 'yolo_camera_demo.py' you could run it
        demo_script = os.path.join(SCRIPTS_DIR, "yolo_camera_demo.py")
        if os.path.exists(demo_script):
            threading.Thread(target=self._run_script_capture, args=(demo_script,), daemon=True).start()
        else:
            messagebox.showinfo("Info", "No encontré 'yolo_camera_demo.py'. Puedes crear un script que haga la inferencia en cámara y colocarlo en scripts/ with that name.")

    # función genérica para ejecutar script y capturar stdout -> log_box
    def _run_script_capture(self, script_path):
        try:
            cmd = [sys.executable, script_path]
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        except Exception as e:
            self.log(f"✖ Error iniciando script: {e}")
            return

        try:
            for line in proc.stdout:
                self.log(line.rstrip("\n"))
        except Exception as e:
            self.log(f"✖ Error leyendo salida: {e}")

        proc.wait()
        self.log(f"\n🏁 Script finalizado con código: {proc.returncode}\n")


# =======================
# VENTANA: RECONOCIMIENTO FACIAL
# =======================
class FaceRecognitionWindow:
    def __init__(self, parent):
        self.win = ttk.Toplevel(parent)
        self.win.title("Reconocimiento Facial - NeuroShelf")
        self.win.geometry("900x650")
        self.win.resizable(False, False)

        ttk.Label(self.win, text="Módulo de Reconocimiento Facial", font=("Segoe UI Black", 22), bootstyle="info").pack(pady=10)

        frame = ttk.Frame(self.win, padding=20)
        frame.pack()

        ttk.Button(frame, text="📥 Subir imágenes por persona", bootstyle="warning", width=35,
                   command=self.upload_face_images).grid(row=0, column=0, padx=10, pady=10)

        ttk.Button(frame, text="🧠 Entrenar embeddings", bootstyle="success", width=30,
                   command=self.train_embeddings).grid(row=0, column=1, padx=10, pady=10)

        ttk.Button(frame, text="🎥 Reconocimiento en Tiempo Real", bootstyle="primary", width=35,
                   command=self.recognize_realtime).grid(row=1, column=0, padx=10, pady=10)

        ttk.Button(frame, text="🖼 Reconocer Imagen (por archivo)", bootstyle="info", width=30,
                   command=self.recognize_from_images).grid(row=1, column=1, padx=10, pady=10)

        console_frame = ttk.Frame(self.win)
        console_frame.pack(fill="both", expand=True, pady=15)

        self.log_box = tk.Text(console_frame, height=20, width=110, bg="#0f1720", fg="#d0ffe0")
        self.log_box.pack(side="left", fill="both", expand=True)

        scrollbar = ttk.Scrollbar(console_frame, command=self.log_box.yview)
        scrollbar.pack(side="right", fill="y")
        self.log_box.config(yscrollcommand=scrollbar.set)

    def upload_face_images(self):
        name = simpledialog.askstring("Nombre", "Ingresa el nombre de la persona:")
        if not name:
            return

        person_folder = os.path.join(FACE_DATASET_DIR, name)
        os.makedirs(person_folder, exist_ok=True)

        files = filedialog.askopenfilenames(title="Seleccionar imágenes de la persona", filetypes=[("Imágenes", "*.jpg *.jpeg *.png")])
        if not files:
            return

        def task():
            added = 0
            for f in files:
                try:
                    dest = os.path.join(person_folder, os.path.basename(f))
                    if os.path.exists(dest):
                        base, ext = os.path.splitext(os.path.basename(f))
                        dest = os.path.join(person_folder, f"{base}_{int(time.time())}{ext}")
                    shutil.copy2(f, dest)
                    self.log(f"✔ Copiado: {f}")
                    added += 1
                except Exception as e:
                    self.log(f"✖ Error copiando {f}: {e}")
            self.log(f"\n[OK] {added} imágenes agregadas a {name}")

        threading.Thread(target=task, daemon=True).start()

    def train_embeddings(self):
        if not os.path.exists(SCRIPT_FACE_TRAIN):
            messagebox.showerror("Script no encontrado", f"No encontré: {SCRIPT_FACE_TRAIN}")
            return
        self.log("\n🚀 Iniciando entrenamiento de embeddings...")
        threading.Thread(target=self._run_script_capture, args=(SCRIPT_FACE_TRAIN,), daemon=True).start()

    def recognize_realtime(self):
        if not os.path.exists(SCRIPT_FACE_REALTIME):
            messagebox.showerror("Script no encontrado", f"No encontré: {SCRIPT_FACE_REALTIME}")
            return
        self.log("\n🎥 Lanzando reconocimiento en tiempo real (abrirá la cámara)...")
        # Lanza el script en un proceso separado; la lectura de stdout puede bloquear si el script usa OpenCV GUI.
        try:
            subprocess.Popen([sys.executable, SCRIPT_FACE_REALTIME])
            self.log("✔ Script de reconocimiento en tiempo real lanzado.")
        except Exception as e:
            self.log(f"✖ Error lanzando script realtime: {e}")

    def recognize_from_images(self):
        if not os.path.exists(SCRIPT_FACE_IMAGES):
            messagebox.showerror("Script no encontrado", f"No encontré: {SCRIPT_FACE_IMAGES}")
            return

        files = filedialog.askopenfilenames(title="Seleccionar imágenes", filetypes=[("Imágenes", "*.jpg *.jpeg *.png")])
        if not files:
            return

        self.log("\n🖼 Procesando imágenes para reconocimiento...")
        threading.Thread(target=self._run_script_capture_with_args, args=(SCRIPT_FACE_IMAGES, list(files)), daemon=True).start()

    def _run_script_capture(self, script_path):
        try:
            cmd = [sys.executable, script_path]
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        except Exception as e:
            self.log(f"✖ Error iniciando script: {e}")
            return
        try:
            for line in proc.stdout:
                self.log(line.rstrip("\n"))
        except Exception as e:
            self.log(f"✖ Error leyendo stdout: {e}")
        proc.wait()
        self.log(f"\n🏁 Script finalizado con código: {proc.returncode}\n")

    def _run_script_capture_with_args(self, script_path, args_list):
        try:
            cmd = [sys.executable, script_path] + args_list
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        except Exception as e:
            self.log(f"✖ Error iniciando script con args: {e}")
            return
        try:
            for line in proc.stdout:
                self.log(line.rstrip("\n"))
        except Exception as e:
            self.log(f"✖ Error leyendo stdout: {e}")
        proc.wait()
        self.log(f"\n🏁 Script finalizado con código: {proc.returncode}\n")

    def log(self, text):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        self.log_box.insert("end", f"[{timestamp}] {text}\n")
        self.log_box.see("end")


# =======================
# APLICACIÓN PRINCIPAL
# =======================
class NeuroShelfApp:
    def __init__(self, root):
        self.root = root
        self.root.title("NeuroShelf - AI Control Panel")
        self.root.geometry("920x640")
        self.root.resizable(False, False)

        style = ttk.Style("darkly")

        main_frame = ttk.Frame(self.root, padding=30)
        main_frame.pack(fill="both", expand=True)

        ttk.Label(main_frame, text="NeuroShelf AI System", font=("Segoe UI Black", 30), bootstyle="info").pack(pady=8)
        ttk.Label(main_frame, text="Computer Vision Control Panel", font=("Segoe UI", 14)).pack(pady=4)

        btn_frame = ttk.Frame(main_frame)
        btn_frame.pack(pady=18)

        ttk.Button(btn_frame, text="🧠 Entrenamiento y Detección de Objetos", bootstyle="primary-outline", width=36,
                   command=self.open_object_detection).grid(row=0, column=0, padx=8, pady=8)

        ttk.Button(btn_frame, text="🙂 Reconocimiento Facial", bootstyle="success-outline", width=36,
                   command=self.open_face_recognition).grid(row=0, column=1, padx=8, pady=8)

        ttk.Button(btn_frame, text="🚪 Salir", bootstyle="danger-outline", width=16, command=self.root.quit).grid(row=0, column=2, padx=8, pady=8)

        # asegurar carpetas
        os.makedirs(DATASET_TRAIN_DIR, exist_ok=True)
        os.makedirs(REPRESENTATIVE_DIR, exist_ok=True)
        os.makedirs(FACE_DATASET_DIR, exist_ok=True)

    def open_object_detection(self):
        ObjectDetectionWindow(self.root)

    def open_face_recognition(self):
        FaceRecognitionWindow(self.root)


# =======================
# EJECUCIÓN
# =======================
if __name__ == "__main__":
    root = ttk.Window(themename="superhero")
    app = NeuroShelfApp(root)
    root.mainloop()
