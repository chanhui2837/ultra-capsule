package com.partnercap.flame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 화염 브레스 효과 관리 (화염룡의 피 음용).
 * UUID → 만료시각(ms). F키 홀드 패킷이 오면 전방 원뿔 화염.
 */
public class FlameBreath {

	private static final Map<UUID, Long> UNTIL = new HashMap<>();
	private static final Map<UUID, Long> NEXT_HIT = new HashMap<>();

	public static final double RANGE = 8.0;
	public static final float DAMAGE = 4.0F;

	public static void grant(ServerPlayer player, long durationMs) {
		try {
			synchronized (UNTIL) {
				UNTIL.put(player.getUUID(), System.currentTimeMillis() + durationMs);
				while (UNTIL.size() > 200) {
					try {
						UNTIL.remove(UNTIL.keySet().iterator().next());
					} catch (Exception ex) {
						break;
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static boolean has(ServerPlayer player) {
		try {
			Long until;
			synchronized (UNTIL) {
				until = UNTIL.get(player.getUUID());
			}
			if (until == null) {
				return false;
			}
			if (System.currentTimeMillis() > until) {
				try {
					synchronized (UNTIL) {
						UNTIL.remove(player.getUUID());
					}
				} catch (Exception ignored) {
				}
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static long remainSec(ServerPlayer player) {
		try {
			Long until;
			synchronized (UNTIL) {
				until = UNTIL.get(player.getUUID());
			}
			if (until == null) {
				return 0;
			}
			return Math.max(0, (until - System.currentTimeMillis()) / 1000L);
		} catch (Exception e) {
			return 0;
		}
	}

	/** F 홀드 1틱: 빈손이 아니면 실패. 전방 원뿔에 화염 피해+착화+파티클. */
	public static void breathe(ServerPlayer player) {
		try {
			if (player == null || !player.isAlive()) {
				return;
			}
			if (!has(player)) {
				return;
			}
			// 빈손 조건 (양손 다 비어 있어야)
			try {
				if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
					return;
				}
			} catch (Exception ignored) {
				return;
			}
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			// 서버 과부하 방지: 3틱에 1번만 피해 판정 (파티클은 매번)
			boolean doHit = false;
			try {
				long now = System.currentTimeMillis();
				Long next;
				synchronized (NEXT_HIT) {
					next = NEXT_HIT.get(player.getUUID());
				}
				if (next == null || now >= next) {
					doHit = true;
					synchronized (NEXT_HIT) {
						NEXT_HIT.put(player.getUUID(), now + 150L);
					}
				}
			} catch (Exception ignored) {
				doHit = true;
			}
			Vec3 eye;
			Vec3 dir;
			try {
				eye = player.getEyePosition();
				dir = player.getLookAngle().normalize();
			} catch (Exception e) {
				return;
			}
			Vec3 mouth = eye.add(dir.scale(0.5));
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
						mouth.x, mouth.y - 0.1, mouth.z, 6, dir.x * 0.5, dir.y * 0.5 + 0.15, dir.z * 0.5, 0.06);
			} catch (Exception ignored) {
			}
			if (player.tickCount % 10 == 0) {
				try {
					sl.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
				} catch (Exception ignored) {
				}
			}
			if (!doHit) {
				return;
			}
			try {
				AABB sweep = player.getBoundingBox().inflate(RANGE);
				for (Entity e : sl.getEntities(player, sweep, x -> x instanceof LivingEntity living && living.isAlive())) {
					try {
						Vec3 to = new Vec3(e.getX() - mouth.x, (e.getY() + e.getBbHeight() * 0.5) - mouth.y,
								e.getZ() - mouth.z);
						double t = to.dot(dir);
						if (t < 0.5 || t > RANGE) {
							continue;
						}
						Vec3 closest = mouth.add(dir.scale(t));
						Vec3 center = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
						if (closest.distanceTo(center) > e.getBbWidth() + 1.0) {
							continue;
						}
						LivingEntity le = (LivingEntity) e;
						try {
							le.hurtServer(sl, sl.damageSources().inFire(), DAMAGE);
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
			try {
				Vec3 hit = mouth.add(dir.scale(4.0));
				BlockPos fp = BlockPos.containing(hit.x, hit.y, hit.z);
				if (sl.getBlockState(fp).isAir() && sl.getBlockState(fp.below()).isSolidRender()) {
					sl.setBlockAndUpdate(fp, Blocks.FIRE.defaultBlockState());
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}
