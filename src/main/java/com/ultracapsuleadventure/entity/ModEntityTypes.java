package com.ultracapsuleadventure.entity;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {

	public static final ResourceKey<EntityType<?>> GLOOM_DEVOURER_ID = ResourceKey.create(Registries.ENTITY_TYPE,
			UltraCapsuleAdventureMod.id("gloom_devourer"));

	public static final ResourceKey<EntityType<?>> MECH_ROBOT_ID = ResourceKey.create(Registries.ENTITY_TYPE,
			UltraCapsuleAdventureMod.id("mech_robot"));

	public static EntityType<GloomDevourerEntity> GLOOM_DEVOURER;
	public static EntityType<com.ultracapsuleadventure.mech.MechRobotEntity> MECH_ROBOT;
	public static EntityType<com.ultracapsuleadventure.medusa.MedusaEntity> MEDUSA;
	public static EntityType<com.ultracapsuleadventure.medusa.StatueEntity> STATUE;
	public static EntityType<com.ultracapsuleadventure.doppel.DoppelgangerEntity> DOPPELGANGER;

	public static void register() {
		EntityType.Builder<GloomDevourerEntity> builder = EntityType.Builder
				.<GloomDevourerEntity>of(GloomDevourerEntity::new, MobCategory.MONSTER)
				.sized(1.2F, 2.2F);
		GLOOM_DEVOURER = Registry.register(BuiltInRegistries.ENTITY_TYPE, GLOOM_DEVOURER_ID, builder.build(GLOOM_DEVOURER_ID));
		EntityType.Builder<com.ultracapsuleadventure.mech.MechRobotEntity> mechBuilder = EntityType.Builder
				.<com.ultracapsuleadventure.mech.MechRobotEntity>of(com.ultracapsuleadventure.mech.MechRobotEntity::new, MobCategory.CREATURE)
				.sized(1.4F, 2.8F);
		MECH_ROBOT = Registry.register(BuiltInRegistries.ENTITY_TYPE, MECH_ROBOT_ID, mechBuilder.build(MECH_ROBOT_ID));
		ResourceKey<EntityType<?>> medusaId = ResourceKey.create(Registries.ENTITY_TYPE,
				UltraCapsuleAdventureMod.id("medusa"));
		EntityType.Builder<com.ultracapsuleadventure.medusa.MedusaEntity> medusaBuilder = EntityType.Builder
				.<com.ultracapsuleadventure.medusa.MedusaEntity>of(com.ultracapsuleadventure.medusa.MedusaEntity::new, MobCategory.MONSTER)
				.sized(0.8F, 2.0F);
		MEDUSA = Registry.register(BuiltInRegistries.ENTITY_TYPE, medusaId, medusaBuilder.build(medusaId));
		ResourceKey<EntityType<?>> statueId = ResourceKey.create(Registries.ENTITY_TYPE,
				UltraCapsuleAdventureMod.id("medusa_statue"));
		EntityType.Builder<com.ultracapsuleadventure.medusa.StatueEntity> statueBuilder = EntityType.Builder
				.<com.ultracapsuleadventure.medusa.StatueEntity>of(com.ultracapsuleadventure.medusa.StatueEntity::new, MobCategory.MISC)
				.sized(1.2F, 2.5F);
		STATUE = Registry.register(BuiltInRegistries.ENTITY_TYPE, statueId, statueBuilder.build(statueId));
		ResourceKey<EntityType<?>> doppelId = ResourceKey.create(Registries.ENTITY_TYPE,
				UltraCapsuleAdventureMod.id("doppelganger"));
		EntityType.Builder<com.ultracapsuleadventure.doppel.DoppelgangerEntity> doppelBuilder = EntityType.Builder
				.<com.ultracapsuleadventure.doppel.DoppelgangerEntity>of(com.ultracapsuleadventure.doppel.DoppelgangerEntity::new, MobCategory.MONSTER)
				.sized(0.9F, 2.3F);
		DOPPELGANGER = Registry.register(BuiltInRegistries.ENTITY_TYPE, doppelId, doppelBuilder.build(doppelId));
	}

	public static void registerAttributes() {
		try {
			FabricDefaultAttributeRegistry.register(GLOOM_DEVOURER, GloomDevourerEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(MECH_ROBOT, com.ultracapsuleadventure.mech.MechRobotEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(MEDUSA, com.ultracapsuleadventure.medusa.MedusaEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(STATUE, com.ultracapsuleadventure.medusa.StatueEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(DOPPELGANGER, com.ultracapsuleadventure.doppel.DoppelgangerEntity.createAttributes());
		} catch (Exception ignored) {
		}
	}

	public static boolean isDevourer(Entity e) {
		try {
			return e != null && e.getType() == GLOOM_DEVOURER;
		} catch (Exception ex) {
			return false;
		}
	}
}


