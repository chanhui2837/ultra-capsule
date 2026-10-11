package com.partnercap.item;

import java.util.List;
import java.util.UUID;

import com.partnercap.soul.SoulAlly;
import com.partnercap.soul.SoulData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 소울 캡슐. 불길의 검으로 처치한 영혼을 저장했다가 우클릭으로 아군 소환.
 */
public class SoulCapsuleItem extends Item {

	public SoulCapsuleItem(Properties properties) {
		super(properties);
	}

	/** 항상 인챈트 반짝임 (어두운 곳에서도 존재감) */
	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
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
		ItemStack stack = player.getItemInHand(hand);
		boolean sneak = false;
		try {
			sneak = player.isCrouching();
		} catch (Exception ignored) {
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (sneak) {
			return tryRecapture(player, stack);
		}
		if (SoulData.isEmpty(stack)) {
			msg(player, Component.literal("비어있음. 불길의 검으로 처치해 영혼을 모아오세요."), true);
			return InteractionResult.FAIL;
		}
		summonAll(level, player, stack);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack stack = context.getItemInHand();
		boolean sneak = false;
		try {
			sneak = player.isCrouching();
		} catch (Exception ignored) {
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (sneak) {
			return tryRecapture(player, stack);
		}
		if (SoulData.isEmpty(stack)) {
			return InteractionResult.PASS;
		}
		summonAll(level, player, stack);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
		try {
			if (com.partnercap.medusa.Petrify.isPetrified(entity)) {
				return InteractionResult.PASS;
			}
		} catch (Exception ignored) {
		}
		UUID soulOwner;
		try {
			soulOwner = SoulAlly.soulOwnerOf(entity);
		} catch (Exception e) {
			return InteractionResult.PASS;
		}
		if (soulOwner == null || !soulOwner.equals(player.getUUID())) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		return tryRecapture(player, stack);
	}

	/** 주변 내 소울 전부 회수 (이벤트/직접클릭/웅크리기 우클릭이 공유) */
	public static InteractionResult tryRecapture(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer)) {
			return InteractionResult.PASS;
		}
		List<LivingEntity> souls = SoulAlly.findOwnedSouls(player, PartnerCapsuleItem.CAPTURE_RADIUS);
		if (souls.isEmpty()) {
			msg(player, Component.literal("주변에 회수할 내 소울이 없습니다."), true);
			return InteractionResult.FAIL;
		}
		int n = 0;
		for (LivingEntity s : souls) {
			try {
				String tid = BuiltInRegistries.ENTITY_TYPE.getKey(s.getType()).toString();
				SoulData.addSoul(stack, SoulData.captureFrom(s, tid));
				s.discard();
				n++;
			} catch (Exception ignored) {
			}
		}
		if (n == 0) {
			return InteractionResult.FAIL;
		}
		PartnerCapsuleItem.syncCapsule(player);
		msg(player, Component.literal("영혼 " + n + "기를 회수했다!"), false);
		return InteractionResult.SUCCESS;
	}

	public static void summonAll(Level level, Player player, ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		List<CompoundTag> souls = SoulData.getSouls(stack);
		if (souls.isEmpty()) {
			return;
		}
		EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true);
		int spawned = 0;
		double baseX = player.getX();
		double baseY = player.getY();
		double baseZ = player.getZ();
		for (int i = 0; i < souls.size(); i++) {
			try {
				double ox = ((spawned % 5) - 2) * 1.4 + serverLevel.getRandom().nextDouble() * 0.5;
				double oz = ((spawned / 5) - 1) * 1.4 + serverLevel.getRandom().nextDouble() * 0.5;
				Entity created = SoulData.createSoulEntity(serverLevel, souls.get(i), baseX + ox, baseY + 1, baseZ + oz);
				if (created == null) {
					continue;
				}
				try {
					created.setYRot(serverLevel.getRandom().nextFloat() * 360f);
				} catch (Exception ignored) {
				}
				try {
					int lv = SoulData.getLevel(souls.get(i));
					created.addTag("soullevel-" + Math.max(1, Math.min(3, lv)));
				} catch (Exception ignored) {
				}
				try {
					SoulData.applyBaby(created, souls.get(i));
				} catch (Exception ignored) {
				}
				SoulAlly.makeAlly(serverLevel, created, player.getUUID());
				serverLevel.addFreshEntityWithPassengers(created);
				spawned++;
			} catch (Exception e) {
				// 개별 소환 실패 무시
			}
		}
		SoulData.clearSouls(stack);
		PartnerCapsuleItem.syncCapsule(player);
		msg(player, Component.literal("영혼 " + spawned + "기를 소환했다! 널 위해 싸울 것이다."), false);
	}

	/** 개별 소환 (관리창에서 사용) */
	public static boolean summonOne(Level level, Player player, ItemStack stack, int index) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		List<CompoundTag> souls = SoulData.getSouls(stack);
		if (index < 0 || index >= souls.size()) {
			return false;
		}
		String typeId = SoulData.getTypeId(souls.get(index));
		try {
			Entity created = SoulData.createSoulEntity(serverLevel, souls.get(index),
					player.getX() + 1, player.getY() + 1, player.getZ() + 1);
			if (created == null) {
				return false;
			}
			try {
				List<CompoundTag> cur = SoulData.getSouls(stack);
				if (index >= 0 && index < cur.size()) {
					int lv = SoulData.getLevel(cur.get(index));
					created.addTag("soullevel-" + Math.max(1, Math.min(3, lv)));
					SoulData.applyBaby(created, cur.get(index));
				}
			} catch (Exception ignored) {
			}
			SoulAlly.makeAlly(serverLevel, created, player.getUUID());
			serverLevel.addFreshEntityWithPassengers(created);
			souls.remove(index);
			SoulData.setSouls(stack, souls);
			PartnerCapsuleItem.syncCapsule(player);
			msg(player, Component.literal("영혼 1기를 소환했다!"), false);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		int c = SoulData.count(stack);
		if (c == 0) {
			lines.accept(Component.literal("비어있음 - 불길의 검으로 처치 시 영혼 저장"));
		} else {
			lines.accept(Component.literal("영혼 " + c + "기 보관중"));
			java.util.Map<String, Integer> byType = new java.util.LinkedHashMap<>();
			for (CompoundTag s : SoulData.getSouls(stack)) {
				String t = SoulData.getTypeId(s);
				String shortType = t.contains(":") ? t.substring(t.indexOf(':') + 1) : t;
				byType.put(shortType, byType.getOrDefault(shortType, 0) + 1);
			}
			int shown = 0;
			for (var e : byType.entrySet()) {
				if (shown >= 4) {
					break;
				}
				lines.accept(Component.literal(" - " + e.getKey() + " x" + e.getValue()));
				shown++;
			}
		}
		lines.accept(Component.literal("우클릭: 아군 소울로 소환 (날 위해 싸움)"));
		lines.accept(Component.literal("내 소울에 우클릭 or 웅크리기+우클릭: 다시 보관"));
		lines.accept(Component.literal("H키: 밖에 있으면 저장, 없으면 소환"));
	}
}
