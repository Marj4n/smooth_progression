package org.marj4n.smooth_progression.progression;

public final class ExperienceManager {

    private ExperienceManager() {
    }

    public static int getRequiredExperience(int level) {

        if (level <= 0) {
            return 0;
        }

        return (int) (100 + 25 * Math.pow(level, 2));
    }

    public static boolean canLevelUp(
            PlayerProgression progression
    ) {

        return progression.getExperience()
                >= getRequiredExperience(
                progression.getLevel()
        );
    }
}