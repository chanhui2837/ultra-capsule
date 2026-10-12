package com.partnercap.util;

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
}
