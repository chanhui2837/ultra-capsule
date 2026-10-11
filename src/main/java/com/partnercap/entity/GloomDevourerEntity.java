package com.partnercap.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * 먹보 도깨비: 느리지만 보이는 엔티티를 잡아먹음 (잡기→씹기→삼키기).
 * 먹힌 비플레이어는 즉시 소멸. 플레이어는 큰 피해 후 놓아줌.
 * 동족 도깨비와 아군 소울은 안 먹음.
 */
public class GloomDevourerEntity extends Monster {

	public static final int PHASE_IDLE = 0;
	public static final int PHASE_GRAB = 1;
	public static final int PHASE_CHEW = 2;
	public static final int PHASE_SWALLOW = 3;

	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GloomDevourerEntity.class,
			EntityDataSerializers.INT);

	private int phaseMirror = PHASE_IDLE;
	private int nameTick = 0;
	public LivingEntity devourTarget;

	public GloomDevourerEntity(Level level) {
		this(ModEntityTypes.GLOOM_DEVOURER, level);
	}

	public GloomDevourerEntity(EntityType<? extends GloomDevourerEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PHASE, PHASE_IDLE);
	}

	public int getPhase() {
		try {
			int v = entityData.get(PHASE);
			phaseMirror = v;
			return v;
		} catch (Exception e) {
			return phaseMirror;
		}
	}

	public void setPhase(int phase) {
		phaseMirror = phase;
		try {
			entityData.set(PHASE, phase);
		} catch (Exception ignored) {
		}
	}

	@Override
	public void tick() {
		super.tick();
		// TEMP 진단용 상태 표시 (원인 확정 후 제거)
		if (!level().isClientSide() && tickCount % 10 == 0) {
			try {
				setCustomNameVisible(true);
				String st = switch (getPhase()) {
					case PHASE_GRAB -> "잡기";
					case PHASE_CHEW -> "씹기";
					case PHASE_SWALLOW -> "삼키기";
					default -> "탐색중";
				};
				setCustomName(net.minecraft.network.chat.Component.literal("[" + st + devourTargetSuffix() + "]"));
			} catch (Exception ignored) {
			}
		}
	}

	private String devourTargetSuffix() {
		try {
			if (devourTarget != null && devourTarget.isAlive()) {
				String n = devourTarget.getDisplayName().getString();
				if (n.length() > 10) {
					n = n.substring(0, 10);
				}
				return ":" + n;
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new DevourGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public static class DevourGoal extends Goal {
		private final GloomDevourerEntity mob;
		private LivingEntity victim;
		private int timer;
		private int cooldown;

		public DevourGoal(GloomDevourerEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		private boolean edible(LivingEntity e) {
			if (e == null || !e.isAlive() || e == mob) {
				return false;
			}
			try {
				if (e.isSpectator()) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (e instanceof net.minecraft.world.entity.player.Player p) {
					try {
						if (p.isCreative()) {
							return false;
						}
					} catch (Exception ignored) {
					}
					try {
						if (p.getAbilities() != null && p.getAbilities().instabuild) {
							return false;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				if (e instanceof GloomDevourerEntity) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (e instanceof com.partnercap.medusa.StatueEntity) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (com.partnercap.medusa.Petrify.isPetrified(e)) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				for (String tag : e.entityTags()) {
					if (tag.startsWith(com.partnercap.soul.SoulAlly.TAG_PREFIX)) {
						return false;
					}
				}
			} catch (Exception ignored) {
			}
			try {
				java.util.UUID owner = com.partnercap.soul.SoulAlly.soulOwnerOf(mob);
				if (owner != null && e.getUUID().equals(owner)) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (!mob.hasLineOfSight(e)) {
					return false;
				}
			} catch (Exception ignored) {
			}
			return true;
		}

		private boolean inFront(LivingEntity target) {
			try {
				Vec3 look = mob.getLookAngle();
				double lookLen = Math.sqrt(look.x * look.x + look.z * look.z);
				Vec3 flat = lookLen < 1e-4 ? new Vec3(0, 0, 1)
						: new Vec3(look.x / lookLen, 0, look.z / lookLen);
				Vec3 dir = new Vec3(target.getX() - mob.getX(), 0, target.getZ() - mob.getZ());
				double len = dir.length();
				if (len < 1e-4) {
					return true;
				}
				dir = dir.scale(1.0 / len);
				return flat.dot(dir) > 0.5;
			} catch (Exception e) {
				return true;
			}
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			Level level;
			try {
				level = mob.level();
			} catch (Exception e) {
				return false;
			}
			if (!(level instanceof ServerLevel)) {
				return false;
			}
			net.minecraft.world.phys.AABB box;
			try {
				box = mob.getBoundingBox().inflate(10.0);
			} catch (Exception e) {
				return false;
			}
			List<Entity> found;
			try {
				found = new ArrayList<>(level.getEntities(mob, mob.getBoundingBox().inflate(24.0), e -> e instanceof LivingEntity));
			} catch (Exception e) {
				return false;
			}
			LivingEntity best = null;
			double bestD = 576.0;
			for (Entity e : found) {
				LivingEntity le;
				try {
					le = (LivingEntity) e;
				} catch (Exception ex) {
					continue;
				}
				double d;
				try {
					d = mob.distanceToSqr(le);
				} catch (Exception ex) {
					continue;
				}
				if (d >= bestD) {
					continue;
				}
				boolean ok;
				try {
					ok = edible(le);
				} catch (Exception ex) {
					continue;
				}
				if (!ok) {
					continue;
				}
				if (!inFront(le)) {
					continue;
				}
				best = le;
				bestD = d;
			}
			if (best == null) {
				return false;
			}
			victim = best;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			if (victim == null) {
				return false;
			}
			boolean alive = false;
			try {
				alive = victim.isAlive() || victim instanceof Player;
			} catch (Exception ignored) {
			}
			if (!alive) {
				return false;
			}
			try {
				if (mob.distanceToSqr(victim) > 2500.0) {
					return false;
				}
			} catch (Exception ignored) {
			}
			return true;
		}

		@Override
		public void start() {
			mob.setPhase(PHASE_GRAB);
			mob.devourTarget = victim;
			timer = 8;
			try {
				mob.getNavigation().stop();
				mob.setTarget(victim);
			} catch (Exception ignored) {
			}
			try {
				mob.level().playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT.value(),
						SoundSource.HOSTILE, 1.0f, 0.5f);
			} catch (Exception ignored) {
			}
			super.start();
		}

		@Override
		public void stop() {
			mob.setPhase(PHASE_IDLE);
			mob.devourTarget = null;
			victim = null;
			try {
				mob.setTarget(null);
			} catch (Exception ignored) {
			}
			super.stop();
		}

		@Override
		public void tick() {
			super.tick();
			if (victim == null) {
				mob.setPhase(PHASE_IDLE);
				return;
			}
			boolean isServer = mob.level() instanceof ServerLevel;
			if (!isServer) {
				return;
			}
			try {
				mob.getLookControl().setLookAt(victim, 30.0F, 30.0F);
			} catch (Exception ignored) {
			}
			if (mob.distanceToSqr(victim) > 12.0 && mob.getPhase() == PHASE_GRAB) {
				try {
					mob.getNavigation().moveTo(victim, 1.25);
				} catch (Exception ignored) {
				}
				return;
			}
			timer--;
			int phase = mob.getPhase();
			if (phase == PHASE_GRAB) {
				try {
					mob.getNavigation().stop();
				} catch (Exception ignored) {
				}
				if (timer <= 0) {
					boolean close = false;
					try {
						close = mob.distanceToSqr(victim) <= 9.0;
					} catch (Exception ignored) {
					}
					if (!close) {
						cooldown = 20;
						mob.setPhase(PHASE_IDLE);
						victim = null;
						stop();
						return;
					}
					mob.setPhase(PHASE_CHEW);
					timer = 16;
				}
			} else if (phase == PHASE_CHEW) {
				if (timer % 8 == 0) {
					try {
						mob.level().playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT.value(),
								SoundSource.HOSTILE, 1.0f, 0.7f);
					} catch (Exception ignored) {
					}
					try {
						if (mob.level() instanceof ServerLevel sl) {
							victim.hurtServer(sl, mob.level().damageSources().mobAttack(mob), 2.0F);
						}
					} catch (Exception ignored) {
					}
				}
				if (timer <= 0) {
					mob.setPhase(PHASE_SWALLOW);
					timer = 8;
					if (victim instanceof Player p) {
						try {
							if (mob.level() instanceof ServerLevel sl) {
								p.hurtServer(sl, mob.level().damageSources().mobAttack(mob), 10.0F);
							}
						} catch (Exception ignored) {
						}
						cooldown = 40;
					} else {
						try {
							if (mob.level() instanceof ServerLevel sl) {
								java.util.UUID eater = null;
								try {
									eater = mob.getUUID();
								} catch (Exception ignored) {
								}
								com.partnercap.voidcap.VoidErase.devourRewind(sl, victim, eater);
							}
						} catch (Exception ignored) {
						}
						try {
							if (mob.level() instanceof ServerLevel sl) {
								com.partnercap.voidcap.VoidErase.noteDeath(sl, victim, mob.getUUID());
							}
						} catch (Exception ignored) {
						}
						boolean wasAlive = false;
						try {
							wasAlive = victim.isAlive();
						} catch (Exception ignored) {
						}
						try {
							victim.discard();
						} catch (Exception ignored) {
						}
						if (wasAlive) {
							try {
								mob.heal(6.0F);
							} catch (Exception ignored) {
							}
						}
						cooldown = 20;
					}
					try {
						mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_BURP,
								SoundSource.HOSTILE, 1.0f, 0.8f);
					} catch (Exception ignored) {
					}
				}
			} else if (phase == PHASE_SWALLOW) {
				if (timer <= 0) {
					mob.setPhase(PHASE_IDLE);
					victim = null;
				}
			}
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData data) {
		try {
			return super.finalizeSpawn(level, difficulty, reason, data);
		} catch (Exception e) {
			return data;
		}
	}
}
