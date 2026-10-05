package org.marj4n.smooth_progression.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.marj4n.smooth_progression.network.PlayerNameplateData;
import org.marj4n.smooth_progression.player.PlayerTitle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerNameplatesClient {
    private static final Map<UUID, Display> PLAYERS = new HashMap<>();

    public record Display(Text levelAndClass, Text title) {}

    private PlayerNameplatesClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerNameplateData.CHANNEL,
                (client, handler, buf, sender) -> {
                    int operation = buf.readVarInt();
                    if (operation == PlayerNameplateData.REMOVE) {
                        UUID id = buf.readUuid();
                        client.execute(() -> {
                            if (client.getNetworkHandler() == handler) {
                                remove(id);
                            }
                        });
                        return;
                    }
                    if (operation != PlayerNameplateData.UPDATE) {
                        return;
                    }
                    List<PlayerNameplateData.Snapshot> entries = PlayerNameplateData.readSnapshots(buf);
                    client.execute(() -> {
                        // Discard packets queued by a connection that has since closed.
                        if (client.getNetworkHandler() != handler) {
                            return;
                        }
                        applySnapshots(entries);
                    });
                });
        ClientPlayConnectionEvents.INIT.register((handler, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    // These operations run on the client thread, including connection callbacks.
    static void applySnapshots(List<PlayerNameplateData.Snapshot> entries) {
        for (PlayerNameplateData.Snapshot entry : entries) {
            PLAYERS.put(entry.playerId(), display(entry));
        }
    }

    static void remove(UUID playerId) {
        PLAYERS.remove(playerId);
    }

    static void reset() {
        PLAYERS.clear();
    }

    private static Display display(PlayerNameplateData.Snapshot snapshot) {
        MutableText level = Text.translatable("smooth_progression.nameplate.level",
                Math.max(1, snapshot.level())).formatted(Formatting.GOLD);
        PlayerTitle playerTitle = PlayerTitle.fromCategoryId(snapshot.classId());
        if (playerTitle == null) {
            return new Display(level, null);
        }
        level.append(Text.literal(" • ").formatted(Formatting.DARK_GRAY));
        level.append(Text.literal(playerTitle.displayName()).formatted(playerTitle.color()));
        Text title = snapshot.level() >= PlayerTitle.SPECIAL_TITLE_LEVEL
                ? Text.translatable(playerTitle.translationKey())
                        .formatted(playerTitle.color(), Formatting.ITALIC)
                : null;
        return new Display(level, title);
    }

    public static Display get(UUID playerId) {
        return PLAYERS.get(playerId);
    }
}
