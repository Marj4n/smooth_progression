package org.marj4n.smooth_progression.network;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;
import org.marj4n.smooth_progression.progression.ProgressionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sends changed public level/class data to every modded viewer, not just its owner. */
public final class PlayerNameplateSync {
    private static final Set<UUID> DIRTY = new HashSet<>();
    private static final Set<UUID> PENDING_VIEWERS = new HashSet<>();
    private static final Map<UUID, PlayerNameplateData.Snapshot> LAST = new HashMap<>();

    private PlayerNameplateSync() {}

    public static void request(ServerPlayerEntity player) {
        if (player != null) {
            DIRTY.add(player.getUuid());
        }
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.player.getUuid();
            LAST.remove(id);
            PENDING_VIEWERS.add(id);
            request(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            DIRTY.remove(id);
            PENDING_VIEWERS.remove(id);
            LAST.remove(id);
            for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
                if (!viewer.getUuid().equals(id)
                        && ServerPlayNetworking.canSend(viewer, PlayerNameplateData.CHANNEL)) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeVarInt(PlayerNameplateData.REMOVE);
                    buf.writeUuid(id);
                    ServerPlayNetworking.send(viewer, PlayerNameplateData.CHANNEL, buf);
                }
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> request(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DIRTY.clear();
            PENDING_VIEWERS.clear();
            LAST.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(PlayerNameplateSync::flush);
    }

    private static PlayerNameplateData.Snapshot snapshot(ServerPlayerEntity player) {
        String classId = PufferfishSkillsIntegration.getSelectedClass(player)
                .map(category -> category.getId().toString()).orElse("");
        return new PlayerNameplateData.Snapshot(player.getUuid(),
                ProgressionManager.get(player).getLevel(), classId);
    }

    private static void flush(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        // Category administration commands need not emit skill events. Reconcile
        // them every five seconds; ordinary unlock/level changes are event driven.
        if (server.getTicks() % 100 == 0) {
            players.forEach(PlayerNameplateSync::request);
        }

        if (!PENDING_VIEWERS.isEmpty()) {
            List<PlayerNameplateData.Snapshot> roster = null;
            for (UUID id : new HashSet<>(PENDING_VIEWERS)) {
                ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(id);
                if (viewer == null) {
                    PENDING_VIEWERS.remove(id);
                } else if (ServerPlayNetworking.canSend(viewer, PlayerNameplateData.CHANNEL)) {
                    if (roster == null) {
                        roster = players.stream().map(PlayerNameplateSync::snapshot).toList();
                    }
                    sendSnapshots(viewer, roster);
                    PENDING_VIEWERS.remove(id);
                }
            }
        }

        if (DIRTY.isEmpty()) {
            return;
        }
        Set<UUID> pending = new HashSet<>(DIRTY);
        DIRTY.clear();
        List<PlayerNameplateData.Snapshot> changed = new ArrayList<>();
        for (UUID id : pending) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player == null) {
                continue;
            }
            PlayerNameplateData.Snapshot next = snapshot(player);
            if (!next.equals(LAST.get(id))) {
                LAST.put(id, next);
                changed.add(next);
            }
        }
        if (!changed.isEmpty()) {
            for (ServerPlayerEntity viewer : players) {
                if (ServerPlayNetworking.canSend(viewer, PlayerNameplateData.CHANNEL)) {
                    sendSnapshots(viewer, changed);
                }
            }
        }
    }

    private static void sendSnapshots(ServerPlayerEntity viewer,
                                      Collection<PlayerNameplateData.Snapshot> snapshots) {
        List<PlayerNameplateData.Snapshot> entries = List.copyOf(snapshots);
        for (int start = 0; start < entries.size(); start += PlayerNameplateData.MAX_BATCH_SIZE) {
            int end = Math.min(entries.size(), start + PlayerNameplateData.MAX_BATCH_SIZE);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(PlayerNameplateData.UPDATE);
            PlayerNameplateData.writeSnapshots(buf, entries.subList(start, end));
            ServerPlayNetworking.send(viewer, PlayerNameplateData.CHANNEL, buf);
        }
    }
}
