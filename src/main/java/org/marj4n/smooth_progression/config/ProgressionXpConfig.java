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
import java.util.Map;

/** Central config for the player XP curve and non-combat XP sources. */
public final class ProgressionXpConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("smooth_progression").resolve("experience.json");
    private static volatile ProgressionXpConfig active = new ProgressionXpConfig();

    public Curve curve = new Curve();
    public Combat combat = new Combat();
    public Mining mining = new Mining();
    public Farming farming = new Farming();
    public Fishing fishing = new Fishing();

    public static ProgressionXpConfig get() { return active; }

    public static void load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.notExists(PATH)) {
                try (Writer writer = Files.newBufferedWriter(PATH)) {
                    GSON.toJson(new ProgressionXpConfig(), writer);
                }
            }
            ProgressionXpConfig parsed;
            try (Reader reader = Files.newBufferedReader(PATH)) {
                parsed = GSON.fromJson(reader, ProgressionXpConfig.class);
            }
            if (parsed == null) throw new IllegalArgumentException("Empty experience config");
            parsed.validate();
            active = parsed;
            SmoothProgression.LOGGER.info("Loaded experience config: {}", PATH.toAbsolutePath());
        } catch (Exception e) {
            SmoothProgression.LOGGER.error("Invalid experience config; using defaults: {}", PATH, e);
            active = new ProgressionXpConfig();
        }
    }

    private void validate() {
        if (curve == null || combat == null || mining == null || farming == null || fishing == null)
            throw new IllegalArgumentException("Missing experience config section");
        if (curve.base < 0 || curve.linear < 0 || curve.quadratic < 0)
            throw new IllegalArgumentException("XP curve values must be >= 0");
        if (combat.spawner_multiplier < 0 || combat.spawner_multiplier > 1
                || combat.boss_level_match_multiplier <= 0
                || combat.boss_above_level_multiplier <= 0
                || combat.boss_below_level_multiplier < 0
                || combat.boss_far_below_level_multiplier < 0
                || combat.boss_first_kill_multiplier < 1
                || combat.boss_repeat_multiplier < 0)
            throw new IllegalArgumentException("Invalid combat XP settings");
        validateMap(mining.blocks, "mining.blocks");
        validateMap(farming.blocks, "farming.blocks");
        if (farming.default_mature_crop_xp < 0 || fishing.xp_per_catch < 0)
            throw new IllegalArgumentException("Activity XP must be >= 0");
    }

    private static void validateMap(Map<String, Long> map, String name) {
        if (map == null) throw new IllegalArgumentException(name + " cannot be null");
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null || e.getValue() < 0)
                throw new IllegalArgumentException("Invalid entry in " + name + ": " + e.getKey());
        }
    }

    public static final class Curve {
        public long base = 100L;
        public long linear = 20L;
        public long quadratic = 2L;
    }

    public static final class Combat {
        public double spawner_multiplier = 0.20D;
        /** Boss reward = required XP at boss min level * this value. */
        public double boss_reward_fraction = 0.55D;
        /** Player within +/- this many levels counts as level-matched. */
        public int boss_level_match_range = 5;
        public double boss_level_match_multiplier = 1.0D;
        public double boss_above_level_multiplier = 1.20D;
        public double boss_below_level_multiplier = 0.65D;
        public double boss_far_below_level_multiplier = 0.25D;
        public int boss_far_below_threshold = 15;
        public double boss_first_kill_multiplier = 1.25D;
        public double boss_repeat_multiplier = 0.35D;
    }

    public static final class Mining {
        public boolean enabled = true;
        public Map<String, Long> blocks = defaultMiningBlocks();
        private static Map<String, Long> defaultMiningBlocks() {
            Map<String, Long> m = new LinkedHashMap<>();
            m.put("minecraft:coal_ore", 2L); m.put("minecraft:deepslate_coal_ore", 2L);
            m.put("minecraft:copper_ore", 3L); m.put("minecraft:deepslate_copper_ore", 3L);
            m.put("minecraft:iron_ore", 4L); m.put("minecraft:deepslate_iron_ore", 4L);
            m.put("minecraft:redstone_ore", 5L); m.put("minecraft:deepslate_redstone_ore", 5L);
            m.put("minecraft:lapis_ore", 5L); m.put("minecraft:deepslate_lapis_ore", 5L);
            m.put("minecraft:gold_ore", 6L); m.put("minecraft:deepslate_gold_ore", 6L); m.put("minecraft:nether_gold_ore", 4L);
            m.put("minecraft:diamond_ore", 15L); m.put("minecraft:deepslate_diamond_ore", 15L);
            m.put("minecraft:emerald_ore", 20L); m.put("minecraft:deepslate_emerald_ore", 20L);
            m.put("minecraft:ancient_debris", 30L);
            return m;
        }
    }

    public static final class Farming {
        public boolean enabled = true;
        public long default_mature_crop_xp = 1L;
        public Map<String, Long> blocks = defaultFarmingBlocks();
        private static Map<String, Long> defaultFarmingBlocks() {
            Map<String, Long> m = new LinkedHashMap<>();
            m.put("minecraft:wheat", 1L); m.put("minecraft:carrots", 1L); m.put("minecraft:potatoes", 1L);
            m.put("minecraft:beetroots", 1L); m.put("minecraft:nether_wart", 2L); m.put("minecraft:cocoa", 2L);
            m.put("minecraft:melon", 2L); m.put("minecraft:pumpkin", 2L);
            return m;
        }
    }

    public static final class Fishing {
        public boolean enabled = true;
        public long xp_per_catch = 12L;
    }
}
