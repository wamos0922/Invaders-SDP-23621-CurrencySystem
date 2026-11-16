
package engine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import screen.TutorialScreen;
import story.StoryPanel;

/**
 * GOG-24: Complete Story Mode controller.
 *
 * Owns:
 * - 40 story level settings
 * - Prologue
 * - Story tutorial
 * - Cutscene scheduling
 * - Ending
 *
 * Actual combat is shared with the main game engine.
 */
public final class StoryMode {

    public static final int TOTAL_LEVELS = 40;
    public static final int LEVELS_PER_CHAPTER = 10;

    private static final List<GameSettings> SETTINGS =
            createSettings();

    private StoryMode() {
    }

    public static int getChapter(final int level) {
        validateLevel(level);
        return (level - 1) / LEVELS_PER_CHAPTER + 1;
    }

    public static int getLevelInChapter(final int level) {
        validateLevel(level);
        return (level - 1) % LEVELS_PER_CHAPTER + 1;
    }

    public static boolean isBossLevel(final int level) {
        return level >= 1
                && level <= TOTAL_LEVELS
                && level % LEVELS_PER_CHAPTER == 0;
    }

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
     * Called once when the player starts a new story.
     *
     * Returns false if the player leaves the tutorial.
     */
    public static boolean startStory(
            final Frame frame,
            final int width,
            final int height,
            final int fps) {

        // PROLOGUE: Before tutorial.
        showPanels(frame, 1, 5);

        // Tutorial level 0.5.
        TutorialScreen tutorial =
                new TutorialScreen(width, height, fps);

        int result = frame.setScreen(tutorial);

        if (result != 2) {
            return false;
        }

        // ACT 1: After tutorial, before Level 1.
        showPanels(frame, 6, 8);

        return true;
    }

    /**
     * Called after a successfully completed story level.
     * All story cutscene scheduling belongs here.
     */
    public static void onLevelCompleted(
            final Frame frame,
            final int level) {

        switch (level) {
            case 5:
                // Blorp joins the crew.
                showPanels(frame, 9, 11);
                break;

            case 10:
                // Black hole and Planet 1.
                showPanels(frame, 12, 13);
                break;

            case 20:
                // Journey to Planet 2.
                showPanels(frame, 14, 17);
                break;

            case 30:
                // Escape from Planet 2.
                showPanels(frame, 18, 18);
                break;

            case 35:
                // Crash landing, Dr. Draco,
                // human refuge and five-month preparation.
                showPanels(frame, 19, 26);
                break;

            case 39:
                // The AI's ultimate weapon.
                showPanels(frame, 27, 27);
                break;

            case 40:
                // Victory, sacrifice and ending.
                showPanels(frame, 28, 33);
                break;

            default:
                // No story panel after this level.
                break;
        }
    }

    /**
     * Displays story dialogue in a modal window.
     * The game waits until the player finishes.
     */
    private static void showPanels(
            final Frame frame,
            final int first,
            final int last) {

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    JDialog dialog =
                            new JDialog(frame, "Story Mode", true);

                    StoryPanel panel = new StoryPanel(
                            first, last, dialog::dispose);

                    dialog.setDefaultCloseOperation(
                            JDialog.DO_NOTHING_ON_CLOSE);
                    dialog.setContentPane(panel);
                    dialog.pack();
                    dialog.setResizable(false);
                    dialog.setLocationRelativeTo(frame);

                    try {
                        dialog.setVisible(true);
                    } finally {
                        panel.close();
                        dialog.dispose();
                    }

                } catch (IOException e) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Unable to load story panels "
                                    + first + "-" + last
                                    + ": " + e.getMessage(),
                            "Story Error",
                            JOptionPane.ERROR_MESSAGE);

                    throw new IllegalStateException(
                            "Story data could not be loaded", e);
                }
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Story interrupted", e);

        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Story playback failed", e.getCause());
        }
    }

    /**
     * Builds the 40 Story Mode levels.
     */
    private static List<GameSettings> createSettings() {

        List<GameSettings> settings =
                new ArrayList<GameSettings>(TOTAL_LEVELS);

        // CHAPTER 1: Levels 1-10.
        // Preserve the original game's difficulty settings.

        settings.add(new GameSettings(5, 2, 60, 2000));
        settings.add(new GameSettings(5, 3, 50, 2500));
        settings.add(new GameSettings(5, 4, 40, 2000, true));
        settings.add(new GameSettings(6, 4, 30, 1500));
        settings.add(new GameSettings(5, 4, 5, 1000));

        settings.add(new GameSettings(
                6, 3, 10, 1000,
                Math.round(6 * 3 * 0.20f)));

        settings.add(new GameSettings(6, 4, 5, 500));
        settings.add(new GameSettings(7, 4, 3, 500));

        settings.add(new GameSettings(
                7, 5, 1, 300, true,
                Math.round(7 * 5 * 0.20f)));

        // Level 10 boss placeholder.
        settings.add(new GameSettings(5, 4, 8, 500));

        // CHAPTERS 2-4: Levels 11-40.
        // Temporary formations until unique enemies are added.

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

            speed = Math.max(
                    8, speed - (stage - 1) * 2);

            shooting = Math.max(
                    500, shooting - (stage - 1) * 70);

            boolean diagonalMovement = stage % 3 == 0;

            settings.add(new GameSettings(
                    width,
                    height,
                    speed,
                    shooting,
                    diagonalMovement));
        }

        return Collections.unmodifiableList(settings);
    }
}
