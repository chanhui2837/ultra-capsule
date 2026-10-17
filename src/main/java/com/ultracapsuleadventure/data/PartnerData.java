package com.ultracapsuleadventure.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 보관 캡슐 내부 펫 저장 헬퍼 (MC 26.3 Mojang 매핑).
 * DataComponents.CUSTOM_DATA 안에 {"ultra_capsule_adventure": {"pets": [...]}} 형태로 저장.
 * 26.3 CompoundTag getter는 Optional 반환: getCompound/getList/getString/getFloat/getBoolean + orElse.
 */
public class PartnerData {
	public static final String ROOT_KEY = "ultra_capsule_adventure";
	public static final String PETS_KEY = "pets";

	public static final String PC_TYPE = "PC_Type";
	public static final String PC_HEALTH = "PC_Health";
	public static final String PC_MAX = "PC_MaxHealth";
	public static final String PC_NAME = "PC_Name";
	public static final String PC_SADDLED = "PC_Saddled";
	public static final String PC_ARMOR = "PC_ArmorId";

	private static CompoundTag getCapRoot(ItemStack stack) {
		CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		if (!root.contains(ROOT_KEY)) {
			return new CompoundTag();
		}
		return root.getCompound(ROOT_KEY).orElse(new CompoundTag());
	}

	private static void setCapRoot(ItemStack stack, CompoundTag cap) {
		CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		root.put(ROOT_KEY, cap);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
	}

	public static List<CompoundTag> getPets(ItemStack stack) {
		List<CompoundTag> out = new ArrayList<>();
		CompoundTag cap = getCapRoot(stack);
		if (!cap.contains(PETS_KEY)) {
			return out;
		}
		ListTag list = cap.getList(PETS_KEY).orElse(new ListTag());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i).orElse(null);
			if (t != null) {
				out.add(t.copy());
			}
		}
		return out;
	}

	public static void setPets(ItemStack stack, List<CompoundTag> pets) {
		CompoundTag cap = getCapRoot(stack);
		ListTag list = new ListTag();
		for (CompoundTag pet : pets) {
			list.add(pet.copy());
		}
		cap.put(PETS_KEY, list);
		setCapRoot(stack, cap);
	}

	public static void addPets(ItemStack stack, List<CompoundTag> newcomers) {
		List<CompoundTag> cur = getPets(stack);
		cur.addAll(newcomers);
		setPets(stack, cur);
	}

	public static void clearPets(ItemStack stack) {
		setPets(stack, new ArrayList<>());
	}

	public static int count(ItemStack stack) {
		return getPets(stack).size();
	}

	public static boolean isEmpty(ItemStack stack) {
		return count(stack) == 0;
	}

	public static void updatePet(ItemStack stack, int index, CompoundTag updated) {
		List<CompoundTag> cur = getPets(stack);
		if (index < 0 || index >= cur.size()) {
			return;
		}
		cur.set(index, updated.copy());
		setPets(stack, cur);
	}

	public static CompoundTag removePet(ItemStack stack, int index) {
		List<CompoundTag> cur = getPets(stack);
		if (index < 0 || index >= cur.size()) {
			return null;
		}
		CompoundTag removed = cur.remove(index);
		setPets(stack, cur);
		return removed;
	}

	// ---- 표시용 헬퍼 ----
	public static String getTypeId(CompoundTag pet) {
		String v = pet.getString(PC_TYPE).orElse("");
		if (!v.isEmpty()) {
			return v;
		}
		return pet.getString("id").orElse("unknown");
	}

	public static float getHealth(CompoundTag pet) {
		float v = pet.getFloat(PC_HEALTH).orElse(-1f);
		if (v >= 0) {
			return v;
		}
		return pet.getFloat("Health").orElse(20.0f);
	}

	public static float getMaxHealth(CompoundTag pet) {
		return pet.getFloat(PC_MAX).orElse(20.0f);
	}

	public static String getName(CompoundTag pet) {
		String n = pet.getString(PC_NAME).orElse("");
		if (!n.isEmpty()) {
			return n;
		}
		return getTypeId(pet);
	}

	public static boolean isSaddled(CompoundTag pet) {
		return pet.getBoolean(PC_SADDLED).orElse(false);
	}

	public static String getArmorId(CompoundTag pet) {
		return pet.getString(PC_ARMOR).orElse("");
	}
}


