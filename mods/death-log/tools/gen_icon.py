#!/usr/bin/env python3
"""Generates src/main/resources/assets/deathlog/icon.png: a simple flat
tombstone glyph on a solid background, built pixel-by-pixel with only the
standard library (struct/zlib), so no external tool or asset is used."""
import struct
import zlib

SIZE = 128
BG = (0x2B, 0x2B, 0x33, 0xFF)
STONE = (0xC9, 0xC9, 0xD1, 0xFF)
STONE_SHADOW = (0x9A, 0x9A, 0xA6, 0xFF)
CROSS = (0x2B, 0x2B, 0x33, 0xFF)


def make_pixels():
    px = [[BG for _ in range(SIZE)] for _ in range(SIZE)]

    # Tombstone body: rounded-top rectangle roughly centered, occupying the
    # lower 2/3 of the icon.
    left, right = 34, 94
    top, bottom = 40, 108
    dome_cy = top + (right - left) // 2

    for y in range(SIZE):
        for x in range(SIZE):
            if left <= x < right and top <= y < bottom:
                # Round the top corners by clipping outside the dome circle
                # when we're in the top half of the stone.
                if y < dome_cy:
                    cx = (left + right) / 2
                    r = (right - left) / 2
                    if (x - cx) ** 2 + (y - dome_cy) ** 2 > r * r:
                        continue
                color = STONE_SHADOW if x >= right - 6 else STONE
                px[y][x] = color

    # Simple cross carved into the stone face.
    bar_w = 6
    cross_cx = (left + right) // 2
    v_top, v_bottom = 58, 96
    for y in range(v_top, v_bottom):
        for x in range(cross_cx - bar_w // 2, cross_cx + bar_w // 2):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                px[y][x] = CROSS
    h_y = 70
    for y in range(h_y, h_y + bar_w):
        for x in range(cross_cx - 14, cross_cx + 14):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                px[y][x] = CROSS

    return px


def write_png(path, pixels):
    def chunk(tag, data):
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for (r, g, b, a) in row:
            raw.extend((r, g, b, a))

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    with open(path, "wb") as f:
        f.write(sig)
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", idat))
        f.write(chunk(b"IEND", b""))


if __name__ == "__main__":
    import sys

    out = sys.argv[1] if len(sys.argv) > 1 else "icon.png"
    write_png(out, make_pixels())
    print("wrote", out)
