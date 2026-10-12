package com.partnercap.soul;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 소울 캡슐 영혼 저장 (CUSTOM_DATA 안의 {"soul_cap": {"souls": [...]}}).
 * 각 영혼 = {SoulType: "minecraft:zombie", id: "minecraft:zombie"} (신선 소환용 최소 정보).
 */
public class SoulData {
	public static final String ROOT_KEY = "soul_cap";
	public static final String SOULS_KEY = "souls";

	public static final String SOUL_TYPE = "SoulType";
	public static final String SOUL_LEVEL = "SoulLevel";
	public static final String SOUL_BABY = "SoulBaby";
	public static final int MAX_LEVEL = 3;

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

	public static List<CompoundTag> getSouls(ItemStack stack) {
		List<CompoundTag> out = new ArrayList<>();
		CompoundTag cap = getRoot(stack);
		if (!cap.contains(SOULS_KEY)) {
			return out;
		}
		ListTag list = cap.getList(SOULS_KEY).orElse(new ListTag());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i).orElse(null);
			if (t != null) {
				out.add(t.copy());
			}
		}
		return out;
	}

	public static void setSouls(ItemStack stack, List<CompoundTag> souls) {
		CompoundTag cap = getRoot(stack);
		ListTag list = new ListTag();
		for (CompoundTag soul : souls) {
			list.add(soul.copy());
		}
		cap.put(SOULS_KEY, list);
		setRoot(stack, cap);
	}

	public static void addSoul(ItemStack stack, CompoundTag soul) {
		List<CompoundTag> cur = getSouls(stack);
		cur.add(soul.copy());
		setSouls(stack, cur);
	}

	public static void clearSouls(ItemStack stack) {
		setSouls(stack, new ArrayList<>());
	}

	public static int count(ItemStack stack) {
		return getSouls(stack).size();
	}

	public static boolean isEmpty(ItemStack stack) {
		return count(stack) == 0;
	}

	public static boolean getAutoOut(ItemStack stack) {
		try {
			return getRoot(stack).getBoolean("autoOut").orElse(false);
		} catch (Exception e) {
			return false;
		}
	}

	public static void setAutoOut(ItemStack stack, boolean v) {
		try {
			CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag root = custom.copyTag();
			CompoundTag cap = root.getCompound(ROOT_KEY).orElse(new CompoundTag());
			cap.putBoolean("autoOut", v);
			root.put(ROOT_KEY, cap);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
		} catch (Exception ignored) {
		}
	}

	public static String getTypeId(CompoundTag soul) {
		String v = soul.getString(SOUL_TYPE).orElse("");
		if (!v.isEmpty()) {
			return v;
		}
		return soul.getString("id").orElse("unknown");
	}

	public static int getLevel(CompoundTag soul) {
		try {
			return Math.max(1, Math.min(MAX_LEVEL, soul.getInt(SOUL_LEVEL).orElse(1)));
		} catch (Exception e) {
			return 1;
		}
	}

	public static void setLevel(CompoundTag soul, int level) {
		try {
			soul.putInt(SOUL_LEVEL, Math.max(1, Math.min(MAX_LEVEL, level)));
		} catch (Exception ignored) {
		}
	}

	/** 살아있는 소울의 엔티티 태그에서 레벨 읽기 (회수 시 레벨 보존용) */
	public static int levelOf(net.minecraft.world.entity.Entity e) {
		try {
			for (String t : e.entityTags()) {
				if (t.startsWith("soullevel-")) {
					try {
						return Math.max(1, Math.min(MAX_LEVEL, Integer.parseInt(t.substring(11))));
					} catch (Exception ignored) {
					}
					break;
				}
			}
		} catch (Exception ignored) {
		}
		return 1;
	}

	/** 새끼 여부 읽기 (좀비 등 AgeableMob이 아닌 경우 포함) */
	public static boolean babyOf(net.minecraft.world.entity.LivingEntity e) {
		try {
			if (e instanceof net.minecraft.world.entity.AgeableMob age) {
				return age.isBaby();
			}
		} catch (Exception ignored) {
		}
		try {
			if (e instanceof net.minecraft.world.entity.monster.zombie.Zombie z) {
				return z.isBaby();
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/** 포획/회수 시 저장 태그 생성 (타입+레벨+새끼+전수 스냅샷) */
	public static CompoundTag captureFrom(net.minecraft.world.entity.LivingEntity e, String typeId) {
		CompoundTag soul = new CompoundTag();
		soul.putString(SOUL_TYPE, typeId);
		soul.putString("id", typeId);
		int lv = levelOf(e);
		setLevel(soul, lv);
		try {
			soul.putBoolean(SOUL_BABY, babyOf(e));
		} catch (Exception ignored) {
		}
		// 전수 스냅샷: 길들임·안장·갑옷·체력·이름 그대로 보존 (파트너 캡슐과 동일 방식)
		try {
			CompoundTag snap = com.partnercap.util.TameUtil.savePet(e);
			if (snap != null && snap.contains("id")) {
				soul.put("SoulSnap", snap);
			}
		} catch (Exception ignored) {
		}
		try {
			com.partnercap.PartnerCapMod.LOGGER.info(
					"[SoulCap] capture type=" + typeId + " lv=" + lv + " baby=" + getBaby(soul));
		} catch (Exception ignored) {
		}
		return soul;
	}

	public static boolean getBaby(CompoundTag soul) {
		try {
			return soul.getBoolean(SOUL_BABY).orElse(false);
		} catch (Exception e) {
			return false;
		}
	}

	/** 소환 시 새끼 복원 (구버전 최소 태그용 폴백; 스냅샷이 있으면 이미 새끼) */
	public static void applyBaby(net.minecraft.world.entity.Entity created, CompoundTag soul) {
		try {
			if (!getBaby(soul)) {
				return;
			}
			if (created instanceof net.minecraft.world.entity.AgeableMob age) {
				try {
					age.setAge(-24000);
				} catch (Exception ignored) {
				}
				return;
			}
			if (created instanceof net.minecraft.world.entity.monster.zombie.Zombie z) {
				try {
					z.setBaby(true);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 소울 태그에서 엔티티 생성 (전수 스냅샷 우선, 없으면 타입만으로 신선 스폰).
	 * 위치 지정 + 체력 0 이하(시체 흡수분) 풀피 보정까지 수행. 소환·동맹화·스폰은 호출자 몫.
	 */
	public static net.minecraft.world.entity.Entity createSoulEntity(
			net.minecraft.server.level.ServerLevel level, CompoundTag soul, double x, double y, double z) {
		try {
			CompoundTag tag = null;
			try {
				tag = soul.getCompound("SoulSnap").orElse(null);
			} catch (Exception ignored) {
			}
			if (tag == null || !tag.contains("id")) {
				tag = new CompoundTag();
				tag.putString("id", getTypeId(soul));
			}
			net.minecraft.world.entity.EntitySpawnRequest request = new net.minecraft.world.entity.EntitySpawnRequest(
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, true);
			net.minecraft.world.entity.Entity created = net.minecraft.world.entity.EntityType.loadEntityRecursive(
					tag.copy(), level, request, net.minecraft.world.entity.EntityProcessor.NOP);
			if (created == null) {
				return null;
			}
			try {
				created.setPos(x, y, z);
			} catch (Exception ignored) {
			}
			try {
				if (created instanceof net.minecraft.world.entity.LivingEntity living
						&& living.getHealth() <= 0f) {
					try {
						living.setHealth(living.getMaxHealth());
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				com.partnercap.PartnerCapMod.LOGGER.info(
						"[SoulCap] summon type=" + getTypeId(soul) + " lv=" + getLevel(soul)
								+ " baby=" + getBaby(soul) + " snap=" + (tag.contains("id") && tag.contains("Health")));
			} catch (Exception ignored) {
			}
			return created;
		} catch (Exception e) {
			return null;
		}
	}
}
