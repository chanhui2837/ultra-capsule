#!/usr/bin/env python3
"""
DoppelgangerModel.java 의 근사 오프라인 미리보기.
- 파츠 계층/피벗/박스/UV 는 Java 모델과 동일한 수치를 쓴다.
- 포즈 공식(setupAnim)도 Java 와 같은 식으로 옮겼다.
※ 실제 게임 화면이 아니다. UV 방향·포즈 부호 점검용 근사 렌더러.
"""
import math
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, ".")
import gen_texture as G

TEX = Image.open(sys.argv[1] if len(sys.argv) > 1 else "doppelganger.png").convert("RGBA")
TP = TEX.load()
DEG = math.pi / 180.0


def clamp(v, a, b):
    return max(a, min(b, v))


# ---------------------------------------------------------------- 수학
def rot_zyx(x, y, z):
    """v' = Rz(Ry(Rx(v)))"""
    cx, sx = math.cos(x), math.sin(x)
    cy, sy = math.cos(y), math.sin(y)
    cz, sz = math.cos(z), math.sin(z)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mm(Rz, mm(Ry, Rx))


def mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mv(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


class Part:
    def __init__(self, name, off=(0, 0, 0), boxes=None):
        self.name = name
        self.off = list(off)
        self.rot = [0.0, 0.0, 0.0]
        self.boxes = boxes or []  # (tex, (x,y,z), (w,h,d), inflate)
        self.kids = {}

    def add(self, p):
        self.kids[p.name] = p
        return p

    def reset(self):
        self.rot = [0.0, 0.0, 0.0]
        for k in self.kids.values():
            k.reset()


# ---------------------------------------------------------------- 모델 정의 (Java 와 동일 수치)
def build():
    root = Part("root")
    legs = {}
    for side, sx, tex in (("r", -2.2, "leg_r"), ("l", 2.2, "leg_l")):
        leg = root.add(Part("leg_" + side, (sx, 11, 0), [(tex, (-1.5, 0, -1.5), (3, 13, 3), 0)]))
        leg.add(Part("foot_" + side, (0, 11, 0), [("foot", (-1.5, 0, -3.5), (3, 2, 5), 0)]))
    chest = root.add(Part("chest", (0, 11, 0), [
        ("torso", (-4, -13, -2), (8, 13, 4), 0),
        ("cloak", (-4, -13, -2), (8, 13, 4), 0.55)]))
    for nm, x, z in (("hem_fr", -2, -2.6), ("hem_fl", 2, -2.6), ("hem_br", -2, 2.6), ("hem_bl", 2, 2.6)):
        chest.add(Part(nm, (x, 0.2, z), [("hem", (-1, 0, -0.5), (2, 5, 1), 0)]))
    head = chest.add(Part("head", (0, -13, 0), [("head", (-4, -8, -4), (8, 6, 8), 0)]))
    head.add(Part("jaw", (0, -2, 3), [("jaw", (-4, 0, -7), (8, 2, 7), 0)]))
    head.add(Part("hood", (0, 0, 0), [("hood", (-4, -8, -4), (8, 8, 8), 0.6)]))
    for side, sx, arm_tex, sl_tex in (("r", -5.8, "arm_r", "sleeve_r"), ("l", 5.8, "arm_l", "sleeve_l")):
        arm = chest.add(Part("arm_" + side, (sx, -11, 0), [
            (arm_tex, (-1.5, -2, -1.5), (3, 12, 3), 0),
            (sl_tex, (-1.5, -2, -1.5), (3, 12, 3), 0.5)]))
        hand = arm.add(Part("hand_" + side, (0, 10, 0), [("palm", (-2, 0, -1), (4, 3, 2), 0)]))
        for i, fx in enumerate((-2, -1, 0, 1)):
            f = hand.add(Part("f%d_%s" % (i, side), (fx, 3, 0), [("finger1", (0, 0, -0.5), (1, 3, 1), 0)]))
            f.add(Part("t%d_%s" % (i, side), (0, 3, 0), [("finger2", (0, 0, -0.5), (1, 3, 1), 0)]))
        tx = 2 if side == "r" else -3
        th = hand.add(Part("thumb_" + side, (tx, 0.5, -0.5), [("finger1", (0, 0, -0.5), (1, 3, 1), 0)]))
        th.add(Part("thumbtip_" + side, (0, 3, 0), [("finger2", (0, 0, -0.5), (1, 2, 1), 0)]))
    return root


def find(p, name):
    if p.name == name:
        return p
    for k in p.kids.values():
        r = find(k, name)
        if r:
            return r
    return None


# ---------------------------------------------------------------- 포즈 (Java setupAnim 과 동일 식)
def pose(root, t, pos, spd, phase, atk, yaw_deg, pitch_deg):
    root.reset()
    g = lambda n: find(root, n)
    amp = clamp(spd, 0.0, 1.0)
    p = pos * 0.6662
    c = math.cos(p)
    ambush = phase == 1
    swing = math.sin(math.sqrt(atk) * math.pi) if atk > 0 else 0.0
    swingL = math.sin(clamp((atk - 0.2) / 0.8, 0.0, 1.0) * math.pi) if atk > 0 else 0.0
    twitch = (max(0.0, math.sin(t * 0.17)) ** 12) * (0.0 if ambush else 1.0)

    chest = g("chest")
    hunch = -0.04 if ambush else 0.26
    chest.rot[0] = hunch + 0.025 * math.sin(t * 0.07) + amp * 0.10 + swing * 0.38
    chest.rot[1] = c * 0.10 * amp + (math.sin(math.sqrt(atk) * math.pi * 2) * 0.28 if atk > 0 else 0.0)
    chest.rot[2] = c * 0.04 * amp
    chest.off[1] = 11 + 0.25 * math.sin(t * 0.07) - 0.6 * amp * math.sin(p) ** 2

    for s, sign in (("r", 1), ("l", -1)):
        leg = g("leg_" + s)
        leg.rot[0] = sign * c * 0.85 * amp
        g("foot_" + s).rot[0] = -leg.rot[0] * 0.6

    head = g("head")
    yaw = clamp(yaw_deg, -60, 60) * DEG
    pit = clamp(pitch_deg, -40, 40) * DEG
    head.rot[1] = yaw - chest.rot[1] * 0.6
    head.rot[0] = pit - chest.rot[0] * 0.85 + (-0.05 if ambush else -0.10) + 0.03 * math.sin(t * 0.05)
    tilt = 0.0 if ambush else (0.16 + 0.07 * math.sin(t * 0.031)) * (1.0 - amp * 0.6)
    head.rot[2] = tilt + twitch * 0.14
    g("jaw").rot[0] = (0.55 if ambush else 0.10 + 0.06 * math.sin(t * 0.13)) + swing * 0.5 + twitch * 0.5

    splay = 0.30 if ambush else 0.07 + 0.03 * math.sin(t * 0.06)
    for s, sign in (("r", 1), ("l", -1)):
        arm = g("arm_" + s)
        base = -1.25 if ambush else -0.05
        arm.rot[0] = base - sign * c * 0.55 * amp + 0.03 * math.sin(t * 0.05 + (0 if s == "r" else 1.7))
        arm.rot[2] = sign * splay
        if s == "r":
            arm.rot[0] -= swing * 1.5
            arm.rot[2] -= swing * 0.4
        else:
            arm.rot[0] -= swingL * 1.3
            arm.rot[2] += swingL * 0.4
        hand = g("hand_" + s)
        hand.rot[0] = 0.25 if ambush else 0.15
        for i in range(4):
            curl = (0.55 if ambush else 0.35) + 0.08 * math.sin(t * 0.09 + i * 0.9) + swing * 0.8
            curl += (i - 1.5) * 0.05
            f = g("f%d_%s" % (i, s))
            tp = g("t%d_%s" % (i, s))
            f.rot[2] = -sign * curl * 0.9
            tp.rot[2] = -sign * curl * 1.25
        th = g("thumb_" + s)
        th.rot[2] = -sign * (0.35 + swing * 0.4)
        g("thumbtip_" + s).rot[2] = -sign * 0.5

    for nm, front in (("hem_fr", True), ("hem_fl", True), ("hem_br", False), ("hem_bl", False)):
        leg = g("leg_r" if nm.endswith("r") else "leg_l")
        sw = leg.rot[0]
        h = g(nm)
        h.rot[0] = (min(0.0, sw) if front else max(0.0, sw)) * 0.6 - chest.rot[0] * 0.7 \
            + 0.05 * math.sin(t * 0.08 + len(nm) + (0 if front else 1.3))


# ---------------------------------------------------------------- 렌더
def face_corners(box, face):
    (x0, y0, z0), (w, h, d), inf = box
    X0, X1 = x0 - inf, x0 + w + inf
    Y0, Y1 = y0 - inf, y0 + h + inf
    Z0, Z1 = z0 - inf, z0 + d + inf
    # (u 시작점, u 끝점, v 시작점, v 끝점) 각 점은 3D
    if face == "north":  # z=Z0, u: x 증가, v: y 증가
        return (X0, Y0, Z0), (X1, Y0, Z0), (X0, Y1, Z0)
    if face == "south":  # z=Z1, u: x 감소
        return (X1, Y0, Z1), (X0, Y0, Z1), (X1, Y1, Z1)
    if face == "west":  # x=X0, u: z 감소(뒤→앞)
        return (X0, Y0, Z1), (X0, Y0, Z0), (X0, Y1, Z1)
    if face == "east":  # x=X1, u: z 증가(앞→뒤)
        return (X1, Y0, Z0), (X1, Y0, Z1), (X1, Y1, Z0)
    if face == "top":  # y=Y0, u: x 증가, v: z 감소(뒤→앞)
        return (X0, Y0, Z1), (X1, Y0, Z1), (X0, Y0, Z0)
    if face == "bottom":  # y=Y1, u: x 증가, v: z 증가(앞→뒤)
        return (X0, Y1, Z0), (X1, Y1, Z0), (X0, Y1, Z1)


SHADE = {"top": 1.0, "bottom": 0.5, "north": 0.8, "south": 0.8, "east": 0.62, "west": 0.62}
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0),
          "top": (0, -1, 0), "bottom": (0, 1, 0)}


def collect(part, parent_R, parent_T, out):
    R = mm(parent_R, rot_zyx(*part.rot))
    T = [parent_T[i] + mv(parent_R, part.off)[i] for i in range(3)]
    for (tex, o, size, inf) in part.boxes:
        out.append((R, T, tex, (o, size, inf)))
    for k in part.kids.values():
        collect(k, R, T, out)


def render(root, azimuth_deg, scale=8, size=(260, 340), yaw_body=0.0):
    quads = []
    boxes = []
    collect(root, [[1, 0, 0], [0, 1, 0], [0, 0, 1]], [0, 0, 0], boxes)
    phi = azimuth_deg * DEG
    cam_c = (math.sin(phi), 0.0, math.cos(phi))  # 카메라 방향(월드)
    cam_r = (math.cos(phi), 0.0, -math.sin(phi))
    for (R, T, tex, (o, sz, inf)) in boxes:
        fr = G.faces(tex)
        for face, (fx, fy, fw, fh) in fr.items():
            p00, p10, p01 = face_corners((o, sz, inf)[0:3] if False else (o, sz, inf), face)
            nrm = mv(R, NORMAL[face])
            # 모델→월드: X=x, Y=-y, Z=-z
            nw = (nrm[0], -nrm[1], -nrm[2])
            facing = nw[0] * cam_c[0] + nw[1] * cam_c[1] + nw[2] * cam_c[2]
            if facing < -0.0001:
                continue  # 뒷면 컬링 (알파 컷아웃은 NoCull 이지만 시각 단순화)
            for j in range(fh):
                for i in range(fw):
                    a = TP[fx + i, fy + j]
                    if a[3] < 128:
                        continue
                    pts = []
                    for (du, dv) in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                        s, t_ = du / fw, dv / fh
                        v = [p00[k] + (p10[k] - p00[k]) * s + (p01[k] - p00[k]) * t_ for k in range(3)]
                        w = mv(R, v)
                        w = [w[0] + T[0], w[1] + T[1], w[2] + T[2]]
                        W = (w[0], -w[1] + 24.0, -w[2])  # 월드(픽셀 단위, 위가 +Y)
                        sx = W[0] * cam_r[0] + W[2] * cam_r[2]
                        dep = W[0] * cam_c[0] + W[2] * cam_c[2]
                        pts.append((sx, W[1], dep))
                    depth = sum(p[2] for p in pts) / 4
                    sh = SHADE[face]
                    col = (int(a[0] * sh), int(a[1] * sh), int(a[2] * sh))
                    quads.append((depth, [(p[0], p[1]) for p in pts], col))
    quads.sort(key=lambda q: q[0])
    W_, H_ = size
    im = Image.new("RGB", (W_, H_), (46, 64, 84))
    d = ImageDraw.Draw(im)
    ox, oy = W_ / 2, H_ - 24
    for depth, pts, col in quads:
        poly = [(ox + x * scale, oy - y * scale) for (x, y) in pts]
        d.polygon(poly, fill=col, outline=col)
    return im


def sheet(path):
    root = build()
    views = []
    cases = [
        ("idle/stalk front", dict(t=30, pos=0, spd=0, phase=0, atk=0, yaw=0, pitch=0), 0),
        ("stalk 3/4", dict(t=30, pos=0, spd=0, phase=0, atk=0, yaw=0, pitch=0), 40),
        ("stalk side", dict(t=30, pos=0, spd=0, phase=0, atk=0, yaw=0, pitch=0), 90),
        ("stalk back", dict(t=30, pos=0, spd=0, phase=0, atk=0, yaw=0, pitch=0), 180),
        ("walk A (3/4)", dict(t=50, pos=1.9, spd=0.8, phase=0, atk=0, yaw=0, pitch=0), 40),
        ("walk B (side)", dict(t=50, pos=3.5, spd=0.8, phase=0, atk=0, yaw=0, pitch=0), 90),
        ("ambush front", dict(t=30, pos=0, spd=0, phase=1, atk=0, yaw=0, pitch=0), 0),
        ("ambush 3/4", dict(t=30, pos=0, spd=0.5, phase=1, atk=0, yaw=0, pitch=0), 40),
        ("attack 0.3 (3/4)", dict(t=30, pos=0, spd=0, phase=1, atk=0.3, yaw=0, pitch=0), 40),
        ("attack 0.6 (side)", dict(t=30, pos=0, spd=0, phase=1, atk=0.6, yaw=0, pitch=0), 90),
    ]
    tiles = []
    for name, kw, az in cases:
        pose(root, kw["t"], kw["pos"], kw["spd"], kw["phase"], kw["atk"], kw["yaw"], kw["pitch"])
        im = render(root, az)
        d = ImageDraw.Draw(im)
        d.text((6, 4), name, fill=(235, 235, 235))
        tiles.append(im)
    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    W_, H_ = tiles[0].size
    out = Image.new("RGB", (W_ * cols, H_ * rows))
    for i, t in enumerate(tiles):
        out.paste(t, ((i % cols) * W_, (i // cols) * H_))
    out.save(path)


if __name__ == "__main__":
    G.check_layout()
    sheet("preview_sheet.png")
    print("ok")
