package com.ultracapsuleadventure.medusa;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 메두사 (적대). 시선 석화 + 근접 독 공격.
 * - 매 틱 가장 가까운 생물을 응시 (몸ごと 회전)
 * - 서로 마주보면 즉시 석화 (눈 섬광 + 석상)
 * - 근접은 독 공격
 */
public class MedusaEntity extends net.minecraft.world.entity.monster.Monster {

	private static final EntityDataAccessor<Integer> GAZE = SynchedEntityData.defineId(MedusaEntity.class,
			EntityDataSerializers.INT);

	public MedusaEntity(EntityType<? extends MedusaEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return net.minecraft.world.entity.monster.Monster.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FOLLOW_RANGE, 28.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GAZE, 0);
	}

	public int getGazeFlash() {
		try {
			return Math.max(0, entityData.get(GAZE));
		} catch (Exception e) {
			return 0;
		}
	}

	/** 응시 고정 타이머: 키=메두사UUID>피해자UUID, 값=주시 시작 시각(ms). 0.25초 유지 시 석화 */
	private static final java.util.Map<String, Long> GAZE_LOCK = new java.util.HashMap<>();
	private static final long GAZE_HOLD_MS = 250L;

	private void setGazeFlash(int v) {
		try {
			entityData.set(GAZE, Math.max(0, v));
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void registerGoals() {
		try {
			this.goalSelector.addGoal(0, new FloatGoal(this));
			this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
			this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6));
			this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
			this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		} catch (Exception ignored) {
		}
		try {
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
			this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
			this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean r = false;
		try {
			r = super.doHurtTarget(level, target);
		} catch (Exception ignored) {
		}
		try {
			if (r && target instanceof LivingEntity living && living.isAlive()) {
				living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
			}
		} catch (Exception ignored) {
		}
		return r;
	}

	@Override
	public void tick() {
		try {
			super.tick();
		} catch (Exception e) {
			return;
		}
		// 석화된 메두사는 동결: 응시·석화 판정 자체를 돌리지 않는다
		try {
			if (this instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
				return;
			}
		} catch (Exception ignored) {
		}
		if (level().isClientSide()) {
			return;
		}
		try {
			int g = getGazeFlash();
			if (g > 0) {
				setGazeFlash(g - 1);
			}
		} catch (Exception ignored) {
		}
		if (tickCount % 3 != 0) {
			return;
		}
		try {
			if (!(level() instanceof ServerLevel sl)) {
				return;
			}
			// 아군 소울 여부 (주인 UUID)
			java.util.UUID medusaOwner = null;
			try {
				medusaOwner = com.ultracapsuleadventure.soul.SoulAlly.soulOwnerOf(this);
			} catch (Exception ignored) {
			}
			// 가장 가까운 생물을 응시 (아군 소울 메두사는 주인·동료를 제외한 적만 대상)
			LivingEntity nearest = null;
			double best = Petrify.GAZE_RANGE * Petrify.GAZE_RANGE;
			AABB box = getBoundingBox().inflate(Petrify.GAZE_RANGE);
			List<LivingEntity> found = new ArrayList<>();
			try {
				for (Entity e : sl.getEntities(this, box, x -> x instanceof LivingEntity)) {
					LivingEntity le = (LivingEntity) e;
					try {
						if (!le.isAlive() || le == this) {
							continue;
						}
						if (le instanceof StatueEntity) {
							continue;
						}
						if (le.isSpectator()) {
							continue;
						}
						if (le instanceof Player p) {
							try {
								if (p.isCreative()) {
									continue;
								}
							} catch (Exception ignored) {
							}
						}
						found.add(le);
						boolean friend = false;
						try {
							friend = medusaOwner != null && isSoulFriend(medusaOwner, le);
						} catch (Exception ignored) {
						}
						double d = distanceToSqr(le);
						if (!friend && d < best) {
							best = d;
							nearest = le;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			// 아군 소울은 공격 대상이 있을 때만 적을 향해 몸을 돌린다 (그 외엔 이동 방향 그대로 → 뒤로 걷는 문제 방지)
			boolean allyIdle = false;
			try {
				allyIdle = medusaOwner != null && getTarget() == null;
			} catch (Exception ignored) {
			}
			if (nearest != null && !allyIdle) {
				try {
					double dx = nearest.getX() - getX();
					double dz = nearest.getZ() - getZ();
					float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
					setYRot(yaw);
					yHeadRot = yaw;
					yHeadRotO = yaw;
				} catch (Exception ignored) {
				}
			}
			// 서로 마주보면 즉시 석화 (단, 아군 소울 메두사는 주인 일행을 석화하지 않음)
			long nowMs = 0L;
			try {
				nowMs = System.currentTimeMillis();
			} catch (Exception ignored) {
			}
			String meId = "";
			try {
				meId = getUUID().toString();
			} catch (Exception ignored) {
			}
			for (LivingEntity le : found) {
				boolean mutual = false;
				try {
					mutual = Petrify.mutualGaze(this, le);
				} catch (Exception ignored) {
				}
				String lockKey = null;
				try {
					lockKey = meId + ">" + le.getUUID().toString();
				} catch (Exception ignored) {
				}
				if (!mutual) {
					try {
						if (lockKey != null) {
							synchronized (GAZE_LOCK) {
								GAZE_LOCK.remove(lockKey);
							}
						}
					} catch (Exception ignored) {
					}
					continue;
				}
				try {
					if (medusaOwner != null && isSoulFriend(medusaOwner, le)) {
						try {
							if (lockKey != null) {
								synchronized (GAZE_LOCK) {
									GAZE_LOCK.remove(lockKey);
								}
							}
						} catch (Exception ignored) {
						}
						continue;
					}
				} catch (Exception ignored) {
				}
				// 고정 중에는 눈 섬광 유지 (경고 표시)
				try {
					setGazeFlash(10);
				} catch (Exception ignored) {
				}
				boolean held = false;
				try {
					if (lockKey != null) {
						synchronized (GAZE_LOCK) {
							Long start = GAZE_LOCK.get(lockKey);
							if (start == null) {
								if (GAZE_LOCK.size() > 200) {
									GAZE_LOCK.clear();
								}
								GAZE_LOCK.put(lockKey, nowMs);
							} else if (nowMs - start >= GAZE_HOLD_MS) {
								held = true;
								GAZE_LOCK.remove(lockKey);
							}
						}
					}
				} catch (Exception ignored) {
				}
				if (!held) {
					continue;
				}
				try {
					setGazeFlash(20);
				} catch (Exception ignored) {
				}
				try {
					sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
							getX(), getY() + 1.6, getZ(), 10, 0.3, 0.2, 0.3, 0.05);
					sl.playSound(null, blockPosition(),
							net.minecraft.sounds.SoundEvents.ENDERMAN_STARE,
							SoundSource.HOSTILE, 1.0f, 1.2f);
				} catch (Exception ignored) {
				}
				try {
					if (le instanceof ServerPlayer sp) {
						Petrify.petrifyPlayer(sl, sp, this);
					} else {
						Petrify.petrify(sl, le, this);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static boolean isMedusa(Entity e) {
		try {
			return e != null && e.getType() == com.ultracapsuleadventure.entity.ModEntityTypes.MEDUSA;
		} catch (Exception ex) {
			return false;
		}
	}

	/** 아군 소울 메두사의 응시에서 제외할 대상: 주인·동료 소울·주인의 길들인 몹 */
	private boolean isSoulFriend(java.util.UUID medusaOwner, LivingEntity le) {
		try {
			if (le == null || medusaOwner == null) {
				return false;
			}
			try {
				if (le.getUUID().equals(medusaOwner)) {
					return true;
				}
			} catch (Exception ignored) {
			}
			try {
				java.util.UUID so = com.ultracapsuleadventure.soul.SoulAlly.soulOwnerOf(le);
				if (medusaOwner.equals(so)) {
					return true;
				}
			} catch (Exception ignored) {
			}
			try {
				if (level() instanceof ServerLevel sl) {
					for (ServerPlayer p : sl.players()) {
						try {
							if (p.getUUID().equals(medusaOwner)
									&& com.ultracapsuleadventure.util.TameUtil.isOwnedBy(le, p)) {
								return true;
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
		return false;
	}
}


