package com.ultracapsuleadventure.silicate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * 규산 도구·갑옷 생성기.
 * 도구/방어구 생성 API는 마크 버전마다 시그니처가 달라서 리플렉션으로 여러 방식을 순서대로 시도한다.
 * (어떤 방식도 안 맞으면 일반 아이템으로 등록되고 로그에 경고가 남는다 — 크래시는 나지 않는다.)
 */
final class SilicateGear {

	private SilicateGear() {
	}

	private static Object armorMaterial;
	private static boolean armorMaterialTried;

	/** kind: pickaxe, axe, shovel, hoe, sword */
	static Item tool(String kind, ToolMaterial material, Item.Properties props) {
		float dmg;
		float spd;
		switch (kind) {
			case "pickaxe" -> { dmg = 1.0f; spd = -2.8f; }
			case "axe" -> { dmg = 5.0f; spd = -3.0f; }
			case "shovel" -> { dmg = 1.5f; spd = -3.0f; }
			case "hoe" -> { dmg = -3.0f; spd = 0.0f; }
			default -> { dmg = 3.0f; spd = -2.4f; }
		}
		String itemClass = switch (kind) {
			case "axe" -> "AxeItem";
			case "shovel" -> "ShovelItem";
			case "hoe" -> "HoeItem";
			default -> null;
		};
		Item.Properties p = props;
		boolean ok = false;
		// 방식 A: Item.Properties#pickaxe/axe/shovel/hoe/sword(ToolMaterial, float, float)
		try {
			Method m = Item.Properties.class.getMethod(kind, ToolMaterial.class, float.class, float.class);
			p = (Item.Properties) m.invoke(props, material, dmg, spd);
			ok = true;
		} catch (Exception ignored) {
		}
		// 방식 B: ToolMaterial#applyToolProperties / applySwordProperties
		if (!ok) {
			try {
				if (kind.equals("sword")) {
					Method m = ToolMaterial.class.getMethod("applySwordProperties", Item.Properties.class, float.class, float.class);
					p = (Item.Properties) m.invoke(material, props, dmg, spd);
					ok = true;
				} else {
					Object tag = switch (kind) {
						case "pickaxe" -> net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE;
						case "axe" -> net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE;
						case "shovel" -> net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL;
						default -> net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE;
					};
					for (Method m : ToolMaterial.class.getMethods()) {
						if (!m.getName().equals("applyToolProperties")) {
							continue;
						}
						Class<?>[] t = m.getParameterTypes();
						if (t.length >= 4 && t[0] == Item.Properties.class) {
							Object[] args = new Object[t.length];
							args[0] = props;
							args[1] = tag;
							args[2] = dmg;
							args[3] = spd;
							for (int i = 4; i < t.length; i++) {
								args[i] = t[i] == float.class ? (Object) 0.0f : (Object) 0;
							}
							p = (Item.Properties) m.invoke(material, args);
							ok = true;
							break;
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		if (!ok) {
			UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 규산 {} 속성 적용 실패 - 일반 아이템으로 등록", kind);
		}
		// 도끼/삽/괭이는 전용 클래스(껍질 벗기기/갈기/길 만들기)가 있으면 사용
		if (itemClass != null) {
			Item it = newItem("net.minecraft.world.item." + itemClass, p, material, dmg, spd);
			if (it != null) {
				return it;
			}
		}
		return new Item(p);
	}

	private static Item newItem(String cls, Item.Properties p, ToolMaterial material, float dmg, float spd) {
		try {
			Class<?> c = Class.forName(cls);
			for (Constructor<?> k : c.getDeclaredConstructors()) {
				Class<?>[] t = k.getParameterTypes();
				try {
					if (t.length == 1 && t[0] == Item.Properties.class) {
						k.setAccessible(true);
						return (Item) k.newInstance(p);
					}
					if (t.length == 4 && t[0] == ToolMaterial.class && t[3] == Item.Properties.class) {
						k.setAccessible(true);
						return (Item) k.newInstance(material, dmg, spd, p);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	// ------------------------------------------------------------------ armor

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object buildArmorMaterial(ResourceKey<?> unused) {
		if (armorMaterialTried) {
			return armorMaterial;
		}
		armorMaterialTried = true;
		try {
			Class<?> mat = null;
			for (String n : new String[] {"net.minecraft.world.item.equipment.ArmorMaterial", "net.minecraft.world.item.ArmorMaterial"}) {
				try {
					mat = Class.forName(n);
					break;
				} catch (ClassNotFoundException ignored) {
				}
			}
			Class<?> type = null;
			for (String n : new String[] {"net.minecraft.world.item.equipment.ArmorType", "net.minecraft.world.item.ArmorItem$Type"}) {
				try {
					type = Class.forName(n);
					break;
				} catch (ClassNotFoundException ignored) {
				}
			}
			if (mat == null || type == null) {
				return null;
			}
			EnumMap defense = new EnumMap(type);
			for (Object c : type.getEnumConstants()) {
				String n = ((Enum) c).name();
				switch (n) {
					case "HELMET" -> defense.put(c, 3);
					case "CHESTPLATE" -> defense.put(c, 8);
					case "LEGGINGS" -> defense.put(c, 6);
					case "BOOTS" -> defense.put(c, 3);
					case "BODY" -> defense.put(c, 11);
					default -> {
					}
				}
			}
			Object equipSound = null;
			try {
				Field f = net.minecraft.sounds.SoundEvents.class.getField("ARMOR_EQUIP_DIAMOND");
				equipSound = f.get(null);
			} catch (Exception ignored) {
			}
			ResourceKey<net.minecraft.core.Registry<Object>> regKey = ResourceKey
					.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));
			Object assetKey = ResourceKey.create(regKey, UltraCapsuleAdventureMod.id("silicate"));
			Object repairTag = ModSilicate.REPAIR;

			for (Constructor<?> k : mat.getDeclaredConstructors()) {
				Class<?>[] t = k.getParameterTypes();
				if (t.length < 6) {
					continue;
				}
				Object[] args = new Object[t.length];
				int ints = 0;
				int floats = 0;
				boolean fail = false;
				for (int i = 0; i < t.length; i++) {
					Class<?> pt = t[i];
					if (pt == int.class) {
						args[i] = ints++ == 0 ? 35 : 12;          // 내구 배수(다이아33<35<네더라이트37), 인챈트(10<12<15)
					} else if (pt == float.class) {
						args[i] = floats++ == 0 ? 2.5f : 0.05f;  // 강인함(2<2.5<3), 밀치기 저항(0<0.05<0.1)
					} else if (Map.class.isAssignableFrom(pt)) {
						args[i] = defense;
					} else if (pt.getSimpleName().equals("Holder")) {
						args[i] = equipSound;
					} else if (pt.getSimpleName().equals("TagKey")) {
						args[i] = repairTag;
					} else if (pt == ResourceKey.class) {
						args[i] = assetKey;
					} else {
						fail = true;
					}
				}
				if (fail) {
					continue;
				}
				k.setAccessible(true);
				armorMaterial = k.newInstance(args);
				return armorMaterial;
			}
		} catch (Throwable e) {
			UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 규산 방어구 재질 생성 실패: {}", e.toString());
		}
		return null;
	}

	/** slot: HELMET, CHESTPLATE, LEGGINGS, BOOTS */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static Item armor(String slot, Item.Properties props) {
		try {
			Object mat = buildArmorMaterial(null);
			if (mat != null) {
				Class<?> matClass = mat.getClass();
				Class<?> typeClass = null;
				for (Method m : Item.Properties.class.getMethods()) {
					if (m.getName().equals("humanoidArmor") && m.getParameterCount() == 2) {
						typeClass = m.getParameterTypes()[1];
						Object type = Enum.valueOf((Class<Enum>) typeClass, slot);
						Item.Properties p = (Item.Properties) m.invoke(props, mat, type);
						return new Item(p);
					}
				}
				// 구 방식: new ArmorItem(ArmorMaterial, ArmorType, Item.Properties)
				Class<?> armorItem = Class.forName("net.minecraft.world.item.ArmorItem");
				for (Constructor<?> k : armorItem.getDeclaredConstructors()) {
					Class<?>[] t = k.getParameterTypes();
					if (t.length == 3 && t[0].isAssignableFrom(matClass) && t[2] == Item.Properties.class && t[1].isEnum()) {
						Object type = Enum.valueOf((Class<Enum>) t[1], slot);
						k.setAccessible(true);
						return (Item) k.newInstance(mat, type, props);
					}
				}
			}
		} catch (Throwable e) {
			UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 규산 {} 방어구 속성 적용 실패: {}", slot, e.toString());
		}
		UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 규산 {} - 일반 아이템으로 등록 (착용 불가)", slot);
		return new Item(props);
	}
}


