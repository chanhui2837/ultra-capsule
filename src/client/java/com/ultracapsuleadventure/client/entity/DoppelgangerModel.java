package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 도플갱어 (리뉴얼): 검은 후드 + 해골 낯짝(턱 개폐) + 뼈만 남은 긴 손가락(마디 2개) + 찢긴 로브.
 * 텍스처 64x64, UV 배치는 tools/doppelganger/gen_texture.py 의 LAYOUT 과 동일.
 * 계층: root -> chest(허리 피벗) -> head/arms/hem, root -> legs -> feet.
 * 발은 지면(모델 y=24)에 닿고, 팔은 어깨, 머리는 목 피벗이라 구부정함이 자연스럽게 이어진다.
 */
public class DoppelgangerModel extends EntityModel<DoppelgangerRenderState> {

	private static final float DEG = 0.017453292f;

	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart[] arm = new ModelPart[2]; // 0=오른쪽(-x) 1=왼쪽(+x)
	private final ModelPart[] hand = new ModelPart[2];
	private final ModelPart[][] finger = new ModelPart[2][4];
	private final ModelPart[][] tip = new ModelPart[2][4];
	private final ModelPart[] thumb = new ModelPart[2];
	private final ModelPart[] thumbTip = new ModelPart[2];
	private final ModelPart[] leg = new ModelPart[2];
	private final ModelPart[] foot = new ModelPart[2];
	private final ModelPart[] hem = new ModelPart[4]; // 0=앞R 1=앞L 2=뒤R 3=뒤L

	public DoppelgangerModel(ModelPart root) {
		super(root);
		this.chest = root.getChild("chest");
		this.head = this.chest.getChild("head");
		this.jaw = this.head.getChild("jaw");
		String[] s = {"r", "l"};
		for (int k = 0; k < 2; k++) {
			this.arm[k] = this.chest.getChild("arm_" + s[k]);
			this.hand[k] = this.arm[k].getChild("hand_" + s[k]);
			for (int i = 0; i < 4; i++) {
				this.finger[k][i] = this.hand[k].getChild("f" + i + "_" + s[k]);
				this.tip[k][i] = this.finger[k][i].getChild("t" + i + "_" + s[k]);
			}
			this.thumb[k] = this.hand[k].getChild("thumb_" + s[k]);
			this.thumbTip[k] = this.thumb[k].getChild("thumbtip_" + s[k]);
			this.leg[k] = root.getChild("leg_" + s[k]);
			this.foot[k] = this.leg[k].getChild("foot_" + s[k]);
		}
		this.hem[0] = this.chest.getChild("hem_fr");
		this.hem[1] = this.chest.getChild("hem_fl");
		this.hem[2] = this.chest.getChild("hem_br");
		this.hem[3] = this.chest.getChild("hem_bl");
	}

	public static LayerDefinition getTexturedModelData() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 다리 + 발
		root.addOrReplaceChild("leg_r",
				CubeListBuilder.create().texOffs(48, 16).addBox(-1.5f, 0, -1.5f, 3, 13, 3),
				PartPose.offset(-2.2f, 11, 0))
				.addOrReplaceChild("foot_r",
						CubeListBuilder.create().texOffs(30, 49).addBox(-1.5f, 0, -3.5f, 3, 2, 5),
						PartPose.offset(0, 11, 0));
		root.addOrReplaceChild("leg_l",
				CubeListBuilder.create().texOffs(48, 33).addBox(-1.5f, 0, -1.5f, 3, 13, 3),
				PartPose.offset(2.2f, 11, 0))
				.addOrReplaceChild("foot_l",
						CubeListBuilder.create().texOffs(30, 49).addBox(-1.5f, 0, -3.5f, 3, 2, 5),
						PartPose.offset(0, 11, 0));

		// 가슴 (허리 피벗) = 속옷 + 로브
		PartDefinition chest = root.addOrReplaceChild("chest",
				CubeListBuilder.create()
						.texOffs(0, 16).addBox(-4, -13, -2, 8, 13, 4)
						.texOffs(24, 16).addBox(-4, -13, -2, 8, 13, 4, new CubeDeformation(0.55f)),
				PartPose.offset(0, 11, 0));

		// 찢긴 자락 (걸을 때 흔들림)
		chest.addOrReplaceChild("hem_fr", CubeListBuilder.create().texOffs(0, 58).addBox(-1, 0, -0.5f, 2, 5, 1),
				PartPose.offset(-2, 0.2f, -2.6f));
		chest.addOrReplaceChild("hem_fl", CubeListBuilder.create().texOffs(0, 58).addBox(-1, 0, -0.5f, 2, 5, 1),
				PartPose.offset(2, 0.2f, -2.6f));
		chest.addOrReplaceChild("hem_br", CubeListBuilder.create().texOffs(0, 58).addBox(-1, 0, -0.5f, 2, 5, 1),
				PartPose.offset(-2, 0.2f, 2.6f));
		chest.addOrReplaceChild("hem_bl", CubeListBuilder.create().texOffs(0, 58).addBox(-1, 0, -0.5f, 2, 5, 1),
				PartPose.offset(2, 0.2f, 2.6f));

		// 머리 (목 피벗): 윗머리 + 턱(경첩) + 후드
		PartDefinition head = chest.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 6, 8),
				PartPose.offset(0, -13, 0));
		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(0, 49).addBox(-4, 0, -7, 8, 2, 7),
				PartPose.offset(0, -2, 3));
		head.addOrReplaceChild("hood",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.6f)),
				PartPose.ZERO);

		// 팔 + 소매 + 손(손바닥, 손가락 4 x 2마디, 엄지)
		addArm(chest, "r", -5.8f);
		addArm(chest, "l", 5.8f);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void addArm(PartDefinition chest, String s, float x) {
		boolean right = s.equals("r");
		PartDefinition a = chest.addOrReplaceChild("arm_" + s,
				CubeListBuilder.create()
						.texOffs(right ? 0 : 12, 33).addBox(-1.5f, -2, -1.5f, 3, 12, 3)
						.texOffs(right ? 24 : 36, 33).addBox(-1.5f, -2, -1.5f, 3, 12, 3, new CubeDeformation(0.5f)),
				PartPose.offset(x, -11, 0));
		PartDefinition h = a.addOrReplaceChild("hand_" + s,
				CubeListBuilder.create().texOffs(46, 49).addBox(-2, 0, -1, 4, 3, 2),
				PartPose.offset(0, 10, 0));
		float[] fx = {-2, -1, 0, 1};
		for (int i = 0; i < 4; i++) {
			PartDefinition f = h.addOrReplaceChild("f" + i + "_" + s,
					CubeListBuilder.create().texOffs(58, 49).addBox(0, 0, -0.5f, 1, 3, 1),
					PartPose.offset(fx[i], 3, 0));
			f.addOrReplaceChild("t" + i + "_" + s,
					CubeListBuilder.create().texOffs(58, 53).addBox(0, 0, -0.5f, 1, 3, 1),
					PartPose.offset(0, 3, 0));
		}
		PartDefinition th = h.addOrReplaceChild("thumb_" + s,
				CubeListBuilder.create().texOffs(58, 49).addBox(0, 0, -0.5f, 1, 3, 1),
				PartPose.offset(right ? 2 : -3, 0.5f, -0.5f));
		th.addOrReplaceChild("thumbtip_" + s,
				CubeListBuilder.create().texOffs(58, 53).addBox(0, 0, -0.5f, 1, 2, 1),
				PartPose.offset(0, 3, 0));
	}

	private static float clampf(float v, float a, float b) {
		return Math.max(a, Math.min(b, v));
	}

	@Override
	public void setupAnim(DoppelgangerRenderState state) {
		super.setupAnim(state); // 모든 파츠 포즈 초기화

		final float t = state.ageInTicks;
		final float amp = clampf(state.walkAnimationSpeed, 0f, 1f);
		final float p = state.walkAnimationPos * 0.6662f;
		final float c = Mth.cos(p);
		final boolean ambush = state.phase == 1;
		final float atk = clampf(state.attack, 0f, 1f);
		final float swing = atk > 0f ? Mth.sin(Mth.sqrt(atk) * Mth.PI) : 0f;
		final float swingL = atk > 0f ? Mth.sin(clampf((atk - 0.2f) / 0.8f, 0f, 1f) * Mth.PI) : 0f;
		float tw = Math.max(0f, Mth.sin(t * 0.17f));
		tw = tw * tw;
		tw = tw * tw;
		tw = tw * tw;
		tw = tw * tw; // ^16: 가끔 번뜩이는 경련
		final float twitch = ambush ? 0f : tw;

		// 가슴: 스토크=구부정, 매복=곧게. 걸을 때 몸을 앞으로 숙이고 비튼다
		final float hunch = ambush ? -0.04f : 0.26f;
		this.chest.xRot = hunch + 0.025f * Mth.sin(t * 0.07f) + amp * 0.10f + swing * 0.38f;
		this.chest.yRot = c * 0.10f * amp + (atk > 0f ? Mth.sin(Mth.sqrt(atk) * Mth.PI * 2f) * 0.28f : 0f);
		this.chest.zRot = c * 0.04f * amp;
		float s = Mth.sin(p);
		this.chest.y = 11f + 0.25f * Mth.sin(t * 0.07f) - 0.6f * amp * s * s;

		// 다리/발: 대각 보행, 발목은 몸 기울기를 일부 상쇄
		for (int k = 0; k < 2; k++) {
			float sign = k == 0 ? 1f : -1f;
			this.leg[k].xRot = sign * c * 0.85f * amp;
			this.foot[k].xRot = -this.leg[k].xRot * 0.6f;
		}

		// 머리: 시선 추적 + 구부정함 상쇄 + 고개 갸웃(스토크) + 턱
		float yaw = clampf(state.yRot, -60f, 60f) * DEG;
		float pit = clampf(state.xRot, -40f, 40f) * DEG;
		this.head.yRot = yaw - this.chest.yRot * 0.6f;
		this.head.xRot = pit - this.chest.xRot * 0.85f + (ambush ? -0.05f : -0.10f) + 0.03f * Mth.sin(t * 0.05f);
		float tilt = ambush ? 0f : (0.16f + 0.07f * Mth.sin(t * 0.031f)) * (1f - amp * 0.6f);
		this.head.zRot = tilt + twitch * 0.14f;
		this.jaw.xRot = (ambush ? 0.55f : 0.10f + 0.06f * Mth.sin(t * 0.13f)) + swing * 0.5f + twitch * 0.5f;

		// 팔: 걷기 스윙(다리 반대) / 매복 시 앞으로 뻗음 / 공격 시 할퀴기
		final float splay = ambush ? 0.30f : 0.07f + 0.03f * Mth.sin(t * 0.06f);
		for (int k = 0; k < 2; k++) {
			float sign = k == 0 ? 1f : -1f;
			float base = ambush ? -1.25f : -0.05f;
			ModelPart a = this.arm[k];
			a.xRot = base - sign * c * 0.55f * amp + 0.03f * Mth.sin(t * 0.05f + (k == 0 ? 0f : 1.7f));
			a.zRot = sign * splay;
			if (k == 0) {
				a.xRot -= swing * 1.5f;
				a.zRot -= swing * 0.4f;
			} else {
				a.xRot -= swingL * 1.3f;
				a.zRot += swingL * 0.4f;
			}
			this.hand[k].xRot = ambush ? 0.25f : 0.15f;
			for (int i = 0; i < 4; i++) {
				float curl = (ambush ? 0.55f : 0.35f) + 0.08f * Mth.sin(t * 0.09f + i * 0.9f) + swing * 0.8f
						+ (i - 1.5f) * 0.05f;
				this.finger[k][i].zRot = -sign * curl * 0.9f;
				this.tip[k][i].zRot = -sign * curl * 1.25f;
			}
			this.thumb[k].zRot = -sign * (0.35f + swing * 0.4f);
			this.thumbTip[k].zRot = -sign * 0.5f;
		}

		// 자락: 앞 자락은 앞으로 뻗는 다리에, 뒤 자락은 뒤로 가는 다리에 밀리고 바람에 살랑
		for (int i = 0; i < 4; i++) {
			boolean front = i < 2;
			float sw = this.leg[i % 2].xRot;
			this.hem[i].xRot = (front ? Math.min(0f, sw) : Math.max(0f, sw)) * 0.6f - this.chest.xRot * 0.7f
					+ 0.05f * Mth.sin(t * 0.08f + i + (front ? 0f : 1.3f));
		}
	}
}


