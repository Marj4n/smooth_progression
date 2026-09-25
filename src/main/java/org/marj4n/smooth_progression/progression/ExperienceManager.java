package org.marj4n.smooth_progression.progression;

import org.marj4n.smooth_progression.config.ProgressionXpConfig;

public final class ExperienceManager {
    private ExperienceManager() {}

    /**
     * XP required for ONE level-up.
     * Configurable formula: base + linear * level + quadratic * level^2.
     * Default: 100 + 20L + 2L^2 (18,100 XP at level 90).
     */
    public static long getRequiredExperience(int level) {
        if (level < 1) level = 1;
        ProgressionXpConfig.Curve c = ProgressionXpConfig.get().curve;
        long l = level;
        try {
            return Math.max(1L, Math.addExact(c.base,
                    Math.addExact(Math.multiplyExact(c.linear, l),
                            Math.multiplyExact(c.quadratic, Math.multiplyExact(l, l)))));
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    public static boolean canLevelUp(PlayerProgression progression) {
        if (progression == null || progression.getLevel() >= Integer.MAX_VALUE) return false;
        return progression.getExperience() >= getRequiredExperience(progression.getLevel());
    }
}
