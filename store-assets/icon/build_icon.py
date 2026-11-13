#!/usr/bin/env python3
"""Builds every Ikshana icon asset from the owner's artwork, store-assets/icon/source/ikshana.png.

The owner's final choice (13 Nov 2026, "final"), after the drawn golden iris, its samples and a
Gemini medallion: a glossy dark-blue lens with a glowing cyan centre, three teal arcs of sound
leaving it to the right, on flat navy. It came from DeepSeek, made from a prompt that said only
what the app is for. It is a raster, 1254 px square, so this script fits it, and draws nothing
of its own except the one-colour mark the themed icon and the status bar need.

  app/src/main/res/mipmap-*/ic_launcher_foreground.png   adaptive foreground: the artwork,
                                                          fading to transparent at its edges
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
import json, math, os, shutil, subprocess, sys
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(HERE, "source", "ikshana.png")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# Measured on the source (13 Nov): every pixel that differs from the navy lies within 428 px of
# (616, 626), the lens on the left and the outer arc's edge on the right.
ART_CX, ART_CY, ART_R = 616, 626, 428
# The artwork's own flat navy (its corners and edge midpoints read (11..13, 22..24, 42..45)).
NAVY = (13, 23, 44)
# The artwork's radius on the 108-unit tile: 33.5, inside the circle of radius 33.5 or more that
# every launcher mask keeps, so neither the lens nor an arc's tip is ever cut.
R_TILE = 33.5
WORK = 1080   # every layer is drawn at 10 px a unit and scaled down with Lanczos


def foreground(size):
    """The artwork scaled onto the 108-unit tile round its centre. Past the artwork it fades out
    over the source's own navy, so no edge of the square source ever shows on the background."""
    k = (R_TILE * WORK / 108) / ART_R
    src = Image.open(SOURCE).convert("RGB")
    big = src.resize((round(src.width * k), round(src.height * k)), Image.LANCZOS)
    ox, oy = round(WORK / 2 - ART_CX * k), round(WORK / 2 - ART_CY * k)
    layer = Image.new("RGBA", (WORK, WORK), (0, 0, 0, 0))
    layer.paste(big, (ox, oy))
    r = (ART_R + 40) * k
    mask = Image.new("L", (WORK, WORK), 0)
    ImageDraw.Draw(mask).ellipse((WORK / 2 - r, WORK / 2 - r, WORK / 2 + r, WORK / 2 + r), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(25 * k * 2))
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
# The artwork's parts in source pixels, for the one-colour mark: the lens centre, its bright ring,
# the glowing centre, and the two outer arcs of sound (radius, half-span in degrees).
LENS_X, LENS_Y, LENS_RING, LENS_DOT = 548, 627, 275, 78
ARCS = ((395, 48), (500, 46))


def mark_paths(k, colour):
    """The lens in one colour, scaled by k about the tile's centre: its ring, its centre, and
    the two outer arcs of sound. The artwork's innermost arc touches the ring and is left out:
    at status-bar size the two run together."""
    s = R_TILE / ART_R * k
    def p(x, y):
        return f"{54 + (x - ART_CX) * s:.2f},{54 + (y - ART_CY) * s:.2f}"
    def circle(r):
        rr = r * s
        return f"M{p(LENS_X - r, LENS_Y)} a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0"
    out = (f'<path android:pathData="{circle(LENS_RING)}" android:strokeColor="{colour}" '
           f'android:strokeWidth="{24 * s:.2f}" android:fillColor="#00000000"/>')
    out += f'<path android:pathData="{circle(LENS_DOT)}" android:fillColor="{colour}"/>'
    for r, span in ARCS:
        a = math.radians(span)
        x0, y0 = LENS_X + r * math.cos(a), LENS_Y - r * math.sin(a)
        x1, y1 = LENS_X + r * math.cos(a), LENS_Y + r * math.sin(a)
        out += (f'<path android:pathData="M{p(x0, y0)} A{r * s:.2f},{r * s:.2f} 0 0,1 {p(x1, y1)}" '
                f'android:strokeColor="{colour}" android:strokeWidth="{30 * s:.2f}" '
                f'android:strokeLineCap="round" android:fillColor="#00000000"/>')
    return out


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: the lens and its arcs of sound, in one colour the launcher tints.\n"
              "         Built by store-assets/icon/build_icon.py; do not edit by hand. -->\n"
              + mark_paths(1.1, "#FF000000") + "\n</vector>\n")

NOTIFICATION = (VECTOR_HEAD.format(w=24)
                + "    <!-- Status-bar glyph: white on transparent, the same lens and arcs, filling the canvas.\n"
                "         Built by store-assets/icon/build_icon.py. -->\n"
                + mark_paths(1.35, "#FFFFFFFF") + "\n</vector>\n")

ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')

FEATURE = '''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:"Space Grotesk";font-weight:700;src:url("../../app/src/main/res/font/space_grotesk_bold.ttf")}
@font-face{font-family:"Space Grotesk";font-weight:400;src:url("../../app/src/main/res/font/space_grotesk_regular.ttf")}
body{margin:0;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 50%,#18294A 0%,#0D172C 55%,#070D1A 100%);
font-family:"Space Grotesk",Inter,system-ui,sans-serif;color:#F7EEDB}
.icon{position:absolute;left:84px;top:94px;width:312px;height:312px;border-radius:72px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.7),0 0 90px rgba(40,220,240,.16)}
.icon img{width:100%;height:100%;display:block}
h1{position:absolute;left:460px;top:150px;margin:0;font-size:92px;letter-spacing:-3px;font-weight:700}
p{position:absolute;left:464px;top:262px;margin:0;font-size:31px;color:#CFE3E6;max-width:520px;line-height:1.3}
.ml{position:absolute;left:466px;top:352px;font-size:26px;color:#3FE0F0}
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
