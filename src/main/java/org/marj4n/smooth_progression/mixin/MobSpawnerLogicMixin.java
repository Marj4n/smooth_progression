package org.marj4n.smooth_progression.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.MobSpawnerLogic;

import org.marj4n.smooth_progression.combat.MobXpOrigin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogicMixin {

    // =========================================================
    // MARK VANILLA SPAWNER ENTITIES
    //
    // Target:
    //
    // MobSpawnerLogic.serverTick()
    //
    // world.spawnNewEntityAndPassengers(entity)
    //
    // Mark the entity immediately before the spawn call.
    // =========================================================

    @ModifyArg(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;spawnNewEntityAndPassengers(Lnet/minecraft/entity/Entity;)Z"
            ),
            index = 0
    )
    private Entity smoothProgression$markSpawnerEntity(
            Entity entity
    ) {

        markEntityAndPassengers(entity);

        return entity;
    }

    // =========================================================
    // MARK ROOT ENTITY AND PASSENGERS
    // =========================================================

    private static void markEntityAndPassengers(
            Entity entity
    ) {

        if (entity == null) {
            return;
        }

        MobXpOrigin.markSpawner(entity);

        for (Entity passenger : entity.getPassengerList()) {

            markEntityAndPassengers(passenger);
        }
    }
}