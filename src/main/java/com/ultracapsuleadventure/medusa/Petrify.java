package com.ultracapsuleadventure.medusa;

import com.ultracapsuleadventure.entity.ModEntityTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 석화 실행기. 스냅샷(위치·방향·자세) → 석상 스폰 → 원본 제거.
 * 석상은 각 Mob 렌더러의 모델을 돌 텍스처로 고정 렌더한다.
 */
public class Petrify {

	/** 시선 판정용 코사인 (약 31도) */
	public static final double GAZE_COS = 0.86;
	public static final double GAZE_RANGE = 16.0;

	private static final java.util.Map<java.util.UUID, java.util.UUID> PETRIFIER = new java.util.HashMap<>();

	/** 누가 누구를 석화시켰는지 기록 (가해자 삭제 시 해제용) */
	public static void recordPetrifier(java.util.UUID victim, java.util.UUID petrifier) {
		if (victim == null || petrifier == null) {
			return;
		}
		try {
			synchronized (PETRIFIER) {
				PETRIFIER.put(victim, petrifier);
				while (PETRIFIER.size() > 200) {
					try {
						PETRIFIER.remove(PETRIFIER.keySet().iterator().next());
					} catch (Exception ex) {
						break;
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 먹힌 자의 석화 기록을 먹보에게 승계 (먹보 삭제 시 같이 해제되도록) */
	public static void transferPetrifier(java.util.UUID oldId, java.util.UUID newId) {
		if (oldId == null || newId == null || oldId.equals(newId)) {
			return;
		}
		try {
			synchronized (PETRIFIER) {
				for (var en : new java.util.ArrayList<>(PETRIFIER.entrySet())) {
					try {
						if (oldId.equals(en.getValue())) {
							PETRIFIER.put(en.getKey(), newId);
						}
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 가해자가 존재 삭제되면 그 가해자가 석화시킨 애들 해제. 해제 수 반환 */
	public static int releaseByErased(net.minecraft.server.MinecraftServer server, java.util.UUID erasedId) {
		int n = 0;
		java.util.List<java.util.UUID> vids = new java.util.ArrayList<>();
		synchronized (PETRIFIER) {
			for (var en : PETRIFIER.entrySet()) {
				try {
					if (erasedId.equals(en.getValue())) {
						vids.add(en.getKey());
					}
				} catch (Exception ignored) {
				}
			}
			for (java.util.UUID v : vids) {
				PETRIFIER.remove(v);
			}
		}
		for (java.util.UUID vid : vids) {
			try {
				net.minecraft.world.entity.Entity e = null;
				try {
					for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
						try {
							for (Entity ent : level.getAllEntities()) {
								if (ent.getUUID().equals(vid)) {
									e = ent;
									break;
								}
							}
						} catch (Exception ignored) {
						}
						if (e != null) {
							break;
						}
					}
				} catch (Exception ignored) {
				}
				if (e instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
					access.UltraCapsuleAdventure$setPetrified(false);
					try {
						if (e instanceof LivingEntity living) {
							living.hurtTime = 0;
							living.deathTime = 0;
						}
					} catch (Exception ignored) {
					}
					n++;
				}
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	public static boolean isStatue(Entity e) {
		return e instanceof StatueEntity;
	}

	public static boolean canPetrify(Entity e) {
		try {
			if (!(e instanceof LivingEntity living)) {
				return false;
			}
			if (e instanceof EnderDragon) {
				return false;
			}
			if (!living.isAlive() || living.isRemoved()) {
				return false;
			}
			if (e instanceof StatueEntity) {
				return false;
			}
			if (e.isSpectator()) {
				return false;
			}
			if (e instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
				return false;
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean isPetrified(Entity e) {
		try {
			return e instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified();
		} catch (Exception ex) {
			return false;
		}
	}

	/** 관찰자가 목표 지점을 바라보는지 */
	public static boolean lookingAt(LivingEntity observer, Vec3 targetPos, double cosMin) {
		try {
			Vec3 eye = observer.getEyePosition();
			Vec3 dir = observer.getLookAngle();
			Vec3 to = new Vec3(targetPos.x - eye.x, targetPos.y - eye.y, targetPos.z - eye.z);
			double len = to.length();
			if (len < 1e-4) {
				return true;
			}
			return dir.dot(to.scale(1.0 / len)) > cosMin;
		} catch (Exception e) {
			return false;
		}
	}

	/** 서로 마주봄: 메두사→대상, 대상→메두사 모두 */
	public static boolean mutualGaze(MedusaEntity medusa, LivingEntity victim) {
		try {
			if (victim instanceof EnderDragon) {
				if (!victim.isAlive() || victim.isRemoved()) {
					return false;
				}
				if (medusa instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
					return false;
				}
				Vec3 mEye = medusa.getEyePosition();
				Vec3 vEye = victim.getEyePosition();
				if (mEye.distanceTo(vEye) > GAZE_RANGE) {
					return false;
				}
				boolean mLook = lookingAt(medusa, vEye, GAZE_COS);
				boolean vLook = lookingAt(victim, mEye, GAZE_COS);
				if (!mLook || !vLook) {
					return false;
				}
				try {
					if (!medusa.hasLineOfSight(victim)) {
						return false;
					}
				} catch (Exception ignored) {
				}
				return true;
			}
			if (!canPetrify(victim)) {
				return false;
			}
			try {
				if (BlindfoldItem.protects(victim)) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (medusa instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
					return false;
				}
			} catch (Exception ignored) {
			}
			Vec3 mEye = medusa.getEyePosition();
			Vec3 vEye = victim.getEyePosition();
			if (mEye.distanceTo(vEye) > GAZE_RANGE) {
				return false;
			}
			boolean mLook = false;
			boolean vLook = false;
			try {
				mLook = lookingAt(medusa, vEye, GAZE_COS);
			} catch (Exception ignored) {
			}
			try {
				vLook = lookingAt(victim, mEye, GAZE_COS);
			} catch (Exception ignored) {
			}
			if (!mLook || !vLook) {
				return false;
			}
			try {
				if (!medusa.hasLineOfSight(victim)) {
					return false;
				}
			} catch (Exception ignored) {
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 엔더드래곤 전용: 일반 석화 대신 체력 피해를 입힌다.
	 * 마지막 타격이 메두사 시선/머리라면, 그때만 돌 덮개 처형을 수행한다.
	 */
	public static boolean applyMedusaDragonDamage(ServerLevel level, LivingEntity victim, MedusaEntity medusa,
			boolean fromGaze, boolean fromHead) {
		return applyMedusaDragonDamage(level, victim, medusa, null, fromGaze, fromHead);
	}

	public static boolean applyMedusaDragonDamage(ServerLevel level, LivingEntity victim, MedusaEntity medusa,
			Player attacker, boolean fromGaze, boolean fromHead) {
		try {
			if (!(victim instanceof EnderDragon dragon)) {
				return false;
			}
			if (!dragon.isAlive() || dragon.isRemoved()) {
				return false;
			}

			float damage = (float) Math.max(1.0D, Math.min(dragon.getMaxHealth() * 0.18D, 18.0D));
			float current = Math.max(0.0F, dragon.getHealth());
			float next = Math.max(0.0F, current - damage);
			dragon.setHealth(next);

			if (next <= 0.0F && (fromGaze || fromHead)) {
				try {
					dragon.setHealth(0.0F);
				} catch (Exception ignored) {
				}
				try {
					dragon.setDeltaMovement(0.0D, 0.0D, 0.0D);
				} catch (Exception ignored) {
				}
				try {
					dragon.setNoAi(true);
				} catch (Exception ignored) {
				}
				try {
					if (dragon instanceof PetrifiedAccess access) {
						access.UltraCapsuleAdventure$setPetrified(true);
					}
				} catch (Exception ignored) {
				}
				try {
					dragon.kill(level);
				} catch (Exception ignored) {
				}
			}
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	/**
	 * 일반 엔티티 석화: 원본을 그 자리에서 동결 (자세·모델 그대로, tick 정지).
	 * 성공 시 피해자 본체 반환.
	 */
	public static LivingEntity petrify(ServerLevel level, LivingEntity victim) {
		return petrify(level, victim, (MedusaEntity) null);
	}

	public static LivingEntity petrify(ServerLevel level, LivingEntity victim, java.util.UUID petrifier) {
		LivingEntity r = petrify(level, victim, (MedusaEntity) null);
		if (r != null && petrifier != null) {
			try {
				recordPetrifier(r.getUUID(), petrifier);
			} catch (Exception ignored) {
			}
		}
		return r;
	}

	public static LivingEntity petrify(ServerLevel level, LivingEntity victim, MedusaEntity medusa) {
		try {
			if (!canPetrify(victim)) {
				return null;
			}
			try {
				if (victim instanceof PetrifiedAccess access) {
					access.UltraCapsuleAdventure$setPetrified(true);
				} else {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
			try {
				recordPetrifier(victim.getUUID(), medusa == null ? null : medusa.getUUID());
			} catch (Exception ignored) {
			}
			try {
				if (victim.isVehicle()) {
					victim.ejectPassengers();
				}
			} catch (Exception ignored) {
			}
			try {
				for (Entity passenger : new java.util.ArrayList<>(victim.getPassengers())) {
					try {
						passenger.stopRiding();
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				victim.setDeltaMovement(0, 0, 0);
			} catch (Exception ignored) {
			}
			try {
				victim.hurtTime = 0;
				victim.deathTime = 0;
			} catch (Exception ignored) {
			}
			try {
				victim.clearFire();
			} catch (Exception ignored) {
			}
			try {
				victim.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
			} catch (Exception ignored) {
			}
			try {
				victim.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
			} catch (Exception ignored) {
			}
			try {
				victim.setSilent(true);
			} catch (Exception ignored) {
			}
			try {
				if (victim instanceof net.minecraft.world.entity.Mob mob) {
					mob.setPersistenceRequired();
					try {
						mob.setTarget(null);
					} catch (Exception ignored) {
					}
					try {
						mob.getNavigation().stop();
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
						victim.getX(), victim.getY() + 1, victim.getZ(), 14, 0.5, 0.9, 0.5, 0.06);
			} catch (Exception ignored) {
			}
			try {
				level.playSound(null, victim.blockPosition(),
						net.minecraft.sounds.SoundEvents.STONE_BREAK,
						SoundSource.HOSTILE, 1.0f, 0.7f);
			} catch (Exception ignored) {
			}
			return victim;
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean tryMinePetrified(ServerPlayer sp, LivingEntity victim) {
		try {
			if (sp == null || victim == null) {
				return false;
			}
			if (!(victim.level() instanceof ServerLevel sl)) {
				return false;
			}
			net.minecraft.world.item.ItemStack held = net.minecraft.world.item.ItemStack.EMPTY;
			try {
				held = sp.getMainHandItem();
			} catch (Exception ignored) {
			}
			boolean pickaxe = false;
			try {
				pickaxe = !held.isEmpty() && held.isCorrectToolForDrops(
						net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
			} catch (Exception ignored) {
			}
			if (!pickaxe) {
				return false;
			}
			for (int i = 0; i < 2; i++) {
				try {
					net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
							sl, victim.getX(), victim.getY() + 0.8, victim.getZ(),
							new net.minecraft.world.item.ItemStack(
									net.minecraft.world.level.block.Blocks.COBBLESTONE.asItem()));
					sl.addFreshEntity(drop);
				} catch (Exception ignored) {
				}
			}
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
						victim.getX(), victim.getY() + 1, victim.getZ(),
						10, 0.5, 0.8, 0.5, 0.05);
			} catch (Exception ignored) {
			}
			try {
				sl.playSound(null, victim.blockPosition(),
						net.minecraft.sounds.SoundEvents.STONE_BREAK,
						SoundSource.BLOCKS, 1.0f, 1.0f);
			} catch (Exception ignored) {
			}
			try {
				victim.discard();
			} catch (Exception ignored) {
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	/** 플레이어 석화: 그 자리·그 자세로 플레이어 모양 마네킹 데코이를 소환해 대신 석화 → 플레이어 즉시 사망 */
	public static LivingEntity petrifyPlayer(ServerLevel level, ServerPlayer player, MedusaEntity medusa) {
		try {
			if (player == null || !player.isAlive()) {
				return null;
			}
			float yaw = 0f;
			float pitch = 0f;
			float head = 0f;
			float wpos = 0f;
			float wspd = 0f;
			boolean crouch = false;
			try {
				yaw = player.getYRot();
				pitch = player.getXRot();
				head = player.getYHeadRot();
			} catch (Exception ignored) {
			}
			try {
				wpos = player.walkAnimation.position();
				wspd = player.walkAnimation.speed();
			} catch (Exception ignored) {
			}
			try {
				crouch = player.isCrouching();
			} catch (Exception ignored) {
			}
			String pname = "";
			try {
				pname = player.getGameProfile().name();
			} catch (Exception ignored) {
			}
			LivingEntity decoy = null;
			try {
				net.minecraft.world.entity.EntityType<?> type = null;
				try {
					type = BuiltInRegistries.ENTITY_TYPE
							.get(net.minecraft.resources.Identifier.parse("minecraft:mannequin"))
							.map(h -> h.value()).orElse(null);
				} catch (Exception ignored) {
				}
				if (type != null) {
					net.minecraft.world.entity.EntitySpawnRequest request = new net.minecraft.world.entity.EntitySpawnRequest(
							net.minecraft.world.entity.EntitySpawnReason.LOAD, true);
					net.minecraft.world.entity.Entity e = null;
					try {
						e = type.create(level, request);
					} catch (Exception ignored) {
					}
					if (e instanceof LivingEntity living) {
						decoy = living;
					}
				}
			} catch (Exception e) {
				return null;
			}
			if (decoy == null) {
				return null;
			}
			try {
				decoy.setPos(player.getX(), player.getY(), player.getZ());
				decoy.setYRot(yaw);
			} catch (Exception ignored) {
			}
			try {
				decoy.yBodyRot = yaw;
				decoy.yHeadRot = head;
				decoy.yHeadRotO = head;
			} catch (Exception ignored) {
			}
			try {
				decoy.walkAnimation.position(wpos);
				decoy.walkAnimation.speed(wspd);
			} catch (Exception ignored) {
			}
			try {
				if (crouch) {
					decoy.setPose(net.minecraft.world.entity.Pose.CROUCHING);
				}
			} catch (Exception ignored) {
			}
			try {
				if (decoy instanceof PetrifiedAccess access) {
					access.UltraCapsuleAdventure$setPetrified(true);
				} else {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
			try {
				decoy.setDeltaMovement(0, 0, 0);
			} catch (Exception ignored) {
			}
			try {
				decoy.setSilent(true);
			} catch (Exception ignored) {
			}
			try {
				level.addFreshEntityWithPassengers(decoy);
			} catch (Exception e) {
				return null;
			}
			try {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
						decoy.getX(), decoy.getY() + 1, decoy.getZ(), 16, 0.5, 1.0, 0.5, 0.06);
			} catch (Exception ignored) {
			}
			try {
				DamageSource src;
				try {
					src = level.damageSources().magic();
				} catch (Exception e2) {
					src = level.damageSources().mobAttack(medusa);
				}
				player.hurtServer(level, src, 1000.0f);
			} catch (Exception ignored) {
			}
			return decoy;
		} catch (Exception e) {
			return null;
		}
	}
}



