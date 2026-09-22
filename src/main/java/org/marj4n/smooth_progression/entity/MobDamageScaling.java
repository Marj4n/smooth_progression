package org.marj4n.smooth_progression.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.Registries;
import org.marj4n.smooth_progression.config.MobScalingConfig;

public final class MobDamageScaling {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    private MobDamageScaling() {}

    public static float scaleDamage(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {

        if (amount <= 0.0f || !Float.isFinite(amount)) {
            return amount;
        }

        MobScalingConfig config = MobScalingConfig.get();

        if (!config.enabled) {
            return amount;
        }

        Entity attacker = resolveAttacker(source);

        // Only hostile mobs are scaled.
        // Players, pets, and other entities are not affected.
        if (!(attacker instanceof HostileEntity mob)) {
            return amount;
        }

        String id = Registries.ENTITY_TYPE
                .getId(mob.getType())
                .toString();

        if (config.entities.blacklist.contains(id)) {
            return amount;
        }

        if (config.bosses.entities.contains(id)) {
            return amount;
        }

        if (config.entities.mode.equals("whitelist")
                && !config.entities.whitelist.contains(id)) {

            return amount;
        }

        // The mob must have received a Smooth Progression level.
        int level = readLevel(mob);

        if (level < 1) {
            return amount;
        }

        double multiplier = Math.min(
                config.scaling.max_damage_multiplier,
                1.0 + config.scaling.damage_per_level
                        * (level - 1)
        );

        if (!Double.isFinite(multiplier)
                || multiplier <= 1.0) {

            return amount;
        }

        double scaled = amount * multiplier;

        // Avoid returning infinity if another mod supplies
        // an extremely large damage value.
        if (!Double.isFinite(scaled)) {
            return Float.MAX_VALUE;
        }

        return (float) Math.min(
                Float.MAX_VALUE,
                scaled
        );
    }

    private static Entity resolveAttacker(
            DamageSource source
    ) {

        // Vanilla DamageSource usually exposes the shooter
        // as attacker for arrows and other indirect damage.
        Entity attacker = source.getAttacker();

        if (attacker != null) {
            return attacker;
        }

        // Fallback for projectile sources where the attacker
        // is not populated but the projectile has an owner.
        Entity directSource = source.getSource();

        if (directSource instanceof ProjectileEntity projectile) {

            return projectile.getOwner();
        }

        return directSource;
    }

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