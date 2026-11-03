package org.marj4n.smooth_progression.platform;

public final class PlatformRegistry {
    private PlatformRegistry() {}

    private static net.minecraftforge.eventbus.api.IEventBus bus;
    private static final java.util.Map<
                    net.minecraft.registry.Registry<?>,
                    net.minecraftforge.registries.DeferredRegister<?>>
            REGISTERS = new java.util.IdentityHashMap<>();

    public static void initialize(net.minecraftforge.eventbus.api.IEventBus modBus) {
        bus = modBus;
    }

    @SuppressWarnings("unchecked")
    public static <V, T extends V> T register(
            net.minecraft.registry.Registry<V> registry,
            net.minecraft.util.Identifier id,
            T value) {
        if (bus == null) throw new IllegalStateException("Forge registry bridge not initialized");
        if (!id.getNamespace().equals("smooth_progression"))
            throw new IllegalArgumentException("Foreign registry namespace: " + id);
        var deferred =
                (net.minecraftforge.registries.DeferredRegister<V>)
                        REGISTERS.computeIfAbsent(
                                registry,
                                key -> {
                                    var created =
                                            net.minecraftforge.registries.DeferredRegister.create(
                                                    registry.getKey(), "smooth_progression");
                                    created.register(bus);
                                    return created;
                                });
        deferred.register(id.getPath(), () -> value);
        return value;
    }
}
