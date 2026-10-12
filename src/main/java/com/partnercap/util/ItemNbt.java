package com.partnercap.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * ItemStack <-> CompoundTag 왕복 (캡슐 슬롯 저장용).
 */
public class ItemNbt {

	public static CompoundTag write(HolderLookup.Provider registries, ItemStack stack) {
		try {
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
			out.store("v", ItemStack.CODEC, stack.copy());
			CompoundTag root = out.buildResult();
			return root.getCompound("v").orElse(new CompoundTag());
		} catch (Exception e) {
			return new CompoundTag();
		}
	}

	public static ItemStack read(HolderLookup.Provider registries, CompoundTag tag) {
		try {
			if (tag == null || tag.isEmpty()) {
				return ItemStack.EMPTY;
			}
			CompoundTag wrap = new CompoundTag();
			wrap.put("v", tag.copy());
			ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, registries, wrap);
			return in.read("v", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public static String displayName(ItemStack stack) {
		try {
			if (stack.isEmpty()) {
				return "(없음)";
			}
			Component hover = stack.getHoverName();
			return hover == null ? stack.getItem().toString() : hover.getString();
		} catch (Exception e) {
			return "(없음)";
		}
	}
}
