"""Mirrors ZoneBarRenderer.render and Karoo's header/number to produce docs/preview.png."""
from PIL import Image, ImageDraw, ImageFont
import os

SS = 4  # supersampling for anti-aliasing only; geometry is unscaled
COLORS = [(0x8E, 0x8E, 0x93), (0x0A, 0x84, 0xFF), (0x30, 0xD1, 0x58), (0xFF, 0x9F, 0x0A), (0xFF, 0x37, 0x5F)]
DIMMED_ALPHA = 80
ZONES = [(90, 120), (121, 140), (141, 155), (156, 170), (171, 195)]
HRS = [112, 133, 148, 163, 182]
FONT = "/System/Library/Fonts/SFNS.ttf"


def zone_position(hr):
    for i, (lo, hi) in enumerate(ZONES):
        if lo <= hr <= hi:
            return i, (hr - lo) / (hi - lo)
    return None


def render_bar(width, height, position):
    """Returns RGBA image (transparent background) matching ZoneBarRenderer.render."""
    img = Image.new("RGBA", (width * SS, height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    n = len(ZONES)
    inset = width * 0.05
    bar = min(max(height * 0.06, 5), 12)
    mr = bar * 0.85
    bottom = height - height * 0.05 - (mr - bar / 2)
    top = bottom - bar
    gap = bar * 0.6
    seg = (width - 2 * inset - gap * (n - 1)) / n
    r = bar / 2
    S = lambda v: v * SS
    for i in range(n):
        left = inset + i * (seg + gap)
        a = 255 if position and i == position[0] else DIMMED_ALPHA
        d.rounded_rectangle([S(left), S(top), S(left + seg), S(bottom)], radius=S(r), fill=COLORS[i] + (a,))
    if position:
        ring = bar * 0.3
        outline = bar * 0.12
        extent = mr + ring / 2 + outline
        left = inset + position[0] * (seg + gap)
        if seg >= 2 * extent:
            cx = left + extent + (seg - 2 * extent) * position[1]
        else:
            cx = left + seg / 2
        cy = (top + bottom) / 2

        # Fill disk, white ring, and black outline are opaque and concentric, so they
        # equal stacked disks: outline outer edge, ring outer edge, ring inner edge.
        for radius, color in (
            (mr + ring / 2 + outline, (0, 0, 0, 255)),
            (mr + ring / 2, (255, 255, 255, 255)),
            (mr - ring / 2, COLORS[position[0]] + (255,)),
        ):
            d.ellipse([S(cx - radius), S(cy - radius), S(cx + radius), S(cy + radius)], fill=color)
    return img.resize((width, height), Image.LANCZOS)


def main():
    W, H = 1040, 1240
    canvas = Image.new("RGB", (W, H), (40, 40, 40))
    for row, hr in enumerate(HRS):
        y = 20 + row * 244
        for x, fw in ((20, 640), (680, 340)):
            field = Image.new("RGBA", (fw, 220), (0, 0, 0, 255))
            field.alpha_composite(render_bar(fw, 220, zone_position(hr)))
            d = ImageDraw.Draw(field)
            d.text((14, 12), "HR ZONE BAR", font=ImageFont.truetype(FONT, 22), fill="white")
            f = ImageFont.truetype(FONT, int(min(96, fw / 3.2)))
            d.text((fw - 14, 128), str(hr), font=f, fill="white", anchor="rs")
            canvas.paste(field.convert("RGB"), (x, y))
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview.png")
    canvas.save(out)


if __name__ == "__main__":
    main()
