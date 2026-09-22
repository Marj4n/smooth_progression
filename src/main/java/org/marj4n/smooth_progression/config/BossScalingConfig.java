package org.marj4n.smooth_progression.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.marj4n.smooth_progression.SmoothProgression;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BossScalingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("smooth_progression").resolve("boss_scaling.json");
    private static volatile BossScalingConfig active = new BossScalingConfig();
    public boolean enabled = true;
    public List<String> entities = List.of("minecraft:wither", "minecraft:ender_dragon");
    public Map<String, BossLevel> levels = defaultLevels();
    private static Map<String, BossLevel> defaultLevels() {
        Map<String, BossLevel> result = new LinkedHashMap<>();
        BossLevel wither = new BossLevel(); wither.min_level = 65;
        BossLevel dragon = new BossLevel(); dragon.min_level = 100;
        result.put("minecraft:wither", wither);
        result.put("minecraft:ender_dragon", dragon);
        return result;
    }
    public double health_per_level = 0.04;
    public double max_health_multiplier = 5.0;
    public double damage_reduction_per_missing_level = 0.05;
    public double boss_damage_bonus_per_missing_level = 0.08;
    public double minimum_player_damage_multiplier = 0.05;
    public double maximum_boss_damage_multiplier = 6.0;
    public double xp_per_level = 0.01;
    public double max_xp_multiplier = 3.0;

    public static BossScalingConfig get() { return active; }
    public boolean contains(String id) { return entities.contains(id) || levels.containsKey(id); }

    public static void load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.notExists(PATH)) {
                try (Writer writer = Files.newBufferedWriter(PATH)) {
                    GSON.toJson(new BossScalingConfig(), writer);
                }
            }
            BossScalingConfig parsed;
            try (Reader reader = Files.newBufferedReader(PATH)) {
                parsed = GSON.fromJson(reader, BossScalingConfig.class);
            }
            if (parsed == null) throw new IllegalArgumentException("Empty boss scaling config");
            parsed.validate();
            active = parsed;
            SmoothProgression.LOGGER.info("Loaded boss scaling config: {}", PATH.toAbsolutePath());
        } catch (Exception e) {
            SmoothProgression.LOGGER.error("Invalid boss scaling config; using defaults: {}", PATH, e);
            active = new BossScalingConfig();
        }
    }

    private void validate() {
        if (entities == null || levels == null || entities.stream().anyMatch(id -> id == null || id.isBlank()))
            throw new IllegalArgumentException("Invalid boss entities");
        if (!Double.isFinite(health_per_level) || health_per_level < 0
                || !Double.isFinite(max_health_multiplier) || max_health_multiplier < 1 || max_health_multiplier > 100
                || !Double.isFinite(damage_reduction_per_missing_level) || damage_reduction_per_missing_level < 0
                || !Double.isFinite(boss_damage_bonus_per_missing_level) || boss_damage_bonus_per_missing_level < 0
                || !Double.isFinite(minimum_player_damage_multiplier) || minimum_player_damage_multiplier <= 0 || minimum_player_damage_multiplier > 1
                || !Double.isFinite(maximum_boss_damage_multiplier) || maximum_boss_damage_multiplier < 1 || maximum_boss_damage_multiplier > 100
                || !Double.isFinite(xp_per_level) || xp_per_level < 0
                || !Double.isFinite(max_xp_multiplier) || max_xp_multiplier < 1 || max_xp_multiplier > 100)
            throw new IllegalArgumentException("Invalid boss progression settings");
        for (Map.Entry<String, BossLevel> entry : levels.entrySet()) {
            BossLevel boss = entry.getValue();
            if (entry.getKey() == null || entry.getKey().isBlank() || boss == null
                    || boss.min_level < 1 || boss.min_level > 1_000_000
                    || !Double.isFinite(boss.hp_multiplier) || boss.hp_multiplier <= 0 || boss.hp_multiplier > 100
                    || !Double.isFinite(boss.xp_multiplier) || boss.xp_multiplier < 0 || boss.xp_multiplier > 100)
                throw new IllegalArgumentException("Invalid boss entry: " + entry.getKey());
        }
    }

    public static final class BossLevel {
        public int min_level = 1;
        public double hp_multiplier = 1.0;
        public double xp_multiplier = 1.0;
    }
}
