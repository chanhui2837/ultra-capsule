package com.ultracapsuleadventure.soul;

import java.util.ArrayList;
import java.util.List;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 체내 소울 저장소: 소울 캡슐을 삼키면 영혼이 플레이어 몸속에 저장된다.
 * Fabric Attachment (영속 저장, 로그아웃/재시작 유지).
 * H키로 소환/저장 토글. 사망 시 캡슐로 옮겨진다 (SoulEvents).
 */
public class SoulBody {

	public static AttachmentType<List<CompoundTag>> TYPE = null;

	public static void register() {
		try {
			if (TYPE != null) {
				return;
			}
			TYPE = AttachmentRegistry.createPersistent(
					UltraCapsuleAdventureMod.id("soul_body"),
					CompoundTag.CODEC.listOf());
		} catch (Exception ignored) {
		}
	}

	public static List<CompoundTag> get(Player player) {
		List<CompoundTag> out = new ArrayList<>();
		try {
			if (TYPE == null || player == null) {
				return out;
			}
			List<CompoundTag> v = ((AttachmentTarget) player).getAttached(TYPE);
			if (v != null) {
				for (CompoundTag t : v) {
					try {
						out.add(t.copy());
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	public static void set(Player player, List<CompoundTag> souls) {
		try {
			if (TYPE == null || player == null) {
				return;
			}
			List<CompoundTag> copy = new ArrayList<>();
			for (CompoundTag t : souls) {
				try {
					copy.add(t.copy());
				} catch (Exception ignored) {
				}
			}
			((AttachmentTarget) player).setAttached(TYPE, copy);
		} catch (Exception ignored) {
		}
	}

	public static void add(Player player, CompoundTag soul) {
		try {
			List<CompoundTag> cur = get(player);
			cur.add(soul.copy());
			set(player, cur);
		} catch (Exception ignored) {
		}
	}

	public static void clear(Player player) {
		try {
			set(player, new ArrayList<>());
		} catch (Exception ignored) {
		}
	}

	/** 전부 꺼내고 비운다 (소환/사망 이전용) */
	public static List<CompoundTag> takeAll(Player player) {
		List<CompoundTag> out = get(player);
		clear(player);
		return out;
	}

	public static int count(Player player) {
		try {
			return get(player).size();
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isEmpty(Player player) {
		return count(player) == 0;
	}
}


