package com.ultracapsuleadventure.item;

import java.util.ArrayList;
import java.util.List;

import com.ultracapsuleadventure.data.PartnerData;
import com.ultracapsuleadventure.util.TameUtil;

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
 * 보관 캡슐 본체 (MC 26.3).
 * - 길들인 몹 우클릭: 반경 64 내 내 길들임 몹 전부 저장
 * - 빈 곳/블록 우클릭: 저장된 몹 전부 소환
 * - C키: GUI (클라이언트에서 처리)
 */
public class UltraCapsuleAdventureItem extends Item {

	public static final double CAPTURE_RADIUS = 64.0;

	public UltraCapsuleAdventureItem(Properties properties) {
		super(properties);
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

	/** 서버 변경분을 클라이언트에 강제 재동기화 (캡슐 GUI stale 방지) */
	public static void syncCapsule(Player player) {
		if (player instanceof ServerPlayer sp) {
			try {
				sp.inventoryMenu.broadcastChanges();
			} catch (Exception ignored) {
			}
			try {
				sp.inventoryMenu.sendAllDataToRemote();
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!TameUtil.isOwnedBy(entity, player)) {
			return InteractionResult.PASS;
		}
		return tryCapture(player, stack);
	}

	/** 캡슐 캡처 본체 (서버). UseEntityCallback과 아이템 우클릭이 공유. */
	public static InteractionResult tryCapture(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		List<LivingEntity> owned = TameUtil.findOwnedTames(player, CAPTURE_RADIUS);
		if (owned.isEmpty()) {
			msg(player, Component.literal("주변에 저장할 길들인 동물이 없습니다."), true);
			return InteractionResult.FAIL;
		}
		List<CompoundTag> toStore = new ArrayList<>();
		int n = 0;
		for (LivingEntity living : owned) {
			try {
				toStore.add(TameUtil.savePet(living));
				living.discard();
				n++;
			} catch (Exception e) {
				// 개별 실패 무시
			}
		}
		if (toStore.isEmpty()) {
			msg(player, Component.literal("저장 실패!"), true);
			return InteractionResult.FAIL;
		}
		PartnerData.addPets(stack, toStore);
		syncCapsule(player);
		msg(player, Component.literal(n + "마리를 캡슐에 저장했습니다! (C키로 확인)"), false);
		return InteractionResult.SUCCESS;
	}

	/** 허공 우클릭 소환 (26.3: InteractionResult 반환) */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return PartnerData.isEmpty(stack) ? InteractionResult.PASS : InteractionResult.SUCCESS;
		}
		if (PartnerData.isEmpty(stack)) {
			msg(player, Component.literal("캡슐이 비어있습니다. 길들인 동물에 우클릭하세요."), true);
			return InteractionResult.FAIL;
		}
		summonAll(level, player, stack);
		return InteractionResult.SUCCESS;
	}

	/** 블록 우클릭 소환도 지원 */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack stack = context.getItemInHand();
		if (level.isClientSide()) {
			return PartnerData.isEmpty(stack) ? InteractionResult.PASS : InteractionResult.SUCCESS;
		}
		if (PartnerData.isEmpty(stack)) {
			return InteractionResult.PASS;
		}
		summonAll(level, player, stack);
		return InteractionResult.SUCCESS;
	}

	public static void summonAll(Level level, Player player, ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		List<CompoundTag> pets = PartnerData.getPets(stack);
		if (pets.isEmpty()) {
			return;
		}
		EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true);
		int spawned = 0;
		double baseX = player.getX();
		double baseY = player.getY();
		double baseZ = player.getZ();
		for (int i = 0; i < pets.size(); i++) {
			CompoundTag pet = pets.get(i);
			try {
				Entity created = EntityType.loadEntityRecursive(pet, serverLevel, request, EntityProcessor.NOP);
				if (created == null) {
					continue;
				}
				double ox = ((i % 5) - 2) * 1.2 + serverLevel.getRandom().nextDouble() * 0.5;
				double oz = ((i / 5) - 1) * 1.2 + serverLevel.getRandom().nextDouble() * 0.5;
				created.setPos(baseX + ox, baseY + 1, baseZ + oz);
				try {
					created.setYRot(serverLevel.getRandom().nextFloat() * 360f);
				} catch (Exception ignored) {
				}
				TameUtil.applyEquipment(created, pet);
				serverLevel.addFreshEntityWithPassengers(created);
				spawned++;
			} catch (Exception e) {
				// 개별 소환 실패 무시
			}
		}
		PartnerData.clearPets(stack);
		syncCapsule(player);
		msg(player, Component.literal(spawned + "마리를 소환했습니다!"), false);
	}

	/** 개별 소환 (GUI에서 사용) */
	public static boolean summonOne(Level level, Player player, ItemStack stack, int index) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		CompoundTag pet = PartnerData.removePet(stack, index);
		if (pet == null) {
			return false;
		}
		try {
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true);
			Entity created = EntityType.loadEntityRecursive(pet, serverLevel, request, EntityProcessor.NOP);
			if (created == null) {
				PartnerData.addPets(stack, List.of(pet));
				return false;
			}
			created.setPos(player.getX() + 1, player.getY() + 1, player.getZ() + 1);
			TameUtil.applyEquipment(created, pet);
			serverLevel.addFreshEntityWithPassengers(created);
			syncCapsule(player);
			return true;
		} catch (Exception e) {
			PartnerData.addPets(stack, List.of(pet));
			return false;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		int c = PartnerData.count(stack);
		if (c == 0) {
			lines.accept(Component.literal("비어있음 - 길들인 동물에 우클릭 (반경 64 전부 저장)"));
		} else {
			lines.accept(Component.literal("보관중: " + c + "마리"));
			List<CompoundTag> pets = PartnerData.getPets(stack);
			for (int i = 0; i < Math.min(4, pets.size()); i++) {
				CompoundTag p = pets.get(i);
				lines.accept(Component.literal(" - " + PartnerData.getName(p)
						+ " " + String.format("%.1f", PartnerData.getHealth(p))
						+ "/" + String.format("%.1f", PartnerData.getMaxHealth(p))));
			}
			if (pets.size() > 4) {
				lines.accept(Component.literal("... 외 " + (pets.size() - 4) + "마리"));
			}
		}
		lines.accept(Component.literal("우클릭: 모두 소환 / C키: 캡슐 관리창"));
		lines.accept(Component.literal("관리창: 회복·풀회복·이름변경·안장/갑옷·불러내기"));
	}
}



