package org.marj4n.smooth_progression.platform;

public final class PlatformRegistry {
    private PlatformRegistry() {}

    public static <V, T extends V> T register(
            net.minecraft.registry.Registry<V> registry,
            net.minecraft.util.Identifier id,
            T value) {
        return net.minecraft.registry.Registry.register(registry, id, value);
    }
}
