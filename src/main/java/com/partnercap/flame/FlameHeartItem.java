package com.partnercap.flame;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** 화염몽의 심장: 열관 제작 + 화염룡의 피 3병 제작 재료. */
public class FlameHeartItem extends Item {

	public FlameHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		try {
			lines.accept(Component.literal("열관 제작 재료 (조약돌 8 + 심장)"));
			lines.accept(Component.literal("작업대: 심장 위 + 유리병 아래 = 화염룡의 피 3병"));
		} catch (Exception ignored) {
		}
	}
}
