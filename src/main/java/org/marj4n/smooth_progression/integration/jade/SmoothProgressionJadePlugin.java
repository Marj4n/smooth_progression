package org.marj4n.smooth_progression.integration.jade;

import net.minecraft.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

/** Loaded through Jade's entrypoint only when Jade is installed. */
public final class SmoothProgressionJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Bosses such as the Ender Dragon are not HostileEntity subclasses.
        registration.registerEntityDataProvider(MobLevelProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobLevelProvider.INSTANCE, LivingEntity.class);
    }
}
