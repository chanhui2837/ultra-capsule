package com.partnercap.soul;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

/**
 * 소환된 영혼을 아군으로 만듦: 주인 공격 안 함 + 주인을 위해 싸움 + 따라다님.
 * 외형: 투명 + 파란 발광 (일반 개체와 구분).
 * 26.x에서는 Mob의 selector가 protected라 리플렉션으로 접근 (언오브 상태라 이름固定).
 */
public class SoulAlly {

	public static final String TAG_PREFIX = "soulowner-";
	public static final String TEAM_NAME = "soul_allies";

	private static Field TARGET_SEL_FIELD = null;
	private static Field GOAL_SEL_FIELD = null;

	static GoalSelector targetSel(Mob mob) {
		try {
			if (TARGET_SEL_FIELD == null) {
				TARGET_SEL_FIELD = Mob.class.getDeclaredField("targetSelector");
				TARGET_SEL_FIELD.setAccessible(true);
			}
			return (GoalSelector) TARGET_SEL_FIELD.get(mob);
		} catch (Exception e) {
			return null;
		}
	}

	static GoalSelector goalSel(Mob mob) {
		try {
			if (GOAL_SEL_FIELD == null) {
				GOAL_SEL_FIELD = Mob.class.getDeclaredField("goalSelector");
				GOAL_SEL_FIELD.setAccessible(true);
			}
			return (GoalSelector) GOAL_SEL_FIELD.get(mob);
		} catch (Exception e) {
			return null;
		}
	}

	/** 엔티티에 기록된 소울 주인 (없으면 null) */
	public static UUID soulOwnerOf(Entity entity) {
		try {
			for (String tag : entity.entityTags()) {
				if (tag.startsWith(TAG_PREFIX)) {
					try {
						return UUID.fromString(tag.substring(TAG_PREFIX.length()));
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 이미 아군 골이 박혀있는지 (재시작 후 재적용 판단용) */
	public static boolean hasSoulGoals(Mob mob) {
		try {
			GoalSelector ts = targetSel(mob);
			if (ts == null) {
				return false;
			}
			for (WrappedGoal w : ts.getAvailableGoals()) {
				if (w.getGoal() instanceof SoulDefendGoal) {
					return true;
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/** 반경 내 해당 플레이어의 소울 수집 */
	public static List<LivingEntity> findOwnedSouls(Player player, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		try {
			if (!(player.level() instanceof ServerLevel level)) {
				return out;
			}
			AABB box = player.getBoundingBox().inflate(radius);
			for (Entity e : level.getEntities(player, box, x -> x instanceof LivingEntity living && living.isAlive())) {
				try {
					if (com.partnercap.medusa.Petrify.isPetrified(e)) {
						continue;
					}
					UUID o = soulOwnerOf(e);
					if (o != null && o.equals(player.getUUID())) {
						out.add((LivingEntity) e);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	public static void makeAlly(ServerLevel level, Entity entity, UUID ownerId) {		try {
			entity.addTag(TAG_PREFIX + ownerId.toString());
		} catch (Exception ignored) {
		}
		if (entity instanceof Mob mob) {
			try {
				mob.setPersistenceRequired();
			} catch (Exception ignored) {
			}
			GoalSelector ts = targetSel(mob);
			if (ts != null) {
				try {
					ts.removeAllGoals(g -> true);
				} catch (Exception ignored) {
				}
				try {
					ts.addGoal(1, new SoulDefendGoal(mob, ownerId));
					ts.addGoal(2, new SoulHuntGoal(mob, ownerId));
					ts.addGoal(3, new SoulGuardGoal(mob, ownerId));
				} catch (Exception ignored) {
				}
			}
			GoalSelector gs = goalSel(mob);
			if (gs != null) {
				try {
					for (WrappedGoal w : new java.util.ArrayList<>(gs.getAvailableGoals())) {
						Goal gg = null;
						try {
							gg = w.getGoal();
						} catch (Exception ignored) {
							continue;
						}
						if (gg instanceof SoulFollowGoal) {
							gs.removeGoal(gg);
							continue;
						}
						try {
							if (gg instanceof net.minecraft.world.entity.ai.goal.target.TargetGoal) {
								gs.removeGoal(gg);
								continue;
							}
						} catch (Exception ignored) {
						}
						String nm = "";
						try {
							nm = gg.getClass().getSimpleName();
						} catch (Exception ignored) {
							continue;
						}
						// 큐브류 상시 방향전환/점프 골이 따라오기를 굶주리게 해서 소울에서 제거
						if (nm.contains("RandomDirection") || nm.contains("KeepOnJumping")) {
							try {
								gs.removeGoal(gg);
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception ignored) {
				}
				try {
					gs.addGoal(5, new SoulFollowGoal(mob, ownerId));
				} catch (Exception ignored) {
				}
			}
			try {
				mob.setTarget(null);
			} catch (Exception ignored) {
			}
		}
		// 원거리몹 기본 무기 (활/쇠뇌 없으면 근접만 함)
		try {
			equipSoulDefaults(entity);
		} catch (Exception ignored) {
		}
		// 유령 외형: 투명 + 발광 + 파란 팀
		try {
			entity.setGlowingTag(true);
		} catch (Exception ignored) {
		}
		// 레벨 보정 (최초 1회만; soulscaled 태그로 중복 방지)
		try {
			int soulLevel = 1;
			try {
				for (String t : entity.entityTags()) {
					if (t.startsWith("soullevel-")) {
						try {
							soulLevel = Math.max(1, Math.min(3, Integer.parseInt(t.substring(11))));
						} catch (Exception ignored) {
						}
						break;
					}
				}
			} catch (Exception ignored) {
			}
			boolean scaled = false;
			try {
				scaled = entity.entityTags().contains("soulscaled");
			} catch (Exception ignored) {
			}
			if (soulLevel > 1 && !scaled && entity instanceof LivingEntity living) {
				float mult = soulLevel >= 3 ? 2.0f : 1.5f;
				try {
					var hp = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
					if (hp != null) {
						hp.setBaseValue(hp.getBaseValue() * mult);
						try {
							living.setHealth((float) hp.getValue());
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
				try {
					var atk = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
					if (atk != null) {
						atk.setBaseValue(atk.getBaseValue() * mult);
					}
				} catch (Exception ignored) {
				}
				try {
					entity.addTag("soulscaled");
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		if (entity instanceof LivingEntity living) {
			try {
				living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 999999, 0, false, false, false));
			} catch (Exception ignored) {
			}
			try {
				living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false, false));
			} catch (Exception ignored) {
			}
		}
		try {
			Scoreboard sb = level.getScoreboard();
			PlayerTeam team = sb.getPlayerTeam(TEAM_NAME);
			if (team == null) {
				team = sb.addPlayerTeam(TEAM_NAME);
				try {
					team.setColor(Optional.of(TeamColor.valueOf("BLUE")));
				} catch (Exception ignored) {
				}
			}
			try {
				team.setAllowFriendlyFire(false);
			} catch (Exception ignored) {
			}
			sb.addPlayerToTeam(entity.getScoreboardName(), team);
			// 주인도 같은 팀에 넣어 큐브 접촉 등 팀 판정 데미지에서 제외
			try {
				for (ServerPlayer p : level.players()) {
					if (p.getUUID().equals(ownerId)) {
						sb.addPlayerToTeam(p.getScoreboardName(), team);
						break;
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 모든 차원에서 주인의 소울을 현재 차원·주변으로 회수.
	 * 다른 차원은 저장 후 재소환(차원 이동)으로, 같은 차원은 그 자리로 순간이동.
	 */
	public static int recallSoulsTo(ServerPlayer player) {
		int n = 0;
		ServerLevel dest;
		try {
			if (!(player.level() instanceof ServerLevel sl)) {
				return 0;
			}
			dest = sl;
		} catch (Exception e) {
			return 0;
		}
		List<Entity> targets = new ArrayList<>();
		try {
			for (ServerLevel level : dest.getServer().getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						if (!(e instanceof LivingEntity)) {
							continue;
						}
						try {
							if (com.partnercap.medusa.Petrify.isPetrified(e)) {
								continue;
							}
							UUID o = soulOwnerOf(e);
							if (o != null && o.equals(player.getUUID()) && e.isAlive()) {
								targets.add(e);
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception e) {
			return 0;
		}
		for (Entity e : targets) {
			try {
				if (!e.isAlive()) {
					continue;
				}
				if (e.level() == dest) {
					e.teleportTo(player.getX() + 1, player.getY() + 1, player.getZ() + 1);
					n++;
					continue;
				}
				CompoundTag tag = com.partnercap.util.TameUtil.savePet((LivingEntity) e);
				e.discard();
				EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, true);
				Entity created = EntityType.loadEntityRecursive(tag, dest, request, EntityProcessor.NOP);
				if (created == null) {
					continue;
				}
				created.setPos(player.getX() + 1, player.getY() + 1, player.getZ() + 1);
				makeAlly(dest, created, player.getUUID());
				dest.addFreshEntityWithPassengers(created);
				n++;
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	/** 서버 틱마다 소울 태그 몹의 아군화 유지 (재시작 후 복구용) */
	public static void tickMaintain(net.minecraft.server.MinecraftServer server) {
		try {
			if (server.getTickCount() % 100 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						if (!(e instanceof Mob mob)) {
							continue;
						}
						UUID owner;
						try {
							owner = soulOwnerOf(e);
						} catch (Exception ex) {
							continue;
						}
						if (owner == null) {
							continue;
						}
						boolean has;
						try {
							has = hasSoulGoals(mob);
						} catch (Exception ex) {
							continue;
						}
						if (!has) {
							try {
								makeAlly(level, e, owner);
							} catch (Exception ex) {
							}
						}
						// 패치 전 소환된 개체의 잔여 타겟팅 제거 (재소환 없이 자동 치유)
						try {
							scrubStaleTargetGoals(mob);
						} catch (Exception ignored) {
						}
						// 맨몸 소환된 원거리몹 무기 보급
						try {
							equipSoulDefaults(mob);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** goalSelector에 남은 구버전 타겟팅 제거 (먹보의 플레이어 어그로 등) */
	public static void scrubStaleTargetGoals(Mob mob) {
		try {
			GoalSelector gs = goalSel(mob);
			if (gs == null) {
				return;
			}
			for (WrappedGoal w : new java.util.ArrayList<>(gs.getAvailableGoals())) {
				Goal gg;
				try {
					gg = w.getGoal();
				} catch (Exception ignored) {
					continue;
				}
				try {
					if (gg instanceof net.minecraft.world.entity.ai.goal.target.TargetGoal) {
						gs.removeGoal(gg);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 원거리몹 소울 기본 무기 지급 (맨몸 소환 시 근접만 해서) */
	public static void equipSoulDefaults(Entity entity) {
		try {
			if (!(entity instanceof LivingEntity living)) {
				return;
			}
			ItemStack hand = ItemStack.EMPTY;
			try {
				hand = living.getMainHandItem();
			} catch (Exception ex) {
				return;
			}
			if (!hand.isEmpty()) {
				return;
			}
			ItemStack give = ItemStack.EMPTY;
			try {
				if (entity instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton) {
					give = new ItemStack(net.minecraft.world.item.Items.BOW);
				} else if (entity instanceof net.minecraft.world.entity.monster.illager.Pillager) {
					give = new ItemStack(net.minecraft.world.item.Items.CROSSBOW);
				}
			} catch (Exception ignored) {
			}
			if (give.isEmpty()) {
				return;
			}
			try {
				living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, give);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	static ServerPlayer ownerOf(Mob mob, UUID ownerId) {
		try {
			if (!(mob.level() instanceof ServerLevel level)) {
				return null;
			}
			for (ServerPlayer p : level.players()) {
				if (p.getUUID().equals(ownerId)) {
					return p;
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	static boolean validFoe(Mob mob, UUID ownerId, LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		try {
			if (target == mob) {
				return false;
			}
			if (target instanceof com.partnercap.medusa.StatueEntity) {
				return false;
			}
			if (com.partnercap.medusa.Petrify.isPetrified(target)) {
				return false;
			}
			if (target.getUUID().equals(ownerId)) {
				return false;
			}
			// 같은 주인의 영혼끼리는 안 싸움
			try {
				for (String tag : target.entityTags()) {
					if (tag.startsWith(TAG_PREFIX)) {
						return false;
					}
				}
			} catch (Exception ignored) {
			}
			if (mob.distanceToSqr(target) > 1024.0) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/** 주인을 때린 적을 공격 */
	public static class SoulDefendGoal extends Goal {
		private final Mob mob;
		private final UUID ownerId;
		private LivingEntity attacker;

		public SoulDefendGoal(Mob mob, UUID ownerId) {
			this.mob = mob;
			this.ownerId = ownerId;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			if (owner == null || !owner.isAlive()) {
				return false;
			}
			LivingEntity a;
			int ts;
			try {
				a = owner.getLastHurtByMob();
				ts = owner.getLastHurtByMobTimestamp();
			} catch (Exception e) {
				return false;
			}
			if (a == null || !validFoe(mob, ownerId, a)) {
				return false;
			}
			if (mob.tickCount - ts > 200) {
				return false;
			}
			attacker = a;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			return attacker != null && attacker.isAlive() && owner != null && owner.isAlive()
					&& mob.getTarget() == attacker;
		}

		@Override
		public void start() {
			try {
				mob.setTarget(attacker);
			} catch (Exception ignored) {
			}
			super.start();
		}

		@Override
		public void stop() {
			try {
				if (mob.getTarget() == attacker) {
					mob.setTarget(null);
				}
			} catch (Exception ignored) {
			}
			attacker = null;
			super.stop();
		}
	}

	/** 주인이 때린 적을 같이 공격 */
	public static class SoulHuntGoal extends Goal {
		private final Mob mob;
		private final UUID ownerId;
		private LivingEntity victim;

		public SoulHuntGoal(Mob mob, UUID ownerId) {
			this.mob = mob;
			this.ownerId = ownerId;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			if (owner == null || !owner.isAlive()) {
				return false;
			}
			LivingEntity v;
			int ts;
			try {
				v = owner.getLastHurtMob();
				ts = owner.getLastHurtMobTimestamp();
			} catch (Exception e) {
				return false;
			}
			if (v == null || !validFoe(mob, ownerId, v)) {
				return false;
			}
			if (mob.tickCount - ts > 200) {
				return false;
			}
			victim = v;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			return victim != null && victim.isAlive() && owner != null && owner.isAlive()
					&& mob.getTarget() == victim;
		}

		@Override
		public boolean isInterruptable() {
			return true;
		}

		@Override
		public void start() {
			try {
				mob.setTarget(victim);
			} catch (Exception ignored) {
			}
			super.start();
		}

		@Override
		public void stop() {
			try {
				if (mob.getTarget() == victim) {
					mob.setTarget(null);
				}
			} catch (Exception ignored) {
			}
			victim = null;
			super.stop();
		}
	}

	/** 주인을 노리는 적을 선제 공격 (아직 안 맞았어도 근처 적대몹) */
	public static class SoulGuardGoal extends Goal {
		private final Mob mob;
		private final UUID ownerId;
		private LivingEntity foe;

		public SoulGuardGoal(Mob mob, UUID ownerId) {
			this.mob = mob;
			this.ownerId = ownerId;
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			if (owner == null || !owner.isAlive()) {
				return false;
			}
			LivingEntity found = null;
			try {
				AABB box = owner.getBoundingBox().inflate(12.0);
				for (Entity e : mob.level().getEntities(owner, box, x -> x instanceof Mob)) {
					Mob m = (Mob) e;
					try {
						if (m.getTarget() == owner && validFoe(mob, ownerId, m)) {
							found = m;
							break;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception e) {
				return false;
			}
			if (found == null) {
				return false;
			}
			foe = found;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			return foe != null && foe.isAlive() && owner != null && owner.isAlive()
					&& mob.getTarget() == foe && foe.distanceToSqr(owner) < 1024.0;
		}

		@Override
		public void start() {
			try {
				mob.setTarget(foe);
			} catch (Exception ignored) {
			}
			super.start();
		}

		@Override
		public void stop() {
			try {
				if (mob.getTarget() == foe) {
					mob.setTarget(null);
				}
			} catch (Exception ignored) {
			}
			foe = null;
			super.stop();
		}
	}

	/** 주인 따라다니기 (멀면 순간이동) */
	public static class SoulFollowGoal extends Goal {
		private final Mob mob;
		private final UUID ownerId;
		private int recalc = 0;

		public SoulFollowGoal(Mob mob, UUID ownerId) {
			this.mob = mob;
			this.ownerId = ownerId;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 25.0;
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 9.0;
		}

		@Override
		public void start() {
			recalc = 0;
			super.start();
		}

		@Override
		public void stop() {
			try {
				mob.getNavigation().stop();
			} catch (Exception ignored) {
			}
			super.stop();
		}

		@Override
		public void tick() {
			ServerPlayer owner = ownerOf(mob, ownerId);
			if (owner == null) {
				return;
			}
			super.tick();
			double d = mob.distanceToSqr(owner);
			if (d > 900.0) {
				Level level = mob.level();
				for (int i = 0; i < 6; i++) {
					double x = owner.getX() + (level.getRandom().nextDouble() - 0.5) * 5.0;
					double y = owner.getY() + 1.0;
					double z = owner.getZ() + (level.getRandom().nextDouble() - 0.5) * 5.0;
					try {
						BlockPos p = BlockPos.containing(x, y, z);
						if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
							mob.teleportTo(x + 0.5, p.getY(), z + 0.5);
							try {
								mob.getNavigation().stop();
							} catch (Exception ignored) {
							}
							return;
						}
					} catch (Exception ignored) {
					}
				}
			}
			if (--recalc <= 0) {
				recalc = 5;
				try {
					mob.getNavigation().moveTo(owner, 1.25);
				} catch (Exception ignored) {
				}
			}
			try {
				mob.getLookControl().setLookAt(owner, 10.0F, 10.0F);
			} catch (Exception ignored) {
			}
		}
	}
}
