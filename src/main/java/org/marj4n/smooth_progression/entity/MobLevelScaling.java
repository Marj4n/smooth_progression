package org.marj4n.smooth_progression.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import org.marj4n.smooth_progression.config.MobScalingConfig;

import java.util.List;
import java.util.UUID;

public final class MobLevelScaling {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    private static final String ELITE_TAG =
            "smoothprogression.elite";

    // =========================================================
    // ATTRIBUTE MODIFIER UUIDs
    // =========================================================

    // Original HP modifier.
    // Do not change: existing mobs may already have this modifier.
    private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString(
                    "f6a2791b-857e-48c2-9ab9-df4e22b5f8d0"
            );

    // Legacy melee damage modifier.
    // Keep this UUID to remove the old modifier.
    private static final UUID DAMAGE_MODIFIER_ID =
            UUID.fromString(
                    "8bde76d0-cd5c-4a62-bcc5-b87b7e1a904e"
            );

    // New elite movement speed modifier.
    private static final UUID ELITE_SPEED_MODIFIER_ID =
            UUID.fromString(
                    "5ad64f19-56b1-44cc-9e82-39c1f04e4f21"
            );

    // New elite knockback resistance modifier.
    private static final UUID ELITE_KNOCKBACK_MODIFIER_ID =
            UUID.fromString(
                    "7f2ac68e-944c-47d8-8e4b-2c61bb7d3d92"
            );

    // =========================================================
    // ELITE ATTRIBUTE VALUES
    // =========================================================

    // +15% movement speed.
    private static final double ELITE_SPEED_BONUS = 0.15D;

    // +0.20 knockback resistance.
    private static final double ELITE_KNOCKBACK_BONUS = 0.20D;

    private MobLevelScaling() {
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register(
                MobLevelScaling::onEntityLoad
        );
    }

    // =========================================================
    // ENTITY LOAD
    // =========================================================

    private static void onEntityLoad(
            Entity entity,
            ServerWorld world
    ) {

        // Only hostile mobs.
        // Ghast and other non-HostileEntity mobs are not included.
        if (!(entity instanceof HostileEntity mob)) {
            return;
        }

        // Remove old melee damage scaling modifier.
        // Universal damage scaling is handled elsewhere.
        removeLegacyDamageModifier(mob);

        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return;
        }

        String id = Registries.ENTITY_TYPE
                .getId(mob.getType())
                .toString();

        if (config.entities.blacklist.contains(id)) {
            return;
        }

        // Dedicated boss scaling is not implemented yet.
        if (config.bosses.entities.contains(id)) {
            return;
        }

        if (config.entities.mode.equals("whitelist")
                && !config.entities.whitelist.contains(id)) {

            return;
        }

        String dimensionId = world.getRegistryKey()
                .getValue()
                .toString();

        MobScalingConfig.Dimension dimension =
                config.dimensions.get(dimensionId);

        if (dimension == null || !dimension.enabled) {
            return;
        }

        MobScalingConfig.Override override =
                config.entities.overrides.get(id);

        int level = readLevel(mob);

        boolean firstAssignment = level < 1;

        // =====================================================
        // FIRST LEVEL ASSIGNMENT
        // =====================================================

        if (firstAssignment) {

            level = calculateLevel(
                    mob,
                    world,
                    dimension,
                    config.level_curve
            );

            if (override != null) {

                level = Math.max(
                        level,
                        override.minimum_level
                );
            }

            level = Math.min(
                    config.level_curve.max_level,
                    level
            );

            mob.addCommandTag(
                    LEVEL_PREFIX + level
            );

            // Elite is rolled only once.
            if (config.elite.enabled
                    && (override == null || override.elite_enabled)
                    && world.random.nextDouble()
                    < config.elite.spawn_chance) {

                mob.addCommandTag(ELITE_TAG);
            }
        }

        // =====================================================
        // APPLY SCALING
        // =====================================================

        applyHealthScaling(
                mob,
                level,
                firstAssignment,
                override,
                config
        );

        // Elite attributes are applied independently of HP.
        // This is important because HP may already have
        // its persistent modifier when an entity reloads.
        applyEliteAttributes(mob);
        EliteEquipment.apply(mob, level);
    }

    // =========================================================
    // HEALTH SCALING
    // =========================================================

    private static void applyHealthScaling(
            HostileEntity mob,
            int level,
            boolean firstAssignment,
            MobScalingConfig.Override override,
            MobScalingConfig config
    ) {

        EntityAttributeInstance health =
                mob.getAttributeInstance(
                        EntityAttributes.GENERIC_MAX_HEALTH
                );

        if (health == null) {
            return;
        }

        // Prevent duplicate HP modifiers.
        if (health.getModifier(HEALTH_MODIFIER_ID) != null) {
            return;
        }

        double multiplier = Math.min(
                config.scaling.max_health_multiplier,
                1.0D + config.scaling.health_per_level
                        * (level - 1)
        );

        if (override != null) {

            multiplier *= override.health_multiplier;
        }

        if (mob.getCommandTags().contains(ELITE_TAG)) {

            multiplier *= config.elite.health_multiplier;
        }

        if (!Double.isFinite(multiplier)
                || multiplier <= 1.0D) {

            return;
        }

        float previousMax = mob.getMaxHealth();
        float previousHealth = mob.getHealth();

        health.addPersistentModifier(
                new EntityAttributeModifier(
                        HEALTH_MODIFIER_ID,
                        "Smooth Progression area HP",
                        multiplier - 1.0D,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );

        // Preserve HP percentage on first assignment.
        if (previousMax > 0.0F) {

            float healthRatio = Math.max(
                    0.0F,
                    Math.min(1.0F, previousHealth / previousMax)
            );

            mob.setHealth(
                    Math.min(
                            mob.getMaxHealth(),
                            healthRatio * mob.getMaxHealth()
                    )
            );
        }
    }

    // =========================================================
    // ELITE ATTRIBUTES
    // =========================================================

    private static void applyEliteAttributes(
            HostileEntity mob
    ) {

        // Normal mobs receive no elite modifiers.
        if (!mob.getCommandTags().contains(ELITE_TAG)) {
            return;
        }

        applyEliteSpeed(mob);
        applyEliteKnockbackResistance(mob);
    }

    // =========================================================
    // ELITE MOVEMENT SPEED
    // =========================================================

    private static void applyEliteSpeed(
            HostileEntity mob
    ) {

        EntityAttributeInstance speed =
                mob.getAttributeInstance(
                        EntityAttributes.GENERIC_MOVEMENT_SPEED
                );

        // Some modded mobs may not expose this attribute.
        if (speed == null) {
            return;
        }

        // Already applied: do not stack.
        if (speed.getModifier(
                ELITE_SPEED_MODIFIER_ID
        ) != null) {

            return;
        }

        speed.addPersistentModifier(
                new EntityAttributeModifier(
                        ELITE_SPEED_MODIFIER_ID,
                        "Smooth Progression elite speed",
                        ELITE_SPEED_BONUS,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );
    }

    // =========================================================
    // ELITE KNOCKBACK RESISTANCE
    // =========================================================

    private static void applyEliteKnockbackResistance(
            HostileEntity mob
    ) {

        EntityAttributeInstance knockback =
                mob.getAttributeInstance(
                        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE
                );

        if (knockback == null) {
            return;
        }

        // Already applied: do not stack.
        if (knockback.getModifier(
                ELITE_KNOCKBACK_MODIFIER_ID
        ) != null) {

            return;
        }

        knockback.addPersistentModifier(
                new EntityAttributeModifier(
                        ELITE_KNOCKBACK_MODIFIER_ID,
                        "Smooth Progression elite knockback resistance",
                        ELITE_KNOCKBACK_BONUS,
                        EntityAttributeModifier.Operation.ADDITION
                )
        );
    }

    // =========================================================
    // REMOVE LEGACY DAMAGE MODIFIER
    // =========================================================

    private static void removeLegacyDamageModifier(
            HostileEntity mob
    ) {

        EntityAttributeInstance damage =
                mob.getAttributeInstance(
                        EntityAttributes.GENERIC_ATTACK_DAMAGE
                );

        if (damage == null) {
            return;
        }

        if (damage.getModifier(DAMAGE_MODIFIER_ID) != null) {

            damage.removeModifier(DAMAGE_MODIFIER_ID);
        }
    }

    // =========================================================
    // READ LEVEL
    // =========================================================

    private static int readLevel(
            Entity entity
    ) {

        for (String tag : entity.getCommandTags()) {

            if (!tag.startsWith(LEVEL_PREFIX)) {
                continue;
            }

            try {

                int level = Integer.parseInt(
                        tag.substring(LEVEL_PREFIX.length())
                );

                if (level >= 1) {
                    return level;
                }

            } catch (NumberFormatException ignored) {

                // Ignore malformed level tags.
            }
        }

        return 0;
    }

    // =========================================================
    // CALCULATE LEVEL
    // =========================================================

    private static int calculateLevel(
            Entity entity,
            ServerWorld world,
            MobScalingConfig.Dimension dimension,
            MobScalingConfig.LevelCurve curve
    ) {

        String dimensionId = world.getRegistryKey()
                .getValue()
                .toString();

        BlockPos origin;

        if (dimensionId.equals("minecraft:overworld")) {

            origin = world.getServer()
                    .getOverworld()
                    .getSpawnPos();

        } else {

            // Nether and End use their own coordinate origin.
            origin = BlockPos.ORIGIN;
        }

        double dx = entity.getX() - origin.getX();
        double dz = entity.getZ() - origin.getZ();

        double distance = Math.hypot(dx, dz);

        distance *= dimension.distance_multiplier;

        return Math.max(
                dimension.minimum_level,
                Math.min(
                        curve.max_level,
                        levelFromCurve(distance, curve)
                )
        );
    }

    // =========================================================
    // LEVEL CURVE
    // =========================================================

    private static int levelFromCurve(
            double distance,
            MobScalingConfig.LevelCurve curve
    ) {

        List<MobScalingConfig.LevelTier> tiers =
                curve.tiers;

        for (int i = 0; i < tiers.size() - 1; i++) {

            MobScalingConfig.LevelTier start =
                    tiers.get(i);

            MobScalingConfig.LevelTier end =
                    tiers.get(i + 1);

            if (distance < end.distance) {

                double progress =
                        (distance - start.distance)
                                / (end.distance - start.distance);

                progress = Math.max(
                        0.0D,
                        Math.min(1.0D, progress)
                );

                double level =
                        start.level
                                + progress * (end.level - start.level);

                return Math.min(
                        curve.max_level,
                        (int) Math.floor(level)
                );
            }
        }

        // Beyond the final tier:
        // +1 level per endgame_blocks_per_level blocks.
        MobScalingConfig.LevelTier last =
                tiers.get(tiers.size() - 1);

        double extraLevels = Math.floor(
                Math.max(
                        0.0D,
                        distance - last.distance
                ) / curve.endgame_blocks_per_level
        );

        double finalLevel = Math.min(
                curve.max_level,
                last.level + extraLevels
        );

        return (int) finalLevel;
    }
}