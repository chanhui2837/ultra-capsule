package com.ultracapsuleadventure.doppel;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 도플갱어: 밤의 스토커.
 * - 밤에만 플레이어 근처에 나타남 (스폰은 DoppelEvents 담당)
 * - 스토크 단계: 10~20블록 거리 유지, 눈 마주치면 다른 곳으로 순간이동 (나무 뒤 선호)
 * - 가끔 다른 엔티티로 변장 (실제 바닐라 몹으로 교체, DoppelEvents가 관리)
 * - 며칠 스토킹 후 매복 단계: 플레이어 앞에 나타나 근접 공격
 */
public class DoppelgangerEntity extends Monster {

	public static final int STALK = 0;
	public static final int AMBUSH = 1;

	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DoppelgangerEntity.class,
			EntityDataSerializers.INT);

	private java.util.UUID victimId;
	private int teleportCooldown = 0;
	private int stareCooldown = 0;

	public DoppelgangerEntity(EntityType<? extends DoppelgangerEntity> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 7.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PHASE, STALK);
	}

	public int getPhase() {
		try {
			return entityData.get(PHASE);
		} catch (Exception e) {
			return STALK;
		}
	}

	public void setPhase(int phase) {
		try {
			entityData.set(PHASE, phase);
		} catch (Exception ignored) {
		}
	}

	public boolean isAmbush() {
		return getPhase() == AMBUSH;
	}

	public void setVictim(java.util.UUID id) {
		this.victimId = id;
	}

	public java.util.UUID getVictim() {
		return victimId;
	}

	/** 매복 돌변: 플레이어 정면에 나타나게 하는 것은 DoppelEvents가 좌표 지정 */
	public void beginAmbush() {
		setPhase(AMBUSH);
		try {
			getNavigation().stop();
		} catch (Exception ignored) {
		}
		try {
			if (level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.2, getZ(), 24, 0.4, 0.8, 0.4, 0.05);
				sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.4f, 0.7f);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void registerGoals() {
		try {
			this.goalSelector.addGoal(0, new FloatGoal(this));
			this.goalSelector.addGoal(1, new StalkGoal(this));
			this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
			this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
			this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		} catch (Exception ignored) {
		}
		try {
			this.targetSelector.addGoal(1, new AmbushTargetGoal(this));
		} catch (Exception ignored) {
		}
	}

	/** 매복 단계에서만 스토킹 피해자를 공격 목표로 삼는다 */
	static class AmbushTargetGoal extends NearestAttackableTargetGoal<Player> {
		private final DoppelgangerEntity mob;

		public AmbushTargetGoal(DoppelgangerEntity mob) {
			super(mob, Player.class, true);
			this.mob = mob;
		}

		/** 소울 아군은 스토커가 아니다 (SoulFollowGoal이 이동 담당) */
		private static boolean isSoul(DoppelgangerEntity m) {
			try {
				return com.ultracapsuleadventure.soul.SoulAlly.soulOwnerOf(m) != null;
			} catch (Exception ignored) {
				return false;
			}
		}

		@Override
		public boolean canUse() {
			try {
				if (!mob.isAmbush()) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
			if (isSoul(mob)) {
				return false;
			}
			try {
				if (mob.level() instanceof ServerLevel sl) {
					java.util.UUID vid = mob.getVictim();
					if (vid != null) {
						for (ServerPlayer p : sl.players()) {
							if (p.getUUID().equals(vid) && p.isAlive() && !p.isCreative()
									&& !p.isSpectator()) {
								this.target = p;
								return true;
							}
						}
						return false;
					}
				}
			} catch (Exception ignored) {
			}
			return super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			try {
				if (!mob.isAmbush()) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
			if (isSoul(mob)) {
				return false;
			}
			return super.canContinueToUse();
		}
	}

	/** 스토크 단계 이동: 피해자와 10~20블록 유지, 가까우면 물러나고 멀면 다가간다 */
	static class StalkGoal extends Goal {
		private final DoppelgangerEntity mob;
		private int recalc = 0;

		public StalkGoal(DoppelgangerEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private ServerPlayer victim() {
			try {
				java.util.UUID vid = mob.getVictim();
				if (vid == null || !(mob.level() instanceof ServerLevel sl)) {
					return null;
				}
				for (ServerPlayer p : sl.players()) {
					if (p.getUUID().equals(vid) && p.isAlive()) {
						return p;
					}
				}
			} catch (Exception ignored) {
			}
			return null;
		}

		@Override
		public boolean canUse() {
			try {
				if (mob.isAmbush()) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
			if (AmbushTargetGoal.isSoul(mob)) {
				return false;
			}
			return victim() != null;
		}

		@Override
		public boolean canContinueToUse() {
			try {
				if (mob.isAmbush()) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
			if (AmbushTargetGoal.isSoul(mob)) {
				return false;
			}
			ServerPlayer v = victim();
			return v != null && v.isAlive();
		}

		@Override
		public void tick() {
			super.tick();
			ServerPlayer v;
			try {
				v = victim();
			} catch (Exception e) {
				return;
			}
			if (v == null) {
				return;
			}
			double d2;
			try {
				d2 = mob.distanceToSqr(v);
			} catch (Exception e) {
				return;
			}
			try {
				mob.getLookControl().setLookAt(v, 20.0F, 20.0F);
			} catch (Exception ignored) {
			}
			// 너무 가까우면(6-) 뒤로 물러나고, 적당하면(10~20) 가만히 엿보고, 멀면(20+) 다가간다
			if (--recalc > 0) {
				return;
			}
			recalc = 10;
			try {
				if (d2 < 36.0) {
					// 6블록 안: 반대 방향으로 살짝 후퇴
					Vec3 away = new Vec3(mob.getX() - v.getX(), 0, mob.getZ() - v.getZ());
					double len = away.length();
					if (len > 1e-4) {
						away = away.scale(8.0 / len);
						mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.0);
					}
				} else if (d2 > 400.0) {
					mob.getNavigation().moveTo(v, 1.0);
				} else {
					mob.getNavigation().stop();
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public void tick() {
		try {
			super.tick();
		} catch (Exception e) {
			return;
		}
		if (level().isClientSide()) {
			return;
		}
		if (teleportCooldown > 0) {
			teleportCooldown--;
		}
		if (stareCooldown > 0) {
			stareCooldown--;
		}
		// 낮 화상 데미지 없음: 낮/밤 수명은 DoppelEvents가 관리(새벽에 소멸)하므로
		// 여기서 매초 inFire 피해를 주면 "원인 모를 지속 데미지"처럼 보인다.
		boolean soul = false;
		try {
			soul = com.ultracapsuleadventure.soul.SoulAlly.soulOwnerOf(this) != null;
		} catch (Exception ignored) {
		}
		// 스토크 중 눈 마주치면 순간이동 (쿨다운 40틱, 소울 아군은 제외: 주인이 쳐다봤다고 튀면 안 됨)
		if (!soul && !isAmbush() && teleportCooldown <= 0) {
			try {
				ServerPlayer v = stalkVictimPlayer();
				if (v != null && v.isAlive() && distanceToSqr(v) < 1024.0) {
					boolean staring = false;
					try {
						staring = com.ultracapsuleadventure.medusa.Petrify.lookingAt(v,
								new Vec3(getX(), getY() + 1.6, getZ()), 0.90)
								&& hasLineOfSight(v);
					} catch (Exception ignored) {
					}
					if (staring) {
						teleportAwayFrom(v);
					}
				}
			} catch (Exception ignored) {
			}
		}
		// 스토크 중 너무 멀어지면(48+) 피해자 근처로 몰래 순간이동
		if (!isAmbush() && tickCount % 60 == 0) {
			try {
				ServerPlayer v = stalkVictimPlayer();
				if (v != null && v.isAlive() && distanceToSqr(v) > 2304.0 && teleportCooldown <= 0) {
					teleportNear(v, 12, 18);
				}
			} catch (Exception ignored) {
			}
		}
	}

	private ServerPlayer stalkVictimPlayer() {
		try {
			if (victimId == null || !(level() instanceof ServerLevel sl)) {
				return null;
			}
			for (ServerPlayer p : sl.players()) {
				if (p.getUUID().equals(victimId)) {
					return p;
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 피해자 시야에서 벗어난 다른 곳(나무 뒤 선호)으로 순간이동 */
	public void teleportAwayFrom(ServerPlayer v) {
		try {
			BlockPos best = null;
			// 후보 10곳 중 나무 근처 우선
			for (int i = 0; i < 10; i++) {
				BlockPos c = pickStalkPos(v, 10, 20);
				if (c == null) {
					continue;
				}
				if (best == null) {
					best = c;
				}
				try {
					if (nearTree(c, 5)) {
						best = c;
						break;
					}
				} catch (Exception ignored) {
				}
			}
			if (best == null) {
				return;
			}
			teleportToPos(best);
			teleportCooldown = 40;
		} catch (Exception ignored) {
		}
	}

	public void teleportNear(ServerPlayer v, int minR, int maxR) {
		try {
			BlockPos c = pickStalkPos(v, minR, maxR);
			if (c == null) {
				return;
			}
			teleportToPos(c);
			teleportCooldown = 60;
		} catch (Exception ignored) {
		}
	}

	private void teleportToPos(BlockPos pos) {
		try {
			if (level() instanceof ServerLevel sl) {
				try {
					sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.1);
				} catch (Exception ignored) {
				}
				try {
					sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 0.6f);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			getNavigation().stop();
		} catch (Exception ignored) {
		}
		try {
			if (level() instanceof ServerLevel sl) {
				try {
					sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.1);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 피해자 주변 minR~maxR 고리에서 안전 지면 찾기 */
	private BlockPos pickStalkPos(ServerPlayer v, int minR, int maxR) {
		try {
			for (int i = 0; i < 12; i++) {
				double a = level().getRandom().nextDouble() * Math.PI * 2.0;
				double r = minR + level().getRandom().nextDouble() * (maxR - minR);
				int x = (int) Math.floor(v.getX() + Math.cos(a) * r);
				int z = (int) Math.floor(v.getZ() + Math.sin(a) * r);
				int y = (int) Math.floor(v.getY());
				// y ±6 탐색
				for (int dy = 6; dy >= -6; dy--) {
					BlockPos p = new BlockPos(x, y + dy, z);
					try {
						if (isSafeGround(p)) {
							// 피해자 시야 정면은 피함 (뒤·옆 선호): 피해자 시선과 후보 방향 내적 < 0.3
							try {
								Vec3 look = v.getLookAngle();
								Vec3 to = new Vec3(p.getX() + 0.5 - v.getX(), 0, p.getZ() + 0.5 - v.getZ());
								double len = to.length();
								if (len > 1e-4) {
									to = to.scale(1.0 / len);
									double dot = look.x * to.x + look.z * to.z;
									if (dot > 0.5 && level().getRandom().nextFloat() < 0.8f) {
										continue;
									}
								}
							} catch (Exception ignored) {
							}
							return p;
						}
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private boolean isSafeGround(BlockPos p) {
		try {
			BlockState below = level().getBlockState(p.below());
			BlockState at = level().getBlockState(p);
			BlockState above = level().getBlockState(p.above());
			BlockState above2 = level().getBlockState(p.above().above());
			if (!below.isSolidRender()) {
				return false;
			}
			// 키 2.3블록: 2칸만 비우면 머리(0.3)가 끼어 질식 데미지가 계속 들어간다. 3칸 확보.
			return at.isAir() && above.isAir() && above2.isAir();
		} catch (Exception e) {
			return false;
		}
	}

	private boolean nearTree(BlockPos p, int r) {
		try {
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -3; dy <= 4; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						BlockPos q = p.offset(dx, dy, dz);
						try {
							BlockState s = level().getBlockState(q);
							String id = "";
							try {
								id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
							} catch (Exception ignored) {
							}
							if (id.contains("log") || id.contains("leaves")) {
								return true;
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean r = false;
		try {
			r = super.doHurtTarget(level, target);
		} catch (Exception ignored) {
		}
		// 매복 공격 적중 시 어둠 + 짧은 실명 느낌 (공포 연출)
		try {
			if (r && target instanceof LivingEntity living && living.isAlive()) {
				living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false, true));
			}
		} catch (Exception ignored) {
		}
		return r;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		try {
			out.putInt("DoppelPhase", getPhase());
		} catch (Exception ignored) {
		}
		try {
			if (victimId != null) {
				out.putString("DoppelVictim", victimId.toString());
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			int ph = in.getInt("DoppelPhase").orElse(STALK);
			setPhase(ph == AMBUSH ? AMBUSH : STALK);
		} catch (Exception ignored) {
		}
		try {
			String s = in.getString("DoppelVictim").orElse("");
			if (!s.isEmpty()) {
				victimId = java.util.UUID.fromString(s);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		try {
			return super.finalizeSpawn(level, difficulty, reason, data);
		} catch (Exception e) {
			return data;
		}
	}

	public static boolean isDoppel(Entity e) {
		try {
			return e instanceof DoppelgangerEntity;
		} catch (Exception ex) {
			return false;
		}
	}
}


