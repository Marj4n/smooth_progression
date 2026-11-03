package org.marj4n.smooth_progression.platform.fabric;

public final class FabricEntrypoint implements net.fabricmc.api.ModInitializer {
    @Override
    public void onInitialize() {
        new org.marj4n.smooth_progression.SmoothProgression().onInitialize();
    }
}
