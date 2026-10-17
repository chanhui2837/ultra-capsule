package com.ultracapsuleadventure.morph;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 은신 캡슐 등록 데이터 (CUSTOM_DATA 안의 {"stealth_cap": {"morphs": "a|b", "sel": 0}}).
 * 개수 무제한 등록, 같은 종류 중복 등록 없음 (이미 있으면 선택만 이동).
 */
public class MorphData {
	public static final String ROOT_KEY = "stealth_cap";
	public static final String MORPH_KEY = "morph";

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

	public static final String LIST_KEY = "morphs";
	public static final String SEL_KEY = "sel";

	/** 등록된 엔티티 ID 목록 ("a|b|c" 로 저장, 구버전 단일 morph 키도 읽어 이관) */
	public static List<String> getAll(ItemStack stack) {
		List<String> out = new ArrayList<>();
		try {
			CompoundTag cap = getRoot(stack);
			String joined = cap.getString(LIST_KEY).orElse("");
			if (joined != null && !joined.isEmpty()) {
				for (String part : joined.split("\\|")) {
					if (part != null && part.contains(":") && !out.contains(part)) {
						out.add(part);
					}
				}
			}
			String legacy = cap.getString(MORPH_KEY).orElse("");
			if (out.isEmpty() && legacy != null && legacy.contains(":")) {
				out.add(legacy);
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	public static int getSelectedIndex(ItemStack stack) {
		try {
			int n = getAll(stack).size();
			if (n == 0) {
				return 0;
			}
			int sel = getRoot(stack).getInt(SEL_KEY).orElse(0);
			return Math.max(0, Math.min(n - 1, sel));
		} catch (Exception e) {
			return 0;
		}
	}

	private static void store(ItemStack stack, List<String> list, int sel) {
		CompoundTag cap = getRoot(stack);
		cap.putString(LIST_KEY, String.join("|", list));
		cap.putInt(SEL_KEY, list.isEmpty() ? 0 : Math.max(0, Math.min(list.size() - 1, sel)));
		cap.putString(MORPH_KEY, list.isEmpty() ? "" : list.get(Math.max(0, Math.min(list.size() - 1, sel))));
		setRoot(stack, cap);
	}

	/** 현재 선택된 엔티티 ID (없으면 "") — 변신/표시 코드가 그대로 사용 */
	public static String getMorphId(ItemStack stack) {
		try {
			List<String> all = getAll(stack);
			if (all.isEmpty()) {
				return "";
			}
			return all.get(getSelectedIndex(stack));
		} catch (Exception e) {
			return "";
		}
	}

	/** 목록에 추가하고 선택한다. 이미 있으면 선택만 옮기고 true (중복 저장 없음, 개수 무제한) */
	public static boolean addMorph(ItemStack stack, String typeId) {
		try {
			if (typeId == null || !typeId.contains(":")) {
				return false;
			}
			List<String> all = getAll(stack);
			int idx = all.indexOf(typeId);
			if (idx >= 0) {
				store(stack, all, idx);
				return true;
			}
			all.add(typeId);
			store(stack, all, all.size() - 1);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** 지정 인덱스 1건만 등록 해제 */
	public static void removeAt(ItemStack stack, int index) {
		try {
			List<String> all = getAll(stack);
			if (index < 0 || index >= all.size()) {
				return;
			}
			all.remove(index);
			store(stack, all, Math.min(getSelectedIndex(stack), all.size() - 1));
		} catch (Exception ignored) {
		}
	}

	/** 호환용: 기존 호출은 "추가+선택"으로 동작 (더 이상 덮어쓰지 않음) */
	public static void setMorphId(ItemStack stack, String typeId) {
		try {
			if (typeId == null || typeId.isEmpty()) {
				clear(stack);
			} else {
				addMorph(stack, typeId);
			}
		} catch (Exception ignored) {
		}
	}

	/** 지정 인덱스로 선택 이동 */
	public static void select(ItemStack stack, int index) {
		try {
			List<String> all = getAll(stack);
			if (all.isEmpty()) {
				return;
			}
			store(stack, all, Math.max(0, Math.min(all.size() - 1, index)));
		} catch (Exception ignored) {
		}
	}

	/** 다음 등록 엔티티로 선택 이동. 새 선택 ID 반환 ("" = 없음) */
	public static String cycle(ItemStack stack) {
		try {
			List<String> all = getAll(stack);
			if (all.isEmpty()) {
				return "";
			}
			int next = (getSelectedIndex(stack) + 1) % all.size();
			store(stack, all, next);
			return all.get(next);
		} catch (Exception e) {
			return "";
		}
	}

	/** 선택된 엔티티 1기만 등록 해제 */
	public static void removeSelected(ItemStack stack) {
		try {
			List<String> all = getAll(stack);
			if (all.isEmpty()) {
				return;
			}
			int sel = getSelectedIndex(stack);
			all.remove(sel);
			store(stack, all, Math.min(sel, all.size() - 1));
		} catch (Exception ignored) {
		}
	}

	/** 전체 등록 해제 */
	public static void clear(ItemStack stack) {
		try {
			store(stack, new ArrayList<>(), 0);
		} catch (Exception ignored) {
		}
	}

	public static boolean has(ItemStack stack) {
		String id = getMorphId(stack);
		return id != null && !id.isEmpty() && id.contains(":");
	}

	public static boolean isEmpty(ItemStack stack) {
		return !has(stack);
	}

	public static String shortName(String typeId) {
		if (typeId == null) {
			return "?";
		}
		int i = typeId.indexOf(':');
		return i >= 0 ? typeId.substring(i + 1) : typeId;
	}

	/** 인벤토리에서 은신 캡슐 목록 */
	public static List<ItemStack> findCapsules(net.minecraft.server.level.ServerPlayer player) {
		List<ItemStack> out = new ArrayList<>();
		try {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (!s.isEmpty() && s.getItem() instanceof StealthCapsuleItem) {
					out.add(s);
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}
}


