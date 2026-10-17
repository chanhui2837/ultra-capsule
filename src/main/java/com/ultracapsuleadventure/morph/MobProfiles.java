package com.ultracapsuleadventure.morph;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

/**
 * 몹별 프로필(동료·적·위협) 조회. 데이터는 UltraCapsuleAdventure_morph_profiles.json (26.3 기준 90종).
 * 항목은 "minecraft:zombie" 같은 ID 또는 "#minecraft:undead" 같은 엔티티 태그.
 */
public final class MobProfiles {

	private static final Map<String, List<String>> ALLIES = new HashMap<>();
	private static final Map<String, List<String>> HUNTERS = new HashMap<>();
	private static final Map<String, List<String>> SCARES = new HashMap<>();
	/** 적대 몹이 플레이어 외에 노리는 대상 ("*"=전부). 프로필 없는 몹은 키가 없음 */
	private static final Map<String, List<String>> TARGETS = new HashMap<>();

	static {
		try (InputStream in = MobProfiles.class.getResourceAsStream("/UltraCapsuleAdventure_morph_profiles.json")) {
			if (in != null) {
				JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
						.getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : root.entrySet()) {
					JsonObject o = e.getValue().getAsJsonObject();
					ALLIES.put(e.getKey(), list(o, "allies"));
					HUNTERS.put(e.getKey(), list(o, "hunters"));
					SCARES.put(e.getKey(), list(o, "scares"));
					if (o.has("targets") && !o.get("targets").isJsonNull()) {
						TARGETS.put(e.getKey(), list(o, "targets"));
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	private MobProfiles() {
	}

	private static List<String> list(JsonObject o, String key) {
		List<String> out = new ArrayList<>();
		try {
			JsonArray a = o.getAsJsonArray(key);
			if (a != null) {
				for (JsonElement el : a) {
					out.add(el.getAsString());
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	/** entry("id" 또는 "#tag")가 typeId에 해당하는지 */
	public static boolean matches(String entry, String typeId) {
		try {
			if (entry == null || typeId == null) {
				return false;
			}
			if (!entry.startsWith("#")) {
				return entry.equals(typeId);
			}
			var tag = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(entry.substring(1)));
			return BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(typeId)).map(h -> h.is(tag)).orElse(false);
		} catch (Exception e) {
			return false;
		}
	}

	/** typeId가 vanilla 엔티티 태그(예: "burn_in_daylight")에 속하는지 */
	public static boolean hasTag(String typeId, String tagName) {
		return matches("#minecraft:" + tagName, typeId);
	}

	private static boolean any(List<String> entries, String typeId) {
		if (entries == null) {
			return false;
		}
		for (String s : entries) {
			if (matches(s, typeId)) {
				return true;
			}
		}
		return false;
	}

	/** morphType으로 변신한 플레이어에게 mobType이 동료인가 */
	public static boolean isAlly(String mobType, String morphType) {
		return any(ALLIES.get(morphType), mobType);
	}

	/** morphType 변신체를 적극적으로 사냥하러 오는 몹인가 */
	public static boolean isHunter(String mobType, String morphType) {
		return any(HUNTERS.get(morphType), mobType);
	}

	/** morphType 변신체를 보고 도망치는 몹인가 */
	public static boolean isScared(String mobType, String morphType) {
		return any(SCARES.get(morphType), mobType);
	}

	public static boolean hasHunters(String morphType) {
		List<String> l = HUNTERS.get(morphType);
		return l != null && !l.isEmpty();
	}

	public static boolean hasScares(String morphType) {
		List<String> l = SCARES.get(morphType);
		return l != null && !l.isEmpty();
	}

	/**
	 * 적대 몹(mobType)이 morphType으로 변신한 플레이어를 "먹잇감"으로 보는가.
	 * 바닐라에서 좀비는 주민·철골렘을 노리지만 돼지·소는 노리지 않는다. 프로필 없는 몹은 간섭하지 않음(true).
	 */
	public static boolean targetsMorph(String mobType, String morphType) {
		List<String> t = TARGETS.get(mobType);
		if (t == null) {
			return true;
		}
		return t.contains("*") || any(t, morphType);
	}
}


