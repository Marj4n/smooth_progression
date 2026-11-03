package org.marj4n.smooth_progression.platform.fabric;

public final class FabricClientEntrypoint implements net.fabricmc.api.ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new org.marj4n.smooth_progression.client.ClientProgression().onInitializeClient();
    }
}
