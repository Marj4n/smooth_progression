package org.marj4n.smooth_progression.mixin;

import net.minecraft.entity.boss.WitherEntity;
import org.marj4n.smooth_progression.boss.BossProgression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public abstract class WitherSpawnHealthMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void smoothProgression$finishSpawnHealth(CallbackInfo ci) {
        WitherEntity wither = (WitherEntity) (Object) this;
        if (!wither.getWorld().isClient && wither.getInvulnerableTimer() == 1
                && BossProgression.getBoss(wither) != null) {
            wither.setHealth(wither.getMaxHealth());
        }
    }
}
