package com.partnercap.titan;

import java.util.ArrayList;
import java.util.List;

import com.partnercap.medusa.MedusaHeadItem;
import com.partnercap.util.ItemNbt;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 타이탄 캡슐 저장소 (CUSTOM_DATA 안의 {"titan_cap": {...}}).
 * stored: 수리 맡긴 아이템 1개 / doneAt: 완료 시각(ms)
 * anvil*: 모루 수리 대기 (차원+좌표) / coolUntil: 쿨타임 종료 시각(ms)
 */
public class TitanData {

	public static final String ROOT_KEY = "titan_cap";
	public static final long COOLDOWN_MS = 180000L;
	public static final long ANVIL_DURATION_MS = 90000L;
	public static final long HEAD_DURATION_MS = 75000L;

	/** 아이템에 직접 붙는 점진 수리 작업 태그 */
	public static final String REPAIR_KEY = "titan_repair";

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
			if (!cap.contains("stored")) {
				return false;
			}
			return !cap.getCompound("stored").orElse(new CompoundTag()).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public static ItemStack readStored(ItemStack stack, HolderLookup.Provider registries) {
		try {
			CompoundTag cap = getRoot(stack);
			CompoundTag bet = cap.getCompound("stored").orElse(null);
			if (bet == null) {
				return ItemStack.EMPTY;
			}
			return ItemNbt.read(registries, bet);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public static void writeStored(ItemStack stack, HolderLookup.Provider registries, ItemStack in) {
		CompoundTag cap = getRoot(stack);
		cap.put("stored", ItemNbt.write(registries, in));
		setRoot(stack, cap);
	}

	public static void clearStored(ItemStack stack) {
		try {
			CompoundTag cap = getRoot(stack);
			cap.remove("stored");
			cap.remove("doneAt");
			cap.remove("label");
			setRoot(stack, cap);
		} catch (Exception ignored) {
		}
	}

	public static long getDoneAt(ItemStack stack) {
		try {
			return getRoot(stack).getLong("doneAt").orElse(0L);
		} catch (Exception e) {
			return 0L;
		}
	}

	public static void setDoneAt(ItemStack stack, long v, String label) {
		try {
			CompoundTag cap = getRoot(stack);
			cap.putLong("doneAt", v);
			cap.putString("label", label == null ? "" : label);
			setRoot(stack, cap);
		} catch (Exception ignored) {
		}
	}

	public static String getLabel(ItemStack stack) {
		try {
			return getRoot(stack).getString("label").orElse("");
		} catch (Exception e) {
			return "";
		}
	}

	public static boolean isRepairing(ItemStack stack) {
		return hasStored(stack) && getDoneAt(stack) > 0;
	}

	public static long getCooldownUntil(ItemStack stack) {
		try {
			return getRoot(stack).getLong("coolUntil").orElse(0L);
		} catch (Exception e) {
			return 0L;
		}
	}

	public static void setCooldownUntil(ItemStack stack, long v) {
		try {
			CompoundTag cap = getRoot(stack);
			cap.putLong("coolUntil", v);
			setRoot(stack, cap);
		} catch (Exception ignored) {
		}
	}

	private static CompoundTag getRepairRoot(ItemStack stack) {
		try {
			CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag root = custom.copyTag();
			if (!root.contains(REPAIR_KEY)) {
				return new CompoundTag();
			}
			return root.getCompound(REPAIR_KEY).orElse(new CompoundTag());
		} catch (Exception e) {
			return new CompoundTag();
		}
	}

	private static void setRepairRoot(ItemStack stack, CompoundTag job) {
		try {
			CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag root = custom.copyTag();
			root.put(REPAIR_KEY, job);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
		} catch (Exception ignored) {
		}
	}

	public static boolean isRepairingItem(ItemStack stack) {
		try {
			return stack != null && !stack.isEmpty() && getRepairRoot(stack).contains("doneAt");
		} catch (Exception e) {
			return false;
		}
	}

	public static long getRepairDoneAt(ItemStack stack) {
		try {
			return getRepairRoot(stack).getLong("doneAt").orElse(0L);
		} catch (Exception e) {
			return 0L;
		}
	}

	public static long getRepairDur(ItemStack stack) {
		try {
			return getRepairRoot(stack).getLong("dur").orElse(0L);
		} catch (Exception e) {
			return 0L;
		}
	}

	/** 점진 수리 시작 (아이템은 인벤토리에 그대로, 내구도가 시간에 따라 참) */
	public static void startRepair(ItemStack stack, long durMs) {
		try {
			long now = System.currentTimeMillis();
			int total = 0;
			try {
				total = stack.getDamageValue();
			} catch (Exception ignored) {
			}
			CompoundTag job = new CompoundTag();
			job.putLong("doneAt", now + durMs);
			job.putLong("dur", Math.max(1L, durMs));
			job.putInt("total", Math.max(0, total));
			setRepairRoot(stack, job);
		} catch (Exception ignored) {
		}
	}

	public static void clearRepair(ItemStack stack) {
		try {
			CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag root = custom.copyTag();
			root.remove(REPAIR_KEY);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
		} catch (Exception ignored) {
		}
	}

	/** 해당 플레이어 인벤토리에서 수리 중인 아이템 탐색 */
	public static ItemStack findRepairItem(net.minecraft.server.level.ServerPlayer player) {
		try {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (isRepairingItem(s)) {
					return s;
				}
			}
		} catch (Exception ignored) {
		}
		return ItemStack.EMPTY;
	}

	/** 점진 수리 틱 (1초마다 호출): 내구도 갱신 + 완료 처리 */
	public static void tickRepair(net.minecraft.server.MinecraftServer server) {
		try {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				tickRepairPlayer(p);
			}
		} catch (Exception ignored) {
		}
	}

	private static void tickRepairPlayer(ServerPlayer p) {
		ItemStack job = ItemStack.EMPTY;
		try {
			job = findRepairItem(p);
		} catch (Exception ignored) {
		}
		if (job.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		long doneAt = 0L;
		long dur = 1L;
		int total = 0;
		try {
			doneAt = getRepairDoneAt(job);
			dur = Math.max(1L, getRepairDur(job));
			total = getRepairRoot(job).getInt("total").orElse(0);
		} catch (Exception ignored) {
		}
		// 메두사 머리는 시간이 되면 충전
		boolean isHead = false;
		try {
			isHead = job.getItem() instanceof MedusaHeadItem;
		} catch (Exception ignored) {
		}
		if (isHead) {
			if (now >= doneAt) {
				try {
					MedusaHeadItem.setCharged(job, true);
				} catch (Exception ignored) {
				}
				clearRepair(job);
				finishCooldown(p);
				try {
					p.sendSystemMessage(Component.literal("메두사의 머리 충전 완료!"));
				} catch (Exception ignored) {
				}
			}
			return;
		}
		int cur = 0;
		int max = 0;
		try {
			cur = job.getDamageValue();
			max = job.getMaxDamage();
		} catch (Exception ignored) {
		}
		if (max <= 1) {
			clearRepair(job);
			return;
		}
		if (now >= doneAt || cur <= 0) {
			try {
				job.setDamageValue(0);
			} catch (Exception ignored) {
			}
			clearRepair(job);
			finishCooldown(p);
			try {
				p.sendSystemMessage(Component.literal("수리 완료: " + displayName(job) + "! (쿨타임 3분)"));
			} catch (Exception ignored) {
			}
			return;
		}
		double frac = Math.max(0.0, Math.min(1.0, (doneAt - now) / (double) dur));
		int target = (int) Math.round(total * frac);
		try {
			if (target < cur) {
				job.setDamageValue(Math.max(0, target));
			}
		} catch (Exception ignored) {
		}
	}

	private static void finishCooldown(ServerPlayer p) {
		try {
			ItemStack cap = findTitanCap(p);
			if (cap.isEmpty()) {
				return;
			}
			long cd = COOLDOWN_MS;
			try {
				cd = cooldownMs(cap, p.registryAccess());
			} catch (Exception ignored) {
			}
			setCooldownUntil(cap, System.currentTimeMillis() + cd);
		} catch (Exception ignored) {
		}
	}

	/** 인벤토리 첫 타이탄 캡슐 (쿨타임·인챈트 조회용) */
	public static ItemStack findTitanCap(ServerPlayer p) {
		try {
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				ItemStack s = p.getInventory().getItem(i);
				try {
					if (!s.isEmpty() && s.getItem() instanceof TitanCapsuleItem) {
						return s;
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return ItemStack.EMPTY;
	}

	/** 효율 인챈트: 수리 시간 배율 (Lv당 -10%, 최대 -50%) */
	public static double repairTimeFactor(ItemStack titanCap, HolderLookup.Provider registries) {
		try {
			int eff = com.partnercap.util.EnchantLevels.efficiency(registries, titanCap);
			return Math.max(0.1, 1.0 - 0.1 * eff);
		} catch (Exception e) {
			return 1.0;
		}
	}

	/** 빠른 장전 인챈트 적용 쿨타임 (Lv당 -15%, 최대 -45%) */
	public static long cooldownMs(ItemStack titanCap, HolderLookup.Provider registries) {
		try {
			int qc = com.partnercap.util.EnchantLevels.quickCharge(registries, titanCap);
			double f = Math.max(0.1, 1.0 - 0.15 * qc);
			return Math.max(1000L, (long) (COOLDOWN_MS * f));
		} catch (Exception e) {
			return COOLDOWN_MS;
		}
	}

	public static boolean hasAnvil(ItemStack stack) {
		try {
			return getRoot(stack).contains("anvilDim");
		} catch (Exception e) {
			return false;
		}
	}

	public static void setAnvil(ItemStack stack, String dim, int x, int y, int z) {
		try {
			CompoundTag cap = getRoot(stack);
			cap.putString("anvilDim", dim);
			cap.putInt("anvilX", x);
			cap.putInt("anvilY", y);
			cap.putInt("anvilZ", z);
			setRoot(stack, cap);
		} catch (Exception ignored) {
		}
	}

	public static void clearAnvil(ItemStack stack) {
		try {
			CompoundTag cap = getRoot(stack);
			cap.remove("anvilDim");
			cap.remove("anvilX");
			cap.remove("anvilY");
			cap.remove("anvilZ");
			setRoot(stack, cap);
		} catch (Exception ignored) {
		}
	}

	/** 수리 시간: 등급(최대내구도+인챈트) × 손상도. 메두사 머리는 고정 75초. 타이탄 캡슐 효율 인챈트로 단축 */
	public static long computeDurationMs(ItemStack stack, ItemStack titanCap, HolderLookup.Provider registries) {
		try {
			double factor = 1.0;
			try {
				factor = repairTimeFactor(titanCap, registries);
			} catch (Exception ignored) {
			}
			if (stack.getItem() instanceof MedusaHeadItem) {
				return Math.max(1000L, (long) (HEAD_DURATION_MS * factor));
			}
			int max = 0;
			int dmg = 0;
			try {
				max = stack.getMaxDamage();
				dmg = stack.getDamageValue();
			} catch (Exception ignored) {
			}
			if (max <= 1 || dmg <= 0) {
				return 0L;
			}
			double frac = Math.max(0.0, Math.min(1.0, dmg / (double) max));
			double base = Math.max(6.0, Math.min(150.0, max / 16.0));
			double ench = 0;
			try {
				if (stack.isEnchanted()) {
					ench = 8.0 * Math.min(10, stack.getEnchantments().size());
				}
			} catch (Exception ignored) {
			}
			double secs = (base + ench) * (0.35 + 0.65 * frac);
			secs = Math.max(5.0, Math.min(240.0, secs));
			return Math.max(1000L, (long) (secs * 1000.0 * factor));
		} catch (Exception e) {
			return 30000L;
		}
	}

	/** 구 호환 (효율 미적용) */
	public static long computeDurationMs(ItemStack stack) {
		return computeDurationMs(stack, ItemStack.EMPTY, null);
	}

	public static boolean isRepairableTarget(ItemStack stack) {
		try {
			if (stack == null || stack.isEmpty()) {
				return false;
			}
			if (stack.getItem() instanceof MedusaHeadItem) {
				try {
					return !MedusaHeadItem.isCharged(stack);
				} catch (Exception e) {
					return false;
				}
			}
			// 캡슐류 중 메카 캡슐만 수리 허용 (나머지 캡슐은 명시적으로 제외)
			try {
				if (stack.getItem() instanceof com.partnercap.mech.MechCapsuleItem) {
					return stack.getDamageValue() > 0;
				}
				if (stack.getItem() instanceof com.partnercap.item.PartnerCapsuleItem
						|| stack.getItem() instanceof com.partnercap.item.SoulCapsuleItem
						|| stack.getItem() instanceof com.partnercap.item.GambleCapsuleItem
						|| stack.getItem() instanceof com.partnercap.item.DisassembleCapsuleItem
						|| stack.getItem() instanceof com.partnercap.item.VoidCapsuleItem
						|| stack.getItem() instanceof TitanCapsuleItem) {
					return false;
				}
			} catch (Exception ignored) {
			}
			try {
				if (!stack.isDamageableItem()) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
			try {
				return stack.getDamageValue() > 0;
			} catch (Exception e) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static String displayName(ItemStack stack) {
		try {
			return stack.getHoverName().getString();
		} catch (Exception e) {
			return "?";
		}
	}

	public static List<String> stateLines(ItemStack cap) {
		List<String> out = new ArrayList<>();
		try {
			long now = System.currentTimeMillis();
			long cool = getCooldownUntil(cap);
			if (cool > now) {
				out.add("쿨타임 " + ((cool - now) / 1000) + "초 남음");
				return out;
			}
			if (isRepairing(cap)) {
				long done = getDoneAt(cap);
				String label = getLabel(cap);
				if (done > now) {
					out.add("수리 중: " + label + " (" + ((done - now) / 1000) + "초 남음)");
				} else {
					out.add("수리 완료: " + label + " (꺼내세요)");
				}
				return out;
			}
			if (hasStored(cap)) {
				out.add("보관 중 (꺼내세요)");
				return out;
			}
			out.add("비어있음");
		} catch (Exception ignored) {
		}
		return out;
	}
}
