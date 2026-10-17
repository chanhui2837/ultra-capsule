package com.ultracapsuleadventure.silicate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** 규산 파편. (테스트용) 크리에이티브에서 웅크리고 우클릭하면 운석이 즉시 떨어진다. */
public class SilicateShardItem extends Item {

	public SilicateShardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		boolean creative = false;
		boolean sneaking = false;
		try {
			creative = player.isCreative();
			sneaking = player.isShiftKeyDown();
		} catch (Exception ignored) {
		}
		if (creative && sneaking) {
			if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
				MeteorEvents.forceMeteor(sl, sp);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}


