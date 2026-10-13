package com.partnercap.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 메카 로봇 (리디자인, 원점=머리 꼭대기, +Y 아래).
 * - 다리 3마디(허벅지·정강이·발), 관절 피벗을 실제 고관절/무릎 위치로 수정 (이전엔 다리 중간이 피벗이라 골반에서 벌어졌음)
 * - 팔 3마디(상완·전완·주먹), 견갑은 팔의 자식(팔을 들면 같이 움직임), 우팔 전완에 팔 장착 캐논
 * - 머리 피벗을 목으로 이동, 귀 장갑 추가, 골반 추가
 * - 포드는 캡슐볼 형태(캡 2 + 몸통)
 * - UV 겹침 없음 (128x128: 위쪽 128x64=겉모습, 아래쪽 y64~125=조종석)
 * - 조종석(cockpit): 탑승자 눈 위치(발+2.55)를 원점으로 한 닫힌 캐빈(바닥·계기판·천장·기둥·측벽·후벽·좌석·조종간).
 *   앞면 창은 '비어 있는' 개구부라 유리창처럼 밖이 보인다. 1인칭 탑승(state.cockpitView)일 때만 그려지고
 *   겉모습은 전부 숨긴다. 밖에서는 cockpit 파트가 항상 숨겨져 내부가 보이지 않는다.
 * 변신(u = 1 - transform, 0=캡슐 → 1=로봇, 되돌릴 땐 같은 곡선을 역재생):
 *   0.00~0.10 시동(진동, 이음매 팽창) → 0.06~0.20 뚜껑 개방(경첩) → 0.16~0.50 상체 코어 상승·확대
 *   → 0.20~0.56 다리 3마디 순차 전개(허벅지→정강이→발) + 아래 뚜껑이 지면에서 퍼졌다 소멸 → 0.30~0.50 중간 띠가 몸통으로 흡수
 *   → 0.40~0.56 백팩 → 0.52~0.90 견갑·팔·전완·주먹·캐논 순차 전개 → 0.56~0.80 뚜껑이 날아가 머리로 합체
 *   → 0.70~0.94 머리 기립·귀 장갑·바이저 점등 → 0.90~1.00 안착(살짝 눌렸다 복귀).
 */
public class MechRobotModel extends EntityModel<MechRenderState> {

	private static final float DEG = 0.017453292f;
	/** 조종석이 시선 pitch를 따라가는 비율 (1=헬멧처럼 고정, 0=완전 고정). 살짝 덜 따라가서 캐빈이 몸에 달린 느낌을 줌 */
	private static final float COCKPIT_PITCH_FOLLOW = 0.8f;

	private final ModelPart upper;
	private final ModelPart head;
	private final ModelPart visor;
	private final ModelPart earL;
	private final ModelPart earR;
	private final ModelPart torso;
	private final ModelPart core;
	private final ModelPart pack;
	private final ModelPart pelvis;
	private final ModelPart armL;
	private final ModelPart armR;
	private final ModelPart shoulderL;
	private final ModelPart shoulderR;
	private final ModelPart foreL;
	private final ModelPart foreR;
	private final ModelPart fistL;
	private final ModelPart fistR;
	private final ModelPart cannon;
	private final ModelPart legL;
	private final ModelPart legR;
	private final ModelPart shinL;
	private final ModelPart shinR;
	private final ModelPart footL;
	private final ModelPart footR;
	private final ModelPart podBand;
	private final ModelPart podRing;
	private final ModelPart podTop;
	private final ModelPart domeT;
	private final ModelPart plateT;
	private final ModelPart podBottom;
	private final ModelPart domeB;
	private final ModelPart plateB;
	private final ModelPart cockpit;

	public MechRobotModel(ModelPart root) {
		super(root);
		this.upper = root.getChild("upper");
		this.head = this.upper.getChild("head");
		this.visor = this.head.getChild("visor");
		this.earL = this.head.getChild("ear_l");
		this.earR = this.head.getChild("ear_r");
		this.torso = this.upper.getChild("torso");
		this.core = this.torso.getChild("core");
		this.pack = this.torso.getChild("pack");
		this.pelvis = this.upper.getChild("pelvis");
		this.armL = this.upper.getChild("arm_l");
		this.armR = this.upper.getChild("arm_r");
		this.shoulderL = this.armL.getChild("shoulder_l");
		this.shoulderR = this.armR.getChild("shoulder_r");
		this.foreL = this.armL.getChild("forearm_l");
		this.foreR = this.armR.getChild("forearm_r");
		this.fistL = this.foreL.getChild("fist_l");
		this.fistR = this.foreR.getChild("fist_r");
		this.cannon = this.foreR.getChild("cannon");
		this.legL = root.getChild("leg_l");
		this.legR = root.getChild("leg_r");
		this.shinL = this.legL.getChild("shin_l");
		this.shinR = this.legR.getChild("shin_r");
		this.footL = this.shinL.getChild("foot_l");
		this.footR = this.shinR.getChild("foot_r");
		this.podBand = root.getChild("pod_band");
		this.podRing = root.getChild("pod_ring");
		this.podTop = root.getChild("pod_top");
		this.domeT = this.podTop.getChild("dome_t");
		this.plateT = this.podTop.getChild("plate_t");
		this.podBottom = root.getChild("pod_bottom");
		this.domeB = this.podBottom.getChild("dome_b");
		this.plateB = this.podBottom.getChild("plate_b");
		this.cockpit = root.getChild("cockpit");
	}

	private static void arm(PartDefinition root, String side, float x, boolean withCannon) {
		PartDefinition arm = root.addOrReplaceChild("arm_" + side,
				CubeListBuilder.create().texOffs(80, 16).addBox(-2.5f, 0, -2.5f, 5, 6, 5),
				PartPose.offset(x, -12, 0));
		// 견갑: 팔의 자식 (피벗 = 어깨)
		arm.addOrReplaceChild("shoulder_" + side,
				CubeListBuilder.create().texOffs(48, 0).addBox(-3.5f, -6, -3.5f, 7, 6, 7),
				PartPose.ZERO);
		PartDefinition fore = arm.addOrReplaceChild("forearm_" + side,
				CubeListBuilder.create().texOffs(100, 16).addBox(-2.5f, 0, -2.5f, 5, 6, 5),
				PartPose.offset(0, 6, 0));
		fore.addOrReplaceChild("fist_" + side,
				CubeListBuilder.create().texOffs(80, 27).addBox(-2.5f, 0, -2.5f, 5, 3, 5),
				PartPose.offset(0, 6, 0));
		if (withCannon) {
			// 전완 장착 캐논 (앞쪽으로 뻗은 포신, 적광 팁)
			PartDefinition cannon = fore.addOrReplaceChild("cannon",
					CubeListBuilder.create().texOffs(100, 28).addBox(-2, 1, -9, 4, 4, 8),
					PartPose.ZERO);
			cannon.addOrReplaceChild("cannon_tip",
					CubeListBuilder.create().texOffs(116, 40).addBox(-1.5f, 1.5f, -10, 3, 3, 1),
					PartPose.ZERO);
		}
	}

	private static void leg(PartDefinition root, String side, float x) {
		PartDefinition leg = root.addOrReplaceChild("leg_" + side,
				CubeListBuilder.create().texOffs(0, 40).addBox(-3, 0, -3, 6, 9, 6),
				PartPose.offset(x, 24, 0));
		PartDefinition shin = leg.addOrReplaceChild("shin_" + side,
				CubeListBuilder.create().texOffs(24, 40).addBox(-3, 0, -3, 6, 9, 6),
				PartPose.offset(0, 9, 0));
		shin.addOrReplaceChild("foot_" + side,
				CubeListBuilder.create().texOffs(48, 40).addBox(-3.5f, 0, -5, 7, 3, 9),
				PartPose.offset(0, 8, 0));
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// 상체 그룹 (피벗 = 고관절 y=24): 변신 중 상체 전체를 한꺼번에 줄이고 올리기 위한 부모
		PartDefinition upper = root.addOrReplaceChild("upper", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
		// 머리 (피벗 = 목) + 바이저(청광, 피벗=바이저 중심) + 귀 장갑
		PartDefinition headDef = upper.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
				PartPose.offset(0, -16, 0));
		headDef.addOrReplaceChild("visor",
				CubeListBuilder.create().texOffs(32, 0).addBox(-3, -1, -1, 6, 2, 1),
				PartPose.offset(0, -4, -4));
		headDef.addOrReplaceChild("ear_l",
				CubeListBuilder.create().texOffs(32, 3).addBox(-0.5f, -1.5f, -1.5f, 1, 3, 3),
				PartPose.offset(-4.5f, -4, 0));
		headDef.addOrReplaceChild("ear_r",
				CubeListBuilder.create().texOffs(32, 3).addBox(-0.5f, -1.5f, -1.5f, 1, 3, 3),
				PartPose.offset(4.5f, -4, 0));
		// 가슴 + 코어(피벗=코어 중심) + 백팩(피벗=등판 면, z로 자라남)
		PartDefinition torsoDef = upper.addOrReplaceChild("torso",
				CubeListBuilder.create().texOffs(0, 16).addBox(-7, 8, -5, 14, 14, 10),
				PartPose.offset(0, -24, 0));
		torsoDef.addOrReplaceChild("core",
				CubeListBuilder.create().texOffs(48, 16).addBox(-2, -2, -1, 4, 4, 1),
				PartPose.offset(0, 14, -5));
		torsoDef.addOrReplaceChild("pack",
				CubeListBuilder.create().texOffs(48, 24).addBox(-6, -5, 0, 12, 10, 4),
				PartPose.offset(0, 15, 5));
		upper.addOrReplaceChild("pelvis",
				CubeListBuilder.create().texOffs(80, 0).addBox(-5, -1.5f, -4, 10, 3, 8),
				PartPose.offset(0, -0.5f, 0));
		arm(upper, "l", -9.5f, false);
		arm(upper, "r", 9.5f, true);
		leg(root, "l", -4);
		leg(root, "r", 4);
		// 포드(캡슐): 가운데 띠 + 이음매 링 + 경첩 달린 위 뚜껑(돔+플레이트) + 지면에 닿는 아래 뚜껑(돔+플레이트).
		// 돔은 8x2x8 플레이트를 (1.5, 2, 1.5)배로 늘려 쓴다 (UV 영역 재사용, 크기는 setupAnim에서 지정)
		root.addOrReplaceChild("pod_band",
				CubeListBuilder.create().texOffs(80, 44).addBox(-6, -4, -6, 12, 8, 12),
				PartPose.offset(0, 34, 0));
		root.addOrReplaceChild("pod_ring",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, -1, -4, 8, 2, 8),
				PartPose.offset(0, 34, 0));
		PartDefinition podTop = root.addOrReplaceChild("pod_top", CubeListBuilder.create(), PartPose.offset(0, 30, 5));
		podTop.addOrReplaceChild("dome_t",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, -1, -4, 8, 2, 8),
				PartPose.offset(0, -2, -5));
		podTop.addOrReplaceChild("plate_t",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, -1, -4, 8, 2, 8),
				PartPose.offset(0, -5, -5));
		PartDefinition podBottom = root.addOrReplaceChild("pod_bottom", CubeListBuilder.create(), PartPose.offset(0, 44, 0));
		podBottom.addOrReplaceChild("dome_b",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, -1, -4, 8, 2, 8),
				PartPose.offset(0, -4, 0));
		podBottom.addOrReplaceChild("plate_b",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, -1, -4, 8, 2, 8),
				PartPose.offset(0, -1, 0));
		// 조종석 (피벗 = 탑승자 눈 위치. 1인칭 탑승 중에만 표시. 좌표는 눈 기준 px, +y 아래, 앞 = -z)
		PartDefinition cockpit = root.addOrReplaceChild("cockpit",
				CubeListBuilder.create(),
				PartPose.offset(0, 3.2f, 0));
		cockpit.addOrReplaceChild("deck",
				CubeListBuilder.create().texOffs(0, 64).addBox(-13, 7, -8, 26, 2, 16),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("header",
				CubeListBuilder.create().texOffs(0, 82).addBox(-13, -9, -8, 26, 4, 16),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("back_wall",
				CubeListBuilder.create().texOffs(0, 102).addBox(-13, -9, 8, 26, 18, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("wall_l",
				CubeListBuilder.create().texOffs(84, 64).addBox(-13, -9, -8, 3, 18, 16),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("wall_r",
				CubeListBuilder.create().texOffs(84, 64).addBox(10, -9, -8, 3, 18, 16),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("dash",
				CubeListBuilder.create().texOffs(56, 102).addBox(-10, 3.5f, -8, 20, 6, 4),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("pillar_l",
				CubeListBuilder.create().texOffs(104, 102).addBox(-10, -5, -8, 3, 8, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("pillar_r",
				CubeListBuilder.create().texOffs(104, 102).addBox(7, -5, -8, 3, 8, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("seat",
				CubeListBuilder.create().texOffs(56, 112).addBox(-4, 5, 1, 8, 2, 7),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("seat_back",
				CubeListBuilder.create().texOffs(86, 112).addBox(-4, -2, 6, 8, 7, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("headrest",
				CubeListBuilder.create().texOffs(106, 114).addBox(-2.5f, -5, 6, 5, 3, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("stick_l",
				CubeListBuilder.create().texOffs(0, 122).addBox(-8.5f, 1.5f, -5, 1, 2, 1),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("knob_l",
				CubeListBuilder.create().texOffs(4, 122).addBox(-9, 0.5f, -5.5f, 2, 1, 2),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("stick_r",
				CubeListBuilder.create().texOffs(0, 122).addBox(7.5f, 1.5f, -5, 1, 2, 1),
				PartPose.ZERO);
		cockpit.addOrReplaceChild("knob_r",
				CubeListBuilder.create().texOffs(4, 122).addBox(7, 0.5f, -5.5f, 2, 1, 2),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	/** u가 [a,b] 구간을 지나는 진행도 0~1 */
	private static float seg(float u, float a, float b) {
		return clamp01((u - a) / (b - a));
	}

	private static float smooth(float x) {
		return x * x * (3f - 2f * x);
	}

	/** 살짝 넘쳤다 돌아오는 easeOutBack (철컥 고정되는 느낌) */
	private static float back(float x) {
		float c = 1.1f;
		float c3 = c + 1f;
		float m = x - 1f;
		return 1f + c3 * m * m * m + c * m * m;
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static void scale(ModelPart part, float x, float y, float z) {
		part.xScale = x;
		part.yScale = y;
		part.zScale = z;
	}

	private static void scale(ModelPart part, float s) {
		scale(part, s, s, s);
	}

	@Override
	public void setupAnim(MechRenderState state) {
		super.setupAnim(state);
		float p = clamp01(state.transform);
		float u = 1f - p;
		float t = state.ageInTicks;
		// 보행·시선 같은 "로봇 상태" 동작은 변신이 거의 끝나야 켜진다
		float ready = smooth(seg(u, 0.88f, 1.0f));
		float amp = Mth.clamp(state.walkAnimationSpeed, 0f, 1f) * ready;
		float ph = state.walkAnimationPos * 0.55f;
		float c = Mth.cos(ph);
		float cL = Mth.cos(ph);
		float cR = Mth.cos(ph + Mth.PI);

		// 시동 진동: 캡슐 상태에서만 미세하게 떨림 (모델 전체를 좌우로 흔듦)
		float vib = 1f - smooth(seg(u, 0.0f, 0.12f));
		float vx = Mth.sin(t * 2.3f) * 0.6f * vib;
		float vz = Mth.cos(t * 1.9f) * 0.4f * vib;
		// 보행 시 미세한 상하 출렁임 (발이 뜨지 않도록 0.4px 이내)
		this.root().setPos(vx, -20f - Math.abs(c) * 0.4f * amp, vz);

		// ===== 변신 곡선 =====
		float open = smooth(seg(u, 0.06f, 0.20f));
		float lift = 12f * (1f - smooth(seg(u, 0.24f, 0.56f)));
		float su = lerp(0.30f, 1f, smooth(seg(u, 0.16f, 0.50f)));
		float legS = lerp(0.30f, 1f, smooth(seg(u, 0.20f, 0.52f)));
		float legY = 24f + 12f * (1f - smooth(seg(u, 0.22f, 0.52f)));
		float thighT = smooth(seg(u, 0.28f, 0.44f));
		float shinT = smooth(seg(u, 0.34f, 0.50f));
		float footT = smooth(seg(u, 0.42f, 0.56f));
		float armT = smooth(seg(u, 0.56f, 0.74f));
		float foreT = smooth(seg(u, 0.64f, 0.80f));
		float fistT = smooth(seg(u, 0.72f, 0.82f));
		float cannonT = smooth(seg(u, 0.78f, 0.90f));
		float headT = smooth(seg(u, 0.70f, 0.86f));

		// 다리: 보행 스윙 + 변신 전개(허벅지→정강이→발 순서) + 발 수평 보정
		float thL = cL * 0.7f * amp;
		float thR = cR * 0.7f * amp;
		float kL = Math.max(0f, Mth.sin(ph)) * 0.9f * amp;
		float kR = Math.max(0f, Mth.sin(ph + Mth.PI)) * 0.9f * amp;
		this.legL.y = legY;
		this.legR.y = legY;
		scale(this.legL, legS);
		scale(this.legR, legS);
		this.legL.xRot = thL - 1.9f * (1f - thighT);
		this.legR.xRot = thR - 1.9f * (1f - thighT);
		this.shinL.xRot = kL + 2.2f * (1f - shinT);
		this.shinR.xRot = kR + 2.2f * (1f - shinT);
		this.footL.xRot = -(thL + kL) * 0.7f - 0.3f * (1f - footT);
		this.footR.xRot = -(thR + kR) * 0.7f - 0.3f * (1f - footT);

		// 상체 그룹: 캡슐 속 코어(작고 낮음)에서 점점 커지며 올라옴. 마지막에 철컥 눌렸다 복귀
		float settle = seg(u, 0.90f, 1.0f);
		float dip = Mth.sin(settle * Mth.PI) * (1f - settle) * 1.6f;
		this.upper.y = 24f + lift + dip;
		scale(this.upper, su);

		// 몸통: 어깨 비틀림(보행 반대 방향) + 호흡
		this.torso.yRot = c * 0.08f * amp;
		this.torso.xRot = Mth.sin(t * 0.07f) * 0.012f * ready;
		this.pelvis.yRot = -c * 0.05f * amp;
		scale(this.pelvis, lerp(0.5f, 1f, smooth(seg(u, 0.30f, 0.46f))));
		float pk = smooth(seg(u, 0.40f, 0.56f));
		scale(this.pack, lerp(0.2f, 1f, pk), lerp(0.2f, 1f, pk), Math.max(0.01f, pk));
		float cs = smooth(seg(u, 0.50f, 0.60f));
		scale(this.core, Math.max(0.01f, cs));

		// 팔: 접힌 상태(몸 옆에 붙이고 전완을 접어 올림) → 견갑 → 팔 → 전완 → 주먹 → 캐논 순서로 전개 + 보행 스윙
		float swL = Mth.cos(ph + Mth.PI) * 0.6f * amp;
		float swR = Mth.cos(ph) * 0.6f * amp;
		this.armL.xRot = swL + lerp(-0.2f, 0f, armT);
		this.armR.xRot = swR + lerp(-0.2f, -0.25f, armT);
		this.armL.zRot = -0.12f * (1f - armT);
		this.armR.zRot = 0.12f * (1f - armT);
		this.foreL.xRot = lerp(-2.5f, -0.2f, foreT) + Math.min(0f, swL) * 0.6f;
		this.foreR.xRot = lerp(-2.5f, -0.45f, foreT) + Math.min(0f, swR) * 0.6f;
		float shT = Math.max(0.001f, back(seg(u, 0.52f, 0.64f)));
		scale(this.shoulderL, shT);
		scale(this.shoulderR, shT);
		float fs = lerp(0.3f, 1f, fistT);
		scale(this.fistL, fs);
		scale(this.fistR, fs);
		scale(this.cannon, 1f, 1f, Math.max(0.05f, cannonT));

		// 머리: 목에서 솟아오르며(숙임→정면) 귀 장갑이 펴지고 바이저가 점등. 로봇이 되면 시선 추적
		float yaw = Mth.clamp(state.yRot, -50f, 50f) * DEG * ready;
		float pitch = Mth.clamp(state.xRot, -30f, 30f) * DEG * ready;
		this.head.y = -16f + 7f * (1f - headT);
		this.head.yRot = yaw - this.torso.yRot * 0.6f;
		this.head.xRot = 0.9f * (1f - headT) + pitch;
		scale(this.head, lerp(0.5f, 1f, headT));
		scale(this.visor, Math.max(0.01f, smooth(seg(u, 0.86f, 0.94f))), 1f, 1f);
		float earS = Math.max(0.01f, back(seg(u, 0.84f, 0.92f)));
		scale(this.earL, earS, 1f, 1f);
		scale(this.earR, earS, 1f, 1f);

		// ===== 포드(캡슐) =====
		// 가운데 띠: 약간 부풀었다가 몸통으로 흡수 / 이음매 링: 부풀며 점등 후 소멸
		float bs = 1f - smooth(seg(u, 0.30f, 0.50f));
		float grow = 1f + 0.08f * smooth(seg(u, 0.08f, 0.20f));
		scale(this.podBand, (0.01f + 0.99f * bs) * grow, Math.max(0.01f, bs), (0.01f + 0.99f * bs) * grow);
		this.podBand.y = lerp(34f, 22f, 1f - bs);
		this.podRing.y = this.podBand.y;
		float ringGrow = 1f + 0.35f * smooth(seg(u, 0.08f, 0.22f));
		float ringFade = 1f - smooth(seg(u, 0.22f, 0.34f));
		float pulse = 1f + 0.12f * Mth.sin(t * 0.9f) * vib;
		float rs = 1.8f * ringGrow * ringFade * pulse + 0.001f;
		scale(this.podRing, rs, 1f, rs);
		// 위 뚜껑: 뒤 경첩으로 열림 → 위로 떠올라 머리 위치로 날아가며 줄어 합체
		float fly = smooth(seg(u, 0.56f, 0.80f));
		this.podTop.xRot = lerp(-1.25f * open, 0f, fly);
		this.podTop.y = lerp(30f - 3f * open, 4f, fly);
		this.podTop.z = lerp(5f, 0f, fly);
		scale(this.podTop, lerp(1f, 0.15f, fly));
		scale(this.domeT, 1.5f, 2f, 1.5f);
		scale(this.plateT, 1f);
		// 아래 뚜껑: 지면에서 옆으로 퍼졌다가(착지 충격) 납작하게 눌려 사라짐 → 발바닥 판
		float spread = smooth(seg(u, 0.24f, 0.40f));
		float sink = smooth(seg(u, 0.40f, 0.56f));
		float bw = lerp(1f, 1.5f, spread) * (1f - sink) + 0.001f;
		scale(this.podBottom, bw, Math.max(0.01f, 1f - sink * 0.95f), bw);
		scale(this.domeB, 1.5f, 2f, 1.5f);
		scale(this.plateB, 1f);

		// ===== 가시성 =====
		boolean robotForm = u >= 0.985f;
		// 1인칭 탑승(조종석 시점): 겉모습은 전부 숨기고 조종석 내부만 그린다.
		// 모델이 개체 간 공유되므로 매 프레임 양방향으로 설정해야 다른 메카·3인칭 렌더에 상태가 새지 않는다.
		boolean cockpitOn = false;
		try {
			cockpitOn = state.cockpitView && robotForm;
		} catch (Exception ignored) {
		}
		this.cockpit.visible = cockpitOn;
		if (cockpitOn) {
			// yaw: 실시간 카메라 yaw 기준(몸통 회전 지연 보정) → 창이 항상 정면. pitch: 시선을 일부만 따라감
			float yawRel = Mth.wrapDegrees(state.camYaw - state.bodyYaw);
			this.cockpit.yRot = yawRel * DEG;
			this.cockpit.xRot = Mth.clamp(state.camPitch, -90f, 90f) * COCKPIT_PITCH_FOLLOW * DEG;
		}
		boolean hull = !cockpitOn;
		boolean podOn = !robotForm;
		this.upper.visible = hull && u > 0.09f;
		this.legL.visible = hull && u > 0.18f;
		this.legR.visible = hull && u > 0.18f;
		this.podBand.visible = podOn;
		this.podRing.visible = podOn;
		this.podTop.visible = podOn;
		this.podBottom.visible = podOn;
	}
}
