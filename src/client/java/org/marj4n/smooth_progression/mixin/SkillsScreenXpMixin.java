package org.marj4n.smooth_progression.mixin;

import net.puffish.skillsmod.client.data.ClientCategoryData;
import org.marj4n.smooth_progression.client.ClientProgression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only replaces getter calls made by the Pufferfish window renderer.
 *  The underlying Pufferfish category XP, points and purchases are untouched. */
@Mixin(value = net.puffish.skillsmod.client.gui.SkillsScreen.class, remap = false)
public abstract class SkillsScreenXpMixin {
    @Redirect(method = "drawWindowWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/data/ClientCategoryData;getCurrentLevel()I"), remap = false)
    private int smooth$level(ClientCategoryData category) {
        return ClientProgression.ready() ? ClientProgression.level() : category.getCurrentLevel();
    }
    @Redirect(method = "drawWindowWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/data/ClientCategoryData;getCurrentExperience()I"), remap = false)
    private int smooth$xp(ClientCategoryData category) {
        return ClientProgression.ready() ? ClientProgression.safeInt(ClientProgression.xp()) : category.getCurrentExperience();
    }
    @Redirect(method = "drawWindowWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/data/ClientCategoryData;getRequiredExperience()I"), remap = false)
    private int smooth$required(ClientCategoryData category) {
        return ClientProgression.ready() ? ClientProgression.safeInt(ClientProgression.required()) : category.getRequiredExperience();
    }
    @Redirect(method = "drawWindowWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/data/ClientCategoryData;getExperienceProgress()F"), remap = false)
    private float smooth$progress(ClientCategoryData category) {
        return ClientProgression.ready() ? ClientProgression.progress() : category.getExperienceProgress();
    }
    @Redirect(method = "drawWindowWithCategory", at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/data/ClientCategoryData;getExperienceToNextLevel()I"), remap = false)
    private int smooth$remaining(ClientCategoryData category) {
        return ClientProgression.ready()
                ? (ClientProgression.max() ? 0 : ClientProgression.safeInt(Math.max(0L, ClientProgression.required() - ClientProgression.xp())))
                : category.getExperienceToNextLevel();
    }
}
