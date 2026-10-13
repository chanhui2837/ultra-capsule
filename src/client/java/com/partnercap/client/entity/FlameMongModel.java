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
 * 화염몽 모델: 불타는 원숭이.
 * - 머리: 큰 귀 2개 + 불꽃 볏 3가닥 + 벌어지는 아래턱(화염 połączony)
 * - 몸: 둥근 배 + 어깨 화염털 + 긴 꼬리(흔들림) + 긴 팔
 * - UV 겹침 없음 (128x128: 몸통/머리 64x64 + 긴팔/주먹 우측 여백)
 */
public class FlameMongModel extends EntityModel<FlameMongRenderState> {

	private static final float DEG = 0.017453292f;

	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart earL;
	private final ModelPart earR;
	private final ModelPart crest;
	private final ModelPart body;
	private final ModelPart belly;
	private final ModelPart armL;
	private final ModelPart armR;
	private final ModelPart legL;
	private final ModelPart legR;
	private final ModelPart tail;
	private final ModelPart tailTip;

	public FlameMongModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.earL = this.head.getChild("ear_l");
		this.earR = this.head.getChild("ear_r");
		this.crest = this.head.getChild("crest");
		this.body = root.getChild("body");
		this.belly = this.body.getChild("belly");
		this.armL = root.getChild("arm_l");
		this.armR = root.getChild("arm_r");
		this.legL = root.getChild("leg_l");
		this.legR = root.getChild("leg_r");
		this.tail = this.body.getChild("tail");
		this.tailTip = this.tail.getChild("tail_tip");
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 몸통
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 20).addBox(-5, -9, -3.5f, 10, 11, 7),
				PartPose.offset(0, 11, 0));
		body.addOrReplaceChild("belly",
				CubeListBuilder.create().texOffs(0, 38).addBox(-3.5f, -7, -4.2f, 7, 8, 1.5f),
				PartPose.offset(0, 0, 0));
		// 어깨 화염털
		body.addOrReplaceChild("mane_l",
				CubeListBuilder.create().texOffs(42, 20).addBox(-1.5f, -3, -2, 3, 6, 4),
				PartPose.offsetAndRotation(-5.5f, -7, 0, 0, 0, 0.35f));
		body.addOrReplaceChild("mane_r",
				CubeListBuilder.create().texOffs(42, 20).addBox(-1.5f, -3, -2, 3, 6, 4),
				PartPose.offsetAndRotation(5.5f, -7, 0, 0, 0, -0.35f));
		// 꼬리 (몸 뒤쪽, 위로 말림)
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(34, 38).addBox(-1, -1, 0, 2, 2, 9),
				PartPose.offsetAndRotation(0, -6, 3.2f, -0.9f, 0, 0));
		tail.addOrReplaceChild("tail_tip",
				CubeListBuilder.create().texOffs(34, 49).addBox(-1.2f, -1.2f, 8, 2.5f, 2.5f, 4),
				PartPose.offset(0, 0, 0));

		// 머리
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-5, -7, -4.5f, 10, 8, 9),
				PartPose.offset(0, 2, -1));
		// 주둥이
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(0, 17).addBox(-2.5f, -3, -7, 5, 3, 3),
				PartPose.offset(0, 0, 0));
		// 불타는 눈 2개 (얼굴 앞면)
		head.addOrReplaceChild("eye_l",
				CubeListBuilder.create().texOffs(52, 0).addBox(-1, -1, -0.5f, 2, 2.5f, 1),
				PartPose.offset(-2.2f, -4, -4.6f));
		head.addOrReplaceChild("eye_r",
				CubeListBuilder.create().texOffs(52, 0).addBox(-1, -1, -0.5f, 2, 2.5f, 1),
				PartPose.offset(2.2f, -4, -4.6f));
		// 아래턱 (벌어짐)
		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(18, 17).addBox(-2.5f, 0, -6.8f, 5, 1.5f, 3),
				PartPose.offset(0, 0.5f, 0));
		// 귀
		head.addOrReplaceChild("ear_l",
				CubeListBuilder.create().texOffs(38, 0).addBox(-1, -2.5f, -1.5f, 2, 5, 3),
				PartPose.offsetAndRotation(-5.5f, -3, 0, 0, 0, 0.3f));
		head.addOrReplaceChild("ear_r",
				CubeListBuilder.create().texOffs(38, 0).addBox(-1, -2.5f, -1.5f, 2, 5, 3),
				PartPose.offsetAndRotation(5.5f, -3, 0, 0, 0, -0.3f));
		// 불꽃 볏
		head.addOrReplaceChild("crest",
				CubeListBuilder.create().texOffs(38, 10).addBox(-1.5f, -6, -1.5f, 3, 6, 3),
				PartPose.offsetAndRotation(0, -7, 0, -0.25f, 0, 0));

		// 팔 (김, 너클워크: 땅에 닿을 만큼 길고 끝엔 주먹. UV는 128x128 우측 여백)
		PartDefinition armL = root.addOrReplaceChild("arm_l",
				CubeListBuilder.create().texOffs(64, 0).addBox(-2, -1, -2, 4, 17, 4),
				PartPose.offset(-7, 4, 0));
		armL.addOrReplaceChild("fist",
				CubeListBuilder.create().texOffs(64, 24).addBox(-2.2f, 15, -2.2f, 4.5f, 3.5f, 4.5f),
				PartPose.offset(0, 0, 0));
		PartDefinition armR = root.addOrReplaceChild("arm_r",
				CubeListBuilder.create().texOffs(64, 0).addBox(-2, -1, -2, 4, 17, 4),
				PartPose.offset(7, 4, 0));
		armR.addOrReplaceChild("fist",
				CubeListBuilder.create().texOffs(64, 24).addBox(-2.2f, 15, -2.2f, 4.5f, 3.5f, 4.5f),
				PartPose.offset(0, 0, 0));
		// 다리 (짧음)
		root.addOrReplaceChild("leg_l",
				CubeListBuilder.create().texOffs(16, 47).addBox(-2, 0, -2, 4, 7, 4),
				PartPose.offset(-2.8f, 17, 0));
		root.addOrReplaceChild("leg_r",
				CubeListBuilder.create().texOffs(16, 47).addBox(-2, 0, -2, 4, 7, 4),
				PartPose.offset(2.8f, 17, 0));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(FlameMongRenderState state) {
		super.setupAnim(state);
		try {
			float walk = state.walkAnimationPos;
			float speed = state.walkAnimationSpeed;
			float age = state.ageInTicks;
			boolean moving = speed > 0.08f;
			// 나무 타는 중: 팔 위로, 다리 굽힘, 몸 수직
			if (state.climbing) {
				try {
					this.armL.xRot = -2.4f + Mth.sin(age * 0.3f) * 0.25f;
					this.armR.xRot = -2.4f - Mth.sin(age * 0.3f) * 0.25f;
					this.legL.xRot = 0.9f;
					this.legR.xRot = 0.9f;
					this.body.xRot = -0.25f;
					this.head.xRot = 0.2f;
					this.tail.yRot = Mth.sin(age * 0.2f) * 0.5f;
				} catch (Exception ignored) {
				}
			} else {
				// 사지보행 (마카크식 대각보행: 왼팔+오른발 / 오른팔+왼발 동시)
				float freq = moving && speed > 0.5f ? 0.95f : 0.7f;
				float amp = moving && speed > 0.5f ? 0.95f : 0.7f;
				try {
					this.armL.xRot = Mth.cos(walk * freq) * amp * Math.min(1f, speed * 2f);
					this.armR.xRot = Mth.cos(walk * freq + (float) Math.PI) * amp * Math.min(1f, speed * 2f);
					this.legL.xRot = Mth.cos(walk * freq + (float) Math.PI) * 0.7f * Math.min(1f, speed * 2f);
					this.legR.xRot = Mth.cos(walk * freq) * 0.7f * Math.min(1f, speed * 2f);
				} catch (Exception ignored) {
				}
				// 질주 갤럽: 몸 상하 출렁임 + 앞으로 숙임 (너클워크 자세)
				try {
					float lean = moving ? 0.14f : 0.03f;
					this.body.xRot = lean + (moving ? Mth.sin(walk * freq * 2f) * 0.05f : 0f);
					this.head.xRot = lean * 0.6f;
				} catch (Exception ignored) {
				}
				// 꼬리 균형 (달릴수록 높이 듦)
				try {
					this.tail.xRot = -0.9f + (moving ? 0.35f : Mth.sin(age * 0.07f) * 0.08f);
					this.tail.yRot = Mth.sin(age * 0.12f) * 0.35f;
					this.tailTip.yRot = Mth.sin(age * 0.12f + 1.0f) * 0.4f;
				} catch (Exception ignored) {
				}
			}
			// 입 벌림 (화염 발사 시)
			try {
				float open = state.mouthOpen > 0 ? 0.7f : 0.08f + Mth.sin(age * 0.05f) * 0.03f;
				this.jaw.xRot = open;
			} catch (Exception ignored) {
			}
			// 볏 흔들림
			try {
				this.crest.xRot = -0.25f + Mth.sin(age * 0.15f) * 0.12f;
			} catch (Exception ignored) {
			}
			// 귀 쫑긋
			try {
				this.earL.zRot = 0.3f + Mth.sin(age * 0.09f) * 0.06f;
				this.earR.zRot = -0.3f - Mth.sin(age * 0.09f + 0.7f) * 0.06f;
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}
