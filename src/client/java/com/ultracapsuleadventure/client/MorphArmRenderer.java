package com.ultracapsuleadventure.client;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 1인칭에서 변신한 몹의 "손"(팔/앞다리/날개)을 바닐라 1인칭 팔 자리에 그린다.
 * 바닐라가 이미 팔 위치·스윙 변환을 PoseStack 에 적용해 둔 상태에서 호출되므로
 * 휘두르기·장비 올리기 모션이 그대로 따라온다.
 * 몹 모델의 팔 ModelPart 하나만 담은 별도 모델을 만들어 제출한다
 * (submit 파이프라인은 visible 을 읽지 않으므로 파트 숨김 대신 단일 파트 모델 사용).
 */
public class MorphArmRenderer {

	private static final Map<ModelPart, ArmModel> CACHE = new IdentityHashMap<>();
	private static final String[] RIGHT = { "right_arm", "right_front_leg", "right_wing", "right_hand", "arms" };
	private static final String[] LEFT = { "left_arm", "left_front_leg", "left_wing", "left_hand", "arms" };

	/** 팔 파트 하나만 가진 모델. */
	private static final class ArmModel extends EntityModel<EntityRenderState> {
		ArmModel(ModelPart arm) {
			super(new ModelPart(List.of(), Map.of("arm", arm)));
		}
	}

	/** 그렸으면 true. 팔이 없는 몹(슬라임·물고기 등)이면 false (이때도 사람 팔은 숨김) */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean draw(PoseStack pose, SubmitNodeCollector collector, int light, boolean right) {
		try {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if (player == null || mc.level == null) {
				return false;
			}
			String mid = MorphEvents.morphId(player);
			if (mid == null || mid.isEmpty()) {
				return false;
			}
			Entity dummy = MorphClient.dummyFor(player, mid);
			if (dummy == null) {
				return false;
			}
			EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(dummy);
			if (!(renderer instanceof LivingEntityRenderer lr)) {
				return false;
			}
			Object model = lr.getModel();
			ModelPart arm = findArm(model, right);
			if (arm == null) {
				return false;
			}
			// 몹 모델 팔 파트의 원래 변환(몸체 기준 오프셋)을 0으로 리셋해 바닐라 1인칭 팔 위치에 맞게 그린다.
			float ox = arm.x, oy = arm.y, oz = arm.z;
			float oxr = arm.xRot, oyr = arm.yRot, ozr = arm.zRot;
			arm.x = 0;
			arm.y = 0;
			arm.z = 0;
			arm.xRot = 0;
			arm.yRot = 0;
			arm.zRot = 0;
			LivingEntityRenderState state = (LivingEntityRenderState) renderer.createRenderState(dummy, 1.0f);
			Identifier tex = (Identifier) lr.getTextureLocation(state);
			ArmModel am = CACHE.computeIfAbsent(arm, ArmModel::new);
			collector.submitModel(am, state, pose, RenderTypes.entityCutout(tex), light,
					OverlayTexture.NO_OVERLAY, -1);
			// 원래 변환 복원 (원본 모델 렌더링에 영향 주지 않음)
			arm.x = ox;
			arm.y = oy;
			arm.z = oz;
			arm.xRot = oxr;
			arm.yRot = oyr;
			arm.zRot = ozr;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private static ModelPart findArm(Object model, boolean right) {
		try {
			if (model instanceof HumanoidModel hm) {
				return right ? hm.rightArm : hm.leftArm;
			}
			if (model instanceof Model m) {
				ModelPart root = m.root();
				for (String n : right ? RIGHT : LEFT) {
					if (root.hasChild(n)) {
						return root.getChild(n);
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	public static void clear() {
		CACHE.clear();
	}
}


