import math, itertools

PALETTE = {
    "HOSTILE": 0xD55E00,
    "NEUTRAL": 0xF0E442,
    "PLAYERS": 0x56B4E9,
    "BLOCKS": 0xE69F00,
    "WEATHER": 0x0072B2,  # current, to be replaced
    "AMBIENT": 0x009E73,
    "VOICE": 0xCC79A7,
}

def hex_to_rgb(h):
    return ((h >> 16) & 0xFF, (h >> 8) & 0xFF, h & 0xFF)

def srgb_to_linear(c):
    c = c / 255.0
    if c <= 0.04045:
        return c / 12.92
    return ((c + 0.055) / 1.055) ** 2.4

def linear_to_srgb(c):
    if c <= 0.0031308:
        v = c * 12.92
    else:
        v = 1.055 * (c ** (1/2.4)) - 0.055
    return max(0.0, min(1.0, v))

def rel_luminance(rgb):
    r, g, b = [srgb_to_linear(c) for c in rgb]
    return 0.2126*r + 0.7152*g + 0.0722*b

def contrast_vs_black(rgb):
    L = rel_luminance(rgb)
    return (L + 0.05) / 0.05

# Simplified Brettel-derived protanopia/deuteranopia simulation matrices
# (linear RGB in, linear RGB out), the commonly used "simple" matrices.
PROT = [
    [0.567, 0.433, 0.000],
    [0.558, 0.442, 0.000],
    [0.000, 0.242, 0.758],
]
DEUT = [
    [0.625, 0.375, 0.000],
    [0.700, 0.300, 0.000],
    [0.000, 0.300, 0.700],
]

def apply_matrix(rgb_linear, M):
    r, g, b = rgb_linear
    return (
        M[0][0]*r + M[0][1]*g + M[0][2]*b,
        M[1][0]*r + M[1][1]*g + M[1][2]*b,
        M[2][0]*r + M[2][1]*g + M[2][2]*b,
    )

# sRGB (D65) -> XYZ, linear RGB in [0,1]
def linear_to_xyz(rgb_linear):
    r, g, b = rgb_linear
    X = 0.4124564*r + 0.3575761*g + 0.1804375*b
    Y = 0.2126729*r + 0.7151522*g + 0.0721750*b
    Z = 0.0193339*r + 0.1191920*g + 0.9503041*b
    return (X*100, Y*100, Z*100)

Xn, Yn, Zn = 95.0489, 100.0, 108.8840

def f(t):
    d = 6/29
    if t > d**3:
        return t ** (1/3)
    return t/(3*d*d) + 4/29

def xyz_to_lab(xyz):
    X, Y, Z = xyz
    fx, fy, fz = f(X/Xn), f(Y/Yn), f(Z/Zn)
    L = 116*fy - 16
    a = 500*(fx - fy)
    b = 200*(fy - fz)
    return (L, a, b)

def rgb_to_lab(rgb255, cvd=None):
    linear = [srgb_to_linear(c) for c in rgb255]
    if cvd is not None:
        linear = apply_matrix(linear, cvd)
        linear = [max(0.0, min(1.0, c)) for c in linear]
    xyz = linear_to_xyz(linear)
    return xyz_to_lab(xyz)

def delta_e76(lab1, lab2):
    return math.sqrt(sum((a-b)**2 for a, b in zip(lab1, lab2)))

def min_delta_e_all_visions(rgb255, other_rgb255):
    des = []
    for cvd in (None, PROT, DEUT):
        lab1 = rgb_to_lab(rgb255, cvd)
        lab2 = rgb_to_lab(other_rgb255, cvd)
        des.append(delta_e76(lab1, lab2))
    return min(des), des

# others (excluding WEATHER itself)
others = {k: hex_to_rgb(v) for k, v in PALETTE.items() if k != "WEATHER"}

def evaluate(rgb):
    contrast = contrast_vs_black(rgb)
    result = {"contrast": contrast, "ok_contrast": contrast >= 4.5, "per_color": {}}
    worst_min = 999
    for name, other in others.items():
        m, des = min_delta_e_all_visions(rgb, other)
        result["per_color"][name] = {"min_de": m, "des": des}
        worst_min = min(worst_min, m)
    result["worst_min_de"] = worst_min
    result["ok_de"] = worst_min >= 20
    return result

# Search over blue-ish hues: R low-ish, G varies, B high, biased "blue" not cyan
candidates = []
for r in range(0, 90, 6):
    for g in range(60, 170, 6):
        for b in range(150, 256, 6):
            rgb = (r, g, b)
            res = evaluate(rgb)
            if res["ok_contrast"] and res["ok_de"]:
                candidates.append((rgb, res))

print(f"Found {len(candidates)} candidates meeting both criteria")
# sort by: high min ΔE from PLAYERS specifically, then reasonably vivid, then closest to original hue family
candidates.sort(key=lambda c: (-c[1]["per_color"]["PLAYERS"]["min_de"], -c[1]["contrast"]))
for rgb, res in candidates[:15]:
    print(f"#{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}  contrast={res['contrast']:.2f}  worst_min_dE={res['worst_min_de']:.1f}  players_dE={res['per_color']['PLAYERS']['min_de']:.1f}")
