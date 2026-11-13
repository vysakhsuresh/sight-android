#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the seeing lens that speaks.

Fourth drawing (owner, 13 Nov). The owner rejected the starburst as clip art that says nothing
of the app: "something powerful, which relates to the app's purpose, which users will trust and
install on seeing the icon alone". So the mark is what Ikshana is: a camera lens whose aperture
is an eye (six blades round a glowing iris, the black pupil at its heart, the black hole of the
earlier drawings kept there), with three sound waves leaving it: it sees, and it speaks. It is
drawn as a real, well-made lens (brushed metal barrel, coated glass, catchlights), because
craft is what reads as trustworthy at a glance. No rings receding, no spiral (owner, 13 Nov).

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

Run from the repo root:  python store-assets/icon/build_icon.py
The SVG and XML need only Python. The PNGs need Node with Playwright (render.js) or, on the
owner's laptop, Edge and Pillow (render_edge.py).

Why rasters for the adaptive layers: gradients, blurs and clip paths are the icon, and a
VectorDrawable carries none of the blurs. The themed icon and the notification glyph are
vectors because they must be; they carry the reduced mark (barrel, aperture, waves).
"""
import json, math, os, subprocess, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# Everything is drawn inside the circle of radius 36 about the tile's centre, the part a
# launcher shows; the foreground is still scaled down a little, because at full size the barrel
# touched the edge of a round mask.
FG_SCALE = .88
CX, CY = 46.0, 54.0        # the lens sits left of centre; the waves balance it on the right
R_BODY = 26.5              # outer edge of the metal barrel
R_GLASS = 20.5             # the coated glass
APOTHEM = 8.6              # the aperture opening: a hexagon this far from centre to each edge
R_PUPIL = 3.4
WAVES = ((31.5, 3.4, 1.0), (36.5, 3.1, .85), (41.5, 2.8, .65))   # radius, width, opacity
WAVE_SPAN = 27.0           # degrees either side of horizontal


def pt(r, deg, cx=CX, cy=CY):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


def hexagon():
    rv = APOTHEM / math.cos(math.radians(30))
    return [pt(rv, 60 * k + 15) for k in range(6)]   # turned 15 degrees: a lens, not a nut


def blades():
    """Six aperture blades. Blade k is bounded by hexagon edge k, the continuation of that edge
    out to the glass, the glass's rim, and the continuation of edge k-1 back to the vertex: so
    each blade overlaps the next as real blades do, and their edges sweep like a shutter."""
    v = hexagon()
    def extend(a, b):
        # from b, along a->b, to the glass rim
        dx, dy = b[0] - a[0], b[1] - a[1]
        n = math.hypot(dx, dy); dx /= n; dy /= n
        fx, fy = b[0] - CX, b[1] - CY
        bq = fx * dx + fy * dy
        t = -bq + math.sqrt(bq * bq - (fx * fx + fy * fy - (R_GLASS + 2) ** 2))
        return b[0] + dx * t, b[1] + dy * t
    out = []
    for k in range(6):
        a, b = v[k], v[(k + 1) % 6]
        e_k = extend(a, b)                       # edge k carried past its far vertex
        e_prev = extend(v[(k - 1) % 6], a)       # edge k-1 carried past this blade's vertex
        ang = lambda p: math.degrees(math.atan2(p[1] - CY, p[0] - CX))
        a0, a1 = ang(e_k), ang(e_prev)
        # rim from e_k back to e_prev, the short way round
        d = (a1 - a0 + 540) % 360 - 180
        rim = [pt(R_GLASS + 2, a0 + d * i / 8) for i in range(1, 8)]
        poly = [a, b, e_k] + rim + [e_prev]
        pts = " ".join(f"{x:.2f},{y:.2f}" for x, y in poly)
        shade = "url(#bladeA)" if k % 2 == 0 else "url(#bladeB)"
        out.append(f'<polygon points="{pts}" fill="{shade}" stroke="#6FD3FF" stroke-opacity=".38" stroke-width=".35" stroke-linejoin="round"/>')
    return "".join(out)


def arc_path(r, span, cx=CX, cy=CY):
    x0, y0 = pt(r, -span, cx, cy); x1, y1 = pt(r, span, cx, cy)
    return f"M{x0:.2f},{y0:.2f} A{r:.2f},{r:.2f} 0 0,1 {x1:.2f},{y1:.2f}"


def ring_arc(r, a0, a1, colour, width, op):
    x0, y0 = pt(r, a0); x1, y1 = pt(r, a1)
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return (f'<path d="M{x0:.2f},{y0:.2f} A{r:.2f},{r:.2f} 0 {large},1 {x1:.2f},{y1:.2f}" fill="none" '
            f'stroke="{colour}" stroke-width="{width}" stroke-opacity="{op}" stroke-linecap="round"/>')


HEX = " ".join(f"{x:.2f},{y:.2f}" for x, y in hexagon())

DEFS = f"""<defs>
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#3A7BFF"/><stop offset=".45" stop-color="#1638B8"/><stop offset=".8" stop-color="#1A0F6B"/><stop offset="1" stop-color="#12053F"/></linearGradient><radialGradient id="bgGlow" cx="30%" cy="22%" r="60%"><stop offset="0" stop-color="#9FE7FF" stop-opacity=".45"/><stop offset="1" stop-color="#9FE7FF" stop-opacity="0"/></radialGradient><radialGradient id="spill" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#6FD3FF" stop-opacity=".55"/><stop offset=".55" stop-color="#2F6BFF" stop-opacity=".18"/><stop offset="1" stop-color="#2F6BFF" stop-opacity="0"/></radialGradient><radialGradient id="vignette" cx="50%" cy="50%" r="50%"><stop offset=".72" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity=".35"/></radialGradient>
<linearGradient id="sheen" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".07"/><stop offset=".5" stop-color="#FFFFFF" stop-opacity="0"/></linearGradient>
<radialGradient id="aura" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#6FD3FF" stop-opacity=".55"/><stop offset=".6" stop-color="#2F6BFF" stop-opacity=".18"/><stop offset="1" stop-color="#2F6BFF" stop-opacity="0"/></radialGradient>
<linearGradient id="metal" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F4F8FF"/><stop offset=".28" stop-color="#B9C7DE"/><stop offset=".55" stop-color="#5D6E8E"/><stop offset=".78" stop-color="#C9D5EA"/><stop offset="1" stop-color="#3A4763"/></linearGradient>
<linearGradient id="bevel" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#050A16"/><stop offset="1" stop-color="#34466E"/></linearGradient>
<radialGradient id="glass" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#12357A"/><stop offset=".7" stop-color="#081633"/><stop offset="1" stop-color="#03081A"/></radialGradient>
<linearGradient id="bladeA" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#22408A"/><stop offset="1" stop-color="#060C20"/></linearGradient>
<linearGradient id="bladeB" x1="1" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2B4FA6"/><stop offset="1" stop-color="#081028"/></linearGradient>
<radialGradient id="iris" cx="{CX}" cy="{CY}" r="{APOTHEM * 1.16:.2f}" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".38" stop-color="#A8ECFF"/><stop offset=".7" stop-color="#2FA8FF"/><stop offset="1" stop-color="#1550C8"/></radialGradient>
<linearGradient id="wave" gradientUnits="userSpaceOnUse" x1="{CX + 28}" y1="{CY - 20}" x2="{CX + 41}" y2="{CY + 20}"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".55" stop-color="#9FE7FF"/><stop offset="1" stop-color="#6FD3FF"/></linearGradient>
<linearGradient id="spec" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".55"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></linearGradient>
<clipPath id="glassClip"><circle cx="{CX}" cy="{CY}" r="{R_GLASS}"/></clipPath>
<filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation=".45"/></filter>
<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.6"/></filter>
<filter id="haze" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
<filter id="drop" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.8"/></filter>
</defs>"""

BACKGROUND = ('<rect width="108" height="108" fill="url(#bg)"/><rect width="108" height="108" fill="url(#bgGlow)"/>'
              '<rect width="108" height="108" fill="url(#sheen)"/>')

WAVES_SVG = "".join(
    f'<path d="{arc_path(r, WAVE_SPAN)}" fill="none" stroke="#6FD3FF" stroke-width="{w + 2.2:.2f}" stroke-opacity="{op * .45:.2f}" stroke-linecap="round" filter="url(#glow)"/>'
    f'<path d="{arc_path(r, WAVE_SPAN)}" fill="none" stroke="url(#wave)" stroke-width="{w:.2f}" stroke-opacity="{op:.2f}" stroke-linecap="round"/>'
    for r, w, op in WAVES)

ART = (
    # the light the lens gives off, and its shadow on the field
    f'<circle cx="{CX}" cy="{CY}" r="{R_BODY + 9}" fill="url(#aura)" filter="url(#haze)"/>'
    + f'<circle cx="{CX + 1.5}" cy="{CY + 3}" r="{R_BODY}" fill="#000" opacity=".6" filter="url(#drop)"/>'
    + WAVES_SVG
    # barrel: brushed metal, a dark bevel, the black inner barrel with its cyan index ring
    + f'<circle cx="{CX}" cy="{CY}" r="{R_BODY}" fill="url(#metal)"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_BODY - .6}" fill="none" stroke="#FFFFFF" stroke-opacity=".35" stroke-width=".4"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_BODY - 2.2}" fill="url(#bevel)"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_GLASS + 1.9}" fill="#03060F"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_GLASS + 1.0}" fill="none" stroke="#6FD3FF" stroke-opacity=".75" stroke-width=".45"/>'
    # glass and the shutter inside it
    + f'<circle cx="{CX}" cy="{CY}" r="{R_GLASS}" fill="url(#glass)"/>'
    + f'<g clip-path="url(#glassClip)">{blades()}<circle cx="{CX}" cy="{CY}" r="{APOTHEM * 2.1:.2f}" fill="url(#spill)"/></g>'
    # the iris lit from within, the pupil, its white-hot rim
    + f'<polygon points="{HEX}" fill="url(#iris)"/>'
    + f'<polygon points="{HEX}" fill="none" stroke="#FFFFFF" stroke-opacity=".5" stroke-width=".3"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_PUPIL + .7}" fill="#BDF0FF" opacity=".8" filter="url(#soft)"/>'
    + f'<circle cx="{CX}" cy="{CY}" r="{R_PUPIL}" fill="#000"/>'
    # the iris's light falling on the blades, and one soft violet reflection of the coating
    + f'<g clip-path="url(#glassClip)">'
    + f'<ellipse cx="{CX + 6}" cy="{CY + 9}" rx="9" ry="3.4" transform="rotate(-38 {CX + 6} {CY + 9})" fill="#A98BFF" opacity=".22" filter="url(#glow)"/>'
    # catchlights: a broad soft window top left, a sharp one, a small one opposite
    + f'<ellipse cx="{CX - 7.5}" cy="{CY - 8.5}" rx="10" ry="5.2" transform="rotate(-38 {CX - 7.5} {CY - 8.5})" fill="url(#spec)" opacity=".55"/>'
    + '</g>'
    + f'<ellipse cx="{CX - 4.6}" cy="{CY - 4.8}" rx="2.1" ry="1.1" transform="rotate(-40 {CX - 4.6} {CY - 4.8})" fill="#FFFFFF" opacity=".95"/>'
    + f'<circle cx="{CX + 5.6}" cy="{CY + 6.0}" r=".75" fill="#FFFFFF" opacity=".7"/>'
    # a glint of light on the barrel's top edge
    + ring_arc(R_BODY - .3, 220, 290, "#FFFFFF", .7, .8)
)


def svg(body):
    return f'<svg viewBox="0 0 108 108" width="108" height="108" xmlns="http://www.w3.org/2000/svg">{DEFS}{body}</svg>'


def scaled(body, k):
    return f'<g transform="translate(54 54) scale({k:.5f}) translate(-54 -54)">{body}</g>'


MASTER = svg(BACKGROUND + '<rect width="108" height="108" fill="url(#vignette)"/>' + ART)
FOREGROUND = svg(scaled(ART, FG_SCALE))      # transparent field
BACKGROUND_ONLY = svg(BACKGROUND)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


# ---- vectors: the reduced mark for the themed icon and the status bar --------------------
def mark_paths(k, colour):
    """The mark in one colour, scaled by k about the tile's centre: the barrel as a ring, the
    glass solid with the aperture cut out and the pupil inside it, and the three waves."""
    def p(x, y):
        return 54 + (x - 54) * k, 54 + (y - 54) * k
    def circle(r, cx=CX, cy=CY):
        x, y = p(cx - r, cy); rr = r * k
        return f"M{x:.2f},{y:.2f} a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0"
    out = (f'<path android:pathData="{circle(R_BODY - 1.6)}" android:strokeColor="{colour}" '
           f'android:strokeWidth="{2.6 * k:.2f}" android:fillColor="#00000000"/>')
    # the glass solid, the aperture cut out of it, the pupil left standing inside: a lens, not a nut
    hexd = "M" + " L".join(f"{p(x, y)[0]:.2f},{p(x, y)[1]:.2f}" for x, y in hexagon()) + "Z "
    out += (f'<path android:pathData="{circle(R_GLASS)} {hexd}" android:fillColor="{colour}" '
            f'android:fillType="evenOdd"/>')
    out += f'<path android:pathData="{circle(R_PUPIL + .6)}" android:fillColor="{colour}"/>'
    for r, w, _ in WAVES:
        x0, y0 = p(*pt(r, -WAVE_SPAN)); x1, y1 = p(*pt(r, WAVE_SPAN))
        out += (f'<path android:pathData="M{x0:.2f},{y0:.2f} A{r * k:.2f},{r * k:.2f} 0 0,1 {x1:.2f},{y1:.2f}" '
                f'android:strokeColor="{colour}" android:strokeWidth="{(w + .6) * k:.2f}" '
                f'android:strokeLineCap="round" android:fillColor="#00000000"/>')
    return out


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: the lens, its aperture and the three waves, in one colour the\n"
              "         launcher tints. Built by store-assets/icon/build_icon.py; do not edit by hand. -->\n"
              + mark_paths(.92, "#FF000000") + "\n</vector>\n")

NOTIFICATION = (VECTOR_HEAD.format(w=24)
                + "    <!-- Status-bar glyph: white on transparent, the same reduced mark, filling the canvas.\n"
                "         Built by store-assets/icon/build_icon.py. -->\n"
                + mark_paths(1.3, "#FFFFFFFF") + "\n</vector>\n")

ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')

FEATURE = f'''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{{font-family:"Space Grotesk";font-weight:700;src:url("../../app/src/main/res/font/space_grotesk_bold.ttf")}}
@font-face{{font-family:"Space Grotesk";font-weight:400;src:url("../../app/src/main/res/font/space_grotesk_regular.ttf")}}
body{{margin:0;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 50%,#0A1830 0%,#050B18 55%,#02050C 100%);
font-family:"Space Grotesk",Inter,system-ui,sans-serif;color:#F4F1EA}}
.icon{{position:absolute;left:84px;top:94px;width:312px;height:312px;border-radius:84px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.6)}}
.icon svg{{width:100%;height:100%;display:block}}
h1{{position:absolute;left:460px;top:150px;margin:0;font-size:92px;letter-spacing:-3px;font-weight:700}}
p{{position:absolute;left:464px;top:262px;margin:0;font-size:31px;color:#A8C8E8;max-width:520px;line-height:1.3}}
.ml{{position:absolute;left:466px;top:352px;font-size:26px;color:#6FB7F0}}
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
