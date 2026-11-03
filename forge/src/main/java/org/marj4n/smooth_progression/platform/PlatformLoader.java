package org.marj4n.smooth_progression.platform;

/** Loader-specific mod discovery; also safe during early mixin selection. */
public final class PlatformLoader {
    private static final PlatformLoader INSTANCE = new PlatformLoader();

    private PlatformLoader() {}

    public static PlatformLoader getInstance() {
        return INSTANCE;
    }

    public boolean isModLoaded(String id) {
        return getModVersion(id).isPresent();
    }

    public java.nio.file.Path getConfigDir() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    public java.util.Optional<String> getModVersion(String id) {
        var loaded = net.minecraftforge.fml.ModList.get();
        if (loaded != null)
            return loaded.getMods().stream()
                    .filter(m -> m.getModId().equals(id))
                    .findFirst()
                    .map(m -> m.getVersion().toString());
        var loading = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList();
        if (loading == null) return java.util.Optional.empty();
        return loading.getMods().stream()
                .filter(m -> m.getModId().equals(id))
                .findFirst()
                .map(m -> m.getVersion().toString());
    }
}
