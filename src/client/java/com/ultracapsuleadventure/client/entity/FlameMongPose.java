package com.ultracapsuleadventure.client.entity;

/**
 * 화염몽 애니메이션 계산기 (마인크래프트 클래스 의존 없음).
 * 입력(걸음 위상/속도/시간/시선/입/공격/등반)으로부터 각 부위의 위치 오프셋(dx,dy,dz)과 회전(rx,ry,rz)을 구한다.
 * 모델은 결과 배열을 부위에 그대로 적용만 한다. 같은 코드를 오프라인 미리보기에서도 실행해 자세를 검증한다.
 *
 * 좌표 규칙(마크): y 아래, 앞쪽 -z. xRot 음수 = 팔을 앞으로 듦 / 머리를 위로 / 아래턱 닫힘 방향은 양수가 열림.
 */
public final class FlameMongPose {

	private FlameMongPose() {
	}

	/** 애니메이션되는 부위 이름 (결과 배열 순서). 6칸씩: dx,dy,dz,rx,ry,rz */
	public static final String[] NAMES = {
			"body", "head", "jaw", "ear_l", "ear_r", "crest", "crest_l", "crest_r",
			"mane_l", "mane_r", "spine_a", "spine_b", "spine_c",
			"tail", "tail2", "tail_tip", "tail_fire",
			"arm_l", "fore_l", "fist_l", "arm_r", "fore_r", "fist_r",
			"leg_l", "shin_l", "foot_l", "leg_r", "shin_r", "foot_r"
	};
	public static final int COUNT = NAMES.length;

	private static final int BODY = 0, HEAD = 1, JAW = 2, EAR_L = 3, EAR_R = 4, CREST = 5, CREST_L = 6, CREST_R = 7,
			MANE_L = 8, MANE_R = 9, SPINE_A = 10, SPINE_B = 11, SPINE_C = 12,
			TAIL = 13, TAIL2 = 14, TAIL_TIP = 15, TAIL_FIRE = 16,
			ARM_L = 17, FORE_L = 18, FIST_L = 19, ARM_R = 20, FORE_R = 21, FIST_R = 22,
			LEG_L = 23, SHIN_L = 24, FOOT_L = 25, LEG_R = 26, SHIN_R = 27, FOOT_R = 28;

	/** 기본(정지) 회전: 모델 PartPose의 초기 회전과 같아야 한다. 결과는 이 값을 포함한 "절대" 회전이다. */
	private static final float[][] REST = new float[COUNT][3];

	static {
		set(MANE_L, 0, 0, 0.45f);
		set(MANE_R, 0, 0, -0.45f);
		set(SPINE_A, 0.15f, 0, 0);
		set(SPINE_B, 0.35f, 0, 0);
		set(SPINE_C, 0.6f, 0, 0);
		set(TAIL, 0.55f, 0, 0);
		set(TAIL2, 0.6f, 0, 0);
		set(TAIL_TIP, 0.5f, 0, 0);
		set(TAIL_FIRE, 0.3f, 0, 0);
		set(EAR_L, 0, 0.2f, 0.3f);
		set(EAR_R, 0, -0.2f, -0.3f);
		set(CREST, -0.3f, 0, 0);
		set(CREST_L, -0.15f, 0, 0.45f);
		set(CREST_R, -0.15f, 0, -0.45f);
		set(JAW, 0.08f, 0, 0);
	}

	private static void set(int i, float x, float y, float z) {
		REST[i][0] = x;
		REST[i][1] = y;
		REST[i][2] = z;
	}

	public static float[] restRotation(int i) {
		return REST[i];
	}

	public static float[] newBuffer() {
		return new float[COUNT * 6];
	}

	// ───── 유틸
	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	private static float smooth(float e0, float e1, float x) {
		float t = clamp((x - e0) / (e1 - e0), 0f, 1f);
		return t * t * (3f - 2f * t);
	}

	private static float sin(float v) {
		return (float) Math.sin(v);
	}

	private static float cos(float v) {
		return (float) Math.cos(v);
	}

	private static void rot(float[] o, int part, float rx, float ry, float rz) {
		int b = part * 6;
		o[b + 3] += rx;
		o[b + 4] += ry;
		o[b + 5] += rz;
	}

	private static void pos(float[] o, int part, float dx, float dy, float dz) {
		int b = part * 6;
		o[b] += dx;
		o[b + 1] += dy;
		o[b + 2] += dz;
	}

	/**
	 * @param walkPos  걸음 위상 (state.walkAnimationPos)
	 * @param walkSpd  걸음 속도 (state.walkAnimationSpeed)
	 * @param age      ageInTicks
	 * @param headYaw  머리 좌우(도)
	 * @param headPitch 머리 상하(도)
	 * @param mouth    입 벌림 틱 (0이면 닫힘)
	 * @param attack   공격 스윙 진행도 0~1
	 * @param climbing 벽/나무 오르는 중
	 * @param out      길이 COUNT*6, 호출 전에 비워짐(0). 결과는 절대 회전 + 위치 오프셋
	 */
	public static void solve(float walkPos, float walkSpd, float age, float headYaw, float headPitch, int mouth,
			float attack, boolean climbing, float[] out) {
		java.util.Arrays.fill(out, 0f);
		for (int i = 0; i < COUNT; i++) {
			out[i * 6 + 3] = REST[i][0];
			out[i * 6 + 4] = REST[i][1];
			out[i * 6 + 5] = REST[i][2];
		}

		final float DEG = 0.017453292f;
		float amp = clamp(walkSpd * 1.7f, 0f, 1f);
		float run = clamp((walkSpd - 0.45f) * 2f, 0f, 1f); // 질주 정도
		float ph = walkPos * 0.8f;
		float c = cos(ph), s = sin(ph);
		float c2 = cos(ph * 2f);

		// 공격 곡선: 0~0.35 치켜듦, 0.35~0.55 내려침, 이후 복귀
		float a = clamp(attack, 0f, 1f);
		float wind = smooth(0.0f, 0.34f, a) * (1f - smooth(0.36f, 0.5f, a));
		float slam = smooth(0.34f, 0.5f, a) * (1f - smooth(0.52f, 0.9f, a));
		boolean atk = a > 0f && a < 1f;

		float mouthOpen = clamp(mouth / 5f, 0f, 1f);

		if (climbing) {
			solveClimb(walkPos, walkSpd, age, headYaw, headPitch, mouthOpen, out);
			return;
		}

		// ── 몸: 앞으로 숙인 너클워크 자세
		float lean = 0.42f + 0.12f * run + 0.06f * slam;
		float idle = (1f - amp);
		float breathe = sin(age * 0.09f);
		// 골반 높이: 보폭 끝에서 낮아지고(발이 땅에 닿게) 몸 아래로 지나갈 때 올라감. 다리 피벗도 같이 움직여 몸과 분리되지 않음
		float bob = c * c;
		float hipDrop = 1.1f * amp * bob;
		pos(out, BODY, 0f, hipDrop - 0.5f * amp * (1f - bob) - 0.3f * breathe * idle + 0.5f * slam, 0f);
		pos(out, LEG_L, 0f, hipDrop, 0f);
		pos(out, LEG_R, 0f, hipDrop, 0f);
		rot(out, BODY, lean + 0.015f * breathe * idle - 0.18f * wind, s * 0.09f * amp, s * 0.035f * amp);

		// ── 머리: 몸 숙임을 상쇄해서 앞을 봄 + 시선 추적 + 주기적 두리번
		float look = sin(age * 0.031f) * 0.11f * idle;
		float yaw = clamp(headYaw, -55f, 55f) * DEG + look - s * 0.07f * amp;
		float pit = clamp(headPitch, -35f, 35f) * DEG;
		rot(out, HEAD, -lean * 0.85f + pit + 0.04f * c2 * amp + 0.28f * slam - 0.12f * wind - 0.14f * mouthOpen, yaw, 0f);

		// ── 팔: 대각 보행 (왼팔 + 오른발 / 오른팔 + 왼발)
		float swingA = 0.42f + 0.1f * run;
		float aL = c * swingA * amp;
		float aR = -c * swingA * amp;
		// 팔꿈치: 앞으로 나올 때(회수) 접어서 손을 땅에서 들어올림
		float flexL = clamp(-aL, 0f, 1f) * 1.5f;
		float flexR = clamp(-aR, 0f, 1f) * 1.5f;
		rot(out, ARM_L, -lean + aL + 0.03f * breathe * idle + 0.3f * wind, 0f, 0.06f + 0.02f * breathe * idle);
		rot(out, ARM_R, -lean + aR - 0.03f * breathe * idle - 2.55f * wind + 0.35f * slam, 0f, -0.06f - 0.02f * breathe * idle);
		rot(out, FORE_L, -flexL - 0.35f * wind, 0f, 0f);
		rot(out, FORE_R, -flexR - 0.7f * wind, 0f, 0f);
		// 손목: 땅을 짚을 때 주먹이 바닥과 평행 → 앞팔 각도 보정
		rot(out, FIST_L, 0.5f * flexL * 0.0f + clamp(aL, 0f, 1f) * 0.5f, 0f, 0f);
		rot(out, FIST_R, clamp(aR, 0f, 1f) * 0.5f, 0f, 0f);

		// ── 다리 (대각: 왼팔과 같은 위상에 오른발)
		float lgL = -c * 0.62f * amp;
		float lgR = c * 0.62f * amp;
		float thL = -0.26f + lgL;
		float thR = -0.26f + lgR;
		float knL = 0.42f + clamp(-lgL, 0f, 1f) * 1.25f;
		float knR = 0.42f + clamp(-lgR, 0f, 1f) * 1.25f;
		rot(out, LEG_L, thL, 0f, 0.03f);
		rot(out, LEG_R, thR, 0f, -0.03f);
		rot(out, SHIN_L, knL, 0f, 0f);
		rot(out, SHIN_R, knR, 0f, 0f);
		rot(out, FOOT_L, -(thL + knL) + clamp(-lgL, 0f, 1f) * 0.45f, 0f, 0f);
		rot(out, FOOT_R, -(thR + knR) + clamp(-lgR, 0f, 1f) * 0.45f, 0f, 0f);

		// ── 꼬리: 위로 말려 올라가 균형을 잡음. 마디마다 위상이 지연되는 파동
		float tw = age * 0.12f;
		rot(out, TAIL, 0.2f * run + 0.05f * sin(age * 0.07f) - 0.12f * s * amp, sin(tw) * 0.28f - s * 0.25f * amp, 0f);
		rot(out, TAIL2, 0.05f * sin(age * 0.07f + 1f) + 0.1f * run, sin(tw - 0.8f) * 0.34f - s * 0.2f * amp, 0f);
		rot(out, TAIL_TIP, 0.06f * sin(age * 0.07f + 2f), sin(tw - 1.6f) * 0.4f, 0f);
		rot(out, TAIL_FIRE, 0.1f * sin(age * 0.33f), sin(tw - 2.4f) * 0.35f, sin(age * 0.29f) * 0.15f);

		// ── 불꽃/털: 바람에 날리듯 뒤로 눕고 흔들림 (질주할수록 크게)
		float sweep = 0.12f + 0.4f * run + 0.14f * amp;
		rot(out, CREST, sweep + sin(age * 0.17f) * 0.1f - 0.25f * mouthOpen, 0f, sin(age * 0.13f) * 0.06f);
		rot(out, CREST_L, sweep * 0.8f + sin(age * 0.21f + 1f) * 0.1f, 0f, sin(age * 0.19f) * 0.08f);
		rot(out, CREST_R, sweep * 0.8f + sin(age * 0.23f + 2f) * 0.1f, 0f, -sin(age * 0.17f) * 0.08f);
		rot(out, SPINE_A, sweep * 0.9f + sin(age * 0.2f) * 0.08f, 0f, 0f);
		rot(out, SPINE_B, sweep * 1.1f + sin(age * 0.2f + 0.8f) * 0.09f, 0f, 0f);
		rot(out, SPINE_C, sweep * 1.3f + sin(age * 0.2f + 1.6f) * 0.1f, 0f, 0f);
		rot(out, MANE_L, 0f, 0f, sin(age * 0.11f) * 0.05f - s * 0.06f * amp);
		rot(out, MANE_R, 0f, 0f, -sin(age * 0.11f + 1f) * 0.05f + s * 0.06f * amp);

		// ── 귀: 가끔 쫑긋
		float twitch = smooth(0.92f, 1f, sin(age * 0.047f) * 0.5f + 0.5f);
		rot(out, EAR_L, 0f, 0f, sin(age * 0.09f) * 0.05f + twitch * 0.25f);
		rot(out, EAR_R, 0f, 0f, -sin(age * 0.09f + 0.7f) * 0.05f - twitch * 0.2f);

		// ── 입: 벌림(브레스) + 숨쉬는 미세한 움직임. 공격 치켜들 때 포효
		float jaw = 0.07f + sin(age * 0.05f) * 0.025f + mouthOpen * 0.75f + wind * 0.55f;
		rot(out, JAW, jaw - REST[JAW][0], 0f, 0f);
	}

	/** 나무/벽을 오르는 자세: 몸 거의 수직, 팔 번갈아 위로 뻗기, 다리 번갈아 밀기. */
	private static void solveClimb(float walkPos, float walkSpd, float age, float headYaw, float headPitch,
			float mouthOpen, float[] out) {
		final float DEG = 0.017453292f;
		float ph = age * 0.28f;
		float c = cos(ph), s = sin(ph);
		pos(out, BODY, 0f, -0.4f * Math.abs(c) , 0f);
		rot(out, BODY, -0.12f, s * 0.07f, 0f);
		float yaw = clamp(headYaw, -40f, 40f) * DEG;
		rot(out, HEAD, 0.12f + clamp(headPitch, -35f, 35f) * DEG * 0.5f, yaw, 0f);
		// 팔: 앞쪽 위로 뻗고(-2.5 근처) 번갈아 당김, 팔꿈치는 당길 때 굽힘
		float aL = -2.45f + c * 0.35f;
		float aR = -2.45f - c * 0.35f;
		rot(out, ARM_L, aL, 0f, 0.1f);
		rot(out, ARM_R, aR, 0f, -0.1f);
		rot(out, FORE_L, -0.25f - clamp(-c, 0f, 1f) * 0.7f, 0f, 0f);
		rot(out, FORE_R, -0.25f - clamp(c, 0f, 1f) * 0.7f, 0f, 0f);
		rot(out, FIST_L, -0.3f, 0f, 0f);
		rot(out, FIST_R, -0.3f, 0f, 0f);
		// 다리: 번갈아 무릎을 올려 디딤
		float lL = -1.0f - c * 0.4f;
		float lR = -1.0f + c * 0.4f;
		rot(out, LEG_L, lL, 0f, 0.12f);
		rot(out, LEG_R, lR, 0f, -0.12f);
		rot(out, SHIN_L, 1.5f + c * 0.3f, 0f, 0f);
		rot(out, SHIN_R, 1.5f - c * 0.3f, 0f, 0f);
		rot(out, FOOT_L, -(lL + 1.5f + c * 0.3f) - 0.3f, 0f, 0f);
		rot(out, FOOT_R, -(lR + 1.5f - c * 0.3f) - 0.3f, 0f, 0f);
		// 꼬리는 아래로 늘어뜨려 균형
		rot(out, TAIL, -0.9f, sin(age * 0.12f) * 0.2f, 0f);
		rot(out, TAIL2, -0.1f, sin(age * 0.12f - 0.8f) * 0.25f, 0f);
		rot(out, TAIL_TIP, -0.1f, sin(age * 0.12f - 1.6f) * 0.3f, 0f);
		rot(out, TAIL_FIRE, 0f, sin(age * 0.33f) * 0.3f, 0f);
		rot(out, CREST, 0.35f + sin(age * 0.17f) * 0.08f, 0f, 0f);
		rot(out, CREST_L, 0.3f, 0f, 0f);
		rot(out, CREST_R, 0.3f, 0f, 0f);
		rot(out, SPINE_A, 0.3f, 0f, 0f);
		rot(out, SPINE_B, 0.4f, 0f, 0f);
		rot(out, SPINE_C, 0.5f, 0f, 0f);
		rot(out, JAW, mouthOpen * 0.75f, 0f, 0f);
	}
}


