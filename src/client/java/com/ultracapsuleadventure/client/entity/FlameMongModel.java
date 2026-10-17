package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 화염몽 모델 (리뉴얼): 불붙은 고릴라형 원숭이. 너클워크 사지보행.
 * - 부위 33개, 텍스처 128x64, UV 겹침 없음 (면 단위로 손으로 칠한 픽셀아트)
 * - 몸통에 머리/팔/꼬리가 붙어 어깨·목이 같이 움직이고, 팔은 어깨-팔꿈치-손목, 다리는 허벅지-무릎-발 관절
 * - 애니메이션 계산은 {@link FlameMongPose}에 모아 두었다 (걷기/질주/공격/등반/입 벌림)
 * - 이 파일의 부위 정의는 도구가 생성한다. 기존 부위 이름(body, head, jaw, ear_l, ear_r, crest, tail, tail_tip, arm_l, arm_r, leg_l, leg_r)은 그대로 유지.
 */
public class FlameMongModel extends EntityModel<FlameMongRenderState> implements ArmedModel<FlameMongRenderState> {

	private final ModelPart[] animParts = new ModelPart[FlameMongPose.COUNT];
	private final float[] buf = FlameMongPose.newBuffer();
	private final ModelPart bodyPart;
	private final ModelPart armLPart;
	private final ModelPart foreLPart;
	private final ModelPart fistLPart;
	private final ModelPart armRPart;
	private final ModelPart foreRPart;
	private final ModelPart fistRPart;

	/** FlameMongPose.NAMES 순서의 기본 피벗 위치(x,y,z). 위치 오프셋은 여기에 더한다. */
	private static final float[] REST_POS = {
			0f, 11f, 0f, // body
			0f, -6f, -3f, // head
			0f, 0f, -3f, // jaw
			-4.5f, -3.5f, 1f, // ear_l
			4.5f, -3.5f, 1f, // ear_r
			0f, -7f, 0.5f, // crest
			-2.5f, -7f, 0f, // crest_l
			2.5f, -7f, 0f, // crest_r
			-5.5f, -7f, 0f, // mane_l
			5.5f, -7f, 0f, // mane_r
			0f, -8f, -1.5f, // spine_a
			0f, -8f, 0.5f, // spine_b
			0f, -7.5f, 2.5f, // spine_c
			0f, 1f, 3f, // tail
			0f, 0f, 6f, // tail2
			0f, 0f, 6f, // tail_tip
			0f, 0f, 3f, // tail_fire
			-6.5f, -6f, 0f, // arm_l
			0f, 5f, 0f, // fore_l
			0f, 9f, 0f, // fist_l
			6.5f, -6f, 0f, // arm_r
			0f, 5f, 0f, // fore_r
			0f, 9f, 0f, // fist_r
			-3f, 14f, 1f, // leg_l
			0f, 6f, 0f, // shin_l
			0f, 3f, 0f, // foot_l
			3f, 14f, 1f, // leg_r
			0f, 6f, 0f, // shin_r
			0f, 3f, 0f, // foot_r
	};

	public FlameMongModel(ModelPart root) {
		super(root);
		ModelPart body = root.getChild("body");
		ModelPart belly = body.getChild("belly");
		ModelPart pelvis = body.getChild("pelvis");
		ModelPart mane_l = body.getChild("mane_l");
		ModelPart mane_r = body.getChild("mane_r");
		ModelPart spine_a = body.getChild("spine_a");
		ModelPart spine_b = body.getChild("spine_b");
		ModelPart spine_c = body.getChild("spine_c");
		ModelPart tail = body.getChild("tail");
		ModelPart tail2 = tail.getChild("tail2");
		ModelPart tail_tip = tail2.getChild("tail_tip");
		ModelPart tail_fire = tail_tip.getChild("tail_fire");
		ModelPart head = body.getChild("head");
		ModelPart snout = head.getChild("snout");
		ModelPart brow = head.getChild("brow");
		ModelPart jaw = head.getChild("jaw");
		ModelPart ear_l = head.getChild("ear_l");
		ModelPart ear_r = head.getChild("ear_r");
		ModelPart crest = head.getChild("crest");
		ModelPart crest_l = head.getChild("crest_l");
		ModelPart crest_r = head.getChild("crest_r");
		ModelPart arm_l = body.getChild("arm_l");
		ModelPart fore_l = arm_l.getChild("fore_l");
		ModelPart fist_l = fore_l.getChild("fist_l");
		ModelPart arm_r = body.getChild("arm_r");
		ModelPart fore_r = arm_r.getChild("fore_r");
		ModelPart fist_r = fore_r.getChild("fist_r");
		ModelPart leg_l = root.getChild("leg_l");
		ModelPart shin_l = leg_l.getChild("shin_l");
		ModelPart foot_l = shin_l.getChild("foot_l");
		ModelPart leg_r = root.getChild("leg_r");
		ModelPart shin_r = leg_r.getChild("shin_r");
		ModelPart foot_r = shin_r.getChild("foot_r");

		this.animParts[0] = body;
		this.animParts[1] = head;
		this.animParts[2] = jaw;
		this.animParts[3] = ear_l;
		this.animParts[4] = ear_r;
		this.animParts[5] = crest;
		this.animParts[6] = crest_l;
		this.animParts[7] = crest_r;
		this.animParts[8] = mane_l;
		this.animParts[9] = mane_r;
		this.animParts[10] = spine_a;
		this.animParts[11] = spine_b;
		this.animParts[12] = spine_c;
		this.animParts[13] = tail;
		this.animParts[14] = tail2;
		this.animParts[15] = tail_tip;
		this.animParts[16] = tail_fire;
		this.animParts[17] = arm_l;
		this.animParts[18] = fore_l;
		this.animParts[19] = fist_l;
		this.animParts[20] = arm_r;
		this.animParts[21] = fore_r;
		this.animParts[22] = fist_r;
		this.animParts[23] = leg_l;
		this.animParts[24] = shin_l;
		this.animParts[25] = foot_l;
		this.animParts[26] = leg_r;
		this.animParts[27] = shin_r;
		this.animParts[28] = foot_r;
		this.bodyPart = body;
		this.armLPart = arm_l;
		this.foreLPart = fore_l;
		this.fistLPart = fist_l;
		this.armRPart = arm_r;
		this.foreRPart = fore_r;
		this.fistRPart = fist_r;
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(34, 0).addBox(-5f, -8f, -3.5f, 10f, 8f, 7f),
				PartPose.offset(0f, 11f, 0f));
		PartDefinition belly = body.addOrReplaceChild("belly",
				CubeListBuilder.create().texOffs(20, 35).addBox(-3f, -6f, -4.5f, 6f, 5f, 1f),
				PartPose.offset(0f, 0f, 0f));
		PartDefinition pelvis = body.addOrReplaceChild("pelvis",
				CubeListBuilder.create().texOffs(92, 0).addBox(-4f, 0f, -3f, 8f, 4f, 6f),
				PartPose.offset(0f, 0f, 0f));
		PartDefinition mane_l = body.addOrReplaceChild("mane_l",
				CubeListBuilder.create().texOffs(0, 16).addBox(-2f, -1f, -2.5f, 4f, 5f, 5f),
				PartPose.offsetAndRotation(-5.5f, -7f, 0f, 0f, 0f, 0.45f));
		PartDefinition mane_r = body.addOrReplaceChild("mane_r",
				CubeListBuilder.create().texOffs(18, 16).addBox(-2f, -1f, -2.5f, 4f, 5f, 5f),
				PartPose.offsetAndRotation(5.5f, -7f, 0f, 0f, 0f, -0.45f));
		PartDefinition spine_a = body.addOrReplaceChild("spine_a",
				CubeListBuilder.create().texOffs(34, 35).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
				PartPose.offsetAndRotation(0f, -8f, -1.5f, 0.15f, 0f, 0f));
		PartDefinition spine_b = body.addOrReplaceChild("spine_b",
				CubeListBuilder.create().texOffs(84, 26).addBox(-1f, -5f, -1f, 2f, 5f, 2f),
				PartPose.offsetAndRotation(0f, -8f, 0.5f, 0.35f, 0f, 0f));
		PartDefinition spine_c = body.addOrReplaceChild("spine_c",
				CubeListBuilder.create().texOffs(0, 42).addBox(-1f, -3f, -1f, 2f, 3f, 2f),
				PartPose.offsetAndRotation(0f, -7.5f, 2.5f, 0.6f, 0f, 0f));
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(40, 26).addBox(-1f, -1f, 0f, 2f, 2f, 6f),
				PartPose.offsetAndRotation(0f, 1f, 3f, 0.55f, 0f, 0f));
		PartDefinition tail2 = tail.addOrReplaceChild("tail2",
				CubeListBuilder.create().texOffs(56, 26).addBox(-1f, -1f, 0f, 2f, 2f, 6f),
				PartPose.offsetAndRotation(0f, 0f, 6f, 0.6f, 0f, 0f));
		PartDefinition tail_tip = tail2.addOrReplaceChild("tail_tip",
				CubeListBuilder.create().texOffs(92, 26).addBox(-1.5f, -1.5f, 0f, 3f, 3f, 4f),
				PartPose.offsetAndRotation(0f, 0f, 6f, 0.5f, 0f, 0f));
		PartDefinition tail_fire = tail_tip.addOrReplaceChild("tail_fire",
				CubeListBuilder.create().texOffs(42, 35).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
				PartPose.offsetAndRotation(0f, 0f, 3f, 0.3f, 0f, 0f));
		PartDefinition head = body.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.5f, -7f, -4f, 9f, 8f, 8f),
				PartPose.offset(0f, -6f, -3f));
		PartDefinition snout = head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(50, 35).addBox(-2.5f, -3f, -7f, 5f, 3f, 3f),
				PartPose.offset(0f, 0f, 0f));
		PartDefinition brow = head.addOrReplaceChild("brow",
				CubeListBuilder.create().texOffs(8, 42).addBox(-4.5f, -6f, -5f, 9f, 2f, 1f),
				PartPose.offset(0f, 0f, 0f));
		PartDefinition jaw = head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(66, 35).addBox(-2.5f, 0f, -4f, 5f, 2f, 4f),
				PartPose.offsetAndRotation(0f, 0f, -3f, 0.08f, 0f, 0f));
		PartDefinition ear_l = head.addOrReplaceChild("ear_l",
				CubeListBuilder.create().texOffs(100, 16).addBox(-1f, -2.5f, -2f, 1f, 5f, 4f),
				PartPose.offsetAndRotation(-4.5f, -3.5f, 1f, 0f, 0.2f, 0.3f));
		PartDefinition ear_r = head.addOrReplaceChild("ear_r",
				CubeListBuilder.create().texOffs(110, 16).addBox(0f, -2.5f, -2f, 1f, 5f, 4f),
				PartPose.offsetAndRotation(4.5f, -3.5f, 1f, 0f, -0.2f, -0.3f));
		PartDefinition crest = head.addOrReplaceChild("crest",
				CubeListBuilder.create().texOffs(72, 26).addBox(-1.5f, -5f, -1.5f, 3f, 5f, 3f),
				PartPose.offsetAndRotation(0f, -7f, 0.5f, -0.3f, 0f, 0f));
		PartDefinition crest_l = head.addOrReplaceChild("crest_l",
				CubeListBuilder.create().texOffs(84, 35).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
				PartPose.offsetAndRotation(-2.5f, -7f, 0f, -0.15f, 0f, 0.45f));
		PartDefinition crest_r = head.addOrReplaceChild("crest_r",
				CubeListBuilder.create().texOffs(92, 35).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
				PartPose.offsetAndRotation(2.5f, -7f, 0f, -0.15f, 0f, -0.45f));
		PartDefinition arm_l = body.addOrReplaceChild("arm_l",
				CubeListBuilder.create().texOffs(36, 16).addBox(-2f, -1f, -2f, 4f, 6f, 4f),
				PartPose.offset(-6.5f, -6f, 0f));
		PartDefinition fore_l = arm_l.addOrReplaceChild("fore_l",
				CubeListBuilder.create().texOffs(68, 0).addBox(-1.5f, 0f, -1.5f, 3f, 9f, 3f),
				PartPose.offset(0f, 5f, 0f));
		PartDefinition fist_l = fore_l.addOrReplaceChild("fist_l",
				CubeListBuilder.create().texOffs(0, 26).addBox(-2.5f, 0f, -2.5f, 5f, 4f, 5f),
				PartPose.offset(0f, 9f, 0f));
		PartDefinition arm_r = body.addOrReplaceChild("arm_r",
				CubeListBuilder.create().texOffs(52, 16).addBox(-2f, -1f, -2f, 4f, 6f, 4f),
				PartPose.offset(6.5f, -6f, 0f));
		PartDefinition fore_r = arm_r.addOrReplaceChild("fore_r",
				CubeListBuilder.create().texOffs(80, 0).addBox(-1.5f, 0f, -1.5f, 3f, 9f, 3f),
				PartPose.offset(0f, 5f, 0f));
		PartDefinition fist_r = fore_r.addOrReplaceChild("fist_r",
				CubeListBuilder.create().texOffs(20, 26).addBox(-2.5f, 0f, -2.5f, 5f, 4f, 5f),
				PartPose.offset(0f, 9f, 0f));
		PartDefinition leg_l = root.addOrReplaceChild("leg_l",
				CubeListBuilder.create().texOffs(68, 16).addBox(-2f, 0f, -2f, 4f, 6f, 4f),
				PartPose.offset(-3f, 14f, 1f));
		PartDefinition shin_l = leg_l.addOrReplaceChild("shin_l",
				CubeListBuilder.create().texOffs(100, 35).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f),
				PartPose.offset(0f, 6f, 0f));
		PartDefinition foot_l = shin_l.addOrReplaceChild("foot_l",
				CubeListBuilder.create().texOffs(106, 26).addBox(-2f, 0f, -4f, 4f, 1f, 6f),
				PartPose.offset(0f, 3f, 0f));
		PartDefinition leg_r = root.addOrReplaceChild("leg_r",
				CubeListBuilder.create().texOffs(84, 16).addBox(-2f, 0f, -2f, 4f, 6f, 4f),
				PartPose.offset(3f, 14f, 1f));
		PartDefinition shin_r = leg_r.addOrReplaceChild("shin_r",
				CubeListBuilder.create().texOffs(112, 35).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f),
				PartPose.offset(0f, 6f, 0f));
		PartDefinition foot_r = shin_r.addOrReplaceChild("foot_r",
				CubeListBuilder.create().texOffs(0, 35).addBox(-2f, 0f, -4f, 4f, 1f, 6f),
				PartPose.offset(0f, 3f, 0f));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(FlameMongRenderState state) {
		super.setupAnim(state);
		try {
			FlameMongPose.solve(state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot,
					state.xRot, state.mouthOpen, state.attack, state.climbing, this.buf);
			for (int i = 0; i < FlameMongPose.COUNT; i++) {
				ModelPart p = this.animParts[i];
				int b = i * 6;
				p.setPos(REST_POS[i * 3] + this.buf[b], REST_POS[i * 3 + 1] + this.buf[b + 1],
						REST_POS[i * 3 + 2] + this.buf[b + 2]);
				p.xRot = this.buf[b + 3];
				p.yRot = this.buf[b + 4];
				p.zRot = this.buf[b + 5];
			}
			// 수면: 배를 깔고 엎드린 자세로 덮어씀 (자는 동안은 걷기/공격/입벌림을 모두 무시)
			try {
				if (state.sleeping) {
					applySleepPose();
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 수면 엎드림 자세: 몸을 눕히고 팔다리를 접어 바닥에 붙인다.
	 * 인덱스 기준: 0 body, 1 head, 2 jaw, 17/20 팔, 18/21 팔꿈치, 19/22 주먹,
	 * 23/26 다리, 24/27 무릎, 25/28 발.
	 */
	private void applySleepPose() {
		try {
			ModelPart body = this.animParts[0];
			try {
				body.setPos(body.x, body.y + 5f, body.z);
			} catch (Exception ignored) {
			}
			try {
				body.xRot = 1.25f;
				body.yRot = 0f;
				body.zRot = 0f;
			} catch (Exception ignored) {
			}
			try {
				ModelPart head = this.animParts[1];
				head.xRot = 0.6f;
				head.yRot = 0f;
				head.zRot = 0f;
			} catch (Exception ignored) {
			}
			try {
				ModelPart jaw = this.animParts[2];
				jaw.xRot = 0.1f;
			} catch (Exception ignored) {
			}
			for (int i : new int[] { 17, 20 }) {
				try {
					ModelPart a = this.animParts[i];
					a.xRot = 0.4f;
					a.yRot = 0f;
					a.zRot = 0f;
				} catch (Exception ignored) {
				}
			}
			for (int i : new int[] { 18, 21 }) {
				try {
					ModelPart f = this.animParts[i];
					f.xRot = -0.9f;
					f.yRot = 0f;
					f.zRot = 0f;
				} catch (Exception ignored) {
				}
			}
			for (int i : new int[] { 23, 26 }) {
				try {
					ModelPart l = this.animParts[i];
					l.xRot = -1.35f;
					l.yRot = 0f;
					l.zRot = 0f;
				} catch (Exception ignored) {
				}
			}
			for (int i : new int[] { 24, 27 }) {
				try {
					ModelPart s = this.animParts[i];
					s.xRot = 2.1f;
					s.yRot = 0f;
					s.zRot = 0f;
				} catch (Exception ignored) {
				}
			}
			for (int i : new int[] { 25, 28 }) {
				try {
					ModelPart f = this.animParts[i];
					f.xRot = -0.75f;
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 오른손(막대) 위치로 아이템을 옮김. body→팔→팔꿈치→주먹 체인을 그대로 타고 내려가
	 * 너클워크/공격/등반 자세에서도 손에서 떨어지지 않는다.
	 */	@Override
	public void translateToHand(FlameMongRenderState state, HumanoidArm arm, PoseStack pose) {
		try {
			if (arm == HumanoidArm.LEFT) {
				try {
					this.bodyPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.armLPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.foreLPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.fistLPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
			} else {
				try {
					this.bodyPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.armRPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.foreRPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
				try {
					this.fistRPart.translateAndRotate(pose);
				} catch (Exception ignored) {
				}
			}
			// 주먹 한가운데보다 살짝 아래·앞 (막대 자루를 쥔 느낌)
			try {
				pose.translate(0f, 0.22f, -0.08f);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}


