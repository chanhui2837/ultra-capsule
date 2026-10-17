package com.ultracapsuleadventure.network;

import java.util.List;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;
import com.ultracapsuleadventure.data.PartnerData;
import com.ultracapsuleadventure.item.ModItems;
import com.ultracapsuleadventure.item.UltraCapsuleAdventureItem;
import com.ultracapsuleadventure.util.TameUtil;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * C2S 패킷들 (GUI 버튼 -> 서버). MC 26.3 Fabric Networking.
 */
public class ModPackets {

	private static void tell(ServerPlayer player, String text, boolean overlay) {
		Component c = Component.literal(text);
		if (overlay) {
			player.sendOverlayMessage(c);
		} else {
			player.sendSystemMessage(c);
		}
	}

	// ---------- 패킷 정의 ----------

	public record HealPetPayload(int index, boolean full) implements CustomPacketPayload {
		public static final Type<HealPetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "heal_pet"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HealPetPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, HealPetPayload::index, ByteBufCodecs.BOOL, HealPetPayload::full, HealPetPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RenamePetPayload(int index, String name) implements CustomPacketPayload {
		public static final Type<RenamePetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "rename_pet"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RenamePetPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, RenamePetPayload::index, ByteBufCodecs.STRING_UTF8, RenamePetPayload::name, RenamePetPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RecallPayload() implements CustomPacketPayload {
		public static final Type<RecallPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "recall"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RecallPayload> CODEC =
				StreamCodec.unit(new RecallPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SummonAllPayload() implements CustomPacketPayload {
		public static final Type<SummonAllPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "summon_all"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SummonAllPayload> CODEC =
				StreamCodec.unit(new SummonAllPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SummonOnePayload(int index) implements CustomPacketPayload {
		public static final Type<SummonOnePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "summon_one"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SummonOnePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, SummonOnePayload::index, SummonOnePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record EquipPetPayload(int index, boolean unequip) implements CustomPacketPayload {
		public static final Type<EquipPetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "equip_pet"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EquipPetPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, EquipPetPayload::index, ByteBufCodecs.BOOL, EquipPetPayload::unequip, EquipPetPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulSummonOnePayload(int index) implements CustomPacketPayload {
		public static final Type<SoulSummonOnePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_summon_one"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulSummonOnePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, SoulSummonOnePayload::index, SoulSummonOnePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulDeletePayload(int index) implements CustomPacketPayload {
		public static final Type<SoulDeletePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_delete"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulDeletePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, SoulDeletePayload::index, SoulDeletePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulMergePayload(int index) implements CustomPacketPayload {
		public static final Type<SoulMergePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_merge"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulMergePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, SoulMergePayload::index, SoulMergePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulSummonAllPayload() implements CustomPacketPayload {
		public static final Type<SoulSummonAllPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_summon_all"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulSummonAllPayload> CODEC =
				StreamCodec.unit(new SoulSummonAllPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulRecallPayload() implements CustomPacketPayload {
		public static final Type<SoulRecallPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_recall"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulRecallPayload> CODEC =
				StreamCodec.unit(new SoulRecallPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SoulBodyTogglePayload() implements CustomPacketPayload {
		public static final Type<SoulBodyTogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_body_toggle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SoulBodyTogglePayload> CODEC =
				StreamCodec.unit(new SoulBodyTogglePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TitanRepairStartPayload(int slot) implements CustomPacketPayload {
		public static final Type<TitanRepairStartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "titan_repair_start"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TitanRepairStartPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, TitanRepairStartPayload::slot, TitanRepairStartPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TitanRepairCancelPayload() implements CustomPacketPayload {
		public static final Type<TitanRepairCancelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "titan_repair_cancel"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TitanRepairCancelPayload> CODEC =
				StreamCodec.unit(new TitanRepairCancelPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GambleBetPayload(int slot) implements CustomPacketPayload {
		public static final Type<GambleBetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gamble_bet"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GambleBetPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, GambleBetPayload::slot, GambleBetPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisStagePayload(int slot) implements CustomPacketPayload {
		public static final Type<DisStagePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "dis_stage"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisStagePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, DisStagePayload::slot, DisStagePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record VoidReleasePayload(int index) implements CustomPacketPayload {
		public static final Type<VoidReleasePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "void_release"));
		public static final StreamCodec<RegistryFriendlyByteBuf, VoidReleasePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, VoidReleasePayload::index, VoidReleasePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record VoidErasePayload(int index) implements CustomPacketPayload {
		public static final Type<VoidErasePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "void_erase"));
		public static final StreamCodec<RegistryFriendlyByteBuf, VoidErasePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, VoidErasePayload::index, VoidErasePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GambleInsertPayload(boolean mainHand) implements CustomPacketPayload {
		public static final Type<GambleInsertPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gamble_insert"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GambleInsertPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.BOOL, GambleInsertPayload::mainHand, GambleInsertPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GambleWithdrawPayload() implements CustomPacketPayload {
		public static final Type<GambleWithdrawPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gamble_withdraw"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GambleWithdrawPayload> CODEC =
				StreamCodec.unit(new GambleWithdrawPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record GambleStartPayload() implements CustomPacketPayload {
		public static final Type<GambleStartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gamble_start"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GambleStartPayload> CODEC =
				StreamCodec.unit(new GambleStartPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisInsertPayload(boolean mainHand) implements CustomPacketPayload {
		public static final Type<DisInsertPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "dis_insert"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisInsertPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.BOOL, DisInsertPayload::mainHand, DisInsertPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisWithdrawPayload() implements CustomPacketPayload {
		public static final Type<DisWithdrawPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "dis_withdraw"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisWithdrawPayload> CODEC =
				StreamCodec.unit(new DisWithdrawPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisassemblePayload() implements CustomPacketPayload {
		public static final Type<DisassemblePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "disassemble"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisassemblePayload> CODEC =
				StreamCodec.unit(new DisassemblePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record MechFirePayload() implements CustomPacketPayload {
		public static final Type<MechFirePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "mech_fire"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MechFirePayload> CODEC =
				StreamCodec.unit(new MechFirePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record MechJumpPayload() implements CustomPacketPayload {
		public static final Type<MechJumpPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "mech_jump"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MechJumpPayload> CODEC =
				StreamCodec.unit(new MechJumpPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record MechFoldPayload() implements CustomPacketPayload {
		public static final Type<MechFoldPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "mech_fold"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MechFoldPayload> CODEC =
				StreamCodec.unit(new MechFoldPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TitanDepositPayload() implements CustomPacketPayload {
		public static final Type<TitanDepositPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "titan_deposit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TitanDepositPayload> CODEC =
				StreamCodec.unit(new TitanDepositPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TitanWithdrawPayload() implements CustomPacketPayload {
		public static final Type<TitanWithdrawPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "titan_withdraw"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TitanWithdrawPayload> CODEC =
				StreamCodec.unit(new TitanWithdrawPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 변신 능력 키: 엔더맨 순간이동 / 블레이즈·가스트 등 투사체 발사 */
	public record MorphAbilityPayload() implements CustomPacketPayload {
		public static final Type<MorphAbilityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "morph_ability"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MorphAbilityPayload> CODEC =
				StreamCodec.unit(new MorphAbilityPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record FlameBreathPayload() implements CustomPacketPayload {
		public static final Type<FlameBreathPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "flame_breath"));
		public static final StreamCodec<RegistryFriendlyByteBuf, FlameBreathPayload> CODEC =
				StreamCodec.unit(new FlameBreathPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StealthTogglePayload() implements CustomPacketPayload {
		public static final Type<StealthTogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_toggle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StealthTogglePayload> CODEC =
				StreamCodec.unit(new StealthTogglePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StealthCyclePayload() implements CustomPacketPayload {
		public static final Type<StealthCyclePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_cycle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StealthCyclePayload> CODEC =
				StreamCodec.unit(new StealthCyclePayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record StealthClearPayload() implements CustomPacketPayload {
		public static final Type<StealthClearPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_clear"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StealthClearPayload> CODEC =
				StreamCodec.unit(new StealthClearPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 은신 관리창: i번째 등록 엔티티로 변신 */
	public record StealthMorphPayload(int index) implements CustomPacketPayload {
		public static final Type<StealthMorphPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_morph"));

		public static final StreamCodec<RegistryFriendlyByteBuf, StealthMorphPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, StealthMorphPayload::index, StealthMorphPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 은신 관리창: i번째 등록 해제 */
	public record StealthRemovePayload(int index) implements CustomPacketPayload {
		public static final Type<StealthRemovePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_remove"));

		public static final StreamCodec<RegistryFriendlyByteBuf, StealthRemovePayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, StealthRemovePayload::index, StealthRemovePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 은신 관리창 열 때 서버 상태 요청 */
	public record StealthSyncRequestPayload() implements CustomPacketPayload {
		public static final Type<StealthSyncRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_sync_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StealthSyncRequestPayload> CODEC =
				StreamCodec.unit(new StealthSyncRequestPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 은신 관리창: 인간으로 복귀 (변신 중일 때만 해제) */
	public record StealthUnmorphPayload() implements CustomPacketPayload {
		public static final Type<StealthUnmorphPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_unmorph"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StealthUnmorphPayload> CODEC =
				StreamCodec.unit(new StealthUnmorphPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** 서버→클라 모프 상태 동기화 (Walkers의 PlayerShape.sync와 같은 역할) */
	public record MorphSyncPayload(String playerUuid, String morphId) implements CustomPacketPayload {
		public static final Type<MorphSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "morph_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, MorphSyncPayload::playerUuid,
						ByteBufCodecs.STRING_UTF8, MorphSyncPayload::morphId, MorphSyncPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record OpenUtilityGuiPayload(int kind, boolean mainHand) implements CustomPacketPayload {
		public static final Type<OpenUtilityGuiPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "open_utility_gui"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenUtilityGuiPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.INT, OpenUtilityGuiPayload::kind, ByteBufCodecs.BOOL, OpenUtilityGuiPayload::mainHand, OpenUtilityGuiPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// ---------- 등록 ----------

	public static void registerServerReceivers() {
		PayloadTypeRegistry.serverboundPlay().register(HealPetPayload.TYPE, HealPetPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RenamePetPayload.TYPE, RenamePetPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RecallPayload.TYPE, RecallPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SummonAllPayload.TYPE, SummonAllPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SummonOnePayload.TYPE, SummonOnePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EquipPetPayload.TYPE, EquipPetPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulSummonOnePayload.TYPE, SoulSummonOnePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulDeletePayload.TYPE, SoulDeletePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulMergePayload.TYPE, SoulMergePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulSummonAllPayload.TYPE, SoulSummonAllPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulRecallPayload.TYPE, SoulRecallPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SoulBodyTogglePayload.TYPE, SoulBodyTogglePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TitanRepairStartPayload.TYPE, TitanRepairStartPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TitanRepairCancelPayload.TYPE, TitanRepairCancelPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GambleBetPayload.TYPE, GambleBetPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DisStagePayload.TYPE, DisStagePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VoidReleasePayload.TYPE, VoidReleasePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VoidErasePayload.TYPE, VoidErasePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GambleInsertPayload.TYPE, GambleInsertPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GambleWithdrawPayload.TYPE, GambleWithdrawPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GambleStartPayload.TYPE, GambleStartPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DisInsertPayload.TYPE, DisInsertPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DisWithdrawPayload.TYPE, DisWithdrawPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DisassemblePayload.TYPE, DisassemblePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenUtilityGuiPayload.TYPE, OpenUtilityGuiPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MechFirePayload.TYPE, MechFirePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MechJumpPayload.TYPE, MechJumpPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MechFoldPayload.TYPE, MechFoldPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TitanDepositPayload.TYPE, TitanDepositPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TitanWithdrawPayload.TYPE, TitanWithdrawPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FlameBreathPayload.TYPE, FlameBreathPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MorphAbilityPayload.TYPE, MorphAbilityPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthTogglePayload.TYPE, StealthTogglePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthClearPayload.TYPE, StealthClearPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthCyclePayload.TYPE, StealthCyclePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthMorphPayload.TYPE, StealthMorphPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthRemovePayload.TYPE, StealthRemovePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthUnmorphPayload.TYPE, StealthUnmorphPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StealthSyncRequestPayload.TYPE, StealthSyncRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MorphSyncPayload.TYPE, MorphSyncPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(HealPetPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleHeal(player, payload.index(), payload.full()));
		});
		ServerPlayNetworking.registerGlobalReceiver(RenamePetPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleRename(player, payload.index(), payload.name()));
		});
		ServerPlayNetworking.registerGlobalReceiver(RecallPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleRecall(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(SummonAllPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSummonAll(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(SummonOnePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSummonOne(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(EquipPetPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleEquip(player, payload.index(), payload.unequip()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulSummonOnePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulSummonOne(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulDeletePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulDelete(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulMergePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulMerge(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulSummonAllPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulSummonAll(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulRecallPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulRecall(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(SoulBodyTogglePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleSoulBodyToggle(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(TitanRepairStartPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleTitanRepairStart(player, payload.slot()));
		});
		ServerPlayNetworking.registerGlobalReceiver(TitanRepairCancelPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleTitanRepairCancel(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(GambleBetPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleGambleBet(player, payload.slot()));
		});
		ServerPlayNetworking.registerGlobalReceiver(DisStagePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleDisStage(player, payload.slot()));
		});
		ServerPlayNetworking.registerGlobalReceiver(VoidReleasePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleVoidRelease(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(VoidErasePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleVoidErase(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(GambleInsertPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleGambleInsert(player, payload.mainHand()));
		});
		ServerPlayNetworking.registerGlobalReceiver(GambleWithdrawPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleGambleWithdraw(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(GambleStartPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleGambleStart(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(DisInsertPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleDisInsert(player, payload.mainHand()));
		});
		ServerPlayNetworking.registerGlobalReceiver(DisWithdrawPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleDisWithdraw(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(DisassemblePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleDisassemble(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(MechFirePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				try {
					if (player.getVehicle() instanceof com.ultracapsuleadventure.mech.MechRobotEntity mech) {
						mech.tryFire(player);
					}
				} catch (Exception ignored) {
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(MechJumpPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				try {
					if (player.getVehicle() instanceof com.ultracapsuleadventure.mech.MechRobotEntity mech) {
						mech.tryJump(player);
					}
				} catch (Exception ignored) {
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(MechFoldPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				try {
					if (player.getVehicle() instanceof com.ultracapsuleadventure.mech.MechRobotEntity mech) {
						mech.tryStartFoldWhileRiding(player);
					}
				} catch (Exception ignored) {
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(TitanDepositPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleTitanDeposit(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(TitanWithdrawPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleTitanWithdraw(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(MorphAbilityPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				try {
					com.ultracapsuleadventure.morph.MorphEvents.useAbility(player);
				} catch (Exception ignored) {
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(FlameBreathPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				try {
					com.ultracapsuleadventure.flame.FlameBreath.breathe(player);
				} catch (Exception ignored) {
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthTogglePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthToggle(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthCyclePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthCycle(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthClearPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthClear(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthMorphPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthMorph(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthRemovePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthRemove(player, payload.index()));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthUnmorphPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthUnmorph(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(StealthSyncRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handleStealthSyncRequest(player));
		});
	}

	private static ItemStack slotStack(ServerPlayer player, int slot) {
		try {
			if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
				return ItemStack.EMPTY;
			}
			ItemStack s = player.getInventory().getItem(slot);
			return s == null ? ItemStack.EMPTY : s;
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	private static void handleTitanRepairStart(ServerPlayer player, int slot) {
		ItemStack cap = getTitanCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "타이탄 캡슐이 없습니다.", true);
			return;
		}
		long now = System.currentTimeMillis();
		try {
			long cool = com.ultracapsuleadventure.titan.TitanData.getCooldownUntil(cap);
			if (cool > now) {
				tell(player, "쿨타임 " + ((cool - now) / 1000) + "초 남음!", true);
				return;
			}
		} catch (Exception ignored) {
		}
		try {
			if (!com.ultracapsuleadventure.titan.TitanData.findRepairItem(player).isEmpty()) {
				tell(player, "이미 수리 중!", true);
				return;
			}
		} catch (Exception ignored) {
		}
		try {
			if (com.ultracapsuleadventure.titan.TitanData.isRepairing(cap) || com.ultracapsuleadventure.titan.TitanData.hasStored(cap)
					|| com.ultracapsuleadventure.titan.TitanData.hasAnvil(cap)) {
				tell(player, "이미 작업 중! 먼저 끝내라!", true);
				return;
			}
		} catch (Exception ignored) {
		}
		ItemStack target = slotStack(player, slot);
		if (target.isEmpty() || !com.ultracapsuleadventure.titan.TitanData.isRepairableTarget(target)) {
			tell(player, "닳은 장비/도구, 닳은 메카 캡슐, 미충전 메두사 머리를 선택!", true);
			return;
		}
		long dur = com.ultracapsuleadventure.titan.TitanData.computeDurationMs(target, cap, player.registryAccess());
		if (dur <= 0) {
			tell(player, "수리할 수 없는 아이템!", true);
			return;
		}
		com.ultracapsuleadventure.titan.TitanData.startRepair(target, dur);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "수리 시작: " + com.ultracapsuleadventure.titan.TitanData.displayName(target)
				+ " (" + (dur / 1000) + "초 소요)", false);
	}

	private static void handleTitanRepairCancel(ServerPlayer player) {
		ItemStack job = ItemStack.EMPTY;
		try {
			job = com.ultracapsuleadventure.titan.TitanData.findRepairItem(player);
		} catch (Exception ignored) {
		}
		if (job.isEmpty()) {
			tell(player, "진행 중인 수리가 없습니다.", true);
			return;
		}
		com.ultracapsuleadventure.titan.TitanData.clearRepair(job);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "수리 중단 (진행도 유지, 쿨타임 없음)", true);
	}

	private static void handleGambleBet(ServerPlayer player, int slot) {
		ItemStack cap = findUtilityCapsule(player, ModItems.GAMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			tell(player, "도박 캡슐이 없습니다.", true);
			return;
		}
		if (com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(cap)) {
			tell(player, "이미 장전되어 있습니다.", true);
			return;
		}
		ItemStack target = slotStack(player, slot);
		if (target.isEmpty() || isCapsuleItem(target.getItem())) {
			tell(player, "걸 아이템을 선택하세요 (캡슐 제외).", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		String nm;
		int n;
		try {
			nm = com.ultracapsuleadventure.util.ItemNbt.displayName(target);
			n = target.getCount();
		} catch (Exception e) {
			return;
		}
		com.ultracapsuleadventure.item.GambleCapsuleItem.setBet(cap, registries, target);
		try {
			target.shrink(target.getCount());
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "베팅 장전: " + nm + " x" + n, true);
	}

	private static void handleDisStage(ServerPlayer player, int slot) {
		ItemStack cap = findUtilityCapsule(player, ModItems.DISASSEMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			tell(player, "분해 캡슐이 없습니다.", true);
			return;
		}
		if (com.ultracapsuleadventure.item.DisassembleCapsuleItem.hasStored(cap)) {
			tell(player, "이미 들어있습니다.", true);
			return;
		}
		ItemStack target = slotStack(player, slot);
		if (target.isEmpty() || isCapsuleItem(target.getItem())) {
			tell(player, "분해할 아이템을 선택하세요 (캡슐 제외).", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		com.ultracapsuleadventure.item.DisassembleCapsuleItem.setStored(cap, registries, target);
		ItemStack probe;
		try {
			probe = target.copy();
		} catch (Exception e) {
			probe = ItemStack.EMPTY;
		}
		try {
			target.shrink(1);
		} catch (Exception ignored) {
		}
		try {
			String expect = "";
			if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
				java.util.List<ItemStack> outs = com.ultracapsuleadventure.util.GambleLogic.findReverse(sl, probe);
				if (outs.isEmpty()) {
					expect = "분해 불가 (조합법 없음)";
				} else {
					StringBuilder sb = new StringBuilder("예상: ");
					for (ItemStack o : outs) {
						sb.append(com.ultracapsuleadventure.util.ItemNbt.displayName(o)).append(' ');
					}
					expect = sb.toString().trim();
				}
			}
			com.ultracapsuleadventure.item.DisassembleCapsuleItem.setExpect(cap, expect);
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "장전 완료!", true);
	}

	private static ItemStack getTitanCapsule(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == ModItems.TITAN_CAPSULE) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void handleTitanDeposit(ServerPlayer player) {
		ItemStack cap = getTitanCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		String err = com.ultracapsuleadventure.titan.TitanCapsuleItem.depositFromGui(player, cap);
		if (err != null) {
			tell(player, err, true);
		}
	}

	private static void handleTitanWithdraw(ServerPlayer player) {
		ItemStack cap = getTitanCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		try {
			if (!com.ultracapsuleadventure.titan.TitanData.hasStored(cap)) {
				tell(player, "꺼낼 아이템이 없습니다.", true);
				return;
			}
			com.ultracapsuleadventure.titan.TitanCapsuleItem.withdrawToPlayer(player, cap);
		} catch (Exception ignored) {
		}
	}

	private static ItemStack getStealthCapsule(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	/** 은신 관리창: 변신 중이면 복귀, 아니면 등록된 형태로 변신 */
	private static void handleStealthToggle(ServerPlayer player) {
		if (com.ultracapsuleadventure.morph.MorphEvents.isMorphed(player)) {
			com.ultracapsuleadventure.morph.MorphEvents.unmorph(player, "은신 해제! 원래 모습으로 돌아왔다.");
			return;
		}
		ItemStack cap = getStealthCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "은신 캡슐이 없습니다.", true);
			return;
		}
		String id = com.ultracapsuleadventure.morph.MorphData.getMorphId(cap);
		if (id == null || id.isEmpty()) {
			tell(player, "비어있음. 황혼의 검으로 처치해 등록하세요.", true);
			return;
		}
		String err = com.ultracapsuleadventure.morph.MorphEvents.morph(player, id);
		if (err != null) {
			tell(player, err, true);
		}
	}

	private static void handleStealthClear(ServerPlayer player) {
		ItemStack cap = getStealthCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "은신 캡슐이 없습니다.", true);
			return;
		}
		if (com.ultracapsuleadventure.morph.MorphEvents.isMorphed(player)) {
			com.ultracapsuleadventure.morph.MorphEvents.unmorph(player, null);
		}
		String removed = com.ultracapsuleadventure.morph.MorphData.getMorphId(cap);
		com.ultracapsuleadventure.morph.MorphData.removeSelected(cap);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "등록 해제: " + com.ultracapsuleadventure.morph.MorphData.shortName(removed), true);
	}

	/** 은신 관리창: i번째 등록 엔티티로 변신 (다른 모습이면 갈아타기) */
	private static void handleStealthMorph(ServerPlayer player, int index) {
		ItemStack cap = getStealthCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "은신 캡슐이 없습니다.", true);
			return;
		}
		java.util.List<String> all;
		try {
			all = com.ultracapsuleadventure.morph.MorphData.getAll(cap);
		} catch (Exception e) {
			return;
		}
		if (index < 0 || index >= all.size()) {
			return;
		}
		String id = all.get(index);
		try {
			String cur = com.ultracapsuleadventure.morph.MorphEvents.morphId(player);
			if (id.equals(cur)) {
				tell(player, "이미 " + com.ultracapsuleadventure.morph.MorphData.shortName(id) + "(으)로 변신 중입니다.", true);
				return;
			}
			if (cur != null && !cur.isEmpty()) {
				com.ultracapsuleadventure.morph.MorphEvents.unmorph(player, null);
			}
		} catch (Exception ignored) {
		}
		try {
			com.ultracapsuleadventure.morph.MorphData.select(cap, index);
			com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		} catch (Exception ignored) {
		}
		String err = com.ultracapsuleadventure.morph.MorphEvents.morph(player, id);
		if (err != null) {
			tell(player, err, true);
		}
	}

	/** 은신 관리창: i번째 등록 해제 (변신 중이면 먼저 복귀) */
	private static void handleStealthRemove(ServerPlayer player, int index) {
		ItemStack cap = getStealthCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "은신 캡슐이 없습니다.", true);
			return;
		}
		java.util.List<String> all;
		try {
			all = com.ultracapsuleadventure.morph.MorphData.getAll(cap);
		} catch (Exception e) {
			return;
		}
		if (index < 0 || index >= all.size()) {
			return;
		}
		String removed = all.get(index);
		try {
			String cur = com.ultracapsuleadventure.morph.MorphEvents.morphId(player);
			if (removed.equals(cur)) {
				com.ultracapsuleadventure.morph.MorphEvents.unmorph(player, null);
			}
		} catch (Exception ignored) {
		}
		try {
			com.ultracapsuleadventure.morph.MorphData.removeAt(cap, index);
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "등록 해제: " + com.ultracapsuleadventure.morph.MorphData.shortName(removed), true);
	}

	/** 은신 관리창: 인간으로 복귀. 서버에 기록이 없어도 클라 유령 캐시는 강제 삭제 */
	private static void handleStealthUnmorph(ServerPlayer player) {
		if (!com.ultracapsuleadventure.morph.MorphEvents.isMorphed(player)) {
			tell(player, "이미 인간 모습입니다.", true);
		} else {
			com.ultracapsuleadventure.morph.MorphEvents.unmorph(player, "은신 해제! 원래 모습으로 돌아왔다.");
		}
		try {
			com.ultracapsuleadventure.morph.MorphEvents.sendStateTo(player, player);
		} catch (Exception ignored) {
		}
	}

	/** 은신 관리창 열 때 클라가 서버에 현재 상태 요청 (엇갈림 자가치유) */
	private static void handleStealthSyncRequest(ServerPlayer player) {
		try {
			com.ultracapsuleadventure.morph.MorphEvents.sendStateTo(player, player);
		} catch (Exception ignored) {
		}
	}

	/** 은신 관리창: 등록된 다음 엔티티 선택 */
	private static void handleStealthCycle(ServerPlayer player) {
		ItemStack cap = getStealthCapsule(player);
		if (cap.isEmpty()) {
			tell(player, "은신 캡슐이 없습니다.", true);
			return;
		}
		String next = com.ultracapsuleadventure.morph.MorphData.cycle(cap);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		if (!next.isEmpty()) {
			tell(player, "선택: " + com.ultracapsuleadventure.morph.MorphData.shortName(next), true);
		}
	}

	private static ItemStack getCapsule(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof UltraCapsuleAdventureItem) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == ModItems.ultra_capsule_adventure) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void handleHeal(ServerPlayer player, int index, boolean full) {
		ItemStack cap = getCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		List<CompoundTag> pets = PartnerData.getPets(cap);
		if (index < 0 || index >= pets.size()) {
			return;
		}
		CompoundTag pet = pets.get(index);
		TameUtil.HealResult r = full ? TameUtil.healFull(pet, player) : TameUtil.healOnce(pet, player);
		PartnerData.updatePet(cap, index, pet);
		UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, (full ? "[풀회복] " : "[회복] ") + r.message(), true);
	}

	private static void handleRename(ServerPlayer player, int index, String name) {
		ItemStack cap = getCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		List<CompoundTag> pets = PartnerData.getPets(cap);
		if (index < 0 || index >= pets.size()) {
			return;
		}
		String clean = name == null ? "" : name.trim();
		if (clean.length() > 32) {
			clean = clean.substring(0, 32);
		}
		CompoundTag pet = pets.get(index);
		pet.putString(PartnerData.PC_NAME, clean);
		PartnerData.updatePet(cap, index, pet);
		UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "이름 변경: " + (clean.isEmpty() ? "(없음)" : clean), true);
	}

	private static void handleRecall(ServerPlayer player) {
		int n = TameUtil.recallAllToMe(player);
		tell(player, "불러내기: " + n + "마리를 순간이동시켰습니다! (캡슐 밖 포함)", false);
	}

	private static void handleSummonAll(ServerPlayer player) {
		ItemStack cap = getCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		UltraCapsuleAdventureItem.summonAll(player.level(), player, cap);
	}

	private static void handleSummonOne(ServerPlayer player, int index) {
		ItemStack cap = getCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		boolean ok = UltraCapsuleAdventureItem.summonOne(player.level(), player, cap, index);
		tell(player, ok ? "1마리 소환!" : "소환 실패!", true);
	}

	private static void handleEquip(ServerPlayer player, int index, boolean unequip) {
		ItemStack cap = getCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		List<CompoundTag> pets = PartnerData.getPets(cap);
		if (index < 0 || index >= pets.size()) {
			return;
		}
		CompoundTag pet = pets.get(index);
		if (unequip) {
			TameUtil.GearResult r = TameUtil.unequipToInventory(pet, player);
			PartnerData.updatePet(cap, index, r.tag());
			UltraCapsuleAdventureItem.syncCapsule(player);
			tell(player, "[장비 해제] " + r.message(), true);
			return;
		}
		TameUtil.GearResult r = TameUtil.tryEquipFromInventory(pet, player);
		PartnerData.updatePet(cap, index, r.tag());
		UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "[장비] " + r.message(), true);
	}

	private static ItemStack getSoulCapsule(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == ModItems.SOUL_CAPSULE) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack getSoulCapsuleWithSouls(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem
					&& !com.ultracapsuleadventure.soul.SoulData.isEmpty(s)) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == ModItems.SOUL_CAPSULE && !com.ultracapsuleadventure.soul.SoulData.isEmpty(s)) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void handleSoulSummonOne(ServerPlayer player, int index) {
		ItemStack cap = getSoulCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		boolean ok = com.ultracapsuleadventure.item.SoulCapsuleItem.summonOne(player.level(), player, cap, index);
		tell(player, ok ? "영혼 1기를 소환했다!" : "소환 실패!", true);
	}

	private static void handleSoulDelete(ServerPlayer player, int index) {
		ItemStack cap = getSoulCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		List<CompoundTag> souls = com.ultracapsuleadventure.soul.SoulData.getSouls(cap);
		if (index < 0 || index >= souls.size()) {
			return;
		}
		souls.remove(index);
		com.ultracapsuleadventure.soul.SoulData.setSouls(cap, souls);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "영혼 삭제!", true);
	}

	private static void handleSoulMerge(ServerPlayer player, int index) {
		ItemStack cap = getSoulCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		List<CompoundTag> souls = com.ultracapsuleadventure.soul.SoulData.getSouls(cap);
		if (index < 0 || index >= souls.size()) {
			return;
		}
		CompoundTag base = souls.get(index);
		String type = com.ultracapsuleadventure.soul.SoulData.getTypeId(base);
		int level = com.ultracapsuleadventure.soul.SoulData.getLevel(base);
		if (level >= com.ultracapsuleadventure.soul.SoulData.MAX_LEVEL) {
			tell(player, "이미 최대 레벨(3)이다!", true);
			return;
		}
		int other = -1;
		for (int i = 0; i < souls.size(); i++) {
			if (i == index) {
				continue;
			}
			CompoundTag s = souls.get(i);
			if (com.ultracapsuleadventure.soul.SoulData.getTypeId(s).equals(type)
					&& com.ultracapsuleadventure.soul.SoulData.getLevel(s) == level) {
				other = i;
				break;
			}
		}
		if (other < 0) {
			String shortType = type.contains(":") ? type.substring(type.indexOf(':') + 1) : type;
			tell(player, "같은 Lv." + level + " " + shortType + " 2마리 필요!", true);
			return;
		}
		souls.remove(Math.max(index, other));
		CompoundTag survivor = souls.get(Math.min(index, other));
		com.ultracapsuleadventure.soul.SoulData.setLevel(survivor, level + 1);
		com.ultracapsuleadventure.soul.SoulData.setSouls(cap, souls);
		try {
			com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info(
					"[SoulCap] merge type=" + type + " " + level + "->" + (level + 1));
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		String shortType = type.contains(":") ? type.substring(type.indexOf(':') + 1) : type;
		tell(player, "합성 성공! Lv." + (level + 1) + " " + shortType + " 탄생!", false);
	}

	private static void handleSoulSummonAll(ServerPlayer player) {
		ItemStack cap = getSoulCapsuleWithSouls(player);
		if (cap.isEmpty()) {
			return;
		}
		com.ultracapsuleadventure.item.SoulCapsuleItem.summonAll(player.level(), player, cap);
	}

	private static void handleSoulRecall(ServerPlayer player) {
		int n = com.ultracapsuleadventure.soul.SoulAlly.recallSoulsTo(player);
		tell(player, n == 0 ? "데려올 소울이 없습니다." : "소울 " + n + "기를 불러냈다!", false);
	}

	/** H키: 밖에 소울이 있으면 저장(캡슐 우선, 없으면 몸속), 없으면 소환(몸속→캡슐 순) */
	private static void handleSoulBodyToggle(ServerPlayer player) {
		List<net.minecraft.world.entity.LivingEntity> out = new java.util.ArrayList<>();
		try {
			out = com.ultracapsuleadventure.soul.SoulAlly.findOwnedSouls(player, 512.0);
		} catch (Exception ignored) {
		}
		if (!out.isEmpty()) {
			ItemStack cap = getSoulCapsule(player);
			boolean toCapsule = !cap.isEmpty();
			int n = 0;
			for (net.minecraft.world.entity.LivingEntity s : out) {
				try {
					String tid = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(s.getType()).toString();
					CompoundTag soul = com.ultracapsuleadventure.soul.SoulData.captureFrom(s, tid);
					if (toCapsule) {
						com.ultracapsuleadventure.soul.SoulData.addSoul(cap, soul);
					} else {
						com.ultracapsuleadventure.soul.SoulBody.add(player, soul);
					}
					s.discard();
					n++;
				} catch (Exception ignored) {
				}
			}
			if (toCapsule) {
				com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
				tell(player, n == 0 ? "저장할 소울이 없습니다." : "영혼 " + n + "기를 소울 캡슐에 저장했다! (H: 소환)", false);
			} else {
				tell(player, n == 0 ? "저장할 소울이 없습니다." : "영혼 " + n + "기를 몸속에 저장했다! (H: 소환)", false);
			}
			return;
		}
		List<CompoundTag> body = com.ultracapsuleadventure.soul.SoulBody.takeAll(player);
		if (!body.isEmpty()) {
			summonBodySouls(player, body);
			return;
		}
		ItemStack cap = getSoulCapsuleWithSouls(player);
		if (!cap.isEmpty()) {
			com.ultracapsuleadventure.item.SoulCapsuleItem.summonAll(player.level(), player, cap);
			return;
		}
		tell(player, "소환할 영혼이 없습니다. 캡슐에 모으거나 삼키세요.", true);
	}

	private static void summonBodySouls(ServerPlayer player, List<CompoundTag> body) {
		if (body.isEmpty()) {
			tell(player, "몸속에 저장된 영혼이 없습니다. 소울 캡슐을 웅크리고 우클릭해 삼키세요.", true);
			return;
		}
		if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
			com.ultracapsuleadventure.soul.SoulBody.set(player, body);
			return;
		}
		int spawned = 0;
		double baseX = player.getX();
		double baseY = player.getY();
		double baseZ = player.getZ();
		for (CompoundTag soul : body) {
			try {
				double ox = ((spawned % 5) - 2) * 1.4 + serverLevel.getRandom().nextDouble() * 0.5;
				double oz = ((spawned / 5) - 1) * 1.4 + serverLevel.getRandom().nextDouble() * 0.5;
				net.minecraft.world.entity.Entity created = com.ultracapsuleadventure.soul.SoulData.createSoulEntity(
						serverLevel, soul, baseX + ox, baseY + 1, baseZ + oz);
				if (created == null) {
					continue;
				}
				try {
					created.setYRot(serverLevel.getRandom().nextFloat() * 360f);
				} catch (Exception ignored) {
				}
				try {
					int lv = com.ultracapsuleadventure.soul.SoulData.getLevel(soul);
					created.addTag("soullevel-" + Math.max(1, Math.min(3, lv)));
				} catch (Exception ignored) {
				}
				try {
					com.ultracapsuleadventure.soul.SoulData.applyBaby(created, soul);
				} catch (Exception ignored) {
				}
				com.ultracapsuleadventure.soul.SoulAlly.makeAlly(serverLevel, created, player.getUUID());
				serverLevel.addFreshEntityWithPassengers(created);
				spawned++;
			} catch (Exception ignored) {
			}
		}
		tell(player, "영혼 " + spawned + "기를 소환했다! (H: 저장)", false);
	}

	private static ItemStack getVoidCapsule(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = player.getItemInHand(hand);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.VoidCapsuleItem) {
				return s;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() == ModItems.VOID_CAPSULE) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void handleVoidRelease(ServerPlayer player, int index) {
		ItemStack cap = getVoidCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		boolean ok = com.ultracapsuleadventure.voidcap.VoidErase.release(player, cap, index);
		tell(player, ok ? "풀어줬다!" : "풀어주기 실패!", true);
	}

	private static void handleVoidErase(ServerPlayer player, int index) {
		ItemStack cap = getVoidCapsule(player);
		if (cap.isEmpty()) {
			return;
		}
		String msg = com.ultracapsuleadventure.voidcap.VoidErase.erase(player, cap, index);
		tell(player, msg, false);
	}

	public static void openUtilityGui(ServerPlayer player, int kind, boolean mainHand) {
		try {
			ServerPlayNetworking.send(player, new OpenUtilityGuiPayload(kind, mainHand));
		} catch (Exception ignored) {
		}
	}

	private static ItemStack findUtilityCapsule(ServerPlayer player, net.minecraft.world.item.Item item, boolean mainHand) {
		try {
			ItemStack s = player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
			if (s.getItem() == item) {
				return s;
			}
			ItemStack o = player.getItemInHand(mainHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if (o.getItem() == item) {
				return o;
			}
		} catch (Exception ignored) {
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			try {
				ItemStack s = player.getInventory().getItem(i);
				if (s.getItem() == item) {
					return s;
				}
			} catch (Exception ignored) {
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isCapsuleItem(net.minecraft.world.item.Item item) {
		return item instanceof com.ultracapsuleadventure.item.UltraCapsuleAdventureItem
				|| item instanceof com.ultracapsuleadventure.item.SoulCapsuleItem
				|| item instanceof com.ultracapsuleadventure.item.GambleCapsuleItem
				|| item instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem;
	}

	/** 넣기용 아이템 탐색: 다른 손 → 손 → 인벤토리 순. 캡슐 자체·빈칸 제외 */
	private static ItemStack findDepositItem(ServerPlayer player, ItemStack cap, boolean mainHand) {
		try {
			ItemStack other = player.getItemInHand(mainHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if (!other.isEmpty() && other != cap && !isCapsuleItem(other.getItem())) {
				return other;
			}
		} catch (Exception ignored) {
		}
		try {
			ItemStack main = player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
			if (!main.isEmpty() && main != cap && !isCapsuleItem(main.getItem())) {
				return main;
			}
		} catch (Exception ignored) {
		}
		try {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (!s.isEmpty() && s != cap && !isCapsuleItem(s.getItem())) {
					return s;
				}
			}
		} catch (Exception ignored) {
		}
		return ItemStack.EMPTY;
	}

	private static void handleGambleInsert(ServerPlayer player, boolean mainHand) {
		ItemStack cap = findUtilityCapsule(player, ModItems.GAMBLE_CAPSULE, mainHand);
		if (cap.isEmpty()) {
			return;
		}
		if (com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(cap)) {
			tell(player, "이미 장전되어 있습니다.", true);
			return;
		}
		ItemStack other = ItemStack.EMPTY;
		try {
			other = findDepositItem(player, cap, mainHand);
		} catch (Exception ignored) {
		}
		if (other.isEmpty()) {
			tell(player, "넣을 아이템이 없습니다 (인벤토리 비어있음).", true);
			return;
		}
		if (isCapsuleItem(other.getItem())) {
			tell(player, "캡슐은 칸에 안 들어갑니다. 재료만 넣으세요.", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		String nm;
		int n;
		try {
			nm = com.ultracapsuleadventure.util.ItemNbt.displayName(other);
			n = other.getCount();
		} catch (Exception e) {
			return;
		}
		com.ultracapsuleadventure.item.GambleCapsuleItem.setBet(cap, registries, other);
		try {
			other.shrink(other.getCount());
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "베팅 장전: " + nm + " x" + n, true);
	}

	private static void handleGambleWithdraw(ServerPlayer player) {
		ItemStack cap = findUtilityCapsule(player, ModItems.GAMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			return;
		}
		if (!com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(cap)) {
			tell(player, "꺼낼 베팅이 없습니다.", true);
			return;
		}
		if (com.ultracapsuleadventure.item.GambleCapsuleItem.isActive(cap)) {
			tell(player, "진행 중에는 취소 불가!", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		ItemStack bet = com.ultracapsuleadventure.item.GambleCapsuleItem.readBet(cap, registries);
		com.ultracapsuleadventure.item.GambleCapsuleItem.clearBet(cap);
		if (!bet.isEmpty()) {
			com.ultracapsuleadventure.util.TameUtil.giveToPlayer(player, bet);
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "베팅 회수!", true);
	}

	private static void handleGambleStart(ServerPlayer player) {
		ItemStack cap = findUtilityCapsule(player, ModItems.GAMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			return;
		}
		if (!com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(cap)) {
			tell(player, "먼저 아이템을 넣으세요.", true);
			return;
		}
		if (com.ultracapsuleadventure.item.GambleCapsuleItem.isActive(cap)) {
			tell(player, "이미 진행 중!", true);
			return;
		}
		com.ultracapsuleadventure.item.GambleCapsuleItem.setUntil(cap, System.currentTimeMillis() + 10000L);
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "도박 시작! 10초 후 결과 발표...", false);
	}

	private static void handleDisInsert(ServerPlayer player, boolean mainHand) {
		ItemStack cap = findUtilityCapsule(player, ModItems.DISASSEMBLE_CAPSULE, mainHand);
		if (cap.isEmpty()) {
			return;
		}
		if (com.ultracapsuleadventure.item.DisassembleCapsuleItem.hasStored(cap)) {
			tell(player, "이미 들어있습니다.", true);
			return;
		}
		ItemStack other = ItemStack.EMPTY;
		try {
			other = findDepositItem(player, cap, mainHand);
		} catch (Exception ignored) {
		}
		if (other.isEmpty()) {
			tell(player, "다른 손·인벤토리에 넣을 아이템이 없습니다.", true);
			return;
		}
		if (isCapsuleItem(other.getItem())) {
			tell(player, "캡슐은 칸에 안 들어갑니다. 재료만 넣으세요.", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		com.ultracapsuleadventure.item.DisassembleCapsuleItem.setStored(cap, registries, other);
		ItemStack probe;
		try {
			probe = other.copy();
		} catch (Exception e) {
			probe = ItemStack.EMPTY;
		}
		try {
			other.shrink(other.getCount());
		} catch (Exception ignored) {
		}
		try {
			String expect = "";
			if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
				java.util.List<ItemStack> outs = com.ultracapsuleadventure.util.GambleLogic.findReverse(sl, probe);
				if (outs.isEmpty()) {
					expect = "분해 불가 (조합법 없음)";
				} else {
					StringBuilder sb = new StringBuilder("예상: ");
					for (ItemStack o : outs) {
						sb.append(com.ultracapsuleadventure.util.ItemNbt.displayName(o)).append(' ');
					}
					expect = sb.toString().trim();
				}
			}
			com.ultracapsuleadventure.item.DisassembleCapsuleItem.setExpect(cap, expect);
		} catch (Exception ignored) {
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "장전 완료!", true);
	}

	private static void handleDisWithdraw(ServerPlayer player) {
		ItemStack cap = findUtilityCapsule(player, ModItems.DISASSEMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			return;
		}
		if (!com.ultracapsuleadventure.item.DisassembleCapsuleItem.hasStored(cap)) {
			tell(player, "꺼낼 아이템이 없습니다.", true);
			return;
		}
		net.minecraft.core.HolderLookup.Provider registries;
		try {
			registries = player.registryAccess();
		} catch (Exception e) {
			return;
		}
		ItemStack stored = com.ultracapsuleadventure.item.DisassembleCapsuleItem.readStored(cap, registries);
		com.ultracapsuleadventure.item.DisassembleCapsuleItem.clearStored(cap);
		if (!stored.isEmpty()) {
			com.ultracapsuleadventure.util.TameUtil.giveToPlayer(player, stored);
		}
		com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(player);
		tell(player, "회수!", true);
	}

	private static void handleDisassemble(ServerPlayer player) {
		ItemStack cap = findUtilityCapsule(player, ModItems.DISASSEMBLE_CAPSULE, true);
		if (cap.isEmpty()) {
			return;
		}
		String msg = com.ultracapsuleadventure.util.GambleLogic.disassemble(player, cap);
		tell(player, msg, false);
	}
}



