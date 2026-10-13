# Ultra Capsule Adventure (`partnercap`) — AI Context

Minecraft Java **26.3 (Wilderness Bound)** / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3 /
Loom 1.18-SNAPSHOT / Java 25. Mod ID `partnercap`, single creative tab `partnercap:main`.

Read `README.md` for full feature docs (Korean). This file is the code map.

## Build / Run

```bat
.\gradlew.bat runClient   # test (auto rebuilds; must fully restart game after edits)
.\gradlew.bat build       # jar -> build\libs\
```

## Entry Points

- `src/main/java/com/partnercap/PartnerCapMod.java` — `onInitialize()` registers everything in order:
  fluids/blocks → entities+attributes → items+tab → silicate/meteor/worldgen → packets →
  `SoulBody`, `MechRobotEntity::tickThrows`, `SoulEvents`, `MedusaEvents`, `GambleLogic`,
  `VoidErase`, `TitanData.tickRepair` → `UseEntityCallback` (capture priority:
  petrified-mine > soul-recapture > void-capture > partner-capture).
- `src/client/java/com/partnercap/client/PartnerCapClient.java` — keys (`C` manage, `H` soul toggle),
  entity renderers, model layers, `ArmorRenderer` (blindfold), mech cockpit view flag (`MechRenderState.cockpitView`) is set in `MechRobotRenderer.extractRenderState`.
- `src/main/java/com/partnercap/item/ModItems.java` — all 13 items + creative tab.

## Module Map

| Area | Key files |
|---|---|
| Partner capsule (tamed-mob storage) | `item/PartnerCapsuleItem.java`, `data/PartnerData.java`, `util/TameUtil.java` |
| Soul capsule + Flame Sword (kill→ally souls, Lv1-3 merge) | `item/SoulCapsuleItem.java`, `item/FlameSwordItem.java`, `soul/SoulData.java`, `soul/SoulEvents.java`, `soul/SoulAlly.java`, `soul/SoulBody.java` |
| Gamble capsule (10s bet, chance/mult) | `item/GambleCapsuleItem.java`, `util/GambleLogic.java` |
| Disassemble capsule (reverse-recipe) | `item/DisassembleCapsuleItem.java`, `util/GambleLogic.java` (`findReverse`/`disassemble`) |
| Void capsule (capture/release/erase+rewind) | `item/VoidCapsuleItem.java`, `voidcap/VoidData.java`, `voidcap/VoidErase.java` |
| Titan capsule (gradual repair/anvil/head charge) | `titan/TitanCapsuleItem.java`, `titan/TitanData.java` |
| Mech capsule + rideable robot | `mech/MechCapsuleItem.java`, `mech/MechRobotEntity.java`, `mech/MechEvents.java` (rider shield + aggro redirect), `client/entity/Mech*` (cockpit = `cockpit` part in `MechRobotModel`, texture `mech_robot.png` is 128x128: top 128x64 = exterior, y64+ = cockpit; visible only when `cockpitView`, hides the whole exterior) |
| Medusa / petrify / head / blindfold | `medusa/MedusaEntity.java` (gaze 16m/cos0.86/250ms lock), `medusa/MedusaEvents.java`, `medusa/Petrify.java`, `medusa/StatueEntity.java`, `medusa/MedusaHeadItem.java` (durability 6 = 5 uses), `medusa/BlindfoldItem.java` (helmet slot, blindness, gaze immune) |
| Silicate gear + meteor | `silicate/ModSilicate.java`, `silicate/SilicateGear.java`, `silicate/MeteorEvents.java`, `silicate/SilicateBlock.java` |
| Gloom Devourer + void liquid | `entity/GloomDevourerEntity.java`, `entity/ModEntityTypes.java`, `fluid/ModFluids.java`, `fluid/ModBlocks.java`, `fluid/VoidFluid.java`, `fluid/ModWorldGen.java` |
| Flame Mong + wand/heart/yeolgwan/blood | `flame/FlameMongEntity.java` (desert spawn, lava home, breath, wand slow), `flame/FlameMongEvents.java` (spawn/drops/combat), `flame/ModFlame.java` (items/block/entity reg), `flame/YeolgwanBlock.java` + `YeolgwanBlockEntity.java` (fuel-free 2x blast smelt), `flame/TranscendWandItem.java`, `flame/FlameHeartItem.java`, `flame/DragonBloodBottleItem.java`, `flame/FlameBreath.java` (F-key breath), `client/entity/FlameMong*` |
| Enchant integration (vanilla enchants on capsules) | `util/EnchantLevels.java`, tags in `data/minecraft/tags/item/enchantable/` |
| Network (28 C2S + 1 S2C) | `network/ModPackets.java` — `record` payloads + `StreamCodec`, handlers run via `server.execute()` |
| Client UI (`C` tabs PARTNER/SOUL/VOID/TITAN/GAMBLE/DIS/MECH, `H`) | `client/PartnerCapScreen.java`, `client/PartnerCapDetailScreen.java`, `client/GambleCapScreen.java`, `client/DisassembleCapScreen.java`, `client/EntityPreview.java`, `client/GuiDraw.java` |
| Mixins (server 5 + client 11) | `partnercap.mixins.json`, `partnercap.client.mixins.json`; petrify freeze/render-stone, mech rider hide |

## Storage Conventions

All state in `DataComponents.CUSTOM_DATA` (26.x has no `ItemStack.getTag()`):
`partner_cap.pets[]`, `soul_cap.souls[]+autoOut`, `void_cap.voids[]`,
`titan_cap.{stored,doneAt,anvil,coolUntil}`, `titan_repair.{doneAt,dur,total}` (on target item),
`gamble_cap.{bet,until}`, `dis_cap.{item,expect}`, `medusa_head` (legacy, migrating to damage value).
Entity snapshots via `saveAsPassenger`/`loadEntityRecursive` + `PC_*` cache fields.
`CompoundTag` getters return `Optional` — always `orElse`.
After any server-side capsule mutation call `PartnerCapsuleItem.syncCapsule(player)`.

## 26.3 API Notes

- `Identifier.fromNamespaceAndPath`, `BuiltInRegistries`, `Item.Properties().setId(key)` required.
- `CustomPacketPayload + StreamCodec + PayloadTypeRegistry.serverboundPlay()`.
- No `PickaxeItem` class — pickaxe check is `stack.isCorrectToolForDrops(Blocks.COBBLESTONE...)`.
- Villager trades are data-driven only (`data/*/villager_trade/**` + `data/minecraft/tags/villager_trade/**`).
- Enchant applicability is data-driven (`Enchantment.canEnchant` = `supported_items` tag check);
  table needs `ENCHANTABLE` component on the item.
- Ridable entities MUST override `getControllingPassenger()` or the server snap-backs every
  `MoveVehicle` packet; movement flows `aiStep → travelRidden → travel()` — never call `travel()` manually in `tick()`.

## Code Style

- Defensive `try { ... } catch (Exception ignored) {}` everywhere — keep it, server tick must never die.
- UI strings/messages/tooltips in Korean. Keep `README.md` in sync when behavior changes.
- Never commit `build/`, `run/`, `.gradle/`, `.idea/` (gitignored).
