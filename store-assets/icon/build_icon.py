#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the golden iris that reads braille and speaks.

Fifth drawing, chosen by the owner on 13 Nov from twenty-odd concepts ("5G it is"). A real iris
drawn fibre by fibre in gold metal, set in a polished gold ring on black; the pupil holds the
braille cell for "i" (dots 2 and 4 raised and glowing, the other four felt as rings), because
Ikshana is made for the blind first; two gold waves leave it, because it speaks what it sees.
The owner rejected flat line art twice ("a kindergarten student will take it from the
internet"): the rendering (fibres, collar, limbal ring, catchlights, glow) is the point.

Generated, not traced. The same geometry, drawn once here, becomes:

  app/src/main/res/mipmap-*/ic_launcher_foreground.png   adaptive foreground (the art, inside
                                                          the 72 dp the launcher shows)
  app/src/main/res/mipmap-*/ic_launcher_background.png   adaptive background (the field)
  app/src/main/res/mipmap-*/ic_launcher.png, _round.png  legacy rasters, 48 dp
  app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml    the adaptive icon declarations
  app/src/main/res/drawable/ic_launcher_monochrome.xml   Android 13+ themed icon (vector)
  app/src/main/res/drawable/ic_notification.xml          status-bar glyph (vector)
  store-assets/icon/ikshana-icon.svg                     master artwork, 108 dp canvas
  store-assets/icon_512.png                              Play Store icon
  store-assets/feature_graphic_1024x500.png              Play Store feature graphic

Run from the repo root, in Git Bash (from the PowerShell tool headless Edge writes nothing):
  python -P store-assets/icon/build_icon.py
The SVG and XML need only Python. The PNGs need Node with Playwright (render.js) or, on the
owner's laptop, Edge and Pillow (render_edge.py).

Why rasters for the adaptive layers: gradients, blurs and 260 fibres are the icon, and a
VectorDrawable carries none of the blurs. The themed icon and the notification glyph are
vectors because they must be; they carry the reduced mark (ring, iris, braille, waves).
"""
import json, math, os, random, subprocess, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# Everything sits within 31 of the tile's centre, inside the circle of radius 36 a launcher
# shows, so the foreground is drawn at the size the owner approved.
FG_SCALE = 1.0
CX, CY = 48.0, 54.0        # the iris sits left of centre; the waves balance it on the right
R_RING = 23.0              # outer edge of the gold ring
R_IRIS = 21.2
R_PUPIL = 8.2
DOT_DX, DOT_DY, DOT_R = 2.5, 3.9, 1.15   # the braille cell inside the pupil
WAVES = ((29.5, 2.8, 1.0), (35.0, 2.4, .7))   # radius, width, opacity
WAVE_SPAN = 26.0           # degrees either side of horizontal
FIBRE_SEED = 3             # the fibres are random but fixed: the same icon on every build


def pt(r, deg, cx=CX, cy=CY):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


def arc_path(r, span, cx=CX, cy=CY):
    x0, y0 = pt(r, -span, cx, cy); x1, y1 = pt(r, span, cx, cy)
    return f"M{x0:.2f},{y0:.2f} A{r:.2f},{r:.2f} 0 0,1 {x1:.2f},{y1:.2f}"


def braille_dots():
    """(x, y, raised) for the six dots of the braille "i": dots 2 and 4 raised."""
    out = []
    for col, x in enumerate((CX - DOT_DX, CX + DOT_DX)):
        for row, y in enumerate((CY - DOT_DY, CY, CY + DOT_DY)):
            out.append((x, y, col * 3 + row + 1 in (2, 4)))
    return out


def braille():
    out = ""
    for x, y, raised in braille_dots():
        if raised:
            out += (f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{DOT_R * 2.4:.2f}" fill="#FFC85F" opacity=".5" filter="url(#bl1)"/>'
                    f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{DOT_R:.2f}" fill="url(#dotLit)"/>'
                    f'<circle cx="{x - DOT_R * .35:.2f}" cy="{y - DOT_R * .4:.2f}" r="{DOT_R * .3:.2f}" fill="#fff" opacity=".9"/>')
        else:
            out += (f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{DOT_R * .8:.2f}" fill="none" stroke="#FFD27A" '
                    f'stroke-opacity=".4" stroke-width="{DOT_R * .32:.2f}"/>')
    return out


def fibres():
    """The iris's stroma: 260 curved fibres from the pupil's collar to the rim, in seven golds."""
    rnd = random.Random(FIBRE_SEED)
    out = []
    for _ in range(260):
        a = rnd.uniform(0, 2 * math.pi)
        r0, r1 = rnd.uniform(7.2, 9), rnd.uniform(15.5, 21.5)
        col = rnd.choice(("#FFE7A8", "#F2C65A", "#D99A2B", "#A86E1C", "#FFF4D6", "#C8902E", "#7A4E10"))
        mid, bend = rnd.uniform(.3, .7), rnd.uniform(-.12, .12)
        rm = r0 + (r1 - r0) * mid
        out.append(f'<path d="M{CX + r0 * math.cos(a):.2f},{CY + r0 * math.sin(a):.2f} '
                   f'Q{CX + rm * math.cos(a + bend):.2f},{CY + rm * math.sin(a + bend):.2f} '
                   f'{CX + r1 * math.cos(a):.2f},{CY + r1 * math.sin(a):.2f}" stroke="{col}" '
                   f'stroke-width="{rnd.uniform(.25, .6):.2f}" opacity="{rnd.uniform(.5, .95):.2f}" fill="none"/>')
    return "".join(out)


# Filter regions are in the tile's own space: a box-relative region on a thin arc is a few units
# wide and cut the waves' glow into rectangles (13 Nov).
DEFS = """<defs>
<filter id="bl1" filterUnits="userSpaceOnUse" x="-20" y="-20" width="148" height="148"><feGaussianBlur stdDeviation=".9"/></filter>
<filter id="bl2" filterUnits="userSpaceOnUse" x="-20" y="-20" width="148" height="148"><feGaussianBlur stdDeviation="2"/></filter>
<filter id="bl8" filterUnits="userSpaceOnUse" x="-20" y="-20" width="148" height="148"><feGaussianBlur stdDeviation="8"/></filter>
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1E1C19"/><stop offset=".55" stop-color="#0C0B0A"/><stop offset="1" stop-color="#030303"/></linearGradient>
<linearGradient id="gloss" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".07"/><stop offset=".45" stop-color="#fff" stop-opacity="0"/></linearGradient>
<radialGradient id="irisBase" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#FFF1C4"/><stop offset=".45" stop-color="#E0A63A"/><stop offset=".85" stop-color="#6E4410"/><stop offset="1" stop-color="#1A0F02"/></radialGradient>
<radialGradient id="limbal" cx="50%" cy="50%" r="50%"><stop offset=".78" stop-color="#140B02" stop-opacity="0"/><stop offset="1" stop-color="#0A0601" stop-opacity=".95"/></radialGradient>
<radialGradient id="collar" cx="50%" cy="50%" r="50%"><stop offset=".3" stop-color="#FFF1C4" stop-opacity="0"/><stop offset=".42" stop-color="#FFF1C4" stop-opacity=".55"/><stop offset=".6" stop-color="#FFF1C4" stop-opacity="0"/></radialGradient>
<linearGradient id="metalRing" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFF4CC"/><stop offset=".3" stop-color="#E8C268"/><stop offset=".55" stop-color="#7A4E10"/><stop offset=".78" stop-color="#F7D47E"/><stop offset="1" stop-color="#4A3008"/></linearGradient>
<linearGradient id="waveG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF4D6"/><stop offset=".5" stop-color="#F2C65A"/><stop offset="1" stop-color="#C8902E"/></linearGradient>
<radialGradient id="dotLit" cx="35%" cy="30%" r="75%"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".5" stop-color="#FFE7A8"/><stop offset="1" stop-color="#E0A030"/></radialGradient>
<linearGradient id="win" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".7"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
</defs>"""

BACKGROUND = '<rect width="108" height="108" fill="url(#bg)"/><rect width="108" height="108" fill="url(#gloss)"/>'

WAVES_SVG = "".join(
    f'<path d="{arc_path(r, WAVE_SPAN)}" fill="none" stroke="#FFB84D" stroke-width="{w + 2}" opacity=".35" filter="url(#bl2)" stroke-linecap="round"/>'
    f'<path d="{arc_path(r, WAVE_SPAN)}" fill="none" stroke="url(#waveG)" stroke-width="{w}" opacity="{op}" stroke-linecap="round"/>'
    for r, w, op in WAVES)

ART = (
    # the warm light the iris gives off, and its shadow
    f'<circle cx="{CX}" cy="{CY}" r="30" fill="#FFB84D" opacity=".22" filter="url(#bl8)"/>'
    + f'<circle cx="{CX + 1}" cy="{CY + 2.5}" r="{R_RING}" fill="#000" opacity=".7" filter="url(#bl2)"/>'
    + WAVES_SVG
    # the polished gold ring
    + f'<circle cx="{CX}" cy="{CY}" r="{R_RING}" fill="url(#metalRing)"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_RING - .1}" fill="none" stroke="#FFF6D8" stroke-opacity=".6" stroke-width=".35"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_IRIS + .4}" fill="#0A0704"/>'
    # the iris: base colour, fibres, the bright collar round the pupil, the dark limbal ring
    + f'<circle cx="{CX}" cy="{CY}" r="{R_IRIS}" fill="url(#irisBase)"/>'
    + fibres()
    + f'<circle cx="{CX}" cy="{CY}" r="{R_IRIS}" fill="url(#collar)"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_IRIS}" fill="url(#limbal)"/>'
    # the pupil, and the braille "i" glowing in it
    + f'<circle cx="{CX}" cy="{CY}" r="{R_PUPIL}" fill="#000"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_PUPIL}" fill="none" stroke="#FFD27A" stroke-opacity=".35" stroke-width=".3"/>'
    + braille()
    # catchlights: a soft window top left, a small one opposite
    + f'<ellipse cx="{CX - 8}" cy="{CY - 9}" rx="7.5" ry="3.8" transform="rotate(-35 {CX - 8} {CY - 9})" fill="url(#win)"/>'
    + f'<circle cx="{CX + 7}" cy="{CY + 7.5}" r=".9" fill="#fff" opacity=".7"/>'
)


def svg(body):
    return f'<svg viewBox="0 0 108 108" width="108" height="108" xmlns="http://www.w3.org/2000/svg">{DEFS}{body}</svg>'


def scaled(body, k):
    return f'<g transform="translate(54 54) scale({k:.5f}) translate(-54 -54)">{body}</g>'


MASTER = svg(BACKGROUND + ART)
FOREGROUND = svg(scaled(ART, FG_SCALE))      # transparent field
BACKGROUND_ONLY = svg(BACKGROUND)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


# ---- vectors: the reduced mark for the themed icon and the status bar --------------------
def mark_paths(k, colour):
    """The mark in one colour, scaled by k about the tile's centre: the ring, the iris solid with
    the pupil cut out, the two raised braille dots standing in the pupil, and the two waves."""
    def p(x, y):
        return 54 + (x - 54) * k, 54 + (y - 54) * k
    def circle(r, cx=CX, cy=CY):
        x, y = p(cx - r, cy); rr = r * k
        return f"M{x:.2f},{y:.2f} a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0"
    out = (f'<path android:pathData="{circle(R_RING - .8)}" android:strokeColor="{colour}" '
           f'android:strokeWidth="{1.6 * k:.2f}" android:fillColor="#00000000"/>')
    out += (f'<path android:pathData="{circle(R_IRIS - 1.6)} {circle(R_PUPIL + .6)}" android:fillColor="{colour}" '
            f'android:fillType="evenOdd"/>')
    for x, y, raised in braille_dots():
        if raised:
            out += f'<path android:pathData="{circle(DOT_R * 1.5, x, y)}" android:fillColor="{colour}"/>'
    for r, w, _ in WAVES:
        x0, y0 = p(*pt(r, -WAVE_SPAN)); x1, y1 = p(*pt(r, WAVE_SPAN))
        out += (f'<path android:pathData="M{x0:.2f},{y0:.2f} A{r * k:.2f},{r * k:.2f} 0 0,1 {x1:.2f},{y1:.2f}" '
                f'android:strokeColor="{colour}" android:strokeWidth="{(w + .6) * k:.2f}" '
                f'android:strokeLineCap="round" android:fillColor="#00000000"/>')
    return out


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: the ring, the iris, the braille i in its pupil and the two waves,\n"
              "         in one colour the launcher tints. Built by store-assets/icon/build_icon.py; do not\n"
              "         edit by hand. -->\n"
              + mark_paths(.95, "#FF000000") + "\n</vector>\n")

NOTIFICATION = (VECTOR_HEAD.format(w=24)
                + "    <!-- Status-bar glyph: white on transparent, the same reduced mark, filling the canvas.\n"
                "         Built by store-assets/icon/build_icon.py. -->\n"
                + mark_paths(1.4, "#FFFFFFFF") + "\n</vector>\n")

ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')

FEATURE = f'''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{{font-family:"Space Grotesk";font-weight:700;src:url("../../app/src/main/res/font/space_grotesk_bold.ttf")}}
@font-face{{font-family:"Space Grotesk";font-weight:400;src:url("../../app/src/main/res/font/space_grotesk_regular.ttf")}}
body{{margin:0;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 50%,#24201A 0%,#0C0B0A 55%,#030303 100%);
font-family:"Space Grotesk",Inter,system-ui,sans-serif;color:#F7EEDB}}
.icon{{position:absolute;left:84px;top:94px;width:312px;height:312px;border-radius:84px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.7),0 0 90px rgba(255,184,77,.18)}}
.icon svg{{width:100%;height:100%;display:block}}
h1{{position:absolute;left:460px;top:150px;margin:0;font-size:92px;letter-spacing:-3px;font-weight:700}}
p{{position:absolute;left:464px;top:262px;margin:0;font-size:31px;color:#D9C9A6;max-width:520px;line-height:1.3}}
.ml{{position:absolute;left:466px;top:352px;font-size:26px;color:#E8C268}}
</style></head><body><div class="icon">{MASTER}</div><h1>Ikshana</h1>
<p>A camera that answers, and cannot upload what it sees.</p><div class="ml">ഈക്ഷണ · ईक्षण</div></body></html>'''


def main():
    write(os.path.join(HERE, "ikshana-icon.svg"), MASTER)
    write(os.path.join(HERE, "foreground.svg"), FOREGROUND)
    write(os.path.join(HERE, "background.svg"), BACKGROUND_ONLY)
    write(os.path.join(HERE, "feature.html"), FEATURE)
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), MONOCHROME)
    write(os.path.join(RES, "drawable", "ic_notification.xml"), NOTIFICATION)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), ADAPTIVE)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), ADAPTIVE)

    jobs = []
    for d, k in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{d}")
        jobs.append(dict(svg="foreground.svg", size=int(108 * k), out=os.path.join(folder, "ic_launcher_foreground.png"), transparent=True))
        jobs.append(dict(svg="background.svg", size=int(108 * k), out=os.path.join(folder, "ic_launcher_background.png")))
        # legacy icons show the art at full size, cropped: the launcher masked these before API 26
        jobs.append(dict(svg="ikshana-icon.svg", size=int(48 * k), out=os.path.join(folder, "ic_launcher.png"), radius=0.2, transparent=True))
        jobs.append(dict(svg="ikshana-icon.svg", size=int(48 * k), out=os.path.join(folder, "ic_launcher_round.png"), radius=0.5, transparent=True))
    jobs.append(dict(svg="ikshana-icon.svg", size=512, out=os.path.join(ROOT, "store-assets", "icon_512.png")))
    jobs.append(dict(html="feature.html", width=1024, height=500, out=os.path.join(ROOT, "store-assets", "feature_graphic_1024x500.png")))
    with open(os.path.join(HERE, "jobs.json"), "w") as f:
        json.dump(jobs, f, indent=1)
    if "--no-png" in sys.argv:
        print("vectors and SVGs written; PNGs skipped")
        return
    import shutil
    if shutil.which("node"):
        subprocess.check_call(["node", os.path.join(HERE, "render.js"), os.path.join(HERE, "jobs.json")])
    else:
        # The owner's laptop has Edge and Pillow but no Node (12 Nov 2026).
        subprocess.check_call([sys.executable, os.path.join(HERE, "render_edge.py"), os.path.join(HERE, "jobs.json")])
    os.remove(os.path.join(HERE, "jobs.json"))
    print("done")


if __name__ == "__main__":
    main()
