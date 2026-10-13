package com.partnercap.flame;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 화염몽: 불 타입 원숭이 (중립, 용암 휴식처 수호).
 * - 사막에서만 소량 자연 스폰, 집=근처 용암 웅덩이 (32m 탐색, 없으면 그 자리)
 * - 평소엔 온순: 주변에 랜덤 불쏘기(입에서 불), 나무 오르내리기, 용암 안에서 휴식
 * - 휴식처 용암 10m 안에 플레이어가 접근하면 돌변: 화염+초월의 막대(슬로우) 공격
 * - 오른손엔 항상 초월의 막대 (지팡이 겨누면 이동속도 저하, 쿨 10초)
 */
public class FlameMongEntity extends Monster {

	public static final float BREATH_DAMAGE = 6.0F;
	public static final double BREATH_RANGE = 12.0;
	/** 한 번 불을 뿜면 유지되는 시간 (3초 길게) */
	public static final int BREATH_DURATION = 60;
	/** 브레스 중 주변 착화 반경 */
	public static final double IGNITE_RADIUS = 4.0;
	public static final int WAND_COOLDOWN_TICKS = 200;
	public static final int WAND_SLOW_TICKS = 60;
	public static final double HOME_RADIUS = 32.0;
	public static final double GUARD_RADIUS = 10.0;

	private static final EntityDataAccessor<Integer> WAND_CD = SynchedEntityData.defineId(FlameMongEntity.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MOUTH = SynchedEntityData.defineId(FlameMongEntity.class,
			EntityDataSerializers.INT);

	private BlockPos homeLava = null;
	private int spitTick = 0;
	private int playTick = 0;
	private int homeScanTick = 0;
	/** 지속 브레스 잔여 틱 + 방향 (한 번 쏘면 길게 나감) */
	private int breathTicks = 0;
	private Vec3 breathDir = Vec3.ZERO;

	public FlameMongEntity(EntityType<? extends FlameMongEntity> type, Level level) {
		super(type, level);
		try {
			setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModFlame.TRANSCEND_WAND));
		} catch (Exception ignored) {
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.42)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		try {
			builder.define(WAND_CD, 0);
			builder.define(MOUTH, 0);
		} catch (Exception ignored) {
		}
	}

	public int getWandCooldown() {
		try {
			return Math.max(0, entityData.get(WAND_CD));
		} catch (Exception e) {
			return 0;
		}
	}

	public int getMouthOpen() {
		try {
			return Math.max(0, entityData.get(MOUTH));
		} catch (Exception e) {
			return 0;
		}
	}

	private void setWandCooldown(int v) {
		try {
			entityData.set(WAND_CD, Math.max(0, v));
		} catch (Exception ignored) {
		}
	}

	private void setMouthOpen(int v) {
		try {
			entityData.set(MOUTH, Math.max(0, v));
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void registerGoals() {
		try {
			this.goalSelector.addGoal(0, new FloatGoal(this));
			this.goalSelector.addGoal(1, new FlameSpitGoal(this));
			this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4, false));
			this.goalSelector.addGoal(3, new LavaRestGoal(this));
			this.goalSelector.addGoal(4, new TreePlayGoal(this));
			this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
			this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
			this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		} catch (Exception ignored) {
		}
		try {
			// 평소엔 플레이어를 노리지 않음: 휴식처 수호 타겟은 tick에서 동적 지정
			this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
			this.targetSelector.addGoal(2, new HomeGuardTargetGoal(this));
		} catch (Exception ignored) {
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		SpawnGroupData r = null;
		try {
			r = super.finalizeSpawn(level, difficulty, reason, data);
		} catch (Exception ignored) {
		}
		try {
			setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModFlame.TRANSCEND_WAND));
		} catch (Exception ignored) {
		}
		try {
			findHomeLava();
		} catch (Exception ignored) {
		}
		return r;
	}

	/** 근처 용암을 집으로 삼는다 (없으면 현재 위치를 집으로). */
	public void findHomeLava() {
		try {
			if (!(level() instanceof ServerLevel sl)) {
				return;
			}
			BlockPos center = blockPosition();
			BlockPos best = null;
			double bestD = HOME_RADIUS * HOME_RADIUS;
			int r = (int) HOME_RADIUS;
			for (int dx = -r; dx <= r; dx += 2) {
				for (int dy = -6; dy <= 6; dy += 2) {
					for (int dz = -r; dz <= r; dz += 2) {
						try {
							BlockPos p = center.offset(dx, dy, dz);
							boolean lava = false;
							try {
								lava = sl.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA);
							} catch (Exception ignored) {
							}
							if (!lava) {
								try {
									lava = sl.getBlockState(p).is(Blocks.LAVA);
								} catch (Exception ignored) {
								}
							}
							if (lava) {
								double d = p.distSqr(center);
								if (d < bestD) {
									bestD = d;
									best = p.immutable();
								}
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
			if (best != null) {
				homeLava = best;
			} else if (homeLava == null) {
				homeLava = center.immutable();
			}
		} catch (Exception ignored) {
		}
	}

	public BlockPos getHomeLava() {
		return homeLava;
	}

	/** 휴식처 수호 모드: 집 용암 10m 안에 생존 플레이어가 있으면 true. */
	public boolean isGuarding() {
		try {
			if (homeLava == null) {
				return false;
			}
			if (!(level() instanceof ServerLevel sl)) {
				return false;
			}
			AABB box = new AABB(homeLava).inflate(GUARD_RADIUS);
			for (Player p : sl.players()) {
				try {
					if (!p.isAlive() || p.isSpectator() || p.isCreative()) {
						continue;
					}
					if (box.contains(p.position())) {
						return true;
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/** 입에서 불: 한 번 쏘면 3초간 길게 유지 (tick에서 방출). 수호 중엔 대상 추적. */
	public void breatheFire(Vec3 dir) {
		try {
			if (dir == null || dir.lengthSqr() < 1e-6) {
				try {
					dir = getLookAngle();
				} catch (Exception e) {
					return;
				}
			}
			breathDir = dir.normalize();
			breathTicks = BREATH_DURATION;
			setMouthOpen(BREATH_DURATION + 10);
			try {
				if (level() instanceof ServerLevel sl) {
					sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 1.2f);
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/** 지속 브레스 1회 방출: 전방 원뿔 피해+착화+파티클+주변 착화. */
	private void emitBreathTick() {
		try {
			if (!(level() instanceof ServerLevel sl)) {
				return;
			}
			Vec3 n = breathDir;
			if (n == null || n.lengthSqr() < 1e-6) {
				return;
			}
			Vec3 mouth;
			try {
				mouth = getEyePosition().add(n.scale(0.6));
			} catch (Exception e) {
				return;
			}
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
						mouth.x, mouth.y, mouth.z, 14, n.x * 0.4, n.y * 0.4 + 0.2, n.z * 0.4, 0.08);
			} catch (Exception ignored) {
			}
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
						mouth.x, mouth.y, mouth.z, 4, 0.2, 0.2, 0.2, 0.0);
			} catch (Exception ignored) {
			}
			// 전방 원뿔 안 LivingEntity에게 화염 피해+착화 (자신 제외)
			try {
				AABB sweep = getBoundingBox().inflate(BREATH_RANGE);
				for (Entity e : sl.getEntities(this, sweep, x -> x instanceof LivingEntity living && living.isAlive())) {
					try {
						if (e == this) {
							continue;
						}
						Vec3 to = new Vec3(e.getX() - mouth.x, (e.getY() + e.getBbHeight() * 0.5) - mouth.y,
								e.getZ() - mouth.z);
						double t = to.dot(n);
						if (t < 0 || t > BREATH_RANGE) {
							continue;
						}
						Vec3 closest = mouth.add(n.scale(t));
						Vec3 center = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
						if (closest.distanceTo(center) > e.getBbWidth() + 1.2) {
							continue;
						}
						LivingEntity le = (LivingEntity) e;
						try {
							le.hurtServer(sl, sl.damageSources().inFire(), BREATH_DAMAGE);
						} catch (Exception ignored) {
						}
						try {
							le.setRemainingFireTicks(Math.max(le.getRemainingFireTicks(), 100));
						} catch (Exception ignored) {
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			// 주변 착화: 반경 4m 무작위 지점에 불 붙이기
			try {
				for (int i = 0; i < 6; i++) {
					try {
						if (random.nextFloat() > 0.4f) {
							continue;
						}
						double ox = (random.nextDouble() - 0.5) * 2 * IGNITE_RADIUS;
						double oz = (random.nextDouble() - 0.5) * 2 * IGNITE_RADIUS;
						BlockPos fp = BlockPos.containing(getX() + ox + n.x * 2, getY() + 1, getZ() + oz + n.z * 2);
						if (sl.getBlockState(fp).isAir() && sl.getBlockState(fp.below()).isSolidRender()) {
							sl.setBlockAndUpdate(fp, Blocks.FIRE.defaultBlockState());
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/** 맞으면 즉시 반격: 공격자를 타겟으로 + 긴 브레스. */
	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
		try {
			Entity attacker = null;
			try {
				attacker = source.getEntity();
			} catch (Exception ignored) {
			}
			if (attacker instanceof LivingEntity living && living.isAlive()) {
				try {
					boolean skip = false;
					try {
						if (living instanceof Player p && (p.isCreative() || p.isSpectator())) {
							skip = true;
						}
					} catch (Exception ignored) {
					}
					if (!skip) {
						setTarget(living);
						Vec3 dir = new Vec3(living.getX() - getX(), (living.getY() + 1) - getEyeY(),
								living.getZ() - getZ());
						breatheFire(dir);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			return super.hurtServer(level, source, amount);
		} catch (Exception e) {
			return false;
		}
	}

	/** 초월의 막대: 지팡이를 겨눈 대상(시선 16m) 이동속도 저하. 쿨 10초. */
	public boolean waveWand(LivingEntity target) {
		try {
			if (getWandCooldown() > 0) {
				return false;
			}
			if (target == null || !target.isAlive()) {
				return false;
			}
			if (!(level() instanceof ServerLevel sl)) {
				return false;
			}
			try {
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WAND_SLOW_TICKS, 1, false, true, true));
			} catch (Exception ignored) {
				return false;
			}
			setWandCooldown(WAND_COOLDOWN_TICKS);
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
						target.getX(), target.getY() + 1, target.getZ(), 16, 0.4, 0.8, 0.4, 0.1);
			} catch (Exception ignored) {
			}
			try {
				sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.4f);
			} catch (Exception ignored) {
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void tick() {
		try {
			super.tick();
		} catch (Exception e) {
			return;
		}
		try {
			if (getWandCooldown() > 0) {
				setWandCooldown(getWandCooldown() - 1);
			}
		} catch (Exception ignored) {
		}
		try {
			if (getMouthOpen() > 0) {
				setMouthOpen(getMouthOpen() - 1);
			}
		} catch (Exception ignored) {
		}
		if (level().isClientSide()) {
			return;
		}
		// 지속 브레스 방출 (3틱마다, 대상 있으면 방향 추적)
		try {
			if (breathTicks > 0) {
				breathTicks--;
				try {
					LivingEntity t = getTarget();
					if (t != null && t.isAlive()) {
						breathDir = new Vec3(t.getX() - getX(), (t.getY() + 1) - getEyeY(), t.getZ() - getZ())
								.normalize();
					}
				} catch (Exception ignored) {
				}
				if (breathTicks % 3 == 0) {
					emitBreathTick();
				}
				if (breathTicks <= 0) {
					setMouthOpen(0);
				}
			}
		} catch (Exception ignored) {
		}
		// 용암 면역 유지 + 오른손 지팡이 고정
		try {
			clearFire();
		} catch (Exception ignored) {
		}
		try {
			ItemStack hand = getMainHandItem();
			if (hand.isEmpty() || hand.getItem() != ModFlame.TRANSCEND_WAND) {
				setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModFlame.TRANSCEND_WAND));
			}
		} catch (Exception ignored) {
		}
		// 집 스캔 (10초마다)
		try {
			if (homeScanTick++ > 200) {
				homeScanTick = 0;
				if (homeLava == null) {
					findHomeLava();
				}
			}
		} catch (Exception ignored) {
		}
		// 나무 오르기: 통나무/잎 근처에서 벽에 막히면 위로
		try {
			if (horizontalCollision) {
				boolean nearWood = false;
				try {
					BlockPos p = blockPosition();
					for (BlockPos q : BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 3, 2))) {
						try {
							var st = level().getBlockState(q);
							if (st.is(net.minecraft.tags.BlockTags.LOGS) || st.is(net.minecraft.tags.BlockTags.LEAVES)) {
								nearWood = true;
								break;
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
				if (nearWood) {
					try {
						setDeltaMovement(getDeltaMovement().add(0, 0.45, 0));
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		// 용암 안에서는 둥둥 떠다니며 휴식
		try {
			if (isInLava()) {
				try {
					Vec3 v = getDeltaMovement();
					if (v.y < 0.05) {
						setDeltaMovement(v.x * 0.9, 0.08, v.z * 0.9);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		try {
			super.addAdditionalSaveData(out);
		} catch (Exception ignored) {
		}
		try {
			if (homeLava != null) {
				out.putInt("FlameHomeX", homeLava.getX());
				out.putInt("FlameHomeY", homeLava.getY());
				out.putInt("FlameHomeZ", homeLava.getZ());
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		try {
			super.readAdditionalSaveData(in);
		} catch (Exception ignored) {
		}
		try {
			int x = in.getIntOr("FlameHomeX", Integer.MIN_VALUE);
			if (x != Integer.MIN_VALUE) {
				homeLava = new BlockPos(x, in.getIntOr("FlameHomeY", 64), in.getIntOr("FlameHomeZ", 0));
			}
		} catch (Exception ignored) {
		}
	}

	// ---------- AI 골 ----------

	/** 평소 장난: 5~9초마다 주변 랜덤 방향으로 불 한 번. 수호 중엔 공격 대상에게. */
	static class FlameSpitGoal extends Goal {
		private final FlameMongEntity mong;

		FlameSpitGoal(FlameMongEntity mong) {
			this.mong = mong;
			try {
				setFlags(EnumSet.of(Flag.LOOK));
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canUse() {
			try {
				return !mong.level().isClientSide() && mong.getTarget() == null && mong.tickCount - mong.spitTick > 100 + mong.getRandom().nextInt(80);
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public void start() {
			try {
				mong.spitTick = mong.tickCount;
				Vec3 dir;
				try {
					if (mong.isGuarding() && mong.getTarget() != null) {
						LivingEntity t = mong.getTarget();
						dir = new Vec3(t.getX() - mong.getX(), (t.getY() + 1) - mong.getEyeY(), t.getZ() - mong.getZ());
					} else {
						double a = mong.getRandom().nextDouble() * Math.PI * 2;
						dir = new Vec3(Math.cos(a), 0.1, Math.sin(a));
					}
				} catch (Exception e) {
					return;
				}
				mong.breatheFire(dir);
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}
	}

	/** 용암 휴식: 집 용암이 있고 타겟이 없으면 용암 속으로 이동. */
	static class LavaRestGoal extends Goal {
		private final FlameMongEntity mong;

		LavaRestGoal(FlameMongEntity mong) {
			this.mong = mong;
			try {
				setFlags(EnumSet.of(Flag.MOVE));
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canUse() {
			try {
				if (mong.level().isClientSide() || mong.getTarget() != null) {
					return false;
				}
				if (mong.homeLava == null || mong.isInLava()) {
					return false;
				}
				return mong.tickCount % 60 == 0 && mong.getRandom().nextFloat() < 0.5f;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public void start() {
			try {
				mong.getNavigation().moveTo(mong.homeLava.getX() + 0.5, mong.homeLava.getY(), mong.homeLava.getZ() + 0.5, 1.2);
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canContinueToUse() {
			try {
				return !mong.isInLava() && mong.getTarget() == null && !mong.getNavigation().isDone();
			} catch (Exception e) {
				return false;
			}
		}
	}

	/** 나무 놀이: 근처 통나무로 이동 (오르기는 tick의 벽登반 처리). */
	static class TreePlayGoal extends Goal {
		private final FlameMongEntity mong;
		private BlockPos tree = null;

		TreePlayGoal(FlameMongEntity mong) {
			this.mong = mong;
			try {
				setFlags(EnumSet.of(Flag.MOVE));
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canUse() {
			try {
				if (mong.level().isClientSide() || mong.getTarget() != null || mong.isInLava()) {
					return false;
				}
				if (mong.tickCount - mong.playTick < 200 + mong.getRandom().nextInt(200)) {
					return false;
				}
				BlockPos center = mong.blockPosition();
				BlockPos best = null;
				double bestD = 256.0;
				for (BlockPos q : BlockPos.betweenClosed(center.offset(-16, -4, -16), center.offset(16, 8, 16))) {
					try {
						if (mong.level().getBlockState(q).is(net.minecraft.tags.BlockTags.LOGS)) {
							double d = q.distSqr(center);
							if (d > 4 && d < bestD) {
								bestD = d;
								best = q.immutable();
							}
						}
					} catch (Exception ignored) {
					}
				}
				if (best == null) {
					return false;
				}
				tree = best;
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public void start() {
			try {
				mong.playTick = mong.tickCount;
				if (tree != null) {
					mong.getNavigation().moveTo(tree.getX() + 0.5, tree.getY(), tree.getZ() + 0.5, 1.3);
				}
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean canContinueToUse() {
			try {
				return tree != null && mong.getTarget() == null && !mong.getNavigation().isDone();
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public void stop() {
			tree = null;
		}
	}

	/** 휴식처 수호: 집 용암 근처 플레이어를 공격 대상으로. */
	static class HomeGuardTargetGoal extends NearestAttackableTargetGoal<Player> {
		private final FlameMongEntity mong;

		HomeGuardTargetGoal(FlameMongEntity mong) {
			super(mong, Player.class, true);
			this.mong = mong;
		}

		@Override
		public boolean canUse() {
			try {
				if (!mong.isGuarding()) {
					return false;
				}
				return super.canUse();
			} catch (Exception e) {
				return false;
			}
		}
	}

	public static boolean isFlameMong(Entity e) {
		try {
			return e != null && e.getType() == ModFlame.FLAME_MONG;
		} catch (Exception ex) {
			return false;
		}
	}
}
