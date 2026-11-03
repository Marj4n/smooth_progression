package org.marj4n.smooth_progression.platform;

/** Loader-specific mod discovery; also safe during early mixin selection. */
public final class PlatformLoader {
    private static final PlatformLoader INSTANCE = new PlatformLoader();

    private PlatformLoader() {}

    public static PlatformLoader getInstance() {
        return INSTANCE;
    }

    public boolean isModLoaded(String id) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
    }

    public java.nio.file.Path getConfigDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    }

    public java.util.Optional<String> getModVersion(String id) {
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(id)
                .map(c -> c.getMetadata().getVersion().getFriendlyString());
    }
}
