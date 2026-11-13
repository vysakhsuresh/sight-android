#!/usr/bin/env python3
"""Builds every Ikshana icon asset from the owner's artwork, store-assets/icon/source/ikshana.jpeg.

The owner chose this artwork on 13 Nov 2026, after the drawn golden iris and its samples:
a round medallion on deep navy, a gold-and-amber eye held in flowing gold and teal waves.
It came from Gemini, made from a prompt that said only what the app is for. It is a raster,
2048 px square, so this script fits it and cuts it, and draws nothing of its own except the
one-colour mark the themed icon and the status bar need.

  app/src/main/res/mipmap-*/ic_launcher_foreground.png   adaptive foreground: the medallion
                                                          alone, transparent outside it
  app/src/main/res/mipmap-*/ic_launcher_background.png   adaptive background: the artwork's navy
  app/src/main/res/mipmap-*/ic_launcher.png, _round.png  legacy rasters, 48 dp
  app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml    the adaptive icon declarations
  app/src/main/res/drawable/ic_launcher_monochrome.xml   Android 13+ themed icon (vector)
  app/src/main/res/drawable/ic_notification.xml          status-bar glyph (vector)
  store-assets/icon_512.png                              Play Store icon
  store-assets/feature_graphic_1024x500.png              Play Store feature graphic

Run from the repo root, in Git Bash (from the PowerShell tool headless Edge writes nothing):
  python -P store-assets/icon/build_icon.py
The launcher PNGs need only Pillow. The feature graphic is HTML (its Malayalam and Devanagari
need a browser's shaping), rendered by Node with Playwright (render.js) or Edge (render_edge.py).
"""
import json, os, shutil, subprocess, sys
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(HERE, "source", "ikshana.jpeg")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# Measured on the source (13 Nov): the medallion's rim crosses the centre lines at x 104 and
# 1943, y 104 and 1935, so it is a circle of radius about 918 round (1024, 1020).
DISC_CX, DISC_CY, DISC_R = 1024, 1020, 918
# The artwork's own navy outside the medallion (its corners read (1..6, 7, 34..42)).
NAVY = (3, 8, 40)
# The medallion's radius on the 108-unit tile. A launcher shows the circle of radius 36; at 37.5
# its mask falls just inside the medallion's thin rim, so a round icon is all artwork, and a
# square or squircle mask shows the rim and then the same navy.
R_TILE = 37.5
WORK = 1080   # every layer is drawn at 10 px a unit and scaled down with Lanczos


def foreground(size):
    """The medallion alone, scaled onto the 108-unit tile, transparent outside its rim."""
    k = (R_TILE * WORK / 108) / DISC_R
    src = Image.open(SOURCE).convert("RGB")
    big = src.resize((round(src.width * k), round(src.height * k)), Image.LANCZOS)
    ox, oy = round(WORK / 2 - DISC_CX * k), round(WORK / 2 - DISC_CY * k)
    layer = Image.new("RGBA", (WORK, WORK), (0, 0, 0, 0))
    layer.paste(big, (ox, oy))
    # a soft edge a few pixels wide, just outside the rim, so the rim line is kept whole
    r = (DISC_R + 8) * k
    mask = Image.new("L", (WORK, WORK), 0)
    ImageDraw.Draw(mask).ellipse((WORK / 2 - r, WORK / 2 - r, WORK / 2 + r, WORK / 2 + r), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(1.5))
    layer.putalpha(mask)
    return layer.resize((size, size), Image.LANCZOS)


def background(size):
    return Image.new("RGBA", (size, size), NAVY + (255,))


def full(size):
    return Image.alpha_composite(background(size), foreground(size))


def cut(img, radius):
    """Legacy icons: the whole tile under a rounded-square (radius .2) or round (.5) mask."""
    s = img.width * 4
    m = Image.new("L", (s, s), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(radius * s), fill=255)
    m = m.resize(img.size, Image.LANCZOS)
    out = img.copy()
    out.putalpha(m)
    return out


# ---- vectors: the one-colour mark for the themed icon and the status bar -----------------
def mark_paths(k, colour):
    """An open eye in one colour, scaled by k about the tile's centre: the two lids, and the
    iris solid with the pupil and a catchlight cut out. The medallion's waves are left out:
    at status-bar size they become noise."""
    def p(x, y):
        return f"{54 + (x - 54) * k:.2f},{54 + (y - 54) * k:.2f}"
    def circle(cx, cy, r):
        rr = r * k
        return f"M{p(cx - r, cy)} a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0"
    lids = (f"M{p(24, 54)} Q{p(54, 26)} {p(84, 54)} Q{p(54, 82)} {p(24, 54)} Z "
            f"M{p(28.5, 54)} Q{p(54, 31.5)} {p(79.5, 54)} Q{p(54, 76.5)} {p(28.5, 54)} Z")
    out = f'<path android:pathData="{lids}" android:fillColor="{colour}" android:fillType="evenOdd"/>'
    out += (f'<path android:pathData="{circle(54, 54, 12.5)} {circle(54, 54, 5.2)} {circle(58.2, 49.8, 2)}" '
            f'android:fillColor="{colour}" android:fillType="evenOdd"/>')
    return out


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: an open eye, lids and iris, in one colour the launcher tints.\n"
              "         Built by store-assets/icon/build_icon.py; do not edit by hand. -->\n"
              + mark_paths(1.0, "#FF000000") + "\n</vector>\n")

NOTIFICATION = (VECTOR_HEAD.format(w=24)
                + "    <!-- Status-bar glyph: white on transparent, the same eye, filling the canvas.\n"
                "         Built by store-assets/icon/build_icon.py. -->\n"
                + mark_paths(1.7, "#FFFFFFFF") + "\n</vector>\n")

ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')

FEATURE = '''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:"Space Grotesk";font-weight:700;src:url("../../app/src/main/res/font/space_grotesk_bold.ttf")}
@font-face{font-family:"Space Grotesk";font-weight:400;src:url("../../app/src/main/res/font/space_grotesk_regular.ttf")}
body{margin:0;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 50%,#0B1A44 0%,#030828 55%,#01041A 100%);
font-family:"Space Grotesk",Inter,system-ui,sans-serif;color:#F7EEDB}
.icon{position:absolute;left:84px;top:94px;width:312px;height:312px;border-radius:50%;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.7),0 0 90px rgba(80,200,200,.18)}
.icon img{width:144%;height:144%;margin:-22%;display:block}
h1{position:absolute;left:460px;top:150px;margin:0;font-size:92px;letter-spacing:-3px;font-weight:700}
p{position:absolute;left:464px;top:262px;margin:0;font-size:31px;color:#CFE3E6;max-width:520px;line-height:1.3}
.ml{position:absolute;left:466px;top:352px;font-size:26px;color:#E8C268}
</style></head><body><div class="icon"><img src="feature_icon.png"></div><h1>Ikshana</h1>
<p>A camera that answers, and cannot upload what it sees.</p><div class="ml">ഈക്ഷണ · ईक्षण</div></body></html>'''


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def main():
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), MONOCHROME)
    write(os.path.join(RES, "drawable", "ic_notification.xml"), NOTIFICATION)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), ADAPTIVE)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), ADAPTIVE)
    for d, k in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{d}")
        os.makedirs(folder, exist_ok=True)
        foreground(int(108 * k)).save(os.path.join(folder, "ic_launcher_foreground.png"))
        background(int(108 * k)).save(os.path.join(folder, "ic_launcher_background.png"))
        # legacy icons show the whole tile, cropped: the launcher masked these before API 26
        legacy = full(int(48 * k))
        cut(legacy, .2).save(os.path.join(folder, "ic_launcher.png"))
        cut(legacy, .5).save(os.path.join(folder, "ic_launcher_round.png"))
    full(512).convert("RGB").save(os.path.join(ROOT, "store-assets", "icon_512.png"))
    print("launcher icons and Play icon written")

    if "--no-feature" in sys.argv:
        return
    full(624).save(os.path.join(HERE, "feature_icon.png"))
    write(os.path.join(HERE, "feature.html"), FEATURE)
    jobs = [dict(html="feature.html", width=1024, height=500, out=os.path.join(ROOT, "store-assets", "feature_graphic_1024x500.png"))]
    with open(os.path.join(HERE, "jobs.json"), "w") as f:
        json.dump(jobs, f, indent=1)
    try:
        if shutil.which("node"):
            subprocess.check_call(["node", os.path.join(HERE, "render.js"), os.path.join(HERE, "jobs.json")])
        else:
            # The owner's laptop has Edge and Pillow but no Node (12 Nov 2026).
            subprocess.check_call([sys.executable, os.path.join(HERE, "render_edge.py"), os.path.join(HERE, "jobs.json")])
    finally:
        os.remove(os.path.join(HERE, "jobs.json"))
    print("feature graphic written")


if __name__ == "__main__":
    main()
