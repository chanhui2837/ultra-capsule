package com.partnercap.entity;

import com.partnercap.PartnerCapMod;

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
			PartnerCapMod.id("gloom_devourer"));

	public static final ResourceKey<EntityType<?>> MECH_ROBOT_ID = ResourceKey.create(Registries.ENTITY_TYPE,
			PartnerCapMod.id("mech_robot"));

	public static EntityType<GloomDevourerEntity> GLOOM_DEVOURER;
	public static EntityType<com.partnercap.mech.MechRobotEntity> MECH_ROBOT;
	public static EntityType<com.partnercap.medusa.MedusaEntity> MEDUSA;
	public static EntityType<com.partnercap.medusa.StatueEntity> STATUE;

	public static void register() {
		EntityType.Builder<GloomDevourerEntity> builder = EntityType.Builder
				.<GloomDevourerEntity>of(GloomDevourerEntity::new, MobCategory.MONSTER)
				.sized(1.2F, 2.2F);
		GLOOM_DEVOURER = Registry.register(BuiltInRegistries.ENTITY_TYPE, GLOOM_DEVOURER_ID, builder.build(GLOOM_DEVOURER_ID));
		EntityType.Builder<com.partnercap.mech.MechRobotEntity> mechBuilder = EntityType.Builder
				.<com.partnercap.mech.MechRobotEntity>of(com.partnercap.mech.MechRobotEntity::new, MobCategory.CREATURE)
				.sized(1.4F, 2.8F);
		MECH_ROBOT = Registry.register(BuiltInRegistries.ENTITY_TYPE, MECH_ROBOT_ID, mechBuilder.build(MECH_ROBOT_ID));
		ResourceKey<EntityType<?>> medusaId = ResourceKey.create(Registries.ENTITY_TYPE,
				PartnerCapMod.id("medusa"));
		EntityType.Builder<com.partnercap.medusa.MedusaEntity> medusaBuilder = EntityType.Builder
				.<com.partnercap.medusa.MedusaEntity>of(com.partnercap.medusa.MedusaEntity::new, MobCategory.MONSTER)
				.sized(0.8F, 2.0F);
		MEDUSA = Registry.register(BuiltInRegistries.ENTITY_TYPE, medusaId, medusaBuilder.build(medusaId));
		ResourceKey<EntityType<?>> statueId = ResourceKey.create(Registries.ENTITY_TYPE,
				PartnerCapMod.id("medusa_statue"));
		EntityType.Builder<com.partnercap.medusa.StatueEntity> statueBuilder = EntityType.Builder
				.<com.partnercap.medusa.StatueEntity>of(com.partnercap.medusa.StatueEntity::new, MobCategory.MISC)
				.sized(1.2F, 2.5F);
		STATUE = Registry.register(BuiltInRegistries.ENTITY_TYPE, statueId, statueBuilder.build(statueId));
	}

	public static void registerAttributes() {
		try {
			FabricDefaultAttributeRegistry.register(GLOOM_DEVOURER, GloomDevourerEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(MECH_ROBOT, com.partnercap.mech.MechRobotEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(MEDUSA, com.partnercap.medusa.MedusaEntity.createAttributes());
		} catch (Exception ignored) {
		}
		try {
			FabricDefaultAttributeRegistry.register(STATUE, com.partnercap.medusa.StatueEntity.createAttributes());
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
