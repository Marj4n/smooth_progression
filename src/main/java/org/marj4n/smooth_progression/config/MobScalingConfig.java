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

public final class MobScalingConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("smooth_progression")
            .resolve("mob_scaling.json");

    private static volatile MobScalingConfig active =
            new MobScalingConfig();

    public boolean enabled = true;

    public Map<String, Dimension> dimensions = new LinkedHashMap<>();

    // Default curve is defined here.
    // Old JSON files without level_curve still work.
    public LevelCurve level_curve = new LevelCurve();

    public Scaling scaling = new Scaling();
    public Elite elite = new Elite();
    public Entities entities = new Entities();
    public Bosses bosses = new Bosses();

    public MobScalingConfig() {

        dimensions.put(
                "minecraft:overworld",
                new Dimension(1, 1.0)
        );

        dimensions.put(
                "minecraft:the_nether",
                new Dimension(26, 1.0)
        );

        dimensions.put(
                "minecraft:the_end",
                new Dimension(51, 1.0)
        );
    }

    public static MobScalingConfig get() {
        return active;
    }

    public static void load() {

        try {

            Files.createDirectories(PATH.getParent());

            if (Files.notExists(PATH)) {

                try (Writer writer = Files.newBufferedWriter(PATH)) {

                    GSON.toJson(
                            new MobScalingConfig(),
                            writer
                    );
                }
            }

            MobScalingConfig parsed;

            try (Reader reader = Files.newBufferedReader(PATH)) {

                parsed = GSON.fromJson(
                        reader,
                        MobScalingConfig.class
                );
            }

            if (parsed == null) {

                throw new IllegalArgumentException(
                        "Empty mob scaling config"
                );
            }

            // Backward compatibility for old config files.
            if (parsed.level_curve == null) {

                parsed.level_curve = new LevelCurve();
            }

            if (parsed.scaling == null) {

                parsed.scaling = new Scaling();
            }

            parsed.validate();

            active = parsed;

            SmoothProgression.LOGGER.info(
                    "Loaded mob scaling config: {}",
                    PATH.toAbsolutePath()
            );

        } catch (Exception e) {

            SmoothProgression.LOGGER.error(
                    "Invalid mob scaling config; using defaults: {}",
                    PATH,
                    e
            );

            active = new MobScalingConfig();
        }
    }

    private void validate() {

        if (dimensions == null
                || level_curve == null
                || scaling == null
                || elite == null
                || entities == null
                || bosses == null) {

            throw new IllegalArgumentException(
                    "Missing required config section"
            );
        }

        validateLevelCurve();

        if (!Double.isFinite(scaling.health_per_level)
                || scaling.health_per_level < 0

                || !Double.isFinite(scaling.max_health_multiplier)
                || scaling.max_health_multiplier < 1
                || scaling.max_health_multiplier > 100

                || !Double.isFinite(scaling.damage_per_level)
                || scaling.damage_per_level < 0

                || !Double.isFinite(scaling.max_damage_multiplier)
                || scaling.max_damage_multiplier < 1
                || scaling.max_damage_multiplier > 100

                || !Double.isFinite(elite.spawn_chance)
                || elite.spawn_chance < 0
                || elite.spawn_chance > 1

                || !Double.isFinite(elite.health_multiplier)
                || elite.health_multiplier < 1
                || elite.health_multiplier > 100) {

            throw new IllegalArgumentException(
                    "Invalid scaling or elite numeric value"
            );
        }

        if (entities.mode == null
                || (!entities.mode.equals("all_hostile")
                && !entities.mode.equals("whitelist"))) {

            throw new IllegalArgumentException(
                    "entities.mode must be all_hostile or whitelist"
            );
        }

        if (entities.blacklist == null
                || entities.whitelist == null
                || entities.overrides == null
                || bosses.entities == null) {

            throw new IllegalArgumentException(
                    "Entity lists cannot be null"
            );
        }

        for (Dimension dimension : dimensions.values()) {

            if (dimension == null
                    || dimension.minimum_level < 1
                    || dimension.minimum_level > level_curve.max_level

                    || !Double.isFinite(dimension.distance_multiplier)
                    || dimension.distance_multiplier <= 0

                    || !Double.isFinite(dimension.blocks_per_level)
                    || dimension.blocks_per_level <= 0) {

                throw new IllegalArgumentException(
                        "Invalid dimension settings"
                );
            }
        }

        for (Override override : entities.overrides.values()) {

            if (override == null
                    || override.minimum_level < 1
                    || override.minimum_level > level_curve.max_level

                    || !Double.isFinite(override.health_multiplier)
                    || override.health_multiplier < 0
                    || override.health_multiplier > 100) {

                throw new IllegalArgumentException(
                        "Invalid entity override"
                );
            }
        }
    }

    private void validateLevelCurve() {

        if (level_curve.max_level < 1
                || level_curve.max_level > 1_000_000) {

            throw new IllegalArgumentException(
                    "level_curve.max_level must be between 1 and 1000000"
            );
        }

        if (!Double.isFinite(level_curve.endgame_blocks_per_level)
                || level_curve.endgame_blocks_per_level <= 0) {

            throw new IllegalArgumentException(
                    "Invalid endgame_blocks_per_level"
            );
        }

        if (level_curve.tiers == null
                || level_curve.tiers.size() < 2) {

            throw new IllegalArgumentException(
                    "level_curve.tiers requires at least 2 entries"
            );
        }

        LevelTier previous = null;

        for (LevelTier tier : level_curve.tiers) {

            if (tier == null
                    || !Double.isFinite(tier.distance)
                    || tier.distance < 0
                    || tier.level < 1
                    || tier.level > level_curve.max_level) {

                throw new IllegalArgumentException(
                        "Invalid level curve tier"
                );
            }

            if (previous == null) {

                if (tier.distance != 0.0) {

                    throw new IllegalArgumentException(
                            "First tier must start at distance 0"
                    );
                }

            } else {

                if (tier.distance <= previous.distance) {

                    throw new IllegalArgumentException(
                            "Tier distances must increase"
                    );
                }

                if (tier.level <= previous.level) {

                    throw new IllegalArgumentException(
                            "Tier levels must increase"
                    );
                }
            }

            previous = tier;
        }
    }

    public static final class Dimension {

        public boolean enabled = true;

        public int minimum_level = 1;

        public double distance_multiplier = 1.0;

        // Legacy compatibility field.
        // Level calculation now uses level_curve.
        public double blocks_per_level = 100.0;

        public Dimension() {}

        public Dimension(int minimum, double multiplier) {

            minimum_level = minimum;
            distance_multiplier = multiplier;
        }
    }

    public static final class LevelCurve {

        public int max_level = 200;

        public List<LevelTier> tiers = List.of(

                new LevelTier(0, 1),

                new LevelTier(2_000, 15),

                new LevelTier(6_000, 30),

                new LevelTier(12_000, 50),

                new LevelTier(25_000, 75)
        );

        public double endgame_blocks_per_level = 1_000.0;
    }

    public static final class LevelTier {

        public double distance;

        public int level;

        public LevelTier() {}

        public LevelTier(double distance, int level) {

            this.distance = distance;
            this.level = level;
        }
    }

    public static final class Scaling {

        // HP: +4% per level, maximum x5.
        public double health_per_level = 0.04;

        public double max_health_multiplier = 5.0;

        // STEP 5.3A:
        // Melee damage: +1% per level, maximum x3.
        public double damage_per_level = 0.01;

        public double max_damage_multiplier = 3.0;
    }

    public static final class Elite {

        public boolean enabled = true;

        public double spawn_chance = 0.05;

        public double health_multiplier = 1.5;
    }

    public static final class Entities {

        public String mode = "all_hostile";

        public List<String> blacklist = List.of(
                "minecraft:warden"
        );

        public List<String> whitelist = List.of();

        public Map<String, Override> overrides =
                new LinkedHashMap<>();
    }

    public static final class Override {

        public int minimum_level = 1;

        public double health_multiplier = 1.0;

        public boolean elite_enabled = true;
    }

    public static final class Bosses {

        public boolean enabled = true;

        // Dedicated boss scaling is not implemented yet.
        // These entities remain excluded.
        public List<String> entities = List.of(
                "minecraft:wither",
                "minecraft:ender_dragon"
        );
    }
}