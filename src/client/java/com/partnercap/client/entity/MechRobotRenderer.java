package com.partnercap.client.entity;

import com.partnercap.PartnerCapMod;
import com.partnercap.mech.MechRobotEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MechRobotRenderer extends MobRenderer<MechRobotEntity, MechRenderState, MechRobotModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(PartnerCapMod.id("mech_robot"), "main");
	private static final Identifier TEXTURE = PartnerCapMod.id("textures/entity/mech_robot.png");

	public MechRobotRenderer(EntityRendererProvider.Context context) {
		super(context, new MechRobotModel(context.bakeLayer(LAYER)), 0.8f);
	}

	@Override
	public MechRenderState createRenderState() {
		return new MechRenderState();
	}

	@Override
	public void extractRenderState(MechRobotEntity entity, MechRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		float p = 0f;
		try {
			int mode = entity.getTransformMode();
			int ticks = entity.getTransformTicks();
			// 틱 값은 20Hz로만 갱신되므로 부분 틱(tickProgress)만큼 보정해 프레임 단위로 부드럽게 (카운트다운이라 감산)
			float st = Math.max(0f, ticks - tickProgress);
			if (mode == MechRobotEntity.MODE_TO_ROBOT && ticks > 0) {
				p = Math.max(0f, Math.min(1f, st / (float) MechRobotEntity.ANIM_LEN));
			} else if (mode == MechRobotEntity.MODE_TO_CAPSULE && ticks >= 0) {
				p = Math.max(0f, Math.min(1f, 1f - st / (float) MechRobotEntity.ANIM_LEN));
			}
		} catch (Exception ignored) {
			p = 0f;
		}
		state.transform = p;
		// 1인칭으로 직접 타는 메카: 겉모습 대신 조종석 내부를 그림 (밖에서 보면 조종석은 안 보임)
		boolean cockpit = false;
		float camYaw = 0f;
		float camPitch = 0f;
		try {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			if (mc != null && mc.player != null && mc.player.getVehicle() == entity) {
				try {
					cockpit = mc.options.getCameraType().isFirstPerson();
				} catch (Exception ignored) {
				}
				if (cockpit) {
					// 카메라와 같은 보간값(partial tick)을 써야 조종석 창틀이 시야 회전 중 떨리지 않는다
					camYaw = mc.player.getViewYRot(tickProgress);
					camPitch = mc.player.getViewXRot(tickProgress);
				}
			}
		} catch (Exception ignored) {
		}
		state.cockpitView = cockpit;
		state.camYaw = camYaw;
		state.camPitch = camPitch;
		try {
			state.bodyYaw = Mth.rotLerp(tickProgress, entity.yBodyRotO, entity.yBodyRot);
		} catch (Exception ignored) {
			state.bodyYaw = camYaw;
		}
		try {
			if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
				if (living.scale != 0) {
					living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
					living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
				}
				float s = 0.55f + 0.45f * (1f - p);
				// 포드 단계에서 부들부들 팽창 (시동 거는 느낌)
				try {
					s += 0.035f * (float) Math.sin(state.ageInTicks * 0.55f) * p;
				} catch (Exception ignored) {
				}
				living.scale = s;
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public Identifier getTextureLocation(MechRenderState state) {
		return TEXTURE;
	}
}
