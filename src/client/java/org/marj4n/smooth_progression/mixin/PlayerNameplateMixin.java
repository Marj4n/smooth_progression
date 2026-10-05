package org.marj4n.smooth_progression.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.marj4n.smooth_progression.client.PlayerNameplatesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds two lines above the normal player label using Minecraft's label renderer. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerNameplateMixin extends EntityRenderer<AbstractClientPlayerEntity> {

    protected PlayerNameplateMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;"
            + "Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void smooth_progression$renderNameplate(AbstractClientPlayerEntity player, Text name,
                                                   MatrixStack matrices,
                                                   VertexConsumerProvider vertexConsumers,
                                                   int light, CallbackInfo ci) {
        PlayerNameplatesClient.Display display = PlayerNameplatesClient.get(player.getUuid());
        if (display == null) {
            return;
        }
        // The normal renderer has already applied its visibility/team checks and
        // scoreboard offset. Calling its superclass preserves sneaking, distance,
        // camera rotation and background behavior without re-entering this mixin.
        matrices.push();
        matrices.translate(0.0, 0.28, 0.0);
        super.renderLabelIfPresent(player, display.levelAndClass(), matrices, vertexConsumers, light);
        if (display.title() != null) {
            matrices.translate(0.0, 0.28, 0.0);
            super.renderLabelIfPresent(player, display.title(), matrices, vertexConsumers, light);
        }
        matrices.pop();
    }
}
