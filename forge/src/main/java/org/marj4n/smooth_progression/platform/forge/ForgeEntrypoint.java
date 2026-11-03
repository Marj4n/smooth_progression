package org.marj4n.smooth_progression.platform.forge;

@net.minecraftforge.fml.common.Mod("smooth_progression")
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        org.marj4n.smooth_progression.platform.PlatformRegistry.initialize(bus);
        new org.marj4n.smooth_progression.SmoothProgression().onInitialize();
        net.minecraftforge.fml.DistExecutor.safeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> ForgeClient::register);
    }

    private static final class ForgeClient {
        private static void register() {
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get()
                    .getModEventBus()
                    .addListener(ForgeClient::setup);
        }

        private static void setup(
                net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(
                    () ->
                            new org.marj4n.smooth_progression.client.ClientProgression()
                                    .onInitializeClient());
        }
    }
}
