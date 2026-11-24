"""Cuts the store logo and the in-game mod icon from the master artwork.

The master (curseforge/logo-master.png, 1024x1024) is an illustration made with an image model from the prompt in
curseforge/logo-prompt.txt, with the rounded frame it came with cropped off. Replace the master to change both.

Run from the project root:  python tools/make_logo.py
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
master = Image.open(os.path.join(ROOT, "curseforge", "logo-master.png")).convert("RGB")
master.resize((512, 512), Image.LANCZOS).save(os.path.join(ROOT, "curseforge", "logo.png"))
master.resize((256, 256), Image.LANCZOS).save(os.path.join(ROOT, "src", "main", "resources", "assets", "fromthemist", "icon.png"))
print("logo.png (512) and icon.png (256) written")
