package com.partnercap.item;

import java.util.List;

import com.partnercap.voidcap.VoidData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * 공허 캡슐. 엔티티를 빨아들여 보관, 풀어주기, 존재 삭제로 인과까지 무효화.
 */
public class VoidCapsuleItem extends Item {

	public VoidCapsuleItem(Properties properties) {
		super(properties);
	}

	/** 항상 인챈트 반짝임 (어두운 곳에서도 존재감) */
	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	static void msg(Player player, Component text, boolean overlay) {
		if (player instanceof ServerPlayer sp) {
			if (overlay) {
				sp.sendOverlayMessage(text);
			} else {
				sp.sendSystemMessage(text);
			}
		} else {
			if (overlay) {
				player.sendOverlayMessage(text);
			} else {
				player.sendSystemMessage(text);
			}
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		int c = VoidData.count(stack);
		if (c == 0) {
			lines.accept(Component.literal("비어있음 - 엔티티에 우클릭해 빨아들임"));
		} else {
			lines.accept(Component.literal("보관중: " + c + "기"));
			List<CompoundTag> voids = VoidData.getVoids(stack);
			for (int i = 0; i < Math.min(4, voids.size()); i++) {
				lines.accept(Component.literal(" - " + com.partnercap.data.PartnerData.getName(voids.get(i))));
			}
			if (voids.size() > 4) {
				lines.accept(Component.literal("... 외 " + (voids.size() - 4) + "기"));
			}
		}
		lines.accept(Component.literal("C키: 보기·풀어주기·존재 삭제"));
	}
}
