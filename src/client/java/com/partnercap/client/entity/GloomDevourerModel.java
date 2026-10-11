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
 * 먹보 도깨비 (리디자인).
 * - 머리 피벗을 목 위치로 이동, 턱은 머리의 자식(머리를 돌려도 입이 따라감), 힌지 기준으로 아래로 열림
 * - 뿔은 머리 윗면에 붙고 좌우 대칭(2단 곡선), 등 가시/발톱/발 추가
 * - UV 겹침 없음 (64x64)
 * - 개체 간 공유되는 모델 필드에 상태를 저장하지 않음 (단계 전환 보간은 렌더러의 개체별 상태에서 처리)
 */
public class GloomDevourerModel extends EntityModel<GloomDevourerRenderState> {

	private static final float DEG = 0.017453292f;

	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart hornL;
	private final ModelPart hornR;
	private final ModelPart body;
	private final ModelPart armL;
	private final ModelPart armR;
	private final ModelPart legL;
	private final ModelPart legR;
	private final ModelPart footL;
	private final ModelPart footR;
	private final ModelPart[] spikes = new ModelPart[3];
	private final ModelPart[] clawsL = new ModelPart[3];
	private final ModelPart[] clawsR = new ModelPart[3];

	public GloomDevourerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.hornL = this.head.getChild("horn_l");
		this.hornR = this.head.getChild("horn_r");
		this.armL = root.getChild("arm_l");
		this.armR = root.getChild("arm_r");
		this.legL = root.getChild("leg_l");
		this.legR = root.getChild("leg_r");
		this.footL = this.legL.getChild("foot");
		this.footR = this.legR.getChild("foot");
		for (int i = 0; i < 3; i++) {
			this.spikes[i] = this.body.getChild("spike_" + i);
			this.clawsL[i] = this.armL.getChild("claw_" + i);
			this.clawsR[i] = this.armR.getChild("claw_" + i);
		}
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 몸통 + 등 가시
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 22).addBox(-6, -10, -4, 12, 12, 8),
				PartPose.offset(0, 10, 0));
		for (int i = 0; i < 3; i++) {
			body.addOrReplaceChild("spike_" + i,
					CubeListBuilder.create().texOffs(56, 52).addBox(-1, -4, -1, 2, 4, 2),
					PartPose.offsetAndRotation(0, -8 + 4 * i, 4, -0.7f, 0, 0));
		}

		// 머리 (피벗 = 목)
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-7, -9, -7, 14, 8, 14),
				PartPose.offset(0, 1, -3));
		head.addOrReplaceChild("eye_l",
				CubeListBuilder.create().texOffs(56, 42).addBox(-1.5f, -1.5f, -0.5f, 3, 3, 1),
				PartPose.offset(-3.5f, -5.5f, -7.5f));
		head.addOrReplaceChild("eye_r",
				CubeListBuilder.create().texOffs(56, 42).addBox(-1.5f, -1.5f, -0.5f, 3, 3, 1),
				PartPose.offset(3.5f, -5.5f, -7.5f));
		// 뿔: 바깥으로 기울고 끝은 안쪽으로 휘는 2단 곡선 (좌우 대칭)
		PartDefinition hornL = head.addOrReplaceChild("horn_l",
				CubeListBuilder.create().texOffs(22, 56).addBox(-1.5f, -5, -1.5f, 3, 5, 3),
				PartPose.offsetAndRotation(-5, -9, -1, 0, 0, -0.35f));
		hornL.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(42, 58).addBox(-1, -3, -1, 2, 3, 2),
				PartPose.offsetAndRotation(0, -5, 0, 0, 0, 0.3f));
		PartDefinition hornR = head.addOrReplaceChild("horn_r",
				CubeListBuilder.create().texOffs(22, 56).addBox(-1.5f, -5, -1.5f, 3, 5, 3),
				PartPose.offsetAndRotation(5, -9, -1, 0, 0, 0.35f));
		hornR.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(42, 58).addBox(-1, -3, -1, 2, 3, 2),
				PartPose.offsetAndRotation(0, -5, 0, 0, 0, -0.3f));
		// 턱 (머리의 자식, 힌지 = 머리 중앙 아래). +xRot = 아래로 열림
		PartDefinition jaw = head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(0, 42).addBox(-5, 0, -7, 10, 3, 7),
				PartPose.offset(0, -1, 0));
		float[] toothX = {-3.75f, -1.25f, 1.25f, 3.75f};
		for (int i = 0; i < 4; i++) {
			jaw.addOrReplaceChild("tooth_" + (i + 1),
					CubeListBuilder.create().texOffs(56, 46).addBox(-1, -2, -0.5f, 2, 2, 1),
					PartPose.offset(toothX[i], 0, -6.5f));
		}

		// 팔 + 발톱
		float[] clawX = {-2, 0, 2};
		PartDefinition armL = root.addOrReplaceChild("arm_l",
				CubeListBuilder.create().texOffs(40, 22).addBox(-3, -2, -3, 6, 14, 6),
				PartPose.offset(-9, 2, 0));
		PartDefinition armR = root.addOrReplaceChild("arm_r",
				CubeListBuilder.create().texOffs(40, 22).addBox(-3, -2, -3, 6, 14, 6),
				PartPose.offset(9, 2, 0));
		for (int i = 0; i < 3; i++) {
			armL.addOrReplaceChild("claw_" + i,
					CubeListBuilder.create().texOffs(56, 58).addBox(-0.5f, 0, -0.5f, 1, 3, 1),
					PartPose.offsetAndRotation(clawX[i], 12, -2.5f, 0.35f, 0, 0));
			armR.addOrReplaceChild("claw_" + i,
					CubeListBuilder.create().texOffs(56, 58).addBox(-0.5f, 0, -0.5f, 1, 3, 1),
					PartPose.offsetAndRotation(clawX[i], 12, -2.5f, 0.35f, 0, 0));
		}

		// 다리 + 발
		PartDefinition legL = root.addOrReplaceChild("leg_l",
				CubeListBuilder.create().texOffs(40, 42).addBox(-2, 0, -2, 4, 12, 4),
				PartPose.offset(-3, 12, 0));
		legL.addOrReplaceChild("foot",
				CubeListBuilder.create().texOffs(0, 56).addBox(-2.5f, 0, -4, 5, 2, 6),
				PartPose.offset(0, 10, 0));
		PartDefinition legR = root.addOrReplaceChild("leg_r",
				CubeListBuilder.create().texOffs(40, 42).addBox(-2, 0, -2, 4, 12, 4),
				PartPose.offset(3, 12, 0));
		legR.addOrReplaceChild("foot",
				CubeListBuilder.create().texOffs(0, 56).addBox(-2.5f, 0, -4, 5, 2, 6),
				PartPose.offset(0, 10, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(GloomDevourerRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float pos = state.walkAnimationPos * 0.55f;
		float amp = Mth.clamp(state.walkAnimationSpeed, 0f, 1f);
		float c = Mth.cos(pos);

		float w1 = state.blend1;
		float w2 = state.blend2;
		float w3 = state.blend3;
		float ws = Mth.clamp(w1 + w2 + w3, 0f, 1f);
		float atk = state.attack > 0f ? Mth.sin(state.attack * Mth.PI) : 0f;
		float chomp = Math.abs(Mth.sin(t * 0.45f));

		// 몸통: 걸을 때 상하 출렁임 + 비틀림, 평소엔 숨쉬기
		float bob = -(1f - Math.abs(c)) * 1.0f * amp + Mth.sin(t * 0.09f) * 0.25f;
		this.body.y = 10f + bob;
		this.head.y = 1f + bob;
		this.armL.y = 2f + bob;
		this.armR.y = 2f + bob;
		this.body.xRot = 0.06f - w1 * 0.2f - w3 * 0.12f + atk * 0.15f;
		this.body.yRot = c * 0.07f * amp;
		this.body.zRot = c * 0.035f * amp;
		for (int i = 0; i < 3; i++) {
			this.spikes[i].xRot = -0.7f + Mth.sin(t * 0.1f + i * 0.8f) * 0.05f + amp * 0.1f;
		}

		// 다리: 대각 보행, 발은 지면과 평행에 가깝게 보정
		float lA = c * 0.85f * amp;
		this.legL.xRot = lA;
		this.legR.xRot = -lA;
		this.footL.xRot = -lA * 0.8f;
		this.footR.xRot = lA * 0.8f;

		// 팔: 걷기 스윙 ↔ 포식 단계(1=움켜쥐기, 2=씹기, 3=삼키기)로 부드럽게 전환
		float swingL = -c * 0.75f * amp;
		float swingR = c * 0.75f * amp;
		float tX = ws > 0.001f ? (w1 * -1.9f + w2 * -1.1f + w3 * -0.3f) / (w1 + w2 + w3) : 0f;
		float sway = Mth.sin(t * 0.08f) * 0.03f;
		this.armL.xRot = Mth.lerp(ws, swingL, tX) - atk * 2.0f * (1f - ws) + sway;
		this.armR.xRot = Mth.lerp(ws, swingR, tX) - atk * 2.0f * (1f - ws) - sway;
		this.armL.zRot = 0.07f + 0.15f * w1 + sway;
		this.armR.zRot = -0.07f - 0.15f * w1 - sway;
		float curl = 0.35f + 0.25f * ws;
		for (int i = 0; i < 3; i++) {
			this.clawsL[i].xRot = curl;
			this.clawsR[i].xRot = curl;
		}

		// 머리: 시선 추적(도 → 라디안, 범위 제한) + 단계별 고개짓
		float yaw = Mth.clamp(state.yRot, -50f, 50f) * DEG;
		float pitch = Mth.clamp(state.xRot, -35f, 35f) * DEG;
		this.head.yRot = yaw * (1f - 0.5f * ws);
		this.head.xRot = pitch + 0.08f + w1 * 0.35f + w2 * 0.12f * Mth.sin(t * 0.45f) - w3 * 0.4f + atk * 0.25f;

		// 턱: +xRot = 열림 (이전엔 음수라 머리 안으로 파고들었음)
		this.jaw.xRot = 0.03f + 0.02f * Mth.sin(t * 0.12f)
				+ w1 * 0.35f + w2 * (0.12f + 0.55f * chomp) + w3 * 0.04f + atk * 0.6f;

		// 뿔: 좌우 대칭으로 미세하게 까딱
		float tw = Mth.sin(t * 0.06f) * 0.03f;
		this.hornL.zRot = -0.35f - tw;
		this.hornR.zRot = 0.35f + tw;
	}
}
