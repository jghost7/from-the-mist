"""Generates every texture in the mod. Placeholder art: repaint any PNG and keep its size/layout.

Run from the project root:  python tools/make_textures.py
Box UV layout (same as the Java models): a box w,h,d at texOffs(u,v) has its
front face at (u+d, v+d, w, h) and its top face at (u+d, v, w, d).
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "fromthemist")
TEX = os.path.join(ROOT, "textures")


def save(img, *parts):
    path = os.path.join(TEX, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def noise(size, base, spread, seed, blotch=0.0, blotch_col=None):
    rnd = random.Random(seed)
    w, h = size
    img = Image.new("RGBA", size)
    px = img.load()
    for y in range(h):
        for x in range(w):
            j = rnd.randint(-spread, spread)
            c = [max(0, min(255, base[i] + j)) for i in range(3)]
            if blotch_col and rnd.random() < blotch:
                c = [max(0, min(255, blotch_col[i] + j // 2)) for i in range(3)]
            px[x, y] = (c[0], c[1], c[2], 255)
    return img


def rect(img, x, y, w, h, col):
    ImageDraw.Draw(img).rectangle([x, y, x + w - 1, y + h - 1], fill=col)


# ---------------------------------------------------------------- Hollow (64x64 player layout)
def hollow(seed, shirt, trousers, hair, name):
    rnd = random.Random(seed)
    skin = (214, 208, 196)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def fill(x, y, w, h, col, spread=6):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                j = rnd.randint(-spread, spread)
                img.putpixel((xx, yy), tuple(max(0, min(255, c + j)) for c in col) + (255,))

    fill(0, 0, 32, 16, skin)          # head, all faces
    fill(8, 0, 8, 8, hair)            # top
    fill(0, 8, 8, 3, hair)            # right side hairline
    fill(16, 8, 8, 3, hair)           # left side hairline
    fill(24, 8, 8, 6, hair)           # back
    fill(8, 8, 8, 2, hair)            # fringe
    # face (front at 8,8): wide black eyes and a smile that is a little too wide
    for ex in (9, 13):
        rect(img, ex, 11, 2, 2, (8, 8, 10, 255))
        img.putpixel((ex, 11), (235, 235, 235, 255))
    rect(img, 9, 14, 6, 1, (240, 238, 228, 255))      # teeth
    rect(img, 9, 15, 6, 1, (60, 14, 18, 255))          # lower lip shadow
    img.putpixel((8, 13), (60, 14, 18, 255))
    img.putpixel((15, 13), (60, 14, 18, 255))
    img.putpixel((8, 14), (60, 14, 18, 255))
    img.putpixel((15, 14), (60, 14, 18, 255))

    fill(16, 16, 24, 16, shirt)       # body
    fill(40, 16, 16, 16, shirt)       # right arm
    fill(32, 48, 16, 16, shirt)       # left arm
    fill(0, 16, 16, 16, trousers)     # right leg
    fill(16, 48, 16, 16, trousers)    # left leg
    for ax in (44, 36):               # hands
        ay = 28 if ax == 44 else 60
        fill(ax - 4, ay, 16, 4, skin)
    rect(img, 23, 20, 2, 12, tuple(max(0, c - 40) for c in shirt) + (255,))  # button line
    rect(img, 22, 20, 4, 2, skin + (255,))                                    # collar gap
    # a dark stain low on the shirt
    for _ in range(18):
        img.putpixel((rnd.randint(20, 27), rnd.randint(26, 31)), (70, 12, 14, 255))
    save(img, "entity", name)


hollow(1, (150, 170, 190), (52, 50, 60), (40, 30, 24), "hollow_1.png")
hollow(2, (226, 222, 210), (40, 44, 52), (120, 96, 60), "hollow_2.png")
hollow(3, (140, 52, 48), (70, 60, 50), (20, 20, 22), "hollow_3.png")
hollow(4, (96, 120, 92), (48, 42, 38), (150, 150, 150), "hollow_4.png")

# ---------------------------------------------------------------- Skitterer (64x64)
img = noise((64, 64), (58, 60, 66), 10, 11, 0.12, (96, 92, 84))
# head front at (6,26) 8x6: a cluster of pale eyes above a dark split jaw
for (x, y) in ((7, 27), (9, 26), (11, 26), (12, 27), (8, 28), (11, 28)):
    img.putpixel((x, y), (226, 232, 214, 255))
rect(img, 9, 29, 2, 3, (16, 10, 12, 255))
rect(img, 7, 31, 6, 1, (16, 10, 12, 255))
# pale ridge along the top of the body (top face at (14,0) 10x14)
rect(img, 18, 0, 2, 14, (120, 116, 104, 255))
save(img, "entity", "skitterer.png")

# ---------------------------------------------------------------- Stinger (64x64)
img = noise((64, 64), (132, 112, 96), 12, 21, 0.10, (86, 60, 78))
# head front at (5,21) 6x5
img.putpixel((6, 22), (250, 240, 190, 255))
img.putpixel((9, 22), (250, 240, 190, 255))
rect(img, 6, 24, 4, 2, (30, 12, 16, 255))
# sting box at (24,26): dark tip
rect(img, 24, 26, 16, 8, (38, 22, 30, 255))
# Wings: flat sheets, so the top and bottom faces share one plane. Both are painted the same and the
# shape is symmetric front to back, so they line up whichever way the bottom face is flipped.
# Front wing box 20x0x7 at texOffs(0,40); back wing box 15x0x5 at texOffs(0,48).
rect(img, 0, 40, 64, 16, (0, 0, 0, 0))


def wing(u, v, w, d):
    centre = (d - 1) / 2.0

    def half_width(x):
        t = x / (w - 1)
        if t < 0.3:                      # narrow stalk at the root, widening
            shape = 0.3 + 0.7 * (t / 0.3)
        elif t > 0.78:                   # rounded tip
            shape = math.sqrt(max(0.0, 1.0 - ((t - 0.78) / 0.24) ** 2))
        else:
            shape = 1.0
        return (d / 2.0) * shape

    for x in range(w):
        for z in range(d):
            off = abs(z - centre)
            hw = half_width(x)
            if off > hw:
                continue
            edge = off > hw - 1.0 or x == w - 1 or half_width(min(w - 1, x + 1)) < off
            spar = off < 0.5                                   # the long vein down the middle
            cross = x % 4 == 1 and x > 2                       # cross veins
            if spar or edge:
                col = (58, 40, 52, 235)
            elif cross:
                col = (104, 84, 96, 190)
            else:
                shade = 12 if (x + z) % 2 == 0 else 0          # faint cell pattern in the membrane
                col = (214 - shade, 204 - shade, 210 - shade, 105)
            for face in (u + d, u + d + w):                    # top face, bottom face
                img.putpixel((face + x, v + z), col)


wing(0, 40, 20, 7)
wing(0, 48, 15, 5)
save(img, "entity", "stinger.png")

# ---------------------------------------------------------------- Leviathan (128x128)
img = noise((128, 128), (64, 70, 76), 9, 31, 0.08, (88, 96, 100))
# vertical streaks so the hide reads at eight times scale
rnd = random.Random(32)
d = ImageDraw.Draw(img)
for _ in range(90):
    x, y = rnd.randint(0, 127), rnd.randint(0, 120)
    d.line([x, y, x, y + rnd.randint(3, 9)], fill=(44, 48, 54, 255))
# head lobe front at (50,62) 16x10: a row of dim eyes
for i, x in enumerate((52, 55, 58, 61, 63)):
    img.putpixel((x, 65 + (i % 2)), (214, 226, 196, 255))
# tentacle strip at (32,52) 8x22: paler, wet
for y in range(52, 74):
    for x in range(32, 40):
        r, g, b, a = img.getpixel((x, y))
        img.putpixel((x, y), (min(255, r + 46), min(255, g + 40), min(255, b + 40), 255))
save(img, "entity", "leviathan.png")

# ---------------------------------------------------------------- Colossus (256x128)
# body 28x20x44 at (0,0); hump 20x10x24 at (144,0); lobe 18x14x12 at (144,34);
# thigh 8x30x8 at (0,64); shin 7x32x7 at (32,64); foot 10x3x10 at (60,64);
# feeler 2x34x2 at (100,64); spine 2x10x2 at (108,64).
img = noise((256, 128), (52, 62, 64), 8, 71, 0.10, (104, 112, 96))
rnd = random.Random(72)
d = ImageDraw.Draw(img)
for _ in range(260):                                   # deep folds in the hide
    x, y = rnd.randint(0, 255), rnd.randint(0, 120)
    d.line([x, y, x + rnd.randint(-1, 1), y + rnd.randint(4, 12)], fill=(30, 36, 38, 255))
for _ in range(60):                                    # pale growths
    x, y = rnd.randint(0, 250), rnd.randint(0, 60)
    rect(img, x, y, rnd.randint(1, 3), rnd.randint(1, 2), (140, 146, 120, 255))
# belly (body bottom face at (72,0) 28x44) is paler and wet
for y in range(0, 44):
    for x in range(72, 100):
        r, g, b, a = img.getpixel((x, y))
        img.putpixel((x, y), (min(255, r + 34), min(255, g + 26), min(255, b + 22), 255))
# lobe front at (156,46) 18x14: two rows of sunken amber pits
for i, x in enumerate(range(158, 173, 3)):
    img.putpixel((x, 50 + (i % 2)), (222, 156, 60, 255))
    img.putpixel((x + 1, 54 - (i % 2)), (150, 92, 36, 255))
# feelers: pale flesh, darker toward the tip
for y in range(64, 100):
    for x in range(100, 108):
        fade = (y - 64) * 2
        j = rnd.randint(-6, 6)
        img.putpixel((x, y), (max(60, 196 - fade + j), max(50, 170 - fade + j), max(54, 168 - fade + j), 255))
# spines: bone
for y in range(64, 76):
    for x in range(108, 116):
        j = rnd.randint(-8, 8)
        img.putpixel((x, y), (200 + j, 194 + j, 170 + j, 255))
# feet: dark and worn
for y in range(64, 77):
    for x in range(60, 100):
        r, g, b, a = img.getpixel((x, y))
        img.putpixel((x, y), (r // 2, g // 2, b // 2, 255))
save(img, "entity", "colossus.png")


# ---------------------------------------------------------------- Talisman (item + block face, 16x16)
def talisman(cracked):
    t = noise((16, 16), (124, 122, 114), 7, 41)
    px = t.load()
    for y in range(16):
        for x in range(16):
            inside = 2 <= x <= 13 and 1 <= y <= 14 and not ((x in (2, 13)) and (y in (1, 14)))
            if not inside:
                px[x, y] = (0, 0, 0, 0)
            elif x in (2, 13) or y in (1, 14):
                px[x, y] = (84, 82, 78, 255)
    carve = (44, 34, 30, 255)
    glow = (176, 62, 44, 255)
    # an original mark: a ring, a slit pupil, three strokes beneath
    for (x, y) in ((6, 4), (7, 3), (8, 3), (9, 4), (10, 5), (10, 6), (9, 7), (8, 8), (7, 8), (6, 7), (5, 6), (5, 5)):
        px[x, y] = carve
    for y in (4, 5, 6, 7):
        px[7, y] = glow if not cracked else carve
        px[8, y] = glow if not cracked else carve
    for x in (5, 7, 8, 10):
        px[x, 10] = carve
        px[x, 11] = carve
    px[7, 12] = carve
    px[8, 12] = carve
    px[7, 0] = (70, 56, 40, 255)   # cord
    px[8, 0] = (70, 56, 40, 255)
    if cracked:
        for (x, y) in ((11, 2), (10, 3), (10, 4), (9, 5), (9, 6), (8, 7), (8, 8), (7, 9), (6, 10), (6, 11), (5, 12), (4, 13)):
            px[x, y] = (24, 22, 22, 255)
    return t


save(talisman(False), "item", "talisman.png")
def opaque(t):
    px = t.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0:
                px[x, y] = (84, 82, 78, 255)
    return t


save(opaque(talisman(False)), "block", "talisman.png")
save(opaque(talisman(True)), "block", "talisman_cracked.png")

# ---------------------------------------------------------------- Mist essence (16x16)
t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = t.load()
rnd = random.Random(51)
for y in range(16):
    for x in range(16):
        dist = math.hypot(x - 7.5, (y - 8.5) * 1.15)
        swirl = math.sin(math.atan2(y - 8.5, x - 7.5) * 3 + dist * 0.9)
        if dist < 5.2 + swirl * 1.3:
            v = int(232 - dist * 16 + rnd.randint(-8, 8))
            px[x, y] = (max(120, v - 14), max(130, v - 4), max(140, v), 255)
px[7, 8] = (250, 255, 255, 255)
px[8, 8] = (250, 255, 255, 255)
save(t, "item", "mist_essence.png")

# ---------------------------------------------------------------- Mod icon (128x128)
ic = Image.new("RGBA", (128, 128), (30, 33, 38, 255))
d = ImageDraw.Draw(ic)
for y in range(128):
    v = int(36 + 120 * (y / 127) ** 1.6)
    d.line([0, y, 127, y], fill=(v, v + 3, v + 6, 255))
# long legs coming down out of the top
for x0, x1 in ((30, 18), (52, 46), (78, 84), (100, 112)):
    d.line([x0, 0, x1, 118], fill=(26, 28, 32, 255), width=5)
d.rectangle([24, 0, 104, 14], fill=(26, 28, 32, 255))
ic = ic.filter(ImageFilter.GaussianBlur(1.6))
d = ImageDraw.Draw(ic)
d.rectangle([58, 96, 70, 118], fill=(22, 22, 24, 255))      # a door
d.rectangle([54, 92, 74, 96], fill=(22, 22, 24, 255))
d.rectangle([62, 84, 66, 90], fill=(176, 62, 44, 255))      # the talisman above it
ic.save(os.path.join(ROOT, "icon.png"))

print("textures written to", os.path.normpath(TEX))
