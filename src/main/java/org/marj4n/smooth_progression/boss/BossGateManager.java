package org.marj4n.smooth_progression.boss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.config.BossScalingConfig;
import org.marj4n.smooth_progression.progression.ProgressionManager;

public final class BossGateManager {
    private BossGateManager() {}
    public static boolean canAccess(int playerLevel, int requiredLevel) {
        return playerLevel >= requiredLevel;
    }

    public static float scaleDamage(LivingEntity target, DamageSource source, float amount) {
        if (!Float.isFinite(amount) || amount <= 0) return amount;
        Entity attacker = source.getAttacker();
        if (attacker == null && source.getSource() instanceof ProjectileEntity projectile)
            attacker = projectile.getOwner();
        if (attacker == null) attacker = source.getSource();
        if (attacker == target) return amount;
        BossScalingConfig config = BossScalingConfig.get();
        if (!MobScalingConfig.get().enabled || !config.enabled) return amount;
        BossScalingConfig.BossLevel boss;
        ServerPlayerEntity player;
        boolean playerAttacking;
        if (attacker instanceof ServerPlayerEntity p && (boss = BossProgression.getBoss(target)) != null) {
            player = p;
            playerAttacking = true;
        } else if (target instanceof ServerPlayerEntity p && attacker != null
                && (boss = BossProgression.getBoss(attacker)) != null) {
            player = p;
            playerAttacking = false;
        } else return amount;
        int missing = Math.max(0, boss.min_level - ProgressionManager.get(player).getLevel());
        if (missing == 0) return amount;
        double multiplier = playerAttacking
                ? Math.max(config.minimum_player_damage_multiplier,
                    1.0 - missing * config.damage_reduction_per_missing_level)
                : Math.min(config.maximum_boss_damage_multiplier,
                    1.0 + missing * config.boss_damage_bonus_per_missing_level);
        return (float) Math.min(Float.MAX_VALUE, amount * multiplier);
    }
}
