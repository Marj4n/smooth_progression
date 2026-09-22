package org.marj4n.smooth_progression.progression;

public class PlayerProgression {

    private int level;
    private long experience;
    private int powerLevel;

    public PlayerProgression() {
        this.level = 1;
        this.experience = 0L;
        this.powerLevel = 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = Math.max(0L, experience);
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {
        this.powerLevel = Math.max(0, powerLevel);
    }
}