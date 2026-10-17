package com.ultracapsuleadventure.morph;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 은신 캡슐: 황혼의 검으로 처치한 엔티티를 무제한 등록(중복 제외)하고, 우클릭·C키 관리창으로 변신/복귀.
 * - 체력·공격·비행·호흡·면역 복사, 이동속도는 플레이어 유지 (MorphEvents 틱이 적용)
 * - 언제든 우클릭으로 원래 플레이어 모습 복귀
 */
public class StealthCapsuleItem extends Item {

	public StealthCapsuleItem(Properties properties) {
		super(properties);
	}

	/** 등록된 캡슐은 반짝임 */
	@Override
	public boolean isFoil(ItemStack stack) {
		try {
			return MorphData.has(stack);
		} catch (Exception e) {
			return false;
		}
	}

	private static void msg(Player player, Component text, boolean overlay) {
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
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof StealthCapsuleItem)) {
			return InteractionResult.PASS;
		}
		// 변신 중이면 복귀 (어떤 캡슐을 들어도 복귀 가능)
		if (MorphEvents.isMorphed(sp)) {
			MorphEvents.unmorph(sp, "은신 해제! 원래 모습으로 돌아왔다.");
			return InteractionResult.SUCCESS;
		}
		// 웅크리고 우클릭: 등록된 다음 엔티티 선택
		if (player.isShiftKeyDown() && MorphData.getAll(stack).size() > 1) {
			String next = MorphData.cycle(stack);
			msg(player, Component.literal("선택: " + MorphData.shortName(next) + " (" + (MorphData.getSelectedIndex(stack) + 1)
					+ "/" + MorphData.getAll(stack).size() + ")"), true);
			try {
				com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(sp);
			} catch (Exception ignored) {
			}
			return InteractionResult.SUCCESS;
		}
		String id = MorphData.getMorphId(stack);
		if (id == null || id.isEmpty()) {
			msg(player, Component.literal("비어있음. 황혼의 검으로 엔티티를 처치해 등록하세요."), true);
			return InteractionResult.FAIL;
		}
		String err = MorphEvents.morph(sp, id);
		if (err != null) {
			msg(player, Component.literal(err), true);
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = context.getItemInHand();
		if (!(stack.getItem() instanceof StealthCapsuleItem)) {
			return InteractionResult.PASS;
		}
		if (MorphEvents.isMorphed(sp)) {
			MorphEvents.unmorph(sp, "은신 해제! 원래 모습으로 돌아왔다.");
			return InteractionResult.SUCCESS;
		}
		String id = MorphData.getMorphId(stack);
		if (id == null || id.isEmpty()) {
			return InteractionResult.PASS;
		}
		String err = MorphEvents.morph(sp, id);
		return err == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		String id = MorphData.getMorphId(stack);
		if (id == null || id.isEmpty()) {
			lines.accept(Component.literal("비어있음 - 황혼의 검으로 처치 시 등록"));
		} else {
			java.util.List<String> all = MorphData.getAll(stack);
			int sel = MorphData.getSelectedIndex(stack);
			lines.accept(Component.literal("등록 " + all.size() + "종 — 선택: " + MorphData.shortName(id)));
			for (int i = 0; i < Math.min(6, all.size()); i++) {
				lines.accept(Component.literal((i == sel ? " ▶ " : "   ") + MorphData.shortName(all.get(i))));
			}
			if (all.size() > 6) {
				lines.accept(Component.literal("   ... 외 " + (all.size() - 6) + "종 (C키 관리창)"));
			}
			lines.accept(Component.literal("체력·공격·비행·호흡 복사 (이동속도는 그대로)"));
		}
		lines.accept(Component.literal("우클릭: 변신 / 다시 우클릭: 원래 모습 / 웅크리고 우클릭: 다음 엔티티"));
		lines.accept(Component.literal("C키: 관리창에서 변신·삭제·인간 복귀"));
		lines.accept(Component.literal("같은 종은 선공 전까지 중립 (좀비→좀비 등)"));
	}
}



