package org.marj4n.smooth_progression.boss;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.config.BossScalingConfig;
import java.util.UUID;

public final class BossProgression {
    public static final UUID HEALTH_MODIFIER_ID = UUID.fromString("2c9b713e-b6c1-4bfa-bd93-7b1ad9074d91");
    private BossProgression() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> apply(entity));
    }

    public static BossScalingConfig.BossLevel getBoss(Entity entity) {
        MobScalingConfig config = MobScalingConfig.get();
        if (!config.enabled || !BossScalingConfig.get().enabled || !(entity instanceof LivingEntity)) return null;
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        return BossScalingConfig.get().levels.get(id);
    }

    private static void apply(Entity entity) {
        BossScalingConfig.BossLevel boss = getBoss(entity);
        if (boss == null) return;
        LivingEntity living = (LivingEntity) entity;
        EntityAttributeInstance attribute = living.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null || attribute.getModifier(HEALTH_MODIFIER_ID) != null) return;
        BossScalingConfig config = BossScalingConfig.get();
        double multiplier = Math.min(config.max_health_multiplier,
                1.0 + config.health_per_level * (boss.min_level - 1)) * boss.hp_multiplier;
        if (!Double.isFinite(multiplier) || multiplier <= 1.0) return;
        float oldMax = living.getMaxHealth();
        float ratio = oldMax > 0 ? Math.max(0, Math.min(1, living.getHealth() / oldMax)) : 1;
        attribute.addPersistentModifier(new EntityAttributeModifier(HEALTH_MODIFIER_ID,
                "Smooth Progression fixed boss HP", multiplier - 1,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        living.setHealth(Math.min(living.getMaxHealth(), ratio * living.getMaxHealth()));
    }
}
