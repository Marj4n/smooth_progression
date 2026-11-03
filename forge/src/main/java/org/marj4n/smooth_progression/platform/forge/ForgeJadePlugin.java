package org.marj4n.smooth_progression.platform.forge;

@snownee.jade.api.WailaPlugin
public final class ForgeJadePlugin implements snownee.jade.api.IWailaPlugin {
    private final org.marj4n.smooth_progression.integration.jade.SmoothProgressionJadePlugin
            delegate =
                    new org.marj4n.smooth_progression.integration.jade
                            .SmoothProgressionJadePlugin();

    @Override
    public void register(snownee.jade.api.IWailaCommonRegistration registration) {
        delegate.register(registration);
    }

    @Override
    public void registerClient(snownee.jade.api.IWailaClientRegistration registration) {
        delegate.registerClient(registration);
    }
}
