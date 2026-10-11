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
 * - UV 겹침 없음 (128x64)
 * transform 0→1: 팔다리 접힘+머리 숙임 (서버 회전+스케일과 합쳐져 트랜스포머식 변신).
 */
public class MechRobotModel extends EntityModel<MechRenderState> {

	private static final float DEG = 0.017453292f;

	private final ModelPart head;
	private final ModelPart torso;
	private final ModelPart pelvis;
	private final ModelPart armL;
	private final ModelPart armR;
	private final ModelPart foreL;
	private final ModelPart foreR;
	private final ModelPart cannon;
	private final ModelPart legL;
	private final ModelPart legR;
	private final ModelPart shinL;
	private final ModelPart shinR;
	private final ModelPart footL;
	private final ModelPart footR;
	private final ModelPart pod;

	public MechRobotModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.torso = root.getChild("torso");
		this.pelvis = root.getChild("pelvis");
		this.armL = root.getChild("arm_l");
		this.armR = root.getChild("arm_r");
		this.foreL = this.armL.getChild("forearm_l");
		this.foreR = this.armR.getChild("forearm_r");
		this.cannon = this.foreR.getChild("cannon");
		this.legL = root.getChild("leg_l");
		this.legR = root.getChild("leg_r");
		this.shinL = this.legL.getChild("shin_l");
		this.shinR = this.legR.getChild("shin_r");
		this.footL = this.shinL.getChild("foot_l");
		this.footR = this.shinR.getChild("foot_r");
		this.pod = root.getChild("pod");
	}

	private static void arm(PartDefinition root, String side, float x, boolean withCannon) {
		PartDefinition arm = root.addOrReplaceChild("arm_" + side,
				CubeListBuilder.create().texOffs(80, 16).addBox(-2.5f, 0, -2.5f, 5, 6, 5),
				PartPose.offset(x, 12, 0));
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
		// 머리 (피벗 = 목) + 바이저(청광) + 귀 장갑
		PartDefinition headDef = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
				PartPose.offset(0, 8, 0));
		headDef.addOrReplaceChild("visor",
				CubeListBuilder.create().texOffs(32, 0).addBox(-3, -5, -5, 6, 2, 1),
				PartPose.ZERO);
		headDef.addOrReplaceChild("ear_l",
				CubeListBuilder.create().texOffs(32, 3).addBox(-0.5f, -1.5f, -1.5f, 1, 3, 3),
				PartPose.offset(-4.5f, -4, 0));
		headDef.addOrReplaceChild("ear_r",
				CubeListBuilder.create().texOffs(32, 3).addBox(-0.5f, -1.5f, -1.5f, 1, 3, 3),
				PartPose.offset(4.5f, -4, 0));
		// 가슴 + 코어 + 백팩
		PartDefinition torsoDef = root.addOrReplaceChild("torso",
				CubeListBuilder.create().texOffs(0, 16).addBox(-7, 8, -5, 14, 14, 10),
				PartPose.ZERO);
		torsoDef.addOrReplaceChild("core",
				CubeListBuilder.create().texOffs(48, 16).addBox(-2, 12, -6, 4, 4, 1),
				PartPose.ZERO);
		torsoDef.addOrReplaceChild("pack",
				CubeListBuilder.create().texOffs(48, 24).addBox(-6, 10, 5, 12, 10, 4),
				PartPose.ZERO);
		root.addOrReplaceChild("pelvis",
				CubeListBuilder.create().texOffs(80, 0).addBox(-5, 22, -4, 10, 3, 8),
				PartPose.ZERO);
		arm(root, "l", -9.5f, false);
		arm(root, "r", 9.5f, true);
		leg(root, "l", -4);
		leg(root, "r", 4);
		// 포드 (캡슐볼: 완전 접힘 상태에서만 표시)
		PartDefinition pod = root.addOrReplaceChild("pod",
				CubeListBuilder.create().texOffs(80, 44).addBox(-6, 22, -6, 12, 8, 12),
				PartPose.ZERO);
		pod.addOrReplaceChild("pod_top",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, 20, -4, 8, 2, 8),
				PartPose.ZERO);
		pod.addOrReplaceChild("pod_bottom",
				CubeListBuilder.create().texOffs(48, 52).addBox(-4, 30, -4, 8, 2, 8),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	@Override
	public void setupAnim(MechRenderState state) {
		super.setupAnim(state);
		float p = clamp01(state.transform);
		float u = 1f - p;
		// 스테이지 전개: 다리 → 팔 → 머리 (역순 접힘)
		float legP = clamp01(u / 0.4f);
		float armP = clamp01((u - 0.25f) / 0.4f);
		float headP = clamp01((u - 0.6f) / 0.4f);
		float t = state.ageInTicks;
		float amp = Mth.clamp(state.walkAnimationSpeed, 0f, 1f) * legP;
		float ph = state.walkAnimationPos * 0.55f;
		float c = Mth.cos(ph);
		float cL = Mth.cos(ph);
		float cR = Mth.cos(ph + Mth.PI);

		// 보행 시 미세한 상하 출렁임 (발이 뜨지 않도록 0.4px 이내)
		this.root().setPos(0, -20f - Math.abs(c) * 0.4f * amp, 0);

		// 다리: 허벅지 스윙 + 무릎 굽힘(앞으로 나아가는 구간) + 발 수평 보정
		float thL = cL * 0.7f * amp;
		float thR = cR * 0.7f * amp;
		float kL = Math.max(0f, Mth.sin(ph)) * 0.9f * amp;
		float kR = Math.max(0f, Mth.sin(ph + Mth.PI)) * 0.9f * amp;
		float fold = 1f - legP;
		this.legL.xRot = thL - 1.9f * fold;
		this.legR.xRot = thR - 1.9f * fold;
		this.shinL.xRot = kL + 2.2f * fold;
		this.shinR.xRot = kR + 2.2f * fold;
		this.footL.xRot = -(thL + kL) * 0.7f - 0.3f * fold;
		this.footR.xRot = -(thR + kR) * 0.7f - 0.3f * fold;

		// 몸통: 어깨 비틀림(보행 반대 방향) + 호흡
		float ta = armP;
		this.torso.yRot = c * 0.08f * amp * ta;
		this.torso.xRot = Mth.sin(t * 0.07f) * 0.012f * ta;
		this.pelvis.yRot = -c * 0.05f * amp;

		// 팔: 보행 스윙 + 대기 자세(캐논 팔은 약간 전방 조준) + 폴딩(팔을 안쪽으로 접어 가슴 앞에 모음)
		float af = 1f - armP;
		float swL = Mth.cos(ph + Mth.PI) * 0.6f * amp * armP;
		float swR = Mth.cos(ph) * 0.6f * amp * armP;
		this.armL.xRot = swL + 2.2f * af;
		this.armR.xRot = swR - 0.25f * armP + 2.2f * af;
		this.armL.zRot = -1.1f * af;
		this.armR.zRot = 1.1f * af;
		this.foreL.xRot = -0.2f * armP + Math.min(0f, swL) * 0.6f - 1.6f * af;
		this.foreR.xRot = -0.45f * armP + Math.min(0f, swR) * 0.6f - 1.6f * af;

		// 머리: 시선 추적(도 → 라디안, 범위 제한) + 접힘 시 숙임. 몸통 비틀림 반대로 보정
		float yaw = Mth.clamp(state.yRot, -50f, 50f) * DEG * headP;
		float pitch = Mth.clamp(state.xRot, -30f, 30f) * DEG * headP;
		this.head.yRot = yaw - this.torso.yRot * 0.6f;
		this.head.xRot = 0.9f * (1f - headP) + pitch;

		// 포드(캡슐볼) ↔ 로봇 교체 (교체 순간 섬광으로 가림: 서버 파티클)
		boolean showPod = p >= 0.5f;
		this.pod.visible = showPod;
		this.head.visible = !showPod;
		this.torso.visible = !showPod;
		this.pelvis.visible = !showPod;
		this.armL.visible = !showPod;
		this.armR.visible = !showPod;
		this.legL.visible = !showPod;
		this.legR.visible = !showPod;
	}
}
