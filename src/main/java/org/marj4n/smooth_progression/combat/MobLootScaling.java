package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;

import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.config.BossScalingConfig;

public final class MobLootScaling {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    private static final int MAX_LEVEL = 200;

    private static final int MAX_BONUS_LOOTING = 10;

    // Active only while an eligible mob's loot table
    // is being generated on the current thread.
    private static final ThreadLocal<Integer> ACTIVE_BONUS =
            new ThreadLocal<>();

    private MobLootScaling() {
    }

    // =========================================================
    // ELIGIBILITY
    // =========================================================

    public static boolean canScale(LivingEntity entity) {

        if (!(entity instanceof HostileEntity mob)) {
            return false;
        }

        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return false;
        }

        if (MobXpOrigin.isSpawner(mob)) {
            return false;
        }

        String id = Registries.ENTITY_TYPE
                .getId(mob.getType())
                .toString();

        if (config.entities.blacklist.contains(id)) {
            return false;
        }

        if (BossScalingConfig.get().contains(id)) {
            return false;
        }

        if (config.entities.mode.equals("whitelist")
                && !config.entities.whitelist.contains(id)) {
            return false;
        }

        String dimensionId = mob.getWorld()
                .getRegistryKey()
                .getValue()
                .toString();

        MobScalingConfig.Dimension dimension =
                config.dimensions.get(dimensionId);

        return dimension != null
                && dimension.enabled
                && readLevel(mob) > 1;
    }

    // =========================================================
    // VIRTUAL LOOTING CURVE
    // =========================================================

    public static int getBonusLooting(LivingEntity entity) {

        if (!canScale(entity)) {
            return 0;
        }

        int level = Math.max(
                1,
                Math.min(MAX_LEVEL, readLevel(entity))
        );

        return (int) Math.floor(
                (level - 1) * (double) MAX_BONUS_LOOTING
                        / (MAX_LEVEL - 1)
        );
    }

    // =========================================================
    // ACTIVE LOOT CONTEXT
    // =========================================================

    public static Integer getActiveBonus() {
        return ACTIVE_BONUS.get();
    }

    public static void setActiveBonus(Integer bonus) {

        if (bonus == null || bonus <= 0) {
            ACTIVE_BONUS.remove();
        } else {
            ACTIVE_BONUS.set(bonus);
        }
    }

    // =========================================================
    // LEVEL TAG
    // =========================================================

    private static int readLevel(LivingEntity entity) {

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
            }
        }

        return 0;
    }
}