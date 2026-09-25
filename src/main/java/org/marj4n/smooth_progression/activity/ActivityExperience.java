package org.marj4n.smooth_progression.activity;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.marj4n.smooth_progression.config.ProgressionXpConfig;
import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.XpSource;

public final class ActivityExperience {
    private ActivityExperience() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            awardBlockExperience(
                    serverPlayer,
                    serverWorld,
                    pos,
                    state
            );
        });
    }

    public static void markPlayerPlacedMiningBlock(ServerWorld world, BlockPos pos) {
        ProgressionXpConfig cfg = ProgressionXpConfig.get();
        if (!cfg.mining.enabled) return;

        BlockState placedState = world.getBlockState(pos);
        String id = Registries.BLOCK.getId(placedState.getBlock()).toString();
        Long configuredXp = cfg.mining.blocks.get(id);
        if (configuredXp == null || configuredXp <= 0L) return;

        PlayerPlacedMiningState.get(world).markPlaced(pos);
    }

    private static void awardBlockExperience(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        ProgressionXpConfig cfg = ProgressionXpConfig.get();
        String id = Registries.BLOCK.getId(state.getBlock()).toString();

        if (cfg.mining.enabled) {
            Long xp = cfg.mining.blocks.get(id);
            if (xp != null && xp > 0L) {
                if (PlayerPlacedMiningState.get(world).consumePlaced(pos)) return;
                ExperienceApi.addExperience(player, xp, XpSource.MINING);
                return;
            }
        }

        if (!cfg.farming.enabled || !isMatureCrop(state, id)) return;
        long xp = cfg.farming.blocks.getOrDefault(id, cfg.farming.default_mature_crop_xp);
        if (xp > 0L) ExperienceApi.addExperience(player, xp, XpSource.FARMING);
    }

    private static boolean isMatureCrop(BlockState state, String blockId) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMature(state);
        if (block instanceof NetherWartBlock) return state.contains(Properties.AGE_3) && state.get(Properties.AGE_3) >= 3;
        if (block instanceof CocoaBlock) return state.contains(Properties.AGE_2) && state.get(Properties.AGE_2) >= 2;
        // Reuse the registry ID already resolved by awardBlockExperience().
        return blockId.equals("minecraft:melon") || blockId.equals("minecraft:pumpkin");
    }
}
