package com.ultracapsuleadventure.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 바닐라 인챈트 레벨 읽기 (캡슐 인챈트 연동용).
 * - 도박 캡슐: 행운(성공률) + 약탈(배율)
 * - 타이탄 캡슐: 효율(수리 시간) + 빠른 장전(쿨타임)
 * - 소울 캡슐: 약탈(흡수 개수)
 */
public class EnchantLevels {

	public static int level(HolderLookup.Provider registries, ItemStack stack, ResourceKey<Enchantment> key) {
		try {
			if (registries == null || stack == null || stack.isEmpty() || key == null) {
				return 0;
			}
			HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
			Holder<Enchantment> holder = lookup.getOrThrow(key);
			return Math.max(0, stack.getEnchantments().getLevel(holder));
		} catch (Exception e) {
			return 0;
		}
	}

	/** 행운: 최대 3 */
	public static int fortune(HolderLookup.Provider registries, ItemStack stack) {
		try {
			return Math.min(3, level(registries, stack, Enchantments.FORTUNE));
		} catch (Exception e) {
			return 0;
		}
	}

	/** 약탈: 최대 3 */
	public static int looting(HolderLookup.Provider registries, ItemStack stack) {
		try {
			return Math.min(3, level(registries, stack, Enchantments.LOOTING));
		} catch (Exception e) {
			return 0;
		}
	}

	/** 효율: 최대 5 */
	public static int efficiency(HolderLookup.Provider registries, ItemStack stack) {
		try {
			return Math.min(5, level(registries, stack, Enchantments.EFFICIENCY));
		} catch (Exception e) {
			return 0;
		}
	}

	/** 빠른 장전: 최대 3 */
	public static int quickCharge(HolderLookup.Provider registries, ItemStack stack) {
		try {
			return Math.min(3, level(registries, stack, Enchantments.QUICK_CHARGE));
		} catch (Exception e) {
			return 0;
		}
	}

	// ---------- 캡슐 지정 인챈트 제한 ----------

	/** 인챈트 제한 대상 캡슐인지 (소울·도박·타이탄만 인챈트 가능) */
	public static boolean isCapsule(ItemStack stack) {
		try {
			if (stack == null || stack.isEmpty()) {
				return false;
			}
			var item = stack.getItem();
			return item == com.ultracapsuleadventure.item.ModItems.SOUL_CAPSULE
					|| item == com.ultracapsuleadventure.item.ModItems.GAMBLE_CAPSULE
					|| item == com.ultracapsuleadventure.item.ModItems.TITAN_CAPSULE;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 캡슐별 지정 인챈트 (툴팁·효과와 일치).
	 * - 소울: 약탈 / 도박: 행운·약탈 / 타이탄: 효율·빠른 장전.
	 * 캡슐이 아니면 true (건드리지 않음).
	 */
	public static boolean isCapsuleEnchantAllowed(ItemStack stack,
			Holder<Enchantment> enchantment) {
		try {
			if (enchantment == null) {
				return false;
			}
			var item = stack.getItem();
			if (item == com.ultracapsuleadventure.item.ModItems.SOUL_CAPSULE) {
				return enchantment.is(Enchantments.LOOTING);
			}
			if (item == com.ultracapsuleadventure.item.ModItems.GAMBLE_CAPSULE) {
				return enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.LOOTING);
			}
			if (item == com.ultracapsuleadventure.item.ModItems.TITAN_CAPSULE) {
				return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.QUICK_CHARGE);
			}
		} catch (Exception ignored) {
		}
		return true;
	}

	/**
	 * 모루·책 우회 가드: 지정 외 인챈트가 캡슐에 붙어 있으면 주기적으로 제거.
	 * (인챈트 테이블 표시는 믹스인 CapsuleTableEnchantMixin이 담당)
	 */
	public static void register() {
		try {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 100 != 0) {
						return;
					}
					for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
						try {
							stripPlayer(p);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}

	private static void stripPlayer(net.minecraft.server.level.ServerPlayer player) {
		boolean any = false;
		try {
			if (stripStack(player.getMainHandItem()) | stripStack(player.getOffhandItem())) {
				any = true;
			}
		} catch (Exception ignored) {
		}
		try {
			var inv = player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				try {
					if (stripStack(inv.getItem(i))) {
						any = true;
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		if (any) {
			notifyStripped(player);
			try {
				com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
			} catch (Exception ignored) {
			}
		}
	}

	private static void notifyStripped(net.minecraft.server.level.ServerPlayer player) {
		try {
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					"캡슐에는 지정된 인챈트만 붙을 수 있어 어울리지 않는 인챈트를 지웠다."));
		} catch (Exception ignored) {
		}
	}

	/** 지정 외 인챈트를 제거했으면 true */
	private static boolean stripStack(ItemStack stack) {
		try {
			if (!isCapsule(stack)) {
				return false;
			}
			if (!net.minecraft.world.item.enchantment.EnchantmentHelper.hasAnyEnchantments(stack)) {
				return false;
			}
			boolean[] removed = new boolean[1];
			net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, mut -> {
				try {
					mut.removeIf(holder -> {
						try {
							if (!isCapsuleEnchantAllowed(stack, holder)) {
								removed[0] = true;
								return true;
							}
						} catch (Exception ignored) {
						}
						return false;
					});
				} catch (Exception ignored) {
				}
			});
			return removed[0];
		} catch (Exception ignored) {
			return false;
		}
	}
}



