package org.marj4n.smooth_progression.integration.jade;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.marj4n.smooth_progression.boss.BossProgression;
import org.marj4n.smooth_progression.config.BossScalingConfig;
import org.marj4n.smooth_progression.config.MobScalingConfig;
import org.marj4n.smooth_progression.entity.MobLevelScaling;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MobLevelProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = new Identifier("smooth_progression", "mob_level");
    private static final String LEVEL_KEY = "smooth_progression:mob_level";

    @Override
    public void appendServerData(NbtCompound data, EntityAccessor accessor) {
        // Clear any previous value before handling an unlevelled entity.
        data.remove(LEVEL_KEY);
        if (!MobScalingConfig.get().enabled) {
            return;
        }

        Entity entity = accessor.getEntity();
        BossScalingConfig.BossLevel boss = BossProgression.getBoss(entity);
        int level = boss != null ? boss.min_level
                : entity instanceof HostileEntity ? MobLevelScaling.getLevel(entity) : 0;

        if (level > 0) {
            data.putInt(LEVEL_KEY, level);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        // Command tags and boss configuration are authoritative on the server.
        // Do not calculate a client-side level or invent one while waiting for data.
        int level = accessor.getServerData().getInt(LEVEL_KEY);
        if (level > 0) {
            tooltip.add(Text.translatable("smooth_progression.jade.level", level));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
