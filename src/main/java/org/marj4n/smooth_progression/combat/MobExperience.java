package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.config.BossScalingConfig;
import org.marj4n.smooth_progression.boss.BossProgression;
import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.ExperienceManager;
import org.marj4n.smooth_progression.progression.ProgressionManager;
import org.marj4n.smooth_progression.config.ProgressionXpConfig;
import org.marj4n.smooth_progression.progression.XpSource;

import java.util.UUID;

public final class MobExperience {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    // Must match MobLevelScaling.HEALTH_MODIFIER_ID.
    private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString(
                    "f6a2791b-857e-48c2-9ab9-df4e22b5f8d0"
            );

    private static final double XP_PER_LEVEL = 0.01D;

    private static final double MAX_XP_MULTIPLIER = 3.0D;

    private MobExperience() {
    }

    // =========================================================
    // MOB KILL
    // =========================================================

    public static void onMobKilled(
            LivingEntity victim,
            DamageSource damageSource
    ) {

        if (victim == null || damageSource == null) {
            return;
        }

        // Disable all Smooth Progression mob XP when disabled.
        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return;
        }

        // PvP XP is disabled.
        if (victim instanceof PlayerEntity) {
            return;
        }

        ServerPlayerEntity player =
                KillOwnerResolver.resolve(damageSource);

        if (player == null) {
            return;
        }

        BossScalingConfig.BossLevel boss = BossProgression.getBoss(victim);

        if (boss != null) {
            awardBossExperience(player, victim, boss);
            return;
        }

        long experience = calculateExperience(victim);
        if (experience <= 0L) return;

        // Spawner farming remains possible, but is intentionally inefficient.
        if (MobXpOrigin.isSpawner(victim)) {
            double multiplier = ProgressionXpConfig.get().combat.spawner_multiplier;
            experience = Math.max(1L, Math.round(experience * multiplier));
        }

        ExperienceApi.addExperience(player, experience, XpSource.MOB_KILL);
    }

    private static void awardBossExperience(ServerPlayerEntity player, LivingEntity victim, BossScalingConfig.BossLevel boss) {
        ProgressionXpConfig.Combat c = ProgressionXpConfig.get().combat;
        int playerLevel = ProgressionManager.get(player).getLevel();
        int bossLevel = Math.max(1, boss.min_level);

        double relevance;
        int difference = bossLevel - playerLevel;
        if (Math.abs(difference) <= c.boss_level_match_range) relevance = c.boss_level_match_multiplier;
        else if (difference > c.boss_level_match_range) relevance = c.boss_above_level_multiplier;
        else if (playerLevel - bossLevel >= c.boss_far_below_threshold) relevance = c.boss_far_below_level_multiplier;
        else relevance = c.boss_below_level_multiplier;

        long levelCost = ExperienceManager.getRequiredExperience(bossLevel);
        double reward = levelCost * c.boss_reward_fraction * relevance * boss.xp_multiplier;

        String bossId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
        String firstKillTag = "smoothprogression.boss_killed." + bossId.replace(':', '.');
        boolean firstKill = !player.getCommandTags().contains(firstKillTag);
        reward *= firstKill ? c.boss_first_kill_multiplier : c.boss_repeat_multiplier;
        if (firstKill) player.addCommandTag(firstKillTag);

        if (!Double.isFinite(reward) || reward <= 0D) return;
        long experience = reward >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, Math.round(reward));
        ExperienceApi.addExperience(player, experience, XpSource.BOSS_KILL);
    }

    // =========================================================
    // EXPERIENCE CALCULATION
    //
    // Normal mob:
    // Base Max Health / 2 * Level Multiplier
    //
    // Boss:
    // Base Max Health / 2
    //
    // Blacklisted / excluded mobs:
    // Base Max Health / 2
    //
    // Spawner penalty is handled in onMobKilled().
    // =========================================================

    public static long calculateExperience(
            LivingEntity entity
    ) {

        if (entity == null) {
            return 0L;
        }

        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return 0L;
        }

        double maxHealth = entity.getMaxHealth();

        if (!Double.isFinite(maxHealth)
                || maxHealth <= 0.0D) {

            return 0L;
        }

        // Remove our HP modifier if present.
        // This prevents scaled HP from inflating XP.
        double baseHealth = getHealthBeforeScaling(
                entity,
                maxHealth
        );

        double calculated = baseHealth / 2.0D;

        BossScalingConfig.BossLevel boss = BossProgression.getBoss(entity);
        if (boss != null) {
            // Reverse boss HP modifier too: XP is based on original mod HP.
            EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            EntityAttributeModifier modifier = attribute == null ? null : attribute.getModifier(BossProgression.HEALTH_MODIFIER_ID);
            if (modifier != null && 1.0D + modifier.getValue() > 0.0D) {
                calculated /= (1.0D + modifier.getValue());
            }
            BossScalingConfig settings = BossScalingConfig.get();
            calculated *= Math.min(settings.max_xp_multiplier,
                    1.0D + settings.xp_per_level * (boss.min_level - 1)) * boss.xp_multiplier;
        }

        // Only eligible mobs receive level-based bonus XP.
        if (isEligibleForLevelBonus(entity, config)) {

            int level = getMobLevel(entity);

            if (level > 0) {

                double multiplier = Math.min(
                        MAX_XP_MULTIPLIER,
                        1.0D + XP_PER_LEVEL * (level - 1)
                );

                if (Double.isFinite(multiplier)
                        && multiplier > 0.0D) {

                    calculated *= multiplier;
                }
            }
        }

        if (!Double.isFinite(calculated)
                || calculated >= Long.MAX_VALUE) {

            return Long.MAX_VALUE;
        }

        return Math.max(
                1L,
                Math.round(calculated)
        );
    }

    // =========================================================
    // LEVEL BONUS ELIGIBILITY
    // =========================================================

    private static boolean isEligibleForLevelBonus(
            LivingEntity entity,
            MobScalingConfig config
    ) {

        String entityId = Registries.ENTITY_TYPE
                .getId(entity.getType())
                .toString();

        // Bosses retain normal HP-based XP,
        // but receive no mob-level XP multiplier.
        if (BossScalingConfig.get().contains(entityId)) {
            return false;
        }

        // Blacklisted mobs retain normal HP-based XP.
        if (config.entities.blacklist.contains(entityId)) {
            return false;
        }

        // Whitelist mode:
        // Non-whitelisted mobs retain normal HP-based XP.
        if (config.entities.mode.equals("whitelist")
                && !config.entities.whitelist.contains(entityId)) {

            return false;
        }

        // Disabled or unconfigured dimensions:
        // Normal HP-based XP only.
        String dimensionId = entity.getWorld()
                .getRegistryKey()
                .getValue()
                .toString();

        MobScalingConfig.Dimension dimension =
                config.dimensions.get(dimensionId);

        return dimension != null && dimension.enabled;
    }

    // =========================================================
    // READ MOB LEVEL
    // =========================================================

    private static int getMobLevel(
            LivingEntity entity
    ) {

        for (String tag : entity.getCommandTags()) {

            if (!tag.startsWith(LEVEL_PREFIX)) {
                continue;
            }

            try {

                int level = Integer.parseInt(
                        tag.substring(
                                LEVEL_PREFIX.length()
                        )
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
    // REMOVE OUR HP SCALING FROM XP CALCULATION
    //
    // Attribute modifier uses MULTIPLY_TOTAL:
    //
    // scaledHealth = previousHealth * (1 + modifierValue)
    //
    // Reverse only our modifier.
    // =========================================================

    private static double getHealthBeforeScaling(
            LivingEntity entity,
            double maxHealth
    ) {

        EntityAttributeInstance attribute =
                entity.getAttributeInstance(
                        EntityAttributes.GENERIC_MAX_HEALTH
                );

        if (attribute == null) {
            return maxHealth;
        }

        EntityAttributeModifier modifier =
                attribute.getModifier(
                        HEALTH_MODIFIER_ID
                );

        if (modifier == null) {
            return maxHealth;
        }

        double multiplier =
                1.0D + modifier.getValue();

        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0D) {

            return maxHealth;
        }

        double baseHealth =
                maxHealth / multiplier;

        if (!Double.isFinite(baseHealth)
                || baseHealth <= 0.0D) {

            return maxHealth;
        }

        return baseHealth;
    }
}