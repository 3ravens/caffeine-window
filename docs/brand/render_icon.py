"""Renders the launcher icon (docs/brand/app-icon.svg) to PNG with Pillow.

Usage: python docs/brand/render_icon.py <out_dir>
Writes play-store-icon-512.png (full square, Play applies its own mask) and
preview.png (circle/squircle masks at launcher sizes, for eyeballing).

Geometry mirrors app-icon.svg and ic_launcher_foreground.xml, in 108-unit
adaptive-icon coordinates. Keep all three in sync when the logo changes.
"""

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

BACKGROUND = (0xBE, 0x7A, 0x3D)
GLYPH = (0xED, 0xE2, 0xC4)
SS = 8  # supersampling factor


def cubic(p0, p1, p2, p3, steps=64):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        pts.append((
            u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
            u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1],
        ))
    return pts


def arc(cx, cy, r, start_deg, end_deg, steps=128):
    return [
        (cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
        for a in (start_deg + (end_deg - start_deg) * i / steps for i in range(steps + 1))
    ]


def stroke(draw, pts, width, scale):
    """Round-capped, round-joined stroke: stamps a disc every pixel along the path."""
    r = width * scale / 2
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        x0, y0, x1, y1 = x0 * scale, y0 * scale, x1 * scale, y1 * scale
        n = max(1, math.ceil(math.hypot(x1 - x0, y1 - y0)))
        for i in range(n + 1):
            x, y = x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n
            draw.ellipse((x - r, y - r, x + r, y + r), fill=GLYPH)


def render(size):
    scale = size * SS / 108
    img = Image.new("RGB", (size * SS, size * SS), BACKGROUND)
    d = ImageDraw.Draw(img)

    # steam into the clock face: S-curve, then a 300 degree arc open at the bottom
    steam = cubic((47, 60), (47, 53.5), (42.5, 49.4), (39.5, 46.4)) + arc(48, 37.9, 12, 135, 435)
    stroke(d, steam, 3.2, scale)
    # clock hands
    stroke(d, [(48, 37.9), (48, 30.2)], 3.2, scale)
    stroke(d, [(48, 37.9), (53.8, 43.7)], 3.2, scale)
    # handle ring, overlapping the cup's right side
    stroke(d, arc(71, 68.5, 6, 0, 360), 3.2, scale)
    # cup body
    cup = cubic((67.2, 60), (67.2, 72.5), (59, 82.1), (48, 82.1)) + cubic(
        (48, 82.1), (37, 82.1), (28.8, 72.5), (28.8, 60)
    )
    d.polygon([(x * scale, y * scale) for x, y in cup], fill=GLYPH)

    return img.resize((size, size), Image.LANCZOS)


def masked(img, radius_ratio):
    mask = Image.new("L", (img.width * SS, img.height * SS), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, mask.width - 1, mask.height - 1), radius=mask.width * radius_ratio, fill=255
    )
    mask = mask.resize(img.size, Image.LANCZOS)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    render(512).save(out_dir / "play-store-icon-512.png")

    sheet = Image.new("RGBA", (760, 300), (238, 238, 238, 255))
    big = render(256)
    guide = ImageDraw.Draw(big)
    c, r = 128, 256 * 33 / 108  # 66-unit safe zone
    guide.ellipse((c - r, c - r, c + r, c + r), outline=(255, 0, 0))
    sheet.paste(big, (20, 20))
    x = 300
    for size, ratio in ((160, 0.5), (160, 0.28), (48, 0.5), (32, 0.5)):
        icon = masked(render(size), ratio)
        sheet.paste(icon, (x, 20), icon)
        x += size + 20
    sheet.save(out_dir / "preview.png")


if __name__ == "__main__":
    main()
