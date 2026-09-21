package org.marj4n.smooth_progression.progression;

public class PlayerProgression {

    private int level;
    private int experience;
    private int powerLevel;

    public PlayerProgression() {
        this.level = 1;
        this.experience = 0;
        this.powerLevel = 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {
        this.powerLevel = Math.max(0, powerLevel);
    }
}