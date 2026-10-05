package org.marj4n.smooth_progression.player;

import net.minecraft.util.Formatting;

/** Presentation metadata only; class unlocks remain owned by Smooth Classes. */
public enum PlayerTitle {
    AVANGER("avenger", "Avanger", Formatting.DARK_PURPLE),
    FOREIGNER("foreigner", "Foreigner", Formatting.LIGHT_PURPLE),
    CASTER("caster", "Caster", Formatting.AQUA),
    BERSERKER("berserker", "Berserker", Formatting.RED),
    ARCHER("archer", "Archer", Formatting.GOLD),
    ASSASSIN("assassin", "Assassin", Formatting.GRAY),
    SABER("saber", "Saber", Formatting.YELLOW),
    RULER("ruler", "Ruler", Formatting.WHITE),
    RIDER("rider", "Rider", Formatting.GREEN),
    LANCER("lancer", "Lancer", Formatting.BLUE);

    public static final int SPECIAL_TITLE_LEVEL = 100;

    private final String path;
    private final String displayName;
    private final Formatting color;

    PlayerTitle(String path, String displayName, Formatting color) {
        this.path = path;
        this.displayName = displayName;
        this.color = color;
    }

    public String categoryId() {
        return "smooth_classes:" + path;
    }

    public String displayName() {
        return displayName;
    }

    public Formatting color() {
        return color;
    }

    public String translationKey() {
        return "smooth_progression.title." + path;
    }

    public static PlayerTitle fromCategoryId(String categoryId) {
        for (PlayerTitle title : values()) {
            if (title.categoryId().equals(categoryId)) {
                return title;
            }
        }
        return null;
    }
}
