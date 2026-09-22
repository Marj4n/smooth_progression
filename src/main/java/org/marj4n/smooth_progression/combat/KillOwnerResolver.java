package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class KillOwnerResolver {

    private KillOwnerResolver() {
    }

    // =========================================================
    // RESOLVE PLAYER RESPONSIBLE FOR A KILL
    // =========================================================

    public static ServerPlayerEntity resolve(
            DamageSource damageSource
    ) {

        if (damageSource == null) {
            return null;
        }

        Entity attacker = damageSource.getAttacker();

        // Direct player attacks and damage sources
        // that correctly report the player as attacker.
        if (attacker instanceof ServerPlayerEntity player) {
            return player;
        }

        // Vanilla tameable entities, such as wolves.
        if (attacker instanceof TameableEntity tameable) {

            LivingEntity owner = tameable.getOwner();

            if (owner instanceof ServerPlayerEntity player) {
                return player;
            }
        }

        return null;
    }
}