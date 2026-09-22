package org.marj4n.smooth_progression.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import org.marj4n.smooth_progression.entity.MobDamageScaling;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float smoothprogression$scaleIncomingDamage(
            float amount,
            DamageSource source
    ) {

        LivingEntity target =
                (LivingEntity) (Object) this;

        // Damage scaling must only run on the server.
        if (target.getWorld().isClient()) {
            return amount;
        }

        return MobDamageScaling.scaleDamage(
                target,
                source,
                amount
        );
    }
}