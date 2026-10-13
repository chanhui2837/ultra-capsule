package com.partnercap.mech;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * 메카 탑승자 보호.
 * - 크리에이티브·관전자가 아닌 상태에서 탑승 중이면 플레이어 무적 (어떤 피해도 안 받음)
 * - 플레이어를 노리던 몹은 메카 로봇을 대신 공격 (하차하면 그대로, 보호만 해제)
 */
public class MechEvents {

	/** 탑승 보호 대상인가 (서바이벌·어드벤처 + 메카 탑승 중) */
	public static boolean shields(Entity victim) {
		try {
			if (!(victim instanceof ServerPlayer p)) {
				return false;
			}
			try {
				if (p.isCreative() || p.isSpectator()) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				return p.isAlive() && p.getVehicle() instanceof MechRobotEntity;
			} catch (Exception ignored) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static void register() {
		// 탑승자가 자기 메카를 때리는 판정 차단 (좌클릭 = 레이저 발사인데 근접 공격이 같이 나가던 문제).
		// 클라·서버 양쪽에서 호출되어 공격 패킷 자체를 막는다. (엔티티 쪽 hurtServer 차단은 이중 안전장치)
		try {
			net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT
					.register((player, world, hand, entity, hitResult) -> {
						try {
							if (entity instanceof MechRobotEntity && player.getVehicle() == entity) {
								return net.minecraft.world.InteractionResult.FAIL;
							}
						} catch (Exception ignored) {
						}
						return net.minecraft.world.InteractionResult.PASS;
					});
		} catch (Exception ignored) {
		}
		// 탑승 중 플레이어는 무적
		try {
			ServerLivingEntityEvents.ALLOW_DAMAGE.register((LivingEntity victim, DamageSource source, float amount) -> {
				try {
					if (shields(victim)) {
						return false;
					}
				} catch (Exception ignored) {
				}
				return true;
			});
		} catch (Exception ignored) {
		}
		// 어그로 전환: 탑승자를 노리는 몹 → 메카를 공격 (1초 주기)
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 20 != 0) {
						return;
					}
					for (ServerLevel level : server.getAllLevels()) {
						java.util.List<Entity> all = new java.util.ArrayList<>();
						try {
							for (Entity ent : level.getAllEntities()) {
								all.add(ent);
							}
						} catch (Exception e) {
							continue;
						}
						for (Entity e : all) {
							try {
								if (!(e instanceof MechRobotEntity mech)) {
									continue;
								}
								Entity rider = null;
								try {
									rider = mech.getFirstPassenger();
								} catch (Exception ignored) {
								}
								if (!(rider instanceof ServerPlayer sp) || !shields(sp)) {
									continue;
								}
								AABB box;
								try {
									box = mech.getBoundingBox().inflate(64.0);
								} catch (Exception ignored) {
									continue;
								}
								java.util.List<Entity> mobs;
								try {
									mobs = level.getEntities(mech, box, x -> x instanceof Mob);
								} catch (Exception ignored) {
									continue;
								}
								for (Entity m : mobs) {
									try {
										if (!(m instanceof Mob mob) || mob == mech) {
											continue;
										}
										LivingEntity t = null;
										try {
											t = mob.getTarget();
										} catch (Exception ignored) {
										}
										if (t != null && t.getUUID().equals(sp.getUUID())) {
											try {
												mob.setTarget(mech);
											} catch (Exception ignored) {
											}
										}
									} catch (Exception ignored) {
									}
								}
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}
}
