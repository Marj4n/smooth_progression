package org.marj4n.smooth_progression.mixin;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.marj4n.smooth_progression.activity.ActivityExperience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void smoothProgression$trackPlacedMiningBlock(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted()) return;
        if (!(context.getWorld() instanceof ServerWorld serverWorld)) return;
        if (context.getPlayer() == null) return;
        ActivityExperience.markPlayerPlacedMiningBlock(serverWorld, context.getBlockPos());
    }
}
