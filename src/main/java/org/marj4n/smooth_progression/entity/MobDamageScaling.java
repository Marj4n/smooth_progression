package org.marj4n.smooth_progression.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.Registries;

import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.config.BossScalingConfig;
import org.marj4n.smooth_progression.boss.BossGateManager;

public final class MobDamageScaling {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    private MobDamageScaling() {
    }

    // =========================================================
    // DAMAGE SCALING
    // =========================================================

    public static float scaleDamage(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {

        // Ignore invalid, zero, or negative damage.
        if (amount <= 0.0F || !Float.isFinite(amount)) {
            return amount;
        }

        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return amount;
        }

        // Boss gate handles both player -> boss and boss -> player.
        // Bosses must not also receive normal mob damage scaling.
        float gated = BossGateManager.scaleDamage(target, source, amount);
        if (gated != amount) return gated;

        // Resolve melee attacker or projectile owner.
        Entity attacker = resolveAttacker(source);

        // Prevent scaling self-inflicted damage.
        if (attacker == target) {
            return amount;
        }

        // Only hostile mobs receive damage scaling.
        // Players, pets, and environmental damage are excluded.
        if (!(attacker instanceof HostileEntity mob)) {
            return amount;
        }

        String id = Registries.ENTITY_TYPE
                .getId(mob.getType())
                .toString();

        // =====================================================
        // ENTITY FILTERS
        // =====================================================

        if (config.entities.blacklist.contains(id)) {
            return amount;
        }

        // Boss progression is handled separately in the future.
        if (BossScalingConfig.get().contains(id)) {
            return amount;
        }

        if (config.entities.mode.equals("whitelist")
                && !config.entities.whitelist.contains(id)) {

            return amount;
        }

        // =====================================================
        // LEVEL VALIDATION
        // =====================================================

        int level = readLevel(mob);

        // Unleveled mobs are not scaled.
        if (level < 1) {
            return amount;
        }

        // =====================================================
        // DAMAGE MULTIPLIER
        // =====================================================

        double multiplier = Math.min(
                config.scaling.max_damage_multiplier,
                1.0D + config.scaling.damage_per_level
                        * (level - 1)
        );

        if (!Double.isFinite(multiplier)
                || multiplier <= 1.0D) {

            return amount;
        }

        double scaledDamage = amount * multiplier;

        // Protect against overflow from extremely high damage
        // supplied by other mods.
        if (!Double.isFinite(scaledDamage)) {
            return Float.MAX_VALUE;
        }

        return (float) Math.min(
                Float.MAX_VALUE,
                scaledDamage
        );
    }

    // =========================================================
    // RESOLVE ATTACKER
    // =========================================================

    private static Entity resolveAttacker(
            DamageSource source
    ) {

        // Normal melee attacks and most indirect damage sources.
        Entity attacker = source.getAttacker();

        if (attacker != null) {
            return attacker;
        }

        // Fallback for projectiles without an attacker entry.
        Entity directSource = source.getSource();

        if (directSource instanceof ProjectileEntity projectile) {
            return projectile.getOwner();
        }

        // Allows direct hostile-entity damage sources.
        return directSource;
    }

    // =========================================================
    // READ MOB LEVEL
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
}