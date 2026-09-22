package org.marj4n.smooth_progression.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;

import org.marj4n.smooth_progression.combat.MobLootScaling;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(
            method = "getLooting",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void smoothProgression$virtualLooting(
            LivingEntity entity,
            CallbackInfoReturnable<Integer> cir
    ) {

        Integer bonus = MobLootScaling.getActiveBonus();

        if (bonus == null || bonus <= 0) {
            return;
        }

        int originalLooting = cir.getReturnValue();

        cir.setReturnValue(
                originalLooting + bonus
        );
    }
}