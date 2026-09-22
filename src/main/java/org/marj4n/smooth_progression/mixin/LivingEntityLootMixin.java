package org.marj4n.smooth_progression.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;

import org.marj4n.smooth_progression.combat.MobLootScaling;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {

    @Redirect(
            method = "dropLoot",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/loot/LootTable;" +
                                    "generateLoot(" +
                                    "Lnet/minecraft/loot/context/LootContextParameterSet;" +
                                    "J" +
                                    "Ljava/util/function/Consumer;" +
                                    ")V"
            )
    )
    private void smoothProgression$scaleLoot(
            LootTable lootTable,
            LootContextParameterSet parameters,
            long seed,
            Consumer<ItemStack> originalConsumer
    ) {

        LivingEntity entity =
                (LivingEntity) (Object) this;

        int bonus = MobLootScaling.getBonusLooting(entity);

        if (bonus <= 0) {

            lootTable.generateLoot(
                    parameters,
                    seed,
                    originalConsumer
            );

            return;
        }

        // Preserve the previous context in case another
        // loot table is generated inside this loot table.
        Integer previousBonus =
                MobLootScaling.getActiveBonus();

        MobLootScaling.setActiveBonus(bonus);

        try {

            lootTable.generateLoot(
                    parameters,
                    seed,
                    originalConsumer
            );

        } finally {

            // Always restore the previous context,
            // including when loot generation throws.
            MobLootScaling.setActiveBonus(previousBonus);
        }
    }
}