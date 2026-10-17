package com.ultracapsuleadventure;

import java.util.List;
import java.util.UUID;

import com.ultracapsuleadventure.entity.ModEntityTypes;
import com.ultracapsuleadventure.fluid.ModBlocks;
import com.ultracapsuleadventure.fluid.ModFluids;
import com.ultracapsuleadventure.fluid.ModWorldGen;
import com.ultracapsuleadventure.item.ModItems;
import com.ultracapsuleadventure.item.UltraCapsuleAdventureItem;
import com.ultracapsuleadventure.item.SoulCapsuleItem;
import com.ultracapsuleadventure.item.VoidCapsuleItem;
import com.ultracapsuleadventure.network.ModPackets;
import com.ultracapsuleadventure.soul.SoulAlly;
import com.ultracapsuleadventure.soul.SoulData;
import com.ultracapsuleadventure.soul.SoulEvents;
import com.ultracapsuleadventure.util.GambleLogic;
import com.ultracapsuleadventure.voidcap.VoidErase;
import com.ultracapsuleadventure.util.TameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class UltraCapsuleAdventureMod implements ModInitializer {
	public static final String MOD_ID = "UltraCapsuleAdventure";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[UltraCapsuleAdventure] 보관 캡슐 모드 로딩중... MC 26.3 대응");
		ModFluids.register();
		ModBlocks.register();
		ModEntityTypes.register();
		ModEntityTypes.registerAttributes();
		ModItems.register();
		com.ultracapsuleadventure.silicate.ModSilicate.register();
		com.ultracapsuleadventure.flame.ModFlame.register();
		com.ultracapsuleadventure.doppel.ModDoppel.register();
		com.ultracapsuleadventure.silicate.MeteorEvents.register();
		ModWorldGen.register();
		ModPackets.registerServerReceivers();
		try {
			com.ultracapsuleadventure.soul.SoulBody.register();
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
					.register(com.ultracapsuleadventure.mech.MechRobotEntity::tickThrows);
		} catch (Exception ignored) {
		}
		SoulEvents.register();
		com.ultracapsuleadventure.flame.FlameMongEvents.register();
		com.ultracapsuleadventure.medusa.MedusaEvents.register();
		com.ultracapsuleadventure.medusa.BlindfoldItem.register();
		com.ultracapsuleadventure.mech.MechEvents.register();
		GambleLogic.register();
		VoidErase.register();
		com.ultracapsuleadventure.util.EnchantLevels.register();
		// 타이탄 점진 수리 틱 (1초마다)
		try {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 20 == 0) {
						com.ultracapsuleadventure.titan.TitanData.tickRepair(server);
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}

		// 늑대 앉기/일어서기 같은 바닐라 몹 상호작용보다 먼저 가로채기.
		// 캡슐을 들고 길들인 몹 우클릭 시 앉기 토글이 아니라 캡슐 저장이 실행됨.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack handStack = player.getItemInHand(hand);
			ItemStack otherStack = null;
			try {
				otherStack = player.getItemInHand(
						hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			} catch (Exception ignored) {
			}
			// 석화된 마네킹(플레이어 석상): 우클릭 편집 차단, 곡괭이는 채굴 허용
			try {
				if (entity instanceof net.minecraft.world.entity.decoration.Mannequin
						&& com.ultracapsuleadventure.medusa.Petrify.isPetrified(entity)) {
					if (world.isClientSide()) {
						return InteractionResult.SUCCESS;
					}
					if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp
							&& entity instanceof LivingEntity living) {
						try {
							com.ultracapsuleadventure.medusa.Petrify.tryMinePetrified(sp, living);
						} catch (Exception ignored) {
						}
					}
					return InteractionResult.SUCCESS;
				}
			} catch (Exception ignored) {
			}
			// 소울 캡슐로 내 소울 회수 (어느 손에 들고 있어도 됨)
			ItemStack soulStack = null;
			if (handStack.getItem() instanceof SoulCapsuleItem) {
				soulStack = handStack;
			} else if (otherStack != null && otherStack.getItem() instanceof SoulCapsuleItem) {
				soulStack = otherStack;
			}
			if (soulStack != null) {
				if (!(entity instanceof LivingEntity)) {
					return InteractionResult.PASS;
				}
				UUID soulOwner;
				int tagCount = -1;
				boolean hasGoals = false;
				try {
					soulOwner = SoulAlly.soulOwnerOf(entity);
					try {
						tagCount = entity.entityTags().size();
					} catch (Exception ignored) {
					}
					try {
						if (entity instanceof Mob mobCheck) {
							hasGoals = SoulAlly.hasSoulGoals(mobCheck);
						}
					} catch (Exception ignored) {
					}
				} catch (Exception e) {
					return InteractionResult.PASS;
				}
				try {
				if (com.ultracapsuleadventure.medusa.Petrify.isPetrified(entity)) {
					return InteractionResult.PASS;
				}
			} catch (Exception ignored) {
			}
			if (soulOwner == null || !soulOwner.equals(player.getUUID())) {
					// TEMP 진단: 다음 빌드에서 제거 예정
					if (!world.isClientSide()) {
						try {
							player.sendSystemMessage(Component.literal("[소울진단] 태그수=" + tagCount
									+ " 골=" + hasGoals));
						} catch (Exception ignored) {
						}
					}
					return InteractionResult.PASS;
				}
				if (world.isClientSide()) {
					return InteractionResult.SUCCESS;
				}
				if (hand != InteractionHand.MAIN_HAND) {
					return InteractionResult.PASS;
				}
				return SoulCapsuleItem.tryRecapture(player, soulStack);
			}
			// 공허 캡슐로 엔티티 빨아들이기 (길들임 무관, 플레이어 제외)
			ItemStack voidStack = null;
			if (handStack.getItem() instanceof VoidCapsuleItem) {
				voidStack = handStack;
			} else if (otherStack != null && otherStack.getItem() instanceof VoidCapsuleItem) {
				voidStack = otherStack;
			}
			if (voidStack != null) {
				if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
					return InteractionResult.PASS;
				}
				if (world.isClientSide()) {
					return InteractionResult.SUCCESS;
				}
				if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
					return InteractionResult.PASS;
				}
				boolean ok = com.ultracapsuleadventure.voidcap.VoidErase.capture(sp, voidStack, living);
				return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
			}
			ItemStack capStack = null;
			if (handStack.getItem() instanceof UltraCapsuleAdventureItem) {
				capStack = handStack;
			} else if (otherStack != null && otherStack.getItem() instanceof UltraCapsuleAdventureItem) {
				capStack = otherStack;
			}
			if (capStack == null) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof LivingEntity living)) {
				return InteractionResult.PASS;
			}
			if (!TameUtil.isOwnedBy(living, player)) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide()) {
				// 앉기 토글 선제 차단 (실제 저장은 서버에서)
				return InteractionResult.SUCCESS;
			}
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			return UltraCapsuleAdventureItem.tryCapture(player, capStack);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}



