package com.partnercap.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.partnercap.item.DisassembleCapsuleItem;
import com.partnercap.item.GambleCapsuleItem;
import com.partnercap.item.PartnerCapsuleItem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * 도박 타이머 처리 + 분해 역조합 해석.
 */
public class GambleLogic {

	public static void register() {
		try {
			ServerTickEvents.END_SERVER_TICK.register(GambleLogic::tickGamble);
		} catch (Exception ignored) {
		}
	}

	private static void tickGamble(MinecraftServer server) {
		try {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			long now = System.currentTimeMillis();
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				try {
					for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
						ItemStack cap = p.getInventory().getItem(i);
						if (cap.isEmpty() || !(cap.getItem() instanceof GambleCapsuleItem)) {
							continue;
						}
						long until;
						try {
							until = GambleCapsuleItem.getUntil(cap);
						} catch (Exception e) {
							continue;
						}
						if (until <= 0 || now < until) {
							continue;
						}
						resolveGamble(p, cap);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static void resolveGamble(ServerPlayer player, ItemStack cap) {
		HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		ItemStack bet = GambleCapsuleItem.readBet(cap, registries);
		GambleCapsuleItem.clearBet(cap);
		PartnerCapsuleItem.syncCapsule(player);
		if (bet.isEmpty()) {
			tell(player, "베팅이 소실되었습니다.", true);
			return;
		}
		// 인챈트 연동: 행운 Lv당 성공률 +10% (기본 50%), 약탈 Lv당 배율 +1 (기본 2배)
		int fortune = 0;
		int looting = 0;
		try {
			fortune = EnchantLevels.fortune(registries, cap);
			looting = EnchantLevels.looting(registries, cap);
		} catch (Exception ignored) {
		}
		double chance = 0.5 + 0.1 * fortune;
		int mult = 2 + looting;
		boolean win = Math.random() < chance;
		String betName = ItemNbt.displayName(bet);
		if (win) {
			int total = bet.getCount() * mult;
			int max;
			try {
				max = bet.getMaxStackSize();
			} catch (Exception e) {
				max = 64;
			}
			if (max <= 0) {
				max = 64;
			}
			while (total > 0) {
				int n = Math.min(total, max);
				ItemStack prize = bet.copy();
				try {
					prize.setCount(n);
				} catch (Exception e) {
					break;
				}
				TameUtil.giveToPlayer(player, prize);
				total -= n;
			}
			tell(player, "대박! " + betName + " " + mult + "배 획득!", false);
			try {
				player.level().playSound(null, player.blockPosition(),
						net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
						net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
			} catch (Exception ignored) {
			}
		} else {
			tell(player, "꽝! " + betName + " 소멸...", false);
			try {
				player.level().playSound(null, player.blockPosition(),
						net.minecraft.sounds.SoundEvents.VILLAGER_NO,
						net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f);
			} catch (Exception ignored) {
			}
		}
	}

	private static void tell(Player player, String text, boolean overlay) {
		try {
			Component c = Component.literal(text);
			if (player instanceof ServerPlayer sp) {
				if (overlay) {
					sp.sendOverlayMessage(c);
				} else {
					sp.sendSystemMessage(c);
				}
			} else {
				if (overlay) {
					player.sendOverlayMessage(c);
				} else {
					player.sendSystemMessage(c);
				}
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 제작법 역추적: 입력 아이템을 만드는 shaped/shapeless 조합법의 재료 (각 1개씩).
	 * 없으면 빈 리스트.
	 */
	public static List<ItemStack> findReverse(Level level, ItemStack input) {
		List<ItemStack> out = new ArrayList<>();
		if (input == null || input.isEmpty() || level == null) {
			return out;
		}
		try {
			if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
				return out;
			}
			ContextMap ctx = ContextMap.builder().build();
			for (Object o : sl.recipeAccess().getRecipes()) {
				if (!(o instanceof RecipeHolder<?> holder)) {
					continue;
				}
				Recipe<?> recipe;
				try {
					recipe = holder.value();
				} catch (Exception e) {
					continue;
				}
				if (recipe == null) {
					continue;
				}
				List<?> displays;
				try {
					displays = recipe.display();
				} catch (Exception e) {
					continue;
				}
				if (displays == null) {
					continue;
				}
				for (Object d : displays) {
					List<?> ings = null;
					SlotDisplay res = null;
					try {
						if (d instanceof ShapedCraftingRecipeDisplay sd) {
							ings = sd.ingredients();
							res = sd.result();
						} else if (d instanceof ShapelessCraftingRecipeDisplay sd) {
							ings = sd.ingredients();
							res = sd.result();
						} else {
							continue;
						}
					} catch (Exception e) {
						continue;
					}
					ItemStack result;
					try {
						result = res.resolveForFirstStack(ctx);
					} catch (Exception e) {
						continue;
					}
					if (result == null || result.isEmpty()) {
						continue;
					}
					boolean match = false;
					try {
						match = result.is(input.getItem());
					} catch (Exception e) {
						continue;
					}
					if (!match) {
						continue;
					}
					for (Object io : ings) {
						if (!(io instanceof SlotDisplay s)) {
							continue;
						}
						ItemStack one;
						try {
							one = s.resolveForFirstStack(ctx);
						} catch (Exception e) {
							continue;
						}
						if (one == null || one.isEmpty()) {
							continue;
						}
						ItemStack give = one.copy();
						try {
							give.setCount(1);
						} catch (Exception e) {
							continue;
						}
						out.add(give);
					}
					return out;
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	/** 분해 실행: 1개 소모 → 재료 지급. 불가 사유 메시지도 반환. */
	public static String disassemble(ServerPlayer player, ItemStack cap) {
		HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return "분해 실패";
		}
		ItemStack stored = DisassembleCapsuleItem.readStored(cap, registries);
		if (stored.isEmpty()) {
			return "슬롯이 비어있습니다.";
		}
		boolean damaged = false;
		try {
			damaged = stored.isDamaged();
		} catch (Exception ignored) {
		}
		if (damaged) {
			return "닳은 도구는 분해 불가!";
		}
		List<ItemStack> outs;
		try {
			outs = findReverse(player.level(), stored);
		} catch (Exception e) {
			return "분해 실패";
		}
		if (outs.isEmpty()) {
			return "분해 불가 (조합법 없음)";
		}
		try {
			if (stored.getCount() <= 1) {
				DisassembleCapsuleItem.clearStored(cap);
			} else {
				stored.shrink(1);
				DisassembleCapsuleItem.setStored(cap, registries, stored);
			}
		} catch (Exception e) {
			return "분해 실패";
		}
		StringBuilder sb = new StringBuilder("분해: ");
		for (ItemStack o : outs) {
			try {
				TameUtil.giveToPlayer(player, o.copy());
				sb.append(ItemNbt.displayName(o)).append(' ');
			} catch (Exception ignored) {
			}
		}
		PartnerCapsuleItem.syncCapsule(player);
		return sb.toString().trim();
	}
}
