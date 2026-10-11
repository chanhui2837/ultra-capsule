package com.partnercap.item;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.partnercap.util.ItemNbt;

/**
 * 도박 캡슐. 아이템을 넣고 도박 시작 → 10초 후 2배 or 소멸 (50%).
 */
public class GambleCapsuleItem extends Item {

	public static final String ROOT_KEY = "gamble_cap";
	public static final long GAMBLE_MS = 10000L;

	public GambleCapsuleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			com.partnercap.network.ModPackets.openUtilityGui(sp, 0, hand == InteractionHand.MAIN_HAND);
		}
		return InteractionResult.SUCCESS;
	}

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

	public static boolean hasBet(ItemStack stack) {
		try {
			CompoundTag cap = getRoot(stack);
			if (!cap.contains("bet")) {
				return false;
			}
			return !cap.getCompound("bet").orElse(new CompoundTag()).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public static long getUntil(ItemStack stack) {
		try {
			return getRoot(stack).getLong("until").orElse(0L);
		} catch (Exception e) {
			return 0L;
		}
	}

	public static boolean isActive(ItemStack stack) {
		return hasBet(stack) && getUntil(stack) > 0;
	}

	public static void setBet(ItemStack stack, HolderLookup.Provider registries, ItemStack bet) {
		CompoundTag cap = getRoot(stack);
		cap.put("bet", ItemNbt.write(registries, bet));
		cap.putLong("until", 0L);
		setRoot(stack, cap);
	}

	public static ItemStack readBet(ItemStack stack, HolderLookup.Provider registries) {
		try {
			CompoundTag cap = getRoot(stack);
			CompoundTag bet = cap.getCompound("bet").orElse(null);
			if (bet == null) {
				return ItemStack.EMPTY;
			}
			return ItemNbt.read(registries, bet);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public static void setUntil(ItemStack stack, long until) {
		CompoundTag cap = getRoot(stack);
		cap.putLong("until", until);
		setRoot(stack, cap);
	}

	public static void clearBet(ItemStack stack) {
		setRoot(stack, new CompoundTag());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		if (!hasBet(stack)) {
			lines.accept(Component.literal("비어있음 - 우클릭 후 넣기"));
		} else if (isActive(stack)) {
			lines.accept(Component.literal("도박 진행 중... (10초)"));
		} else {
			lines.accept(Component.literal("베팅 장전됨 - 도박 시작 가능"));
		}
		lines.accept(Component.literal("10초 후 2배 or 소멸 (50%)"));
	}
}
