package com.partnercap.titan;

import java.util.List;

import com.partnercap.item.PartnerCapsuleItem;
import com.partnercap.medusa.MedusaHeadItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 타이탄 캡슐: 아이템 1개 수리 + 손상된 모루 복원.
 * - 다른 손 아이템 → 우클릭 장전 → 시간 경과 → 자동 완성 + 인벤토리 반환 + 3분 쿨타임
 * - 모루 블록 우클릭 → 90초 후 멀쩡한 모루로 복원
 */
public class TitanCapsuleItem extends Item {

	public TitanCapsuleItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		try {
			return TitanData.hasStored(stack) || TitanData.hasAnvil(stack);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isDamagedAnvil(BlockState state) {
		try {
			if (state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)) {
				return true;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack cap = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		long now = System.currentTimeMillis();
		long cool = TitanData.getCooldownUntil(cap);
		if (cool > now) {
			try {
				sp.sendSystemMessage(Component.literal("쿨타임 " + ((cool - now) / 1000) + "초 남음!"));
			} catch (Exception ignored) {
			}
			return InteractionResult.FAIL;
		}
		if (TitanData.isRepairing(cap)) {
			long done = TitanData.getDoneAt(cap);
			String label = TitanData.getLabel(cap);
			try {
				if (done > now) {
					sp.sendSystemMessage(Component.literal("수리 중: " + label + " (" + ((done - now) / 1000) + "초 남음)"));
				} else {
					sp.sendSystemMessage(Component.literal("수리 완료: " + label + " (곧 반환됨)"));
				}
			} catch (Exception ignored) {
			}
			return InteractionResult.SUCCESS;
		}
		if (TitanData.hasStored(cap)) {
			withdrawToPlayer(sp, cap);
			return InteractionResult.SUCCESS;
		}
		boolean sneak = false;
		try {
			sneak = player.isCrouching();
		} catch (Exception ignored) {
		}
		if (sneak) {
			return InteractionResult.PASS;
		}
		ItemStack other = ItemStack.EMPTY;
		try {
			other = player.getItemInHand(
					hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
		} catch (Exception ignored) {
		}
		String err = depositOtherHand(sp, cap, other);
		if (err != null) {
			try {
				sp.sendSystemMessage(Component.literal(err));
			} catch (Exception ignored) {
			}
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	/** 다른 손 아이템 맡기기 (GUI 패킷과 공용). 성공 시 null, 실패 시 메시지 */
	public static String depositOtherHand(ServerPlayer sp, ItemStack cap, ItemStack other) {
		long now = System.currentTimeMillis();
		try {
			long cool = TitanData.getCooldownUntil(cap);
			if (cool > now) {
				return "쿨타임 " + ((cool - now) / 1000) + "초 남음!";
			}
			if (TitanData.isRepairing(cap) || TitanData.hasStored(cap) || TitanData.hasAnvil(cap)) {
				return "이미 작업 중! 먼저 끝내라!";
			}
			if (!TitanData.isRepairableTarget(other)) {
				return "닳은 장비/도구, 닳은 메카 캡슐, 미충전 메두사 머리를 인벤토리에!";
			}
			HolderLookup.Provider registries = sp.registryAccess();
			ItemStack one = other.copyWithCount(1);
			other.shrink(1);
			long dur = TitanData.computeDurationMs(one, cap, registries);
			if (dur <= 0) {
				try {
					com.partnercap.util.TameUtil.giveToPlayer(sp, one);
				} catch (Exception ignored) {
				}
				return "수리할 수 없는 아이템!";
			}
			TitanData.writeStored(cap, registries, one);
			TitanData.setDoneAt(cap, now + dur, TitanData.displayName(one));
			PartnerCapsuleItem.syncCapsule(sp);
			try {
				sp.sendSystemMessage(Component.literal("수리 시작: " + TitanData.displayName(one)
						+ " (" + (dur / 1000) + "초 소요)"));
			} catch (Exception ignored) {
			}
			return null;
		} catch (Exception e) {
			return "맡기기 실패!";
		}
	}

	/** GUI 맡기기: 반대 손 → 인벤토리 순으로 수리 대상 탐색 (손들기 불필요) */
	public static String depositFromGui(ServerPlayer sp, ItemStack cap) {
		ItemStack other = ItemStack.EMPTY;
		try {
			boolean capMain = sp.getMainHandItem() == cap;
			other = capMain ? sp.getOffhandItem()
					: (sp.getOffhandItem() == cap ? sp.getMainHandItem() : sp.getOffhandItem());
		} catch (Exception ignored) {
		}
		try {
			if (!TitanData.isRepairableTarget(other)) {
				for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
					ItemStack s = sp.getInventory().getItem(i);
					if (!s.isEmpty() && s != cap && TitanData.isRepairableTarget(s)) {
						other = s;
						break;
					}
				}
			}
		} catch (Exception ignored) {
		}
		return depositOtherHand(sp, cap, other);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		ItemStack cap = context.getItemInHand();
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		BlockState state;
		try {
			state = level.getBlockState(context.getClickedPos());
		} catch (Exception e) {
			return InteractionResult.PASS;
		}
		if (!isDamagedAnvil(state)) {
			return InteractionResult.PASS;
		}
		long now = System.currentTimeMillis();
		long cool = TitanData.getCooldownUntil(cap);
		if (cool > now) {
			try {
				sp.sendSystemMessage(Component.literal("쿨타임 " + ((cool - now) / 1000) + "초 남음!"));
			} catch (Exception ignored) {
			}
			return InteractionResult.FAIL;
		}
		if (TitanData.hasStored(cap) || TitanData.hasAnvil(cap)) {
			try {
				sp.sendSystemMessage(Component.literal("이미 작업 중! 먼저 끝내라!"));
			} catch (Exception ignored) {
			}
			return InteractionResult.FAIL;
		}
		BlockPos pos = context.getClickedPos();
		long anvilDur = TitanData.ANVIL_DURATION_MS;
		try {
			double f = TitanData.repairTimeFactor(cap, sp.registryAccess());
			anvilDur = Math.max(1000L, (long) (TitanData.ANVIL_DURATION_MS * f));
		} catch (Exception ignored) {
		}
		try {
			String dim = sp.level().dimension().identifier().toString();
			TitanData.setAnvil(cap, dim, pos.getX(), pos.getY(), pos.getZ());
			TitanData.setDoneAt(cap, now + anvilDur, "손상된 모루");
		} catch (Exception e) {
			return InteractionResult.FAIL;
		}
		PartnerCapsuleItem.syncCapsule(sp);
		try {
			sp.sendSystemMessage(Component.literal("모루 수리 시작! (" + (anvilDur / 1000) + "초 소요)"));
		} catch (Exception ignored) {
		}
		return InteractionResult.SUCCESS;
	}

	public static void withdrawToPlayer(ServerPlayer sp, ItemStack cap) {
		HolderLookup.Provider registries;
		try {
			registries = sp.registryAccess();
		} catch (Exception e) {
			return;
		}
		ItemStack stored = TitanData.readStored(cap, registries);
		TitanData.clearStored(cap);
		TitanData.clearAnvil(cap);
		if (!stored.isEmpty()) {
			try {
				com.partnercap.util.TameUtil.giveToPlayer(sp, stored);
			} catch (Exception ignored) {
			}
		}
		PartnerCapsuleItem.syncCapsule(sp);
		try {
			sp.sendSystemMessage(Component.literal("꺼냈다!"));
		} catch (Exception ignored) {
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity,
			net.minecraft.world.entity.EquipmentSlot slot) {
		try {
			super.inventoryTick(stack, level, entity, slot);
		} catch (Exception ignored) {
		}
		try {
			if (!(entity instanceof ServerPlayer sp)) {
				return;
			}
			if (!TitanData.isRepairing(stack)) {
				return;
			}
			long now = System.currentTimeMillis();
			long done = TitanData.getDoneAt(stack);
			if (done > now) {
				try {
					if (entity.tickCount % 60 == 0) {
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
								entity.getX(), entity.getY() + 1, entity.getZ(),
								4, 0.4, 0.6, 0.4, 0.1);
					}
				} catch (Exception ignored) {
				}
				return;
			}
			finishRepair(sp, stack);
		} catch (Exception ignored) {
		}
	}

	private static void finishRepair(ServerPlayer sp, ItemStack cap) {
		long now = System.currentTimeMillis();
		HolderLookup.Provider registries;
		try {
			registries = sp.registryAccess();
		} catch (Exception e) {
			return;
		}
		// 모루 복원
		if (TitanData.hasAnvil(cap)) {
			String dim = "";
			int x = 0;
			int y = -9999;
			int z = 0;
			try {
				net.minecraft.nbt.CompoundTag root = cap
						.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
								net.minecraft.world.item.component.CustomData.EMPTY)
						.copyTag().getCompound(TitanData.ROOT_KEY).orElse(new net.minecraft.nbt.CompoundTag());
				dim = root.getString("anvilDim").orElse("");
				x = root.getInt("anvilX").orElse(0);
				y = root.getInt("anvilY").orElse(-9999);
				z = root.getInt("anvilZ").orElse(0);
			} catch (Exception ignored) {
			}
			boolean restored = false;
			if (y > -9999) {
				try {
					for (ServerLevel sl : sp.level().getServer().getAllLevels()) {
						boolean match = false;
						try {
							match = sl.dimension().identifier().toString().equals(dim);
						} catch (Exception ignored) {
						}
						if (!match) {
							continue;
						}
						BlockPos pos = new BlockPos(x, y, z);
						BlockState cur;
						try {
							cur = sl.getBlockState(pos);
						} catch (Exception e) {
							continue;
						}
						if (!isDamagedAnvil(cur)) {
							continue;
						}
						BlockState fresh = Blocks.ANVIL.defaultBlockState();
						try {
							fresh = fresh.setValue(AnvilBlock.FACING, cur.getValue(AnvilBlock.FACING));
						} catch (Exception ignored) {
						}
						try {
							sl.setBlock(pos, fresh, 3);
							restored = true;
						} catch (Exception ignored) {
						}
						break;
					}
				} catch (Exception ignored) {
				}
			}
			TitanData.clearStored(cap);
			TitanData.clearAnvil(cap);
			try {
				TitanData.setCooldownUntil(cap, now + TitanData.cooldownMs(cap, sp.registryAccess()));
			} catch (Exception ignored) {
				TitanData.setCooldownUntil(cap, now + TitanData.COOLDOWN_MS);
			}
			PartnerCapsuleItem.syncCapsule(sp);
			try {
				sp.sendSystemMessage(Component.literal(restored ? "모루 수리 완료! (쿨타임 3분)" : "모루를 찾을 수 없음! (쿨타임 없음)"));
				if (restored) {
					sp.level().playSound(null, sp.blockPosition(),
							net.minecraft.sounds.SoundEvents.ANVIL_USE,
							SoundSource.PLAYERS, 1.0f, 1.0f);
				}
			} catch (Exception ignored) {
			}
			if (!restored) {
				try {
					TitanData.setCooldownUntil(cap, 0L);
					PartnerCapsuleItem.syncCapsule(sp);
				} catch (Exception ignored) {
				}
			}
			return;
		}
		// 아이템 수리
		ItemStack stored = TitanData.readStored(cap, registries);
		if (stored.isEmpty()) {
			TitanData.clearStored(cap);
			PartnerCapsuleItem.syncCapsule(sp);
			return;
		}
		try {
			if (stored.getItem() instanceof MedusaHeadItem) {
				MedusaHeadItem.setCharged(stored, true);
			} else {
				try {
					stored.setDamageValue(0);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		String label = TitanData.getLabel(cap);
		TitanData.clearStored(cap);
		try {
			TitanData.setCooldownUntil(cap, now + TitanData.cooldownMs(cap, sp.registryAccess()));
		} catch (Exception ignored) {
			TitanData.setCooldownUntil(cap, now + TitanData.COOLDOWN_MS);
		}
		try {
			com.partnercap.util.TameUtil.giveToPlayer(sp, stored);
		} catch (Exception ignored) {
		}
		PartnerCapsuleItem.syncCapsule(sp);
		try {
			sp.sendSystemMessage(Component.literal("수리 완료: " + label + "! (쿨타임 3분)"));
			sp.level().playSound(null, sp.blockPosition(),
					net.minecraft.sounds.SoundEvents.ANVIL_USE,
					SoundSource.PLAYERS, 1.0f, 1.2f);
		} catch (Exception ignored) {
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		try {
			for (String s : TitanData.stateLines(stack)) {
				lines.accept(Component.literal(s));
			}
		} catch (Exception ignored) {
		}
		lines.accept(Component.literal("다른 손 아이템 + 우클릭: 수리 맡기기 (1개)"));
		lines.accept(Component.literal("손상된 모루 우클릭: 모루 복원"));
		lines.accept(Component.literal("완료 후 3분 쿨타임"));
		lines.accept(Component.literal("효율: 수리 시간 단축 · 빠른 장전: 쿨타임 단축"));
	}
}
