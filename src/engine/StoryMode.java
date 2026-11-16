
package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GOG-24: Story Mode level structure.
 * 40 levels, split into four chapters of 10 levels each.
 * Levels 10, 20, 30 and 40 are reserved for boss encounters.
 */
public final class StoryMode {

    public static final int TOTAL_LEVELS = 40;
    public static final int LEVELS_PER_CHAPTER = 10;

    private static final List<GameSettings> SETTINGS = createSettings();

    private StoryMode() {
        // Utility class; no instances needed.
    }

    /** Returns the chapter number (1-4) for a story level. */
    public static int getChapter(final int level) {
        validateLevel(level);
        return (level - 1) / LEVELS_PER_CHAPTER + 1;
    }

    /** Returns the level's position inside its chapter (1-10). */
    public static int getLevelInChapter(final int level) {
        validateLevel(level);
        return (level - 1) % LEVELS_PER_CHAPTER + 1;
    }

    /** Identifies the four planned boss levels. */
    public static boolean isBossLevel(final int level) {
        return level >= 1
                && level <= TOTAL_LEVELS
                && level % LEVELS_PER_CHAPTER == 0;
    }

    /** Returns the difficulty settings for a story level. */
    public static GameSettings getSettings(final int level) {
        validateLevel(level);
        return SETTINGS.get(level - 1);
    }

    private static void validateLevel(final int level) {
        if (level < 1 || level > TOTAL_LEVELS) {
            throw new IllegalArgumentException(
                    "Invalid Story Mode level: " + level);
        }
    }

    /**
     * Builds the fixed Story Mode level list.
     * Chapter 1 preserves the game's original settings.
     * Chapters 2-4 use temporary formations.
     */
    private static List<GameSettings> createSettings() {
        List<GameSettings> settings =
                new ArrayList<GameSettings>(TOTAL_LEVELS);

        // CHAPTER 1: Levels 1-10 (original settings).
        settings.add(new GameSettings(5, 2, 60, 2000));
        settings.add(new GameSettings(5, 3, 50, 2500));
        settings.add(new GameSettings(5, 4, 40, 2000, true));
        settings.add(new GameSettings(6, 4, 30, 1500));
        settings.add(new GameSettings(5, 4, 5, 1000));
        settings.add(new GameSettings(6, 3, 10, 1000,
                Math.round(6 * 3 * 0.20f)));
        settings.add(new GameSettings(6, 4, 5, 500));
        settings.add(new GameSettings(7, 4, 3, 500));
        settings.add(new GameSettings(7, 5, 1, 300, true,
                Math.round(7 * 5 * 0.20f)));
        settings.add(new GameSettings(5, 4, 8, 500));

        // CHAPTERS 2-4: Temporary settings for levels 11-40.
        // Your teammate will later implement distinct enemies,
        // attack patterns and boss encounters for each chapter.
        for (int level = 11; level <= TOTAL_LEVELS; level++) {
            int chapter = getChapter(level);
            int stage = getLevelInChapter(level);

            int width;
            int height;
            int speed;
            int shooting;

            switch (chapter) {
                case 2:
                    width = 5;
                    height = 3;
                    speed = 30;
                    shooting = 1600;
                    break;
                case 3:
                    width = 6;
                    height = 4;
                    speed = 25;
                    shooting = 1400;
                    break;
                case 4:
                    width = 7;
                    height = 4;
                    speed = 20;
                    shooting = 1200;
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected chapter: " + chapter);
            }

            // Temporary difficulty progression within a chapter.
            speed = Math.max(8, speed - (stage - 1) * 2);
            shooting = Math.max(500,
                    shooting - (stage - 1) * 70);

            boolean diagonalMovement = stage % 3 == 0;

            settings.add(new GameSettings(
                    width, height, speed, shooting, diagonalMovement));
        }

        return Collections.unmodifiableList(settings);
    }
}
