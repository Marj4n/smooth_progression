package org.marj4n.smooth_progression.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.SkillsAPI;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;
import org.marj4n.smooth_progression.progression.PlayerProgression;
import org.marj4n.smooth_progression.progression.ProgressionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionSync {
    public static final Identifier CHANNEL = new Identifier("smoothprogression", "ui_progression");
    private static final Set<UUID> DIRTY = new HashSet<>();
    private static final Map<UUID, Snapshot> LAST = new HashMap<>();
    private record Snapshot(int level, long xp, boolean max) {}

    private ProgressionSync() {}

    /** Call on the server thread after a progression change. Sends at end of tick. */
    public static void request(ServerPlayerEntity player) {
        if (player != null) DIRTY.add(player.getUuid());
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Always resend on join, including reconnects to the same server.
            LAST.remove(handler.player.getUuid());
            request(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            DIRTY.remove(id);
            LAST.remove(id);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DIRTY.clear();
            LAST.clear();
        });

        // Skill purchases may change the MAX state without changing level or XP.
        // Flush at end of tick so callbacks see the finished skill transaction.
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> request(player));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> request(player));
        SkillsAPI.registerNewPointEvent((player, categoryId) -> request(player));

        ServerTickEvents.END_SERVER_TICK.register(ProgressionSync::flush);
    }

    private static void flush(MinecraftServer server) {
        if (DIRTY.isEmpty()) return;
        Set<UUID> pending = new HashSet<>(DIRTY);
        DIRTY.clear();
        for (UUID id : pending) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player == null) continue;
            if (!ServerPlayNetworking.canSend(player, CHANNEL)) {
                // Client not ready yet; JOIN normally occurs after channel registration.
                // Do not poll; a later progression/skill event will retry.
                continue;
            }
            PlayerProgression progression = ProgressionManager.get(player);
            Snapshot next = new Snapshot(
                    progression.getLevel(), progression.getExperience(),
                    PufferfishSkillsIntegration.isMaxLevel(player)
            );
            if (next.equals(LAST.get(id))) continue;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(next.level());
            buf.writeLong(next.xp());
            buf.writeBoolean(next.max());
            ServerPlayNetworking.send(player, CHANNEL, buf);
            LAST.put(id, next);
        }
    }
}
