package com.partnercap.mech;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.partnercap.item.ModItems;
import com.partnercap.util.TameUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 탑승형 메카 로봇 (캡슐 변신).
 * - 스스로 움직이지 않음. 주인이 타야 travel()로 조종
 * - 좌클릭(발사 패킷) = 레이저빔 (150뎀, 사거리 64, 쿨 25틱)
 * - 스페이스(점프 패킷) = 점프
 * - 쉬프트 하차 후 웅크리기 우클릭 꾹 = 다시 캡슐로 (폴딩 모션 후 아이템 지급)
 * - 변신 중(펼침/접힘 60틱)은 무적+고정. 트랜스포머식: 회전+부품 폴딩+금속음+전기불꽃
 */
public class MechRobotEntity extends Mob {

	public static final String TAG_OWNER = "mechowner-";

	public static final int ANIM_LEN = 70;
	public static final int MODE_ROBOT = 0;
	public static final int MODE_TO_ROBOT = 1;
	public static final int MODE_TO_CAPSULE = 2;

	public static final float LASER_DAMAGE = 150.0F;
	public static final int LASER_RANGE = 64;
	public static final int LASER_COOLDOWN = 25;
	public static final int BACK_CHARGE_TICKS = 40;

	/** 로봇 최대 체력 = 메카 캡슐 최대 내구도 (둘은 1:1 연동) */
	public static final float MAX_HP = 200.0F;
	public static final int CAPSULE_DURABILITY = 200;
	/**
	 * 탑승자 "눈" 높이 (메카 발 기준 2.55, 메카 바이저 높이, 모델 4/44.5지점).
	 * 플레이어 눈이 항상 이 높이에 오도록 getPassengerRidingPosition에서 탑승자의 눈높이와
	 * 탑승 부착점(Avatar.DEFAULT_VEHICLE_ATTACHMENT, 기본 y=0.6)을 보정한다.
	 * → 1인칭·3인칭 카메라가 메카 얼굴이 되어 메카가 된 것처럼 조종. 레이저도 얼굴에서 발사.
	 */
	public static final float EYE_Y = 2.55F;

	private static final EntityDataAccessor<Integer> TRANSFORM = SynchedEntityData.defineId(MechRobotEntity.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TMODE = SynchedEntityData.defineId(MechRobotEntity.class,
			EntityDataSerializers.INT);

	private int fireCooldown = 0;
	private UUID backCharger = null;
	private int backProgress = 0;
	private int sndTick = 0;
	private float baseYaw = 0f;
	private float lastP = 0f;
	private boolean flashed = false;

	private record ThrowWatch(ServerLevel level, java.util.UUID itemId, UUID owner, long start) {
	}

	private record ReturnWatch(ServerLevel level, java.util.UUID itemId, UUID owner, long start) {
	}

	private static final List<ThrowWatch> THROWS = new ArrayList<>();
	private static final List<ReturnWatch> RETURNS = new ArrayList<>();

	/** 던져진 캡슐 감시 등록 (착지 시 그 자리에서 변신) */
	public static void watchThrown(ServerLevel level, net.minecraft.world.entity.item.ItemEntity item, UUID owner) {
		if (level == null || item == null || owner == null) {
			return;
		}
		try {
			synchronized (THROWS) {
				THROWS.add(new ThrowWatch(level, item.getUUID(), owner, System.currentTimeMillis()));
				while (THROWS.size() > 20) {
					THROWS.remove(0);
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static void watchReturn(ServerLevel level, net.minecraft.world.entity.item.ItemEntity item, UUID owner) {
		if (level == null || item == null || owner == null) {
			return;
		}
		try {
			synchronized (RETURNS) {
				RETURNS.add(new ReturnWatch(level, item.getUUID(), owner, System.currentTimeMillis()));
				while (RETURNS.size() > 20) {
					RETURNS.remove(0);
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static net.minecraft.world.entity.item.ItemEntity findItem(ServerLevel level, java.util.UUID id) {
		try {
			for (Entity e : level.getAllEntities()) {
				if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
					try {
						if (e.getUUID().equals(id) && !e.isRemoved()) {
							return item;
						}
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static ServerPlayer findPlayer(ServerLevel level, UUID id) {
		try {
			for (ServerPlayer p : level.players()) {
				if (p.getUUID().equals(id)) {
					return p;
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 던지기/복귀 감시 (ServerTickEvents 등록용) */
	public static void tickThrows(net.minecraft.server.MinecraftServer server) {
		try {
			List<ThrowWatch> done = new ArrayList<>();
			synchronized (THROWS) {
				for (ThrowWatch w : new ArrayList<>(THROWS)) {
					try {
						net.minecraft.world.entity.item.ItemEntity item = findItem(w.level(), w.itemId());
						if (item == null) {
							done.add(w);
							continue;
						}
						if (System.currentTimeMillis() - w.start() > 30000L) {
							done.add(w);
							continue;
						}
						boolean landed = false;
						try {
							landed = item.onGround() || item.tickCount > 30;
						} catch (Exception ignored) {
						}
						if (!landed) {
							continue;
						}
						Vec3 at = item.position();
						try {
							item.discard();
						} catch (Exception ignored) {
						}
						// 캡슐 내구도 → 로봇 체력 (닳은 캡슐로 변신하면 그만큼 약한 로봇)
						int capsuleDamage = 0;
						try {
							capsuleDamage = Math.max(0, item.getItem().getDamageValue());
						} catch (Exception ignored) {
						}
						MechRobotEntity mech = spawnFromCapsule(w.level(), at, w.owner(), capsuleDamage);
						if (mech != null) {
							try {
								ServerPlayer sp = findPlayer(w.level(), w.owner());
								if (sp != null) {
									sp.sendSystemMessage(Component.literal("로봇 변신!"));
								}
							} catch (Exception ignored) {
							}
						}
						done.add(w);
					} catch (Exception ignored) {
					}
				}
				THROWS.removeAll(done);
			}
		} catch (Exception ignored) {
		}
		try {
			List<ReturnWatch> doneR = new ArrayList<>();
			synchronized (RETURNS) {
				for (ReturnWatch w : new ArrayList<>(RETURNS)) {
					try {
						net.minecraft.world.entity.item.ItemEntity item = findItem(w.level(), w.itemId());
						if (item == null) {
							doneR.add(w);
							continue;
						}
						if (System.currentTimeMillis() - w.start() > 300000L) {
							doneR.add(w);
							continue;
						}
						ServerPlayer sp = findPlayer(w.level(), w.owner());
						if (sp == null || !sp.isAlive()) {
							continue;
						}
						Vec3 to = new Vec3(sp.getX() - item.getX(), (sp.getY() + 1.2) - item.getY(),
								sp.getZ() - item.getZ());
						double len = to.length();
						if (len < 0.4) {
							continue;
						}
						Vec3 v = to.scale(0.45 / len);
						try {
							item.setDeltaMovement(v);
						} catch (Exception ignored) {
						}
					} catch (Exception ignored) {
					}
				}
				RETURNS.removeAll(doneR);
			}
		} catch (Exception ignored) {
		}
	}

	public MechRobotEntity(EntityType<? extends MechRobotEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HP)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 16.0)
				.add(Attributes.SAFE_FALL_DISTANCE, 32.0)
				.add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TRANSFORM, 0);
		builder.define(TMODE, MODE_ROBOT);
	}

	@Override
	protected void registerGoals() {
		try {
			this.goalSelector.addGoal(0, new FloatGoal(this));
		} catch (Exception ignored) {
		}
	}

	public int getTransformTicks() {
		try {
			return Math.max(0, entityData.get(TRANSFORM));
		} catch (Exception e) {
			return 0;
		}
	}

	public int getTransformMode() {
		try {
			return entityData.get(TMODE);
		} catch (Exception e) {
			return MODE_ROBOT;
		}
	}

	private void setTransform(int ticks, int mode) {
		try {
			entityData.set(TRANSFORM, Math.max(0, ticks));
			entityData.set(TMODE, mode);
		} catch (Exception ignored) {
		}
	}

	public static UUID ownerIdOf(Entity entity) {
		try {
			for (String tag : entity.entityTags()) {
				if (tag.startsWith(TAG_OWNER)) {
					try {
						return UUID.fromString(tag.substring(TAG_OWNER.length()));
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	public UUID ownerId() {
		return ownerIdOf(this);
	}

	public boolean isOwner(Player player) {
		try {
			UUID o = ownerId();
			return o != null && player != null && o.equals(player.getUUID());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 조종자 지정 (바닐라 탑승 검증/예측의 기준).
	 * 이게 없으면(Entity 기본 null) 서버가 매 MoveVehicle 패킷을 "not controlling"으로
	 * 원위치 스냅백 + 클라가 패킷 전송·이동 시뮬레이션을 안 해서 타고 가다 순간이동됨.
	 */
	@Override
	public LivingEntity getControllingPassenger() {
		try {
			Entity first = getFirstPassenger();
			if (first instanceof LivingEntity le && le.isAlive()) {
				return le;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static boolean isPetrifiedStatue(Entity entity) {
		try {
			return entity instanceof com.partnercap.medusa.PetrifiedAccess access
					&& access.partnercap$isPetrified();
		} catch (Exception e) {
			return false;
		}
	}

	/** 캡슐 충전 완료 → 로봇 소환 (펼침 모션부터 시작). 캡슐 내구도가 로봇 체력이 됨 */
	public static MechRobotEntity spawnFromCapsule(ServerLevel level, Vec3 pos, UUID owner, int capsuleDamage) {
		MechRobotEntity mech = null;
		try {
			mech = new MechRobotEntity(
					com.partnercap.entity.ModEntityTypes.MECH_ROBOT, level);
		} catch (Exception e) {
			return null;
		}
		try {
			mech.setPos(pos.x, pos.y, pos.z);
		} catch (Exception ignored) {
		}
		try {
			if (owner != null) {
				mech.addTag(TAG_OWNER + owner.toString());
			}
		} catch (Exception ignored) {
		}
		try {
			mech.setPersistenceRequired();
		} catch (Exception ignored) {
		}
		try {
			float hp = MAX_HP - Math.max(0, capsuleDamage);
			mech.setHealth(Math.max(1.0F, Math.min(MAX_HP, hp)));
		} catch (Exception ignored) {
		}
		try {
			mech.setTransform(ANIM_LEN, MODE_TO_ROBOT);
		} catch (Exception ignored) {
		}
		try {
			level.addFreshEntityWithPassengers(mech);
		} catch (Exception e) {
			return null;
		}
		try {
			level.playSound(null, mech.blockPosition(),
					net.minecraft.sounds.SoundEvents.PISTON_EXTEND,
					SoundSource.HOSTILE, 1.2f, 0.6f);
		} catch (Exception ignored) {
		}
		return mech;
	}

	@Override
	public boolean isInvulnerable() {
		try {
			if (getTransformMode() != MODE_ROBOT) {
				return true;
			}
		} catch (Exception ignored) {
		}
		return super.isInvulnerable();
	}

	@Override
	public void travel(Vec3 vec) {
		Entity first = null;
		try {
			first = getFirstPassenger();
		} catch (Exception ignored) {
		}
		LivingEntity driver = (first instanceof LivingEntity le) ? le : null;
		if (driver != null && driver.isAlive() && getTransformMode() == MODE_ROBOT) {
			try {
				setYRot(driver.getYRot());
				yRotO = getYRot();
			} catch (Exception ignored) {
			}
			try {
				setXRot(driver.getXRot() * 0.5f);
			} catch (Exception ignored) {
			}
			try {
				setYHeadRot(getYRot());
			} catch (Exception ignored) {
			}
			float forward = 0f;
			float strafe = 0f;
			try {
				forward = driver.zza;
				strafe = driver.xxa;
			} catch (Exception ignored) {
			}
			double speedMul = 1.0;
			try {
				if (driver.isSprinting()) {
					speedMul = 1.7;
				}
			} catch (Exception ignored) {
			}
			float base = 0.32f * (float) speedMul;
			try {
				this.setSpeed(base);
			} catch (Exception ignored) {
			}
			try {
				super.travel(new Vec3(strafe, vec.y, forward));
				return;
			} catch (Exception ignored) {
			}
		}
		try {
			super.travel(vec);
		} catch (Exception ignored) {
		}
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		try {
			// positionRider는 (이 값 - 탑승자 vehicle attachment)로 탑승자 발을 놓는다(플레이어 기본 y=0.6).
			// 그걸 빼먹으면 눈이 설계 높이보다 0.6칸 낮아져 카메라가 조종석 바닥 아래로 들어간다.
			double eyeH = 1.62;
			double att = 0.0;
			try {
				eyeH = passenger.getEyeHeight();
			} catch (Exception ignored) {
			}
			try {
				att = passenger.getVehicleAttachmentPoint(this).y;
			} catch (Exception ignored) {
			}
			return this.position().add(0.0, EYE_Y - eyeH + att, 0.0);
		} catch (Exception e) {
			return super.getPassengerRidingPosition(passenger);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		try {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			// 석상은 탑승·복귀 모두 불가
			if (isPetrifiedStatue(this)) {
				return InteractionResult.FAIL;
			}
			if (level().isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (getTransformMode() != MODE_ROBOT) {
				return InteractionResult.FAIL;
			}
			if (!isOwner(player)) {
				try {
					player.sendSystemMessage(Component.literal("주인만 조종할 수 있다!"));
				} catch (Exception ignored) {
				}
				return InteractionResult.FAIL;
			}
			if (player.isCrouching()) {
				if (backCharger == null || !backCharger.equals(player.getUUID())) {
					backCharger = player.getUUID();
					backProgress = 0;
					try {
						player.sendSystemMessage(Component.literal("캡슐로 되돌리는 중... 가만히 웅크리고 있어라!"));
					} catch (Exception ignored) {
					}
				}
				return InteractionResult.SUCCESS;
			}
			try {
				player.startRiding(this);
			} catch (Exception e) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS;
		} catch (Exception e) {
			return InteractionResult.PASS;
		}
	}

	/** 레이저 발사 (서버, 탑승+주인+쿨다운 검증) */
	public void tryFire(ServerPlayer driver) {
		try {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			if (fireCooldown > 0) {
				return;
			}
			if (isPetrifiedStatue(this)) {
				return;
			}
			if (getTransformMode() != MODE_ROBOT) {
				return;
			}
			if (driver == null || !isOwner(driver)) {
				return;
			}
			if (driver.getVehicle() != this) {
				return;
			}
			Vec3 eye;
			Vec3 dir;
			try {
				eye = driver.getEyePosition();
				dir = driver.getLookAngle();
			} catch (Exception e) {
				return;
			}
			Vec3 end = eye.add(dir.scale(LASER_RANGE));
			BlockHitResult blockHit = null;
			try {
				blockHit = level.clip(new net.minecraft.world.level.ClipContext(
						eye, end,
						net.minecraft.world.level.ClipContext.Block.OUTLINE,
						net.minecraft.world.level.ClipContext.Fluid.NONE,
						driver));
			} catch (Exception ignored) {
			}
			double range = LASER_RANGE;
			Vec3 blockPos = end;
			try {
				if (blockHit != null) {
					blockPos = blockHit.getLocation();
					range = Math.min(range, eye.distanceTo(blockPos));
				}
			} catch (Exception ignored) {
			}
			LivingEntity hit = null;
			double bestT = range;
			try {
				AABB sweep = new AABB(eye, blockPos).inflate(2.0);
				for (Entity e : level.getEntities(driver, sweep,
						x -> x instanceof LivingEntity living && living.isAlive())) {
					try {
						if (e == driver || e == this) {
							continue;
						}
						if (e.getVehicle() == this || e == driver.getVehicle()) {
							continue;
						}
						// 내 소울은 레이저가 통과 (오폭 방지)
						try {
							UUID foeOwner = com.partnercap.soul.SoulAlly.soulOwnerOf(e);
							if (foeOwner != null && foeOwner.equals(driver.getUUID())) {
								continue;
							}
						} catch (Exception ignored) {
						}
						Vec3 toEnt = new Vec3(e.getX() - eye.x, (e.getY() + e.getBbHeight() * 0.5) - eye.y,
								e.getZ() - eye.z);
						double t = toEnt.dot(dir);
						if (t < 0 || t > bestT) {
							continue;
						}
						Vec3 closest = eye.add(dir.scale(t));
						Vec3 center = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
						if (closest.distanceTo(center) < e.getBbWidth() + 0.9) {
							hit = (LivingEntity) e;
							bestT = t;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			Vec3 beamEnd = (hit != null)
					? new Vec3(hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ())
					: blockPos;
			try {
				double len = eye.distanceTo(beamEnd);
				int steps = Math.max(2, (int) (len / 0.75));
				for (int i = 0; i <= steps; i++) {
					Vec3 p = eye.lerp(beamEnd, i / (double) steps);
					try {
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
								p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
					} catch (Exception ignored) {
					}
					if (i % 2 == 0) {
						try {
							level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
									p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
			try {
				level.playSound(null, blockPosition(),
						net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
						SoundSource.HOSTILE, 1.5f, 0.8f);
			} catch (Exception ignored) {
			}
			if (hit != null) {
				try {
					DamageSource src = level.damageSources().mobAttack(this);
					hit.hurtServer(level, src, LASER_DAMAGE);
				} catch (Exception ignored) {
				}
				try {
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
							beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.3, 0.3, 0.3, 0.0);
				} catch (Exception ignored) {
				}
			} else {
				try {
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
							beamEnd.x, beamEnd.y, beamEnd.z, 6, 0.2, 0.2, 0.2, 0.02);
				} catch (Exception ignored) {
				}
			}
			fireCooldown = LASER_COOLDOWN;
		} catch (Exception ignored) {
		}
	}

	/** 점프 (서버) */
	public void tryJump(ServerPlayer driver) {
		try {
			if (isPetrifiedStatue(this)) {
				return;
			}
			if (getTransformMode() != MODE_ROBOT) {
				return;
			}
			if (driver == null || !isOwner(driver) || driver.getVehicle() != this) {
				return;
			}
			if (!onGround()) {
				return;
			}
			try {
				this.push(0.0, 0.75, 0.0);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public void tick() {
		try {
			super.tick();
		} catch (Exception e) {
			return;
		}
		// 석상 동결: Mob.tick 취소에 더해 조종·복귀충전·변신 진행도 여기서 멈춤
		if (isPetrifiedStatue(this)) {
			return;
		}
		// 탑승 이동은 바닐라 체인(aiStep → travelRidden → travel 오버라이드)이
		// 서버·클라 양쪽에서 1회씩 구동. 여기서 travel()을 직접 부르면 2중 적용 +
		// 클라/서버 발산으로 MoveVehicle 스냅백(원위치 순간이동)이 발생하므로 호출 금지.
		// 탑승자 숨김은 클라 믹스인(LivingEntityRendererPetrifyMixin)이 담당 (갑옷·손·그림자 포함).
		if (level().isClientSide()) {
			return;
		}
		try {
			if (fireCooldown > 0) {
				fireCooldown--;
			}
		} catch (Exception ignored) {
		}
		// 변신 애니메이션 진행 (포드 진동 → 섬광 교체 → 스테이지 전개)
		try {
			int mode = getTransformMode();
			int t = getTransformTicks();
			if (mode != MODE_ROBOT && t > 0) {
				float p = (mode == MODE_TO_ROBOT) ? (t / (float) ANIM_LEN) : (1f - t / (float) ANIM_LEN);
				if (t == ANIM_LEN) {
					try {
						baseYaw = getYRot();
					} catch (Exception ignored) {
					}
					flashed = false;
					lastP = p;
				}
				// 포드 단계에서 진동 (펼쳐질수록 잦아듦)
				try {
					float shake = 9.0f * p;
					setYRot(baseYaw + (float) Math.sin(tickCount * 1.1) * shake);
				} catch (Exception ignored) {
				}
				try {
					setDeltaMovement(getDeltaMovement().scale(0.0));
				} catch (Exception ignored) {
				}
				// 포드↔로봇 교체 순간 섬광 (팝 가림)
				if (!flashed && (lastP - 0.5f) * (p - 0.5f) < 0 && level() instanceof ServerLevel slf) {
					flashed = true;
					try {
						slf.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
								getX(), getY() + 1.5, getZ(), 24, 0.7, 1.0, 0.7, 0.08);
						slf.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
								getX(), getY() + 1.0, getZ(), 12, 0.6, 0.8, 0.6, 0.05);
						slf.playSound(null, blockPosition(),
								net.minecraft.sounds.SoundEvents.PISTON_CONTRACT,
								SoundSource.HOSTILE, 1.2f, 0.9f);
					} catch (Exception ignored) {
					}
				}
				lastP = p;
				sndTick++;
				// 스테이지 금속음 (부품별 쿵)
				if ((t == 55 || t == 40 || t == 25 || t == 12) && level() instanceof ServerLevel sl) {
					try {
						sl.playSound(null, blockPosition(),
								net.minecraft.sounds.SoundEvents.PISTON_EXTEND,
								SoundSource.HOSTILE, 1.1f, 0.65f + (0.5f * (1.0f - t / (float) ANIM_LEN)));
					} catch (Exception ignored) {
					}
				}
				if (sndTick % 8 == 0 && level() instanceof ServerLevel sl) {
					try {
						int n = p > 0.5f ? 7 : 4;
						for (int i = 0; i < n; i++) {
							double ox = (sl.getRandom().nextDouble() - 0.5) * 2.2;
							double oy = sl.getRandom().nextDouble() * (p > 0.5f ? 1.2 : 3.0);
							double oz = (sl.getRandom().nextDouble() - 0.5) * 2.2;
							sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
									getX() + ox, getY() + oy, getZ() + oz, 1, 0, 0, 0, 0.0);
						}
						if (p > 0.5f) {
							sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
									getX(), getY() + 0.4, getZ(), 2, 0.5, 0.2, 0.5, 0.03);
						}
					} catch (Exception ignored) {
					}
				}
				setTransform(t - 1, mode);
				if (t - 1 <= 0) {
					try {
						setYRot(baseYaw);
					} catch (Exception ignored) {
					}
					if (mode == MODE_TO_ROBOT) {
						setTransform(0, MODE_ROBOT);
						if (level() instanceof ServerLevel sl2) {
							try {
								sl2.playSound(null, blockPosition(),
										net.minecraft.sounds.SoundEvents.ANVIL_LAND,
										SoundSource.HOSTILE, 1.0f, 1.4f);
								sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
										getX(), getY() + 1.5, getZ(), 20, 0.8, 1.2, 0.8, 0.06);
							} catch (Exception ignored) {
							}
						}
					} else if (mode == MODE_TO_CAPSULE) {
						finishFoldToCapsule();
					}
				}
				return;
			}
		} catch (Exception ignored) {
		}
		// 캡슐 복귀 충전 진행
		try {
			if (backCharger != null && getTransformMode() == MODE_ROBOT && !isVehicle()) {
				ServerPlayer charger = null;
				if (level() instanceof ServerLevel sl) {
					try {
						for (ServerPlayer p : sl.players()) {
							if (p.getUUID().equals(backCharger)) {
								charger = p;
								break;
							}
						}
					} catch (Exception ignored) {
					}
				}
				boolean ok = charger != null && charger.isAlive() && !charger.isSpectator();
				try {
					ok = ok && charger.isCrouching();
				} catch (Exception ignored) {
					ok = false;
				}
				try {
					ok = ok && distanceToSqr(charger) < 36.0;
				} catch (Exception ignored) {
					ok = false;
				}
				if (!ok) {
					backCharger = null;
					backProgress = 0;
					return;
				}
				backProgress++;
				if (backProgress % 8 == 0 && level() instanceof ServerLevel sl2) {
					int k = (int) (12.0 * backProgress / BACK_CHARGE_TICKS);
					StringBuilder bar = new StringBuilder("캡슐 복귀 ");
					for (int i = 0; i < 12; i++) {
						bar.append(i < k ? "■" : "□");
					}
					try {
						charger.sendSystemMessage(Component.literal(bar.toString()));
					} catch (Exception ignored) {
					}
					try {
						sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
								getX(), getY() + 1.5, getZ(), 4, 0.8, 1.0, 0.8, 0.0);
					} catch (Exception ignored) {
					}
				}
				if (backProgress >= BACK_CHARGE_TICKS) {
					backCharger = null;
					backProgress = 0;
					setTransform(ANIM_LEN, MODE_TO_CAPSULE);
					if (level() instanceof ServerLevel sl3) {
						try {
							sl3.playSound(null, blockPosition(),
									net.minecraft.sounds.SoundEvents.PISTON_EXTEND,
									SoundSource.HOSTILE, 1.2f, 0.6f);
						} catch (Exception ignored) {
						}
					}
				}
			} else {
				backCharger = null;
				backProgress = 0;
			}
		} catch (Exception ignored) {
		}
	}

	private void finishFoldToCapsule() {
		try {
			setTransform(0, MODE_ROBOT);
			UUID owner = ownerId();
			ItemStack cap = ItemStack.EMPTY;
			try {
				cap = new ItemStack(ModItems.MECH_CAPSULE);
			} catch (Exception ignored) {
			}
			// 로봇 체력 → 캡슐 내구도 (70% 체력이면 내구도도 70% 남음). 0은 절대 안 줌(파괴 방지)
			try {
				if (!cap.isEmpty()) {
					float hp = MAX_HP;
					try {
						hp = getHealth();
					} catch (Exception ignored) {
					}
					int dmg = Math.round(MAX_HP - hp);
					dmg = Math.max(0, Math.min(CAPSULE_DURABILITY - 1, dmg));
					cap.setDamageValue(dmg);
				}
			} catch (Exception ignored) {
			}
			if (!cap.isEmpty() && level() instanceof ServerLevel sl) {
				net.minecraft.world.entity.item.ItemEntity back = null;
				try {
					back = new net.minecraft.world.entity.item.ItemEntity(
							sl, getX(), getY() + 1.2, getZ(), cap);
					back.setDeltaMovement(0, 0.35, 0);
					sl.addFreshEntity(back);
				} catch (Exception ignored) {
				}
				// 주인에게 날아가 인벤토리로 빨려 들어감 (주인 없으면 그 자리 드롭)
				if (back != null && owner != null) {
					try {
						watchReturn(sl, back, owner);
					} catch (Exception ignored) {
					}
					try {
						ServerPlayer sp = findPlayer(sl, owner);
						if (sp != null) {
							sp.sendSystemMessage(Component.literal("로봇이 캡슐로 돌아간다!"));
						}
					} catch (Exception ignored) {
					}
				}
				try {
					sl.playSound(null, blockPosition(),
							net.minecraft.sounds.SoundEvents.ANVIL_LAND,
							SoundSource.HOSTILE, 0.8f, 1.6f);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		} finally {
			try {
				discard();
			} catch (Exception ignored) {
			}
		}
	}

	public static AttributeSupplier.Builder baseAttributes() {
		return createAttributes();
	}
}
