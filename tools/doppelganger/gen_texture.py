#!/usr/bin/env python3
"""
도플갱어 텍스처 생성기 (64x64, 마인크래프트 박스 UV 규칙)

레퍼런스: 검은 후드 + 해골 낯짝 + 보라빛 발광 눈 + 크게 웃는 이빨 +
        너덜너덜한 검은 로브 + 뼈만 남은 긴 손가락/맨발.

LAYOUT 의 (u, v, w, h, d) 는 DoppelgangerModel.java 의 texOffs / addBox 크기와 반드시 같아야 한다.
실행하면:
  doppelganger.png       본 텍스처
  doppelganger_eyes.png  발광 눈 레이어 (눈 픽셀만 불투명)
"""
import math
import sys
from PIL import Image

OUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "."

# ---------------------------------------------------------------- 레이아웃
LAYOUT = {
    # name:      (u,  v,  w,  h, d)
    "head":      (0,   0,  8,  6, 8),
    "hood":      (32,  0,  8,  8, 8),
    "torso":     (0,  16,  8, 13, 4),
    "cloak":     (24, 16,  8, 13, 4),
    "leg_r":     (48, 16,  3, 13, 3),
    "arm_r":     (0,  33,  3, 12, 3),
    "arm_l":     (12, 33,  3, 12, 3),
    "sleeve_r":  (24, 33,  3, 12, 3),
    "sleeve_l":  (36, 33,  3, 12, 3),
    "leg_l":     (48, 33,  3, 13, 3),
    "jaw":       (0,  49,  8,  2, 7),
    "foot":      (30, 49,  3,  2, 5),
    "palm":      (46, 49,  4,  3, 2),
    "finger1":   (58, 49,  1,  3, 1),
    "finger2":   (58, 53,  1,  3, 1),
    "hem":       (0,  58,  2,  5, 1),
}


def footprint(u, v, w, h, d):
    return (u, v, 2 * (d + w), d + h)


def faces(name):
    u, v, w, h, d = LAYOUT[name]
    return {
        "west": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),  # 앞면(-z)
        "east": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),  # 뒷면(+z)
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
    }


def check_layout():
    used = {}
    for name, spec in LAYOUT.items():
        x0, y0, fw, fh = footprint(*spec)
        assert x0 + fw <= 64 and y0 + fh <= 64, (name, "out of texture", x0 + fw, y0 + fh)
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                if (x, y) in used:
                    raise AssertionError("UV overlap: %s vs %s at %s" % (name, used[(x, y)], (x, y)))
                used[(x, y)] = name
    return used


# ---------------------------------------------------------------- 색
BONE_HI = (206, 195, 187)
BONE = (170, 156, 150)
BONE_MID = (138, 125, 121)
BONE_LO = (104, 93, 92)
BONE_DK = (66, 58, 60)
CLOTH = [(7, 5, 9), (13, 10, 17), (20, 16, 26), (29, 24, 36), (40, 34, 50), (54, 47, 64)]
GLOW_CORE = (255, 255, 255)
GLOW_MID = (214, 190, 255)
GLOW_HALO = (150, 108, 255)
MOUTH = (5, 3, 7)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
P = img.load()
eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
E = eyes.load()


def hsh(x, y, s=0):
    h = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def cloth(v):
    v = max(0.0, min(0.999, v))
    return CLOTH[int(v * len(CLOTH))]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def put(x, y, c, a=255):
    if 0 <= x < 64 and 0 <= y < 64:
        P[x, y] = (c[0], c[1], c[2], a)


def paint_cloth(rect, seed, base=0.30, streak=0.34, grain=0.20, fade=0.16, ao=0.10, hem=0):
    """세로 주름 결이 있는 천. hem>0 이면 아래 가장자리를 들쭉날쭉 뜯는다 (알파 0)."""
    x0, y0, fw, fh = rect
    prof = []
    for lx in range(fw):
        prof.append(int(hsh(lx, 99, seed) * (hem + 1)) if hem else 0)
    for ly in range(fh):
        for lx in range(fw):
            v = base
            v += streak * (hsh(lx, 0, seed) - 0.5) * 1.4
            v += 0.09 * math.sin(lx * 1.9 + hsh(0, 0, seed) * 6.0)
            v += grain * (hsh(lx, ly, seed + 1) - 0.5) * 1.6
            v -= fade * (ly / max(1, fh - 1))
            if lx == 0 or lx == fw - 1:
                v -= ao
            if ly == 0:
                v -= ao * 0.6
            a = 255
            if hem and ly >= fh - hem:
                cut = hem - (fh - 1 - ly)  # 1..hem (아래일수록 큼)
                if prof[lx] >= (hem + 1 - cut) or hsh(lx, ly, seed + 7) < 0.10 * cut:
                    a = 0
            put(x0 + lx, y0 + ly, cloth(v), a)


def fill(rect, c, a=255):
    x0, y0, fw, fh = rect
    for ly in range(fh):
        for lx in range(fw):
            put(x0 + lx, y0 + ly, c, a)


def paint_bone(rect, seed, hi=BONE, lo=BONE_MID, joint_rows=(), dark_edge=True):
    x0, y0, fw, fh = rect
    for ly in range(fh):
        for lx in range(fw):
            t = hsh(lx, ly, seed)
            c = mix(lo, hi, 0.25 + 0.75 * t)
            if dark_edge and (lx == 0 or lx == fw - 1):
                c = mix(c, BONE_LO, 0.35)
            if ly in joint_rows:
                c = mix(c, BONE_DK, 0.55)
            put(x0 + lx, y0 + ly, c)


# ---------------------------------------------------------------- 얼굴 (8x8 격자, 앞 6행=머리 / 뒤 2행=턱)
#  D=후드 머리카락  B=뼈  b=뼈 그늘  E=눈  M=입 안  T=이빨
FACE = [
    "DDDBBDDD",
    "DDBBBBDD",
    "DbBBBBbD",
    "DbEBBEbD",
    "DbbBBbbD",
    "DMTMMTMD",
    "DTMTTMTD",
    "DDbBBbDD",
]
EYE_PIXELS = []  # 눈 레이어용 (텍스처 절대 좌표)


def paint_face():
    hx, hy, _, _ = faces("head")["north"]
    jx, jy, _, _ = faces("jaw")["north"]
    for row in range(8):
        for col in range(8):
            ch = FACE[row][col]
            if row < 6:
                X, Y = hx + col, hy + row
            else:
                X, Y = jx + col, jy + (row - 6)
            n = hsh(col, row, 31)
            if ch == "D":
                c = cloth(0.16 + 0.22 * n)
            elif ch == "B":
                c = mix(BONE_MID, BONE_HI, 0.35 + 0.65 * n)
                if col in (3, 4) and row <= 2:
                    c = mix(BONE, BONE_HI, 0.4 + 0.6 * n)  # 이마 중앙 하이라이트
            elif ch == "b":
                c = mix(BONE_DK, BONE_LO, 0.25 + 0.5 * n)
            elif ch == "E":
                c = GLOW_CORE
                EYE_PIXELS.append((X, Y))
            elif ch == "M":
                c = mix(MOUTH, CLOTH[1], n * 0.5)
            elif ch == "T":
                c = mix(BONE, BONE_HI, 0.3 + 0.6 * n)
            else:
                c = CLOTH[2]
            put(X, Y, c)
    # 눈 소켓 그림자: 눈 위/아래/옆 픽셀을 어둡게 (눈이 더 튀어 보이게)
    for (X, Y) in EYE_PIXELS:
        for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            tx, ty = X + dx, Y + dy
            px = P[tx, ty]
            # 얼굴 안쪽(뼈)일 때만 어둡게
            if px[0] > 60:
                put(tx, ty, mix((px[0], px[1], px[2]), BONE_DK, 0.7))
        # 눈 주변 은은한 보라 번짐
    # 눈 레이어 (발광)
    for (X, Y) in EYE_PIXELS:
        E[X, Y] = (GLOW_CORE[0], GLOW_CORE[1], GLOW_CORE[2], 255)
        for dx, dy, a in ((-1, 0, 150), (1, 0, 150), (0, -1, 120), (0, 1, 120),
                          (-1, -1, 60), (1, -1, 60), (-1, 1, 60), (1, 1, 60)):
            tx, ty = X + dx, Y + dy
            # 머리 앞면(8..15, 8..13) 범위 안에서만
            if hx <= tx < hx + 8 and hy <= ty < hy + 6:
                E[tx, ty] = (GLOW_HALO[0], GLOW_HALO[1], GLOW_HALO[2], a)
        # 눈 중심에 흰색 → 가장 가까운 4방향에 연보라
    # 본 텍스처에도 눈 옆 픽셀에 연보라를 살짝 섞는다
    for (X, Y) in EYE_PIXELS:
        for dx in (-1, 1):
            tx = X + dx
            px = P[tx, Y]
            put(tx, Y, mix((px[0], px[1], px[2]), GLOW_MID, 0.25))


# ---------------------------------------------------------------- 머리
def paint_head():
    f = faces("head")
    # 옆면: 어두운 머리카락 + 앞쪽 가장자리만 뼈 그늘
    for side in ("west", "east"):
        x0, y0, fw, fh = f[side]
        paint_cloth(f[side], seed=11 if side == "west" else 12, base=0.26, streak=0.4, fade=0.05)
        edge = fw - 1 if side == "west" else 0  # 앞면과 맞닿는 열
        for ly in range(1, fh):
            c = mix(BONE_DK, BONE_LO, hsh(ly, 5, 3))
            put(x0 + edge, y0 + ly, c)
        # 눈 모서리 광택 (측면에서도 눈빛이 비침)
        put(x0 + edge, y0 + 3, mix(BONE_LO, GLOW_MID, 0.55))
    paint_cloth(f["south"], seed=13, base=0.24, streak=0.5, fade=0.05)
    paint_cloth(f["top"], seed=14, base=0.30, streak=0.3, fade=0.0)
    # 가르마
    tx, ty, tw, th = f["top"]
    for ly in range(th):
        put(tx + 3, ty + ly, cloth(0.10))
    # 아래면: 입천장 (좌우 앞뒤 대칭: 이빨이 양쪽 가장자리)
    bx, by, bw, bh = f["bottom"]
    for ly in range(bh):
        for lx in range(bw):
            edge = (ly == 0 or ly == bh - 1) and 1 <= lx <= bw - 2
            put(bx + lx, by + ly, mix(BONE, BONE_HI, hsh(lx, ly, 8)) if edge else MOUTH)
    paint_face()


def paint_jaw():
    f = faces("jaw")
    for side in ("west", "east"):
        x0, y0, fw, fh = f[side]
        for ly in range(fh):
            for lx in range(fw):
                c = mix(BONE_LO, BONE_MID, hsh(lx, ly, 21))
                if ly == 0:
                    c = MOUTH
                put(x0 + lx, y0 + ly, c)
    x0, y0, fw, fh = f["south"]
    for ly in range(fh):
        for lx in range(fw):
            put(x0 + lx, y0 + ly, mix(CLOTH[1], CLOTH[3], hsh(lx, ly, 22)))
    # 위 = 아래턱 안쪽 (대칭 이빨)
    x0, y0, fw, fh = f["top"]
    for ly in range(fh):
        for lx in range(fw):
            edge = (ly == 0 or ly == fh - 1) and 1 <= lx <= fw - 2 and (lx % 2 == 1)
            put(x0 + lx, y0 + ly, mix(BONE, BONE_HI, hsh(lx, ly, 9)) if edge else MOUTH)
    x0, y0, fw, fh = f["bottom"]
    for ly in range(fh):
        for lx in range(fw):
            put(x0 + lx, y0 + ly, mix(BONE_DK, BONE_LO, hsh(lx, ly, 23)))


# ---------------------------------------------------------------- 후드
HOOD_OPEN = [
    "11111111",
    "11000011",
    "10000001",
    "10000001",
    "10000001",
    "10000001",
    "11000011",
    "11100111",
]


def paint_hood():
    f = faces("hood")
    paint_cloth(f["top"], seed=41, base=0.34, streak=0.35, fade=0.0)
    paint_cloth(f["bottom"], seed=42, base=0.10, streak=0.1, fade=0.0)
    paint_cloth(f["west"], seed=43, base=0.30, streak=0.55, grain=0.25, fade=0.12, hem=3)
    paint_cloth(f["east"], seed=44, base=0.30, streak=0.55, grain=0.25, fade=0.12, hem=3)
    paint_cloth(f["south"], seed=45, base=0.28, streak=0.55, grain=0.25, fade=0.12, hem=3)
    # 앞면: 얼굴 구멍(투명) + 후드 테두리
    x0, y0, fw, fh = f["north"]
    paint_cloth(f["north"], seed=46, base=0.30, streak=0.5, fade=0.04, ao=0.12)
    for ly in range(8):
        for lx in range(8):
            if HOOD_OPEN[ly][lx] == "0":
                put(x0 + lx, y0 + ly, CLOTH[0], 0)
    # 테두리 안쪽 가장자리를 어둡게 → 깊이감
    for ly in range(8):
        for lx in range(8):
            if HOOD_OPEN[ly][lx] == "1":
                near = False
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = lx + dx, ly + dy
                    if 0 <= nx < 8 and 0 <= ny < 8 and HOOD_OPEN[ny][nx] == "0":
                        near = True
                if near:
                    px = P[x0 + lx, y0 + ly]
                    put(x0 + lx, y0 + ly, mix((px[0], px[1], px[2]), CLOTH[0], 0.6))


# ---------------------------------------------------------------- 몸통
def paint_torso():
    f = faces("torso")
    for k, r in f.items():
        paint_cloth(r, seed=51 + len(k), base=0.18, streak=0.3, fade=0.10)
    # 가슴 틈: 어렴풋한 갈비뼈 (거의 안 보이는 정도)
    x0, y0, fw, fh = f["north"]
    for ly in (3, 5, 7):
        for lx in (2, 3, 4, 5):
            px = P[x0 + lx, y0 + ly]
            put(x0 + lx, y0 + ly, mix((px[0], px[1], px[2]), BONE_DK, 0.55))
    # 중앙 세로 이음
    for ly in range(fh):
        put(x0 + 3, y0 + ly, cloth(0.08))
        put(x0 + 4, y0 + ly, cloth(0.12))


def paint_cloak():
    f = faces("cloak")
    for k, r in f.items():
        if k in ("top", "bottom"):
            paint_cloth(r, seed=61, base=0.26, streak=0.2, fade=0.0)
        else:
            paint_cloth(r, seed=62 + len(k), base=0.30, streak=0.5, grain=0.26, fade=0.16, hem=4)
    # 앞 트임: 가운데 두 열은 더 어둡게 (로브가 벌어진 모양)
    x0, y0, fw, fh = f["north"]
    for ly in range(fh):
        a = P[x0 + 3, y0 + ly][3]
        b = P[x0 + 4, y0 + ly][3]
        put(x0 + 3, y0 + ly, cloth(0.10), a)
        put(x0 + 4, y0 + ly, cloth(0.06), b)
    # 어깨선 하이라이트
    for k in ("north", "south", "west", "east"):
        x0, y0, fw, fh = f[k]
        for lx in range(fw):
            px = P[x0 + lx, y0]
            put(x0 + lx, y0, mix((px[0], px[1], px[2]), CLOTH[5], 0.35))


# ---------------------------------------------------------------- 팔/소매/손
def paint_arm(name, seed):
    f = faces(name)
    for k, r in f.items():
        x0, y0, fw, fh = r
        if k in ("top", "bottom"):
            fill(r, CLOTH[1])
            continue
        for ly in range(fh):
            for lx in range(fw):
                if ly <= 7:
                    c = cloth(0.18 + 0.28 * hsh(lx, ly, seed) - 0.06 * (ly / 8))
                else:
                    # 팔꿈치 아래: 천 조각을 감은 뼈
                    t = hsh(lx, ly, seed + 3)
                    if (ly == 8 and lx % 2 == 0) or t < 0.22:
                        c = cloth(0.22 + 0.2 * t)
                    else:
                        c = mix(BONE_LO, BONE, 0.2 + 0.8 * t)
                        if lx == 0 or lx == fw - 1:
                            c = mix(c, BONE_DK, 0.4)
                put(x0 + lx, y0 + ly, c)


def paint_sleeve(name, seed):
    f = faces(name)
    for k, r in f.items():
        if k in ("top", "bottom"):
            paint_cloth(r, seed=seed, base=0.25, streak=0.2, fade=0.0)
        else:
            paint_cloth(r, seed=seed + len(k), base=0.30, streak=0.5, grain=0.28, fade=0.12, hem=4)
    # 소맷부리 헤진 부분에 은은한 회색 올 (레퍼런스의 팔꿈치 회색 자국)
    x0, y0, fw, fh = f["north"]
    for ly in (7, 8):
        for lx in range(fw):
            px = P[x0 + lx, y0 + ly]
            if px[3] > 0 and hsh(lx, ly, seed + 9) < 0.45:
                put(x0 + lx, y0 + ly, mix((px[0], px[1], px[2]), BONE_LO, 0.55))


def paint_palm():
    f = faces("palm")
    for k, r in f.items():
        paint_bone(r, seed=71 + len(k), hi=BONE, lo=BONE_MID)
    x0, y0, fw, fh = f["north"]
    for ly in range(fh):  # 손바닥 힘줄
        put(x0 + 1, y0 + ly, mix(BONE_MID, BONE_LO, 0.5))
        put(x0 + 2, y0 + ly, mix(BONE_MID, BONE_LO, 0.5))


def paint_finger(name, tip):
    f = faces(name)
    for k, r in f.items():
        x0, y0, fw, fh = r
        for ly in range(fh):
            for lx in range(fw):
                t = hsh(lx + len(k), ly, 81 + (1 if tip else 0))
                c = mix(BONE_MID, BONE_HI if not tip else BONE, 0.25 + 0.75 * t)
                if ly == 0 and not tip:
                    c = mix(c, BONE_DK, 0.5)  # 마디
                if tip and ly == fh - 1 and k in ("west", "east", "north", "south"):
                    c = mix((60, 50, 46), BONE_DK, 0.4)  # 갈라진 검은 손톱
                if k in ("top", "bottom"):
                    c = mix(BONE_MID, BONE_LO, 0.4)
                put(x0 + lx, y0 + ly, c)


# ---------------------------------------------------------------- 다리/발
def paint_leg(name, seed):
    f = faces(name)
    for k, r in f.items():
        x0, y0, fw, fh = r
        if k in ("top", "bottom"):
            fill(r, CLOTH[1])
            continue
        for ly in range(fh):
            for lx in range(fw):
                base = cloth(0.17 + 0.26 * hsh(lx, ly, seed) - 0.05 * (ly / fh))
                c = base
                if ly >= 7:
                    # 천이 해져서 뼈가 비치는 얼룩
                    patch = hsh(lx * 3 + len(k), ly, seed + 5) + (ly - 7) * 0.085
                    if patch > 0.82:
                        c = mix(BONE_LO, BONE_MID, hsh(lx, ly, seed + 6))
                    elif patch > 0.64:
                        c = mix(base, BONE_DK, 0.7)
                if ly >= fh - 2:  # 발목은 뼈
                    c = mix(BONE_LO, BONE, hsh(lx, ly, seed + 8))
                put(x0 + lx, y0 + ly, c)


def paint_foot():
    f = faces("foot")
    for k, r in f.items():
        x0, y0, fw, fh = r
        for ly in range(fh):
            for lx in range(fw):
                c = mix(BONE_MID, BONE_HI, hsh(lx, ly, 91 + len(k)) * 0.8)
                if k in ("bottom",):
                    c = mix(BONE_LO, BONE_MID, hsh(lx, ly, 92))
                put(x0 + lx, y0 + ly, c)
    # 윗면 발가락 마디 (앞뒤 대칭 줄)
    x0, y0, fw, fh = f["top"]
    for lx in range(fw):
        put(x0 + lx, y0 + 1, mix(BONE_MID, BONE_DK, 0.55))
        put(x0 + lx, y0 + fh - 2, mix(BONE_MID, BONE_DK, 0.55))
    put(x0 + 1, y0 + 0, BONE_DK)
    put(x0 + 1, y0 + fh - 1, BONE_DK)


def paint_hem():
    f = faces("hem")
    for k, r in f.items():
        if k in ("top", "bottom"):
            fill(r, CLOTH[1])
        else:
            paint_cloth(r, seed=95 + len(k), base=0.26, streak=0.5, fade=0.2, hem=3)


def main():
    check_layout()
    paint_head()
    paint_jaw()
    paint_hood()
    paint_torso()
    paint_cloak()
    paint_arm("arm_r", 101)
    paint_arm("arm_l", 102)
    paint_sleeve("sleeve_r", 111)
    paint_sleeve("sleeve_l", 112)
    paint_leg("leg_r", 121)
    paint_leg("leg_l", 122)
    paint_foot()
    paint_palm()
    paint_finger("finger1", False)
    paint_finger("finger2", True)
    paint_hem()
    img.save(OUT_DIR + "/doppelganger.png")
    eyes.save(OUT_DIR + "/doppelganger_eyes.png")
    print("saved. eye pixels:", EYE_PIXELS)


if __name__ == "__main__":
    main()
