package com.partnercap.voidcap;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 공허 캡슐 저장 (CUSTOM_DATA 안의 {"void_cap": {"voids": [...]}}).
 * 각 항목은 엔티티 전체 NBT(saveAsPassenger 그대로)라 성장·쿨타임·상태가 이어짐.
 */
public class VoidData {
	public static final String ROOT_KEY = "void_cap";
	public static final String VOIDS_KEY = "voids";

	private static CompoundTag getRoot(ItemStack stack) {
		CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		if (!root.contains(ROOT_KEY)) {
			return new CompoundTag();
		}
		return root.getCompound(ROOT_KEY).orElse(new CompoundTag());
	}

	private static void setRoot(ItemStack stack, CompoundTag cap) {
		CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		root.put(ROOT_KEY, cap);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
	}

	public static List<CompoundTag> getVoids(ItemStack stack) {
		List<CompoundTag> out = new ArrayList<>();
		CompoundTag cap = getRoot(stack);
		if (!cap.contains(VOIDS_KEY)) {
			return out;
		}
		ListTag list = cap.getList(VOIDS_KEY).orElse(new ListTag());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i).orElse(null);
			if (t != null) {
				out.add(t.copy());
			}
		}
		return out;
	}

	public static void setVoids(ItemStack stack, List<CompoundTag> voids) {
		CompoundTag cap = getRoot(stack);
		ListTag list = new ListTag();
		for (CompoundTag v : voids) {
			list.add(v.copy());
		}
		cap.put(VOIDS_KEY, list);
		setRoot(stack, cap);
	}

	public static void addVoid(ItemStack stack, CompoundTag tag) {
		List<CompoundTag> cur = getVoids(stack);
		cur.add(tag.copy());
		setVoids(stack, cur);
	}

	public static CompoundTag removeVoid(ItemStack stack, int index) {
		List<CompoundTag> cur = getVoids(stack);
		if (index < 0 || index >= cur.size()) {
			return null;
		}
		CompoundTag removed = cur.remove(index);
		setVoids(stack, cur);
		return removed;
	}

	public static int count(ItemStack stack) {
		return getVoids(stack).size();
	}

	public static boolean isEmpty(ItemStack stack) {
		return count(stack) == 0;
	}
}
