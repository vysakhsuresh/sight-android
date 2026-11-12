#!/usr/bin/env python3
"""Renders build_icon.py's jobs with Microsoft Edge in headless mode, for a Windows laptop with no
Node (12 Nov 2026: the owner's laptop has Edge and Pillow, not Node or Playwright).

Each job is drawn at 1024 px or more and scaled down with Pillow's Lanczos filter: headless
Edge will not open a window as small as a 48 px icon, and supersampling gives cleaner edges on
the fine streams than drawing at the target size. Round and rounded-square crops are masks
applied after the scale, as render.js applies them with CSS.

    python store-assets/icon/render_edge.py store-assets/icon/jobs.json
"""
import json, os, subprocess, sys, tempfile
from PIL import Image, ImageDraw

EDGE = next((p for p in (r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
                         r"C:\Program Files\Microsoft\Edge\Application\msedge.exe") if os.path.exists(p)), None)


def shoot(html_path, w, h, out, scale=1):
    subprocess.run([EDGE, "--headless=new", f"--force-device-scale-factor={scale}", "--disable-gpu", "--hide-scrollbars", "--no-first-run",
                    "--default-background-color=00000000", f"--window-size={w},{h}",
                    # its own profile, or a running Edge takes the job and no file is written
                    "--user-data-dir=" + os.path.join(tempfile.gettempdir(), "ikshana-edge-profile"),
                    f"--screenshot={out}", "file:///" + html_path.replace("\\", "/")],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
    # msedge.exe is a launcher: it returns before the headless browser has written the file.
    import time
    for _ in range(300):
        if os.path.exists(out) and os.path.getsize(out) > 0:
            time.sleep(0.3)   # let the write finish
            return
        time.sleep(0.1)
    raise RuntimeError(f"Edge wrote no screenshot for {html_path}")


def main(jobs_path):
    if EDGE is None:
        sys.exit("Microsoft Edge not found")
    here = os.path.dirname(os.path.abspath(jobs_path))
    jobs = json.load(open(jobs_path, encoding="utf-8"))
    tmp = tempfile.mkdtemp(prefix="ikshana-icon-")
    for i, j in enumerate(jobs):
        os.makedirs(os.path.dirname(j["out"]), exist_ok=True)
        shot = os.path.join(tmp, f"{i}.png")
        if "html" in j:
            # Laid out at its own css size, drawn at twice the pixels, scaled back down.
            shoot(os.path.join(here, j["html"]), j["width"], j["height"], shot, scale=2)
            img = Image.open(shot).convert("RGBA").resize((j["width"], j["height"]), Image.LANCZOS)
        else:
            big = max(1024, j["size"])
            svg = open(os.path.join(here, j["svg"]), encoding="utf-8").read()
            svg = svg.replace('width="108" height="108"', f'width="{big}" height="{big}"')
            page = os.path.join(tmp, f"{i}.html")
            with open(page, "w", encoding="utf-8") as f:
                f.write(f'<html><body style="margin:0;background:transparent">{svg}</body></html>')
            shoot(page, big, big, shot)
            img = Image.open(shot).convert("RGBA").crop((0, 0, big, big)).resize((j["size"], j["size"]), Image.LANCZOS)
            if j.get("radius"):
                s = j["size"] * 4
                mask = Image.new("L", (s, s), 0)
                ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(j["radius"] * s), fill=255)
                mask = mask.resize((j["size"], j["size"]), Image.LANCZOS)
                img.putalpha(Image.composite(img.getchannel("A"), Image.new("L", img.size, 0), mask))
        if not j.get("transparent"):
            bg = Image.new("RGBA", img.size, (0, 0, 0, 255))
            img = Image.alpha_composite(bg, img)
        img.save(j["out"])
    print(f"rendered {len(jobs)} files with Edge")


if __name__ == "__main__":
    main(sys.argv[1])
