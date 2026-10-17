package com.ultracapsuleadventure.item;

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

import com.ultracapsuleadventure.util.ItemNbt;

/**
 * 분해 캡슐. 아이템을 넣고 분해 → 제작법의 재료로 되돌림 (제작대의 반대).
 */
public class DisassembleCapsuleItem extends Item {

	public static final String ROOT_KEY = "dis_cap";

	public DisassembleCapsuleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			com.ultracapsuleadventure.network.ModPackets.openUtilityGui(sp, 1, hand == InteractionHand.MAIN_HAND);
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

	public static boolean hasStored(ItemStack stack) {
		try {
			CompoundTag cap = getRoot(stack);
			if (!cap.contains("item")) {
				return false;
			}
			return !cap.getCompound("item").orElse(new CompoundTag()).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public static void setStored(ItemStack stack, HolderLookup.Provider registries, ItemStack item) {
		CompoundTag cap = getRoot(stack);
		cap.put("item", ItemNbt.write(registries, item));
		setRoot(stack, cap);
	}

	public static ItemStack readStored(ItemStack stack, HolderLookup.Provider registries) {
		try {
			CompoundTag cap = getRoot(stack);
			CompoundTag item = cap.getCompound("item").orElse(null);
			if (item == null) {
				return ItemStack.EMPTY;
			}
			return ItemNbt.read(registries, item);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public static void clearStored(ItemStack stack) {
		setRoot(stack, new CompoundTag());
	}

	public static String getExpect(ItemStack stack) {
		try {
			return getRoot(stack).getString("expect").orElse("");
		} catch (Exception e) {
			return "";
		}
	}

	public static void setExpect(ItemStack stack, String s) {
		CompoundTag cap = getRoot(stack);
		cap.putString("expect", s == null ? "" : s);
		setRoot(stack, cap);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		if (!hasStored(stack)) {
			lines.accept(Component.literal("비어있음 - 우클릭 후 넣기"));
		} else {
			lines.accept(Component.literal("분해 대기 중"));
		}
		lines.accept(Component.literal("제작법의 재료로 되돌림 (닳은 도구 불가)"));
	}
}


