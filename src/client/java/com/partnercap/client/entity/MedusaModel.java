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
 * 메두사 (리디자인): 날씬한 여성형 + 3마디 뱀 머리카락 8마리(파동 애니메이션) + 주름 치마 + 금장식.
 * - 머리 피벗을 목으로 이동, 눈은 중심 피벗(섬광 확대가 좌우 대칭으로 커짐)
 * - 팔 2마디(팔꿈치), 치마 2단(걸을 때 흔들림), 뒷머리 흔들림
 * - UV 겹침 없음 (64x64)
 * gazeFlash>0 이면 눈이 커지고 뱀이 곤두선다.
 */
public class MedusaModel extends EntityModel<MedusaRenderState> {

	private static final float DEG = 0.017453292f;
	/** 뱀 뿌리 위치 (x, z): 좌우 대칭 4쌍 */
	private static final float[][] SPOTS = {
			{-4, -1}, {4, -1},
			{-3.5f, -3.5f}, {3.5f, -3.5f},
			{-1.5f, -4}, {1.5f, -4},
			{-4, 2}, {4, 2},
	};

	private static float fwdLean(float z) {
		return Math.max(-0.2f, Math.min(0.35f, -z / 4f * 0.3f));
	}

	private final ModelPart head;
	private final ModelPart eyeL;
	private final ModelPart eyeR;
	private final ModelPart hairBack;
	private final ModelPart[] seg1 = new ModelPart[8];
	private final ModelPart[] seg2 = new ModelPart[8];
	private final ModelPart[] seg3 = new ModelPart[8];
	private final ModelPart[] snakeHead = new ModelPart[8];
	private final ModelPart torso;
	private final ModelPart skirtTop;
	private final ModelPart skirtBottom;
	private final ModelPart armL;
	private final ModelPart armR;
	private final ModelPart foreL;
	private final ModelPart foreR;
	private final ModelPart legL;
	private final ModelPart legR;

	public MedusaModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.eyeL = this.head.getChild("eye_l");
		this.eyeR = this.head.getChild("eye_r");
		this.hairBack = this.head.getChild("hair_back");
		for (int i = 0; i < 8; i++) {
			this.seg1[i] = this.head.getChild("snake_" + i);
			this.seg2[i] = this.seg1[i].getChild("seg2");
			this.seg3[i] = this.seg2[i].getChild("seg3");
			this.snakeHead[i] = this.seg3[i].getChild("shead");
		}
		this.torso = root.getChild("torso");
		this.skirtTop = root.getChild("skirt_top");
		this.skirtBottom = this.skirtTop.getChild("skirt_bottom");
		this.armL = root.getChild("arm_l");
		this.armR = root.getChild("arm_r");
		this.foreL = this.armL.getChild("forearm_l");
		this.foreR = this.armR.getChild("forearm_r");
		this.legL = root.getChild("leg_l");
		this.legR = root.getChild("leg_r");
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 머리 (피벗 = 목, 박스는 목 위로 8)
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
				PartPose.offset(0, 8, 0));
		head.addOrReplaceChild("eye_l",
				CubeListBuilder.create().texOffs(54, 0).addBox(-1, -1, -0.5f, 2, 2, 1),
				PartPose.offset(-2, -4, -4.5f));
		head.addOrReplaceChild("eye_r",
				CubeListBuilder.create().texOffs(54, 0).addBox(-1, -1, -0.5f, 2, 2, 1),
				PartPose.offset(2, -4, -4.5f));
		head.addOrReplaceChild("cap",
				CubeListBuilder.create().texOffs(18, 53).addBox(-4.5f, -9, -4.5f, 9, 2, 9),
				PartPose.ZERO);
		head.addOrReplaceChild("hair_back",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.5f, -8, 4, 9, 12, 2),
				PartPose.ZERO);
		// 뱀 8마리: 3마디 + 머리 (마디마다 약간씩 휘어 자연스러운 곡선)
		for (int i = 0; i < 8; i++) {
			float x = SPOTS[i][0];
			float z = SPOTS[i][1];
			float s = x < 0 ? -1f : 1f;
			PartDefinition snake = head.addOrReplaceChild("snake_" + i,
					CubeListBuilder.create().texOffs(0, 52).addBox(-1, -3, -1, 2, 3, 2),
					PartPose.offsetAndRotation(x, -8.5f, z, fwdLean(z), 0, s * 0.25f));
			PartDefinition s2 = snake.addOrReplaceChild("seg2",
					CubeListBuilder.create().texOffs(0, 52).addBox(-1, -3, -1, 2, 3, 2),
					PartPose.offsetAndRotation(0, -3, 0, 0.1f, 0, s * 0.12f));
			PartDefinition s3 = s2.addOrReplaceChild("seg3",
					CubeListBuilder.create().texOffs(0, 52).addBox(-1, -3, -1, 2, 3, 2),
					PartPose.offsetAndRotation(0, -3, 0, 0.45f, 0, s * 0.05f));
			s3.addOrReplaceChild("shead",
					CubeListBuilder.create().texOffs(8, 52).addBox(-1, -2, -2.5f, 2, 2, 3),
					PartPose.offsetAndRotation(0, -3, 0, 0.3f, 0, 0));
		}

		// 몸통 + 허리띠
		PartDefinition torso = root.addOrReplaceChild("torso",
				CubeListBuilder.create().texOffs(0, 16).addBox(-4, 0, -2, 8, 10, 4),
				PartPose.offset(0, 8, 0));
		torso.addOrReplaceChild("belt",
				CubeListBuilder.create().texOffs(26, 16).addBox(-4.5f, 8, -2.5f, 9, 2, 5),
				PartPose.ZERO);

		// 치마 2단 (골반 피벗)
		PartDefinition skirtTop = root.addOrReplaceChild("skirt_top",
				CubeListBuilder.create().texOffs(26, 24).addBox(-4.5f, 0, -3, 9, 4, 6),
				PartPose.offset(0, 18, 0));
		skirtTop.addOrReplaceChild("skirt_bottom",
				CubeListBuilder.create().texOffs(0, 35).addBox(-5.5f, 0, -4, 11, 9, 8),
				PartPose.offset(0, 4, 0));

		// 팔 2마디 (어깨 피벗, 팔꿈치 피벗)
		PartDefinition armL = root.addOrReplaceChild("arm_l",
				CubeListBuilder.create().texOffs(38, 35).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
				PartPose.offsetAndRotation(-5.5f, 9, 0, 0, 0, 0.08f));
		armL.addOrReplaceChild("forearm_l",
				CubeListBuilder.create().texOffs(38, 44).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
				PartPose.offsetAndRotation(0, 6, 0, -0.15f, 0, 0));
		PartDefinition armR = root.addOrReplaceChild("arm_r",
				CubeListBuilder.create().texOffs(38, 35).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
				PartPose.offsetAndRotation(5.5f, 9, 0, 0, 0, -0.08f));
		armR.addOrReplaceChild("forearm_r",
				CubeListBuilder.create().texOffs(38, 44).addBox(-1.5f, 0, -1.5f, 3, 6, 3),
				PartPose.offsetAndRotation(0, 6, 0, -0.15f, 0, 0));

		// 다리 + 샌들 발
		PartDefinition legL = root.addOrReplaceChild("leg_l",
				CubeListBuilder.create().texOffs(50, 35).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
				PartPose.offset(-2, 28, 0));
		legL.addOrReplaceChild("foot_l",
				CubeListBuilder.create().texOffs(50, 50).addBox(-1.5f, 0, -3, 3, 1, 4),
				PartPose.offset(0, 11, 0));
		PartDefinition legR = root.addOrReplaceChild("leg_r",
				CubeListBuilder.create().texOffs(50, 35).addBox(-1.5f, 0, -1.5f, 3, 12, 3),
				PartPose.offset(2, 28, 0));
		legR.addOrReplaceChild("foot_r",
				CubeListBuilder.create().texOffs(50, 50).addBox(-1.5f, 0, -3, 3, 1, 4),
				PartPose.offset(0, 11, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MedusaRenderState state) {
		super.setupAnim(state);
		this.root().setPos(0, -16, 0);

		float t = state.ageInTicks;
		float pos = state.walkAnimationPos * 0.6f;
		float amp = Mth.clamp(state.walkAnimationSpeed, 0f, 1f);
		float c = Mth.cos(pos);
		float atk = state.attack > 0f ? Mth.sin(state.attack * Mth.PI) : 0f;
		float flash = Math.min(20, Math.max(0, state.gazeFlash));
		float flashF = flash / 20f;

		// 다리 / 치마
		this.legL.xRot = c * 0.6f * amp;
		this.legR.xRot = -c * 0.6f * amp;
		this.skirtTop.xRot = 0f;
		this.skirtBottom.xRot = c * 0.10f * amp;
		this.skirtBottom.zRot = Mth.sin(t * 0.06f) * 0.02f;

		// 몸통: 호흡 + 어깨 비틀림 (골반과 반대 방향)
		this.torso.xRot = Mth.sin(t * 0.08f) * 0.015f + atk * 0.1f;
		this.torso.yRot = -c * 0.08f * amp - atk * 0.2f;

		// 팔: 다리와 반대로 스윙, 팔 쪽으로 갈수록 팔꿈치가 굽음
		float swing = Mth.sin(t * 0.08f) * 0.02f;
		float aL = -c * 0.5f * amp;
		float aR = c * 0.5f * amp - atk * 1.7f;
		this.armL.xRot = aL;
		this.armR.xRot = aR;
		this.armL.zRot = 0.08f + swing;
		this.armR.zRot = -0.08f - swing;
		this.foreL.xRot = -0.15f + Math.min(0f, aL) * 0.5f;
		this.foreR.xRot = -0.15f + Math.min(0f, aR) * 0.5f;

		// 머리: 시선 추적 (도 → 라디안, 범위 제한)
		float yaw = Mth.clamp(state.yRot, -55f, 55f) * DEG;
		float pitch = Mth.clamp(state.xRot, -35f, 35f) * DEG;
		this.head.yRot = yaw;
		this.head.xRot = pitch + atk * 0.2f - flashF * 0.15f;
		this.hairBack.xRot = 0.04f + amp * 0.25f + Mth.sin(t * 0.1f) * 0.03f;

		// 뱀: 뿌리→머리로 위상이 지연되는 파동. 이동 시 뒤로 흩날리고, 석화 섬광 때 곤두서며 벌어짐
		for (int i = 0; i < 8; i++) {
			float s = SPOTS[i][0] < 0 ? -1f : 1f;
			float ph = t * 0.2f + i * 0.9f;
			float back = amp * 0.3f;
			float flare = flashF * 0.3f;
			this.seg1[i].xRot = fwdLean(SPOTS[i][1]) - back + Mth.sin(ph) * 0.16f;
			this.seg1[i].zRot = s * (0.25f + flare) + Mth.cos(ph * 0.8f) * 0.14f;
			this.seg2[i].xRot = 0.1f - back * 0.6f + Mth.sin(ph - 0.7f) * 0.28f;
			this.seg2[i].zRot = s * (0.12f + flare) + Mth.cos(ph * 0.8f - 0.7f) * 0.22f;
			this.seg3[i].xRot = 0.45f + Mth.sin(ph - 1.4f) * 0.4f;
			this.seg3[i].zRot = s * 0.05f + Mth.cos(ph * 0.8f - 1.4f) * 0.3f;
			this.snakeHead[i].xRot = 0.3f + Mth.sin(ph - 2.1f) * 0.3f;
		}

		// 석화 섬광: 눈 확대 (눈 피벗이 중심이라 좌우 대칭으로 커짐)
		float s = 1.0f + flash * 0.035f;
		this.eyeL.xScale = s;
		this.eyeL.yScale = s;
		this.eyeR.xScale = s;
		this.eyeR.yScale = s;
	}
}
