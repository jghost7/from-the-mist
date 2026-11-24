"""Draws the mod's logo as pixel art: a lit cabin with the talisman over its door, a figure standing outside,
and the legs of something enormous coming down through the mist behind it.

Writes curseforge/logo.png (512x512, for store listings) and the in-game mod icon (128x128).
Run from the project root:  python tools/make_logo.py
"""
import os
import random

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
SIZE = 64
rnd = random.Random(3)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


MIST_TOP = (196, 202, 206)
MIST_LOW = (108, 118, 126)
DARK = (22, 25, 30)

# ---- mist: pale above, heavier toward the ground, with a little grain
img = Image.new("RGB", (SIZE, SIZE))
px = img.load()
for y in range(SIZE):
    for x in range(SIZE):
        base = lerp(MIST_TOP, MIST_LOW, (y / (SIZE - 1)) ** 1.4)
        j = rnd.randint(-3, 3)
        px[x, y] = tuple(max(0, min(255, c + j)) for c in base)

d = ImageDraw.Draw(img)


def haze(colour, depth):
    """The further away something is, the more it takes on the colour of the mist."""
    return lerp(colour, (150, 158, 164), depth)


# ---- the leviathan: a far leg, the underside of the body, two near legs
d.line([(9, 0), (5, 50)], fill=haze(DARK, 0.72), width=3)
d.line([(50, 0), (58, 52)], fill=haze(DARK, 0.62), width=3)
d.rectangle([14, 0, 52, 5], fill=haze(DARK, 0.5))
d.rectangle([20, 5, 46, 8], fill=haze(DARK, 0.58))
for x in (24, 29, 35, 40):                              # feelers hanging from it
    d.line([(x, 8), (x + rnd.randint(-1, 1), 16 + rnd.randint(0, 5))], fill=haze((150, 120, 124), 0.45), width=1)
d.line([(19, 4), (13, 28)], fill=haze(DARK, 0.3), width=4)        # near leg, thigh
d.line([(13, 28), (17, 56)], fill=haze(DARK, 0.22), width=4)      # near leg, shin
d.line([(45, 4), (52, 26)], fill=haze(DARK, 0.38), width=4)
d.line([(52, 26), (48, 55)], fill=haze(DARK, 0.3), width=4)

# soften everything so far so it sits in the mist
img = img.filter(ImageFilter.GaussianBlur(0.7))
d = ImageDraw.Draw(img)
px = img.load()

# ---- ground
for y in range(54, SIZE):
    for x in range(SIZE):
        px[x, y] = lerp((46, 52, 50), (20, 23, 24), (y - 54) / 9) if rnd.random() > 0.08 else (58, 66, 60)

# ---- the cabin
d.polygon([(22, 40), (32, 32), (42, 40)], fill=(30, 26, 26))       # roof
d.rectangle([24, 40, 40, 55], fill=(40, 33, 30))                    # wall
for x in range(24, 41, 4):
    d.line([(x, 40), (x, 55)], fill=(31, 26, 24))                   # planks
d.rectangle([34, 43, 38, 47], fill=(232, 176, 84))                  # window, lit
d.line([(36, 43), (36, 47)], fill=(120, 80, 40))
d.line([(34, 45), (38, 45)], fill=(120, 80, 40))
d.rectangle([27, 46, 31, 55], fill=(22, 18, 18))                    # door, shut
px[31, 51] = (150, 130, 90)                                         # handle
d.rectangle([28, 42, 30, 44], fill=(128, 126, 118))                 # the talisman over the door
px[29, 43] = (214, 62, 44)

# warm light spilling from the window onto the ground and the mist
glow = Image.new("RGB", (SIZE, SIZE), (0, 0, 0))
ImageDraw.Draw(glow).ellipse([28, 38, 46, 56], fill=(70, 46, 16))
glow = glow.filter(ImageFilter.GaussianBlur(4))
img = Image.blend(img, Image.composite(img, img, glow.convert("L")), 0)
gp = glow.load()
px = img.load()
for y in range(SIZE):
    for x in range(SIZE):
        if not (24 <= x <= 40 and 40 <= y <= 55):
            px[x, y] = tuple(min(255, px[x, y][i] + gp[x, y][i] // 2) for i in range(3))

# ---- one of the Hollow, standing outside, facing the door
d = ImageDraw.Draw(img)
d.rectangle([15, 47, 17, 55], fill=(52, 58, 66))                    # body
d.rectangle([15, 44, 17, 46], fill=(214, 208, 196))                 # head
px[17, 45] = (20, 16, 16)                                           # the smile side of the face

logo = img.resize((512, 512), Image.NEAREST)
os.makedirs(os.path.join(HERE, "..", "curseforge"), exist_ok=True)
logo.save(os.path.join(HERE, "..", "curseforge", "logo.png"))
img.resize((128, 128), Image.NEAREST).save(os.path.join(HERE, "..", "src", "main", "resources", "assets", "fromthemist", "icon.png"))
print("logo written")
