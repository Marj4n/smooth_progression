package org.marj4n.smooth_progression.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.config.ProgressionXpConfig;
import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.XpSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void smoothprogression$awardFishingXp(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        ProgressionXpConfig.Fishing cfg = ProgressionXpConfig.get().fishing;
        if (!cfg.enabled || cfg.xp_per_catch <= 0L || cir.getReturnValue() == null || cir.getReturnValue() <= 0) return;
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (self.getOwner() instanceof ServerPlayerEntity player) {
            ExperienceApi.addExperience(player, cfg.xp_per_catch, XpSource.FISHING);
        }
    }
}
