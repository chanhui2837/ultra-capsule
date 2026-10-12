package com.partnercap.client.entity;

import java.util.HashMap;
import java.util.Map;

import com.partnercap.PartnerCapMod;
import com.partnercap.medusa.StatueEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * 석상 렌더러: 피해자 모델을 그 순간의 자세로 고정 + 돌 텍스처.
 * 피해자 렌더러의 모델을 빌려 setupAnim(동결 상태) 후 돌 RenderType으로 제출.
 */
public class StatueRenderer extends EntityRenderer<StatueEntity, StatueRenderState> {

	private static final Identifier STONE = PartnerCapMod.id("textures/entity/statue_stone.png");

	private final PlayerModel playerModel;
	private final Map<Integer, Entity> victimCache = new HashMap<>();
	private static final java.util.concurrent.atomic.AtomicInteger NEXT_VICTIM_ID =
			new java.util.concurrent.atomic.AtomicInteger(-2000000);

	private static final net.minecraft.client.renderer.texture.UvMapping IDENTITY_UV =
			new net.minecraft.client.renderer.texture.UvMapping() {
				@Override
				public float getU(float u) {
					return u;
				}

				@Override
				public float getV(float v) {
					return v;
				}
			};

	private static boolean loggedFail = false;
	private static boolean statueDiagDone = false;

	private static void logFail(String where, Exception e) {
		if (loggedFail) {
			return;
		}
		loggedFail = true;
		try {
			com.partnercap.PartnerCapMod.LOGGER.warn("[PartnerCap] statue render failed at " + where, e);
		} catch (Exception ignored) {
		}
	}

	public StatueRenderer(EntityRendererProvider.Context context) {
		super(context);
		PlayerModel pm = null;
		try {
			pm = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
		} catch (Exception ignored) {
		}
		this.playerModel = pm;
		try {
			this.shadowRadius = 0.6f;
		} catch (Exception ignored) {
		}
	}

	@Override
	public StatueRenderState createRenderState() {
		return new StatueRenderState();
	}

	@Override
	public void extractRenderState(StatueEntity entity, StatueRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		try {
			state.victimType = entity.getVictimType();
			state.yaw = entity.getSYaw();
			state.pitch = entity.getSPitch();
			state.body = entity.getSBody();
			state.head = entity.getSHead();
			state.wpos = entity.getSWpos();
			state.wspd = entity.getSWspd();
			state.ftick = entity.getFTick();
			state.baby = entity.getSBaby();
			state.cube = entity.getSCube();
			state.isPlayer = entity.getSIsPlayer();
			state.pname = entity.getSPName();
			state.crouch = entity.getSCrouch();
		} catch (Exception ignored) {
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void submit(StatueRenderState state, PoseStack pose, SubmitNodeCollector collector,
			CameraRenderState camera) {
		try {
			RenderType stone = net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(STONE);
			int light = state.lightCoords;
			if (state.isPlayer) {
				submitPlayer(state, pose, collector, stone, light);
				return;
			}
			submitMob(state, pose, collector, stone, light, camera);
		} catch (Exception ignored) {
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void submitMob(StatueRenderState state, PoseStack pose, SubmitNodeCollector collector,
			RenderType stone, int light, CameraRenderState camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		Entity victim = null;
		try {
			victim = buildVictim(state);
		} catch (Exception e) {
			logFail("buildVictim:" + state.victimType, e);
		}
		if (victim == null) {
			return;
		}
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		EntityRenderer renderer;
		try {
			renderer = dispatcher.getRenderer(victim);
		} catch (Exception e) {
			logFail("getRenderer:" + state.victimType, e);
			return;
		}
		if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
			logFail("notLiving:" + state.victimType, new RuntimeException(renderer.getClass().getName()));
			return;
		}
		Object model;
		try {
			model = livingRenderer.getModel();
		} catch (Exception e) {
			logFail("getModel:" + state.victimType, e);
			return;
		}
		if (!(model instanceof Model modelObj)) {
			logFail("notModel:" + state.victimType, new RuntimeException("no model"));
			return;
		}
		EntityRenderState victimState;
		try {
			victimState = (EntityRenderState) renderer.createRenderState(victim, 1.0f);
		} catch (Exception e) {
			logFail("createState:" + state.victimType, e);
			return;
		}
		try {
			((Model) modelObj).setupAnim(victimState);
		} catch (Exception e) {
			logFail("setupAnim:" + state.victimType, e);
		}
		try {
			pose.rotateDegrees(com.mojang.math.Axis.YP, 180.0f - state.body);
		} catch (Exception ignored) {
		}
		if (state.baby) {
			try {
				pose.scale(0.5f, 0.5f, 0.5f);
			} catch (Exception ignored) {
			}
		}
		try {
			collector.submitModel(modelObj, victimState, pose, stone, light, 0, -1);
		} catch (Exception e) {
			logFail("submitMob:" + state.victimType, e);
			// 폴백: 돌 실패 시 원본 색상 동결 렌더 (투명 방지)
			try {
				((EntityRenderer) livingRenderer).submit(victimState, pose, collector, camera);
			} catch (Exception e2) {
				logFail("fallback:" + state.victimType, e2);
			}
		}
	}

	private final Map<String, Entity> victimByType = new HashMap<>();

	private Entity buildVictim(StatueRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return null;
		}
		String key = state.victimType + "|" + state.baby + "|" + state.cube;
		Entity cached = victimByType.get(key);
		if (cached != null) {
			applyPose(cached, state);
			return cached;
		}
		if (victimByType.size() > 60) {
			try {
				victimByType.clear();
			} catch (Exception ignored) {
			}
		}
		EntityType<?> type = null;
		try {
			Identifier id = Identifier.parse(state.victimType);
			type = BuiltInRegistries.ENTITY_TYPE.get(id).map(h -> h.value()).orElse(null);
		} catch (Exception e) {
			logFail("parse:" + state.victimType, e);
		}
		if (type == null) {
			logFail("type-null:" + state.victimType, new RuntimeException("no type"));
			return null;
		}
		Entity created = null;
		try {
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, true);
			created = type.create(mc.level, request);
			if (created != null) {
				try {
					created.setId(NEXT_VICTIM_ID.getAndDecrement());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception e) {
			logFail("create:" + state.victimType, e);
		}
		if (created == null) {
			logFail("created-null:" + state.victimType, new RuntimeException("no entity"));
			return null;
		}
		applyPose(created, state);
		victimByType.put(key, created);
		return created;
	}

	private void applyPose(Entity created, StatueRenderState state) {
		try {
			created.setYRot(state.yaw);
			created.setXRot(state.pitch);
		} catch (Exception ignored) {
		}
		if (created instanceof LivingEntity living) {
			try {
				living.yBodyRot = state.body;
				living.yHeadRot = state.head;
				living.yHeadRotO = state.head;
			} catch (Exception ignored) {
			}
			try {
				living.walkAnimation.position(state.wpos);
				living.walkAnimation.speed(state.wspd);
			} catch (Exception ignored) {
			}
			try {
				living.tickCount = state.ftick;
			} catch (Exception ignored) {
			}
			try {
				if (state.baby && living instanceof AgeableMob age) {
					age.setAge(-24000);
				}
			} catch (Exception ignored) {
			}
			try {
				if (state.cube > 0 && living instanceof AbstractCubeMob cube) {
					cube.setSize(state.cube, false);
				}
			} catch (Exception ignored) {
			}
		}
	}

	private void submitPlayer(StatueRenderState state, PoseStack pose, SubmitNodeCollector collector,
			RenderType stone, int light) {
		if (playerModel == null) {
			return;
		}
		AvatarRenderState ps;
		try {
			ps = new AvatarRenderState();
		} catch (Exception e) {
			return;
		}
		try {
			ps.outlineColor = net.minecraft.client.renderer.entity.state.EntityRenderState.NO_OUTLINE;
		} catch (Exception ignored) {
		}
		int overlay = 0;
		// 몹 석상과 동일하게 고정 0: 새로 만든 state로 계산하면
		// 피격 흰 플래시가 들어가 하얗게 빛난다
		try {
			ps.walkAnimationPos = state.wpos;
			ps.walkAnimationSpeed = state.wspd;
		} catch (Exception ignored) {
		}
		// 일반 플레이어와 동일하게 외곽 레이어 전부 표시 + 평상시 팔 포즈
		try {
			ps.showHat = true;
			ps.showJacket = true;
			ps.showLeftPants = true;
			ps.showRightPants = true;
			ps.showLeftSleeve = true;
			ps.showRightSleeve = true;
		} catch (Exception ignored) {
		}
		try {
			ps.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
			ps.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
		} catch (Exception ignored) {
		}
		// 진단: 뒤집힘 판정용 1회 로그 (yaw/pitch/body/head)
		try {
			if (!statueDiagDone) {
				statueDiagDone = true;
				com.partnercap.PartnerCapMod.LOGGER.warn(
						"[PartnerCap] player statue pose: yaw=" + state.yaw + " pitch=" + state.pitch
								+ " body=" + state.body + " head=" + state.head + " crouch=" + state.crouch);
			}
		} catch (Exception ignored) {
		}
		try {
			ps.yRot = state.head;
			ps.xRot = state.pitch;
		} catch (Exception ignored) {
		}
		try {
			ps.isCrouching = state.crouch;
		} catch (Exception ignored) {
		}
		try {
			playerModel.setupAnim(ps);
		} catch (Exception ignored) {
		}
		try {
			pose.rotateDegrees(com.mojang.math.Axis.YP, 180.0f - state.body);
		} catch (Exception ignored) {
		}
		if (state.crouch) {
			try {
				pose.translate(0, -0.15, 0);
			} catch (Exception ignored) {
			}
		}
		try {
			collector.submitModel(playerModel, ps, pose, stone, light, overlay, -1);
		} catch (Exception e) {
			logFail("submitPlayer", e);
		}
	}
}
