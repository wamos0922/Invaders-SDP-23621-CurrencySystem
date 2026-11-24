
package engine;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.LayoutManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import screen.StoryTutorialScreen;
import entity.EnemyType;

import story.StoryPanel;
import story.NameEntryPanel;

/**
 * GOG-24: Story Mode Controller
 *
 * Controls:
 * - 40 story levels
 * - 4 chapters
 */
public final class StoryMode {

    // ==========================================
    // STORY MODE CONFIGURATION
    // ==========================================

    public static final int TOTAL_LEVELS = 40;
    public static final int LEVELS_PER_CHAPTER = 10;

    private static String playerName = "Protagonist";

    /**
     * Enemy rows for stages 1-9 of every chapter (game design draft):
     * 2, 2, 3, 3, 4, 4, 4, 5, 5. Stage 10 is the boss level, which has
     * no enemy rows.
     */
    private static final int[] ROWS_PER_STAGE =
            {2, 2, 3, 3, 4, 4, 4, 5, 5, 4};

    /**
     * GOG-84: enemy rows of Act 2 levels 11-19, top (back) to bottom
     * (front), 5 enemies per row. Row counts follow the Game Design Draft
     * (2, 2, 3, 3, 4, 4, 4, 5, 5). Levels 11-13 bring in one special type
     * each; from level 14 the types mix and special rows grow. Polar stays
     * at the back, Beetle at the front.
     */
    private static final EnemyType[][] ACT2_ROWS = {
            // 11: Beetle arrives
            {EnemyType.STORM_KING, EnemyType.BEETLE},
            // 12: Jelly arrives
            {EnemyType.STORM_KING, EnemyType.JELLY},
            // 13: Polar arrives
            {EnemyType.POLAR, EnemyType.STORM_KING, EnemyType.CLOUD_RAY},
            // 14: 2 special rows
            {EnemyType.POLAR, EnemyType.CLOUD_RAY, EnemyType.BEETLE},
            // 15: 2 special rows
            {EnemyType.STORM_KING, EnemyType.JELLY, EnemyType.CLOUD_RAY,
                    EnemyType.BEETLE},
            // 16: 3 special rows
            {EnemyType.POLAR, EnemyType.STORM_KING, EnemyType.JELLY,
                    EnemyType.BEETLE},
            // 17: 4 special rows
            {EnemyType.POLAR, EnemyType.JELLY, EnemyType.JELLY,
                    EnemyType.BEETLE},
            // 18: 4 special rows
            {EnemyType.POLAR, EnemyType.STORM_KING, EnemyType.JELLY,
                    EnemyType.JELLY, EnemyType.BEETLE},
            // 19: all special
            {EnemyType.POLAR, EnemyType.JELLY, EnemyType.JELLY,
                    EnemyType.BEETLE, EnemyType.BEETLE},
    };

    private static final List<GameSettings> SETTINGS =
            createSettings();

    private StoryMode() {
        // Utility class.
    }

    /**
     * Returns the name displayed when the
     * protagonist speaks in story dialogue.
     */
    public static String getPlayerName() {
        return playerName;
    }

    public static void setPlayerName(String name) {
        playerName = (name == null || name.trim().isEmpty())
                ? "Protagonist" : name.trim();
    }

    // ==========================================
    // CHAPTER AND LEVEL INFORMATION
    // ==========================================

    /**
     * Returns the chapter number (1-4).
     */
    public static int getChapter(final int level) {
        validateLevel(level);

        return (level - 1) / LEVELS_PER_CHAPTER + 1;
    }

    /**
     * Returns the level within the chapter (1-10).
     */
    public static int getLevelInChapter(final int level) {
        validateLevel(level);

        return (level - 1) % LEVELS_PER_CHAPTER + 1;
    }

    /**
     * Checks whether the level is a boss level.
     *
     * Boss levels:
     * 10, 20, 30, 40
     */
    public static boolean isBossLevel(final int level) {
        return level >= 1
                && level <= TOTAL_LEVELS
                && level % LEVELS_PER_CHAPTER == 0;
    }

    /**
     * Returns the gameplay settings for a level.
     */
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

    // ==========================================
    // STORY MODE START
    // ==========================================



    public static boolean startStory(
            final Frame frame,
            final int width,
            final int height,
            final int fps) {

        System.out.println(
                "[STORY] Starting Story Mode.");

        // ======================================
        // IN-GAME PLAYER NAME ENTRY
        // ======================================

        final CountDownLatch nameEntered =
                new CountDownLatch(1);

        final AtomicReference<String> chosenName =
                new AtomicReference<>("Protagonist");

        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Story Mode must start from the game thread.");
        }

        SwingUtilities.invokeLater(() -> {

            final Container content =
                    frame.getContentPane();

            final LayoutManager previousLayout =
                    content.getLayout();

            NameEntryPanel namePanel =
                    new NameEntryPanel(name -> {

                        chosenName.set(name);

                        // Restore the game window.
                        content.removeAll();
                        content.setLayout(previousLayout);
                        content.revalidate();
                        content.repaint();

                        frame.requestFocusInWindow();

                        // Resume the game thread.
                        nameEntered.countDown();
                    });

            content.removeAll();
            content.setLayout(new BorderLayout());

            content.add(
                    namePanel,
                    BorderLayout.CENTER);

            content.revalidate();
            content.repaint();

            namePanel.requestFocusInWindow();
        });

        try {
            nameEntered.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Name entry interrupted.", e);
        }

        playerName = chosenName.get();

        System.out.println(
                "[STORY] Player name: " + playerName);

        // ======================================
        // PROLOGUE: PANELS 1-5
        // ======================================

        System.out.println(
                "[STORY] Playing prologue: Panels 1-5.");

        showPanels(frame, 1, 5);

        // ======================================
        // TUTORIAL: LEVEL 0.5
        // ======================================

        System.out.println(
                "[STORY] Starting tutorial.");

        StoryTutorialScreen tutorial =
                new StoryTutorialScreen(width, height, fps);

        int result = frame.setScreen(tutorial);

        if (result != 2) {

            System.out.println(
                    "[STORY] Tutorial cancelled.");

            return false;
        }

        // ======================================
        // ACT 1: PANELS 6-8
        // ======================================

        System.out.println(
                "[STORY] Playing Panels 6-8.");

        showPanels(frame, 6, 8);

        System.out.println(
                "[STORY] Starting Level 1.");

        return true;
    }


    // ==========================================
    // STORY PROGRESSION
    // ==========================================


    public static void onLevelCompleted(
            final Frame frame,
            final int level) {

        System.out.println(
                "[STORY] Completed Level " + level);

        switch (level) {

            // ==================================
            // LEVEL 5 COMPLETED
            // ==================================

            case 5:

                System.out.println(
                        "[STORY] Playing Panels 9-11.");

                // Blorp joins the crew.
                showPanels(frame, 9, 11);

                break;

            // ==================================
            // LEVEL 10 COMPLETED
            // ==================================

            case 10:

                System.out.println(
                        "[STORY] Playing Panels 12-13.");

                // Black hole and first alien planet.
                showPanels(frame, 12, 13);

                break;

            // ==================================
            // LEVEL 20 COMPLETED
            // ==================================

            case 20:

                System.out.println(
                        "[STORY] Playing Panels 14-17.");

                // Journey to the second alien planet.
                showPanels(frame, 14, 17);

                break;

            // ==================================
            // LEVEL 30 COMPLETED
            // ==================================

            case 30:

                System.out.println(
                        "[STORY] Playing Panel 18.");

                // Escape from the second planet.
                showPanels(frame, 18, 18);

                break;

            // ==================================
            // LEVEL 35 COMPLETED
            // ==================================

            case 35:

                System.out.println(
                        "[STORY] Playing Panels 19-26.");

                // Crash landing.
                // Human refuge.
                // Dr. Draco.
                // Five-month preparation.
                showPanels(frame, 19, 26);

                break;

            // ==================================
            // LEVEL 39 COMPLETED
            // ==================================

            case 39:

                System.out.println(
                        "[STORY] Playing Panel 27.");

                // The final AI weapon.
                showPanels(frame, 27, 27);

                break;

            // ==================================
            // LEVEL 40 COMPLETED
            // ==================================

            case 40:

                System.out.println(
                        "[STORY] Playing ending: Panels 28-33.");

                // Final victory.
                // Blorp's sacrifice.
                // Story ending.
                showPanels(frame, 28, 33);

                // GOG-24: Award the True Ending
                // only after the ending cutscene.
                Core.getAchievementManager()
                        .recordTrueEndingCompleted();

                System.out.println(
                        "[STORY] Story Mode completed!");

                break;

            default:

                // Continue to the next gameplay level.
                break;
        }
    }

    // ==========================================
    // STORY PANEL DISPLAY
    // ==========================================

    private static void showPanels(
            final Frame frame,
            final int first,
            final int last) {

        System.out.println(
                "[STORY] Displaying panels "
                        + first + " to " + last);

        // The gameplay loop runs on the main thread.
        // Swing UI operations run on the EDT.
        //
        // This latch lets gameplay wait without
        // freezing Swing's animations or input.
        final CountDownLatch finished =
                new CountDownLatch(1);

        final AtomicReference<Throwable> error =
                new AtomicReference<Throwable>();

        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Story playback must be started "
                            + "from the game thread, not the EDT.");
        }

        SwingUtilities.invokeLater(() -> {

            final Container content =
                    frame.getContentPane();

            final LayoutManager previousLayout =
                    content.getLayout();

            final StoryPanel[] activePanel =
                    new StoryPanel[1];

            final boolean[] restored = {false};

            // Restore the original content pane state.
            Runnable restore = () -> {

                if (restored[0]) {
                    return;
                }

                restored[0] = true;

                if (activePanel[0] != null) {
                    activePanel[0].close();
                }

                content.removeAll();
                content.setLayout(previousLayout);

                content.revalidate();
                content.repaint();

                frame.requestFocusInWindow();

                System.out.println(
                        "[STORY] Finished panels "
                                + first + " to " + last);

                // Resume the waiting gameplay thread.
                finished.countDown();
            };

            try {

                // ==================================
                // CREATE STORY PANEL
                // ==================================

                StoryPanel panel = new StoryPanel(
                        first,
                        last,
                        restore
                );

                activePanel[0] = panel;

                // ==================================
                // DISPLAY IN EXISTING WINDOW
                // ==================================

                content.removeAll();

                content.setLayout(
                        new BorderLayout());

                content.add(
                        panel,
                        BorderLayout.CENTER);

                content.revalidate();
                content.repaint();

                // Give the story panel keyboard focus.
                panel.requestFocusInWindow();

                System.out.println(
                        "[STORY] Story panel loaded "
                                + "inside game window.");

            } catch (IOException e) {

                error.set(e);

                JOptionPane.showMessageDialog(
                        frame,
                        "Could not load story panels "
                                + first + "-" + last
                                + ": " + e.getMessage(),
                        "Story Loading Error",
                        JOptionPane.ERROR_MESSAGE);

                restore.run();

            } catch (RuntimeException e) {

                error.set(e);
                restore.run();
            }
        });

        // ======================================
        // WAIT FOR STORY PANELS TO FINISH
        // ======================================

        try {

            finished.await();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Story playback interrupted.", e);
        }

        // Propagate loading errors instead of
        // silently skipping missing dialogue.
        if (error.get() != null) {

            throw new IllegalStateException(
                    "Unable to play story panels "
                            + first + "-" + last,
                    error.get());
        }
    }

    // ==========================================
    // STORY LEVEL SETTINGS
    // ==========================================

    /**
     * Creates all 40 Story Mode levels.
     *
     * Levels 1-10 preserve the original settings.
     *
     * Levels 11-40 use temporary difficulty
     * progression until their unique enemies
     * and boss mechanics are implemented.
     */
    private static List<GameSettings> createSettings() {

        List<GameSettings> settings =
                new ArrayList<GameSettings>(TOTAL_LEVELS);

        // ======================================
        // CHAPTER 1: LEVELS 1-10
        // ======================================

        // Level 1
        settings.add(
                new GameSettings(5, ROWS_PER_STAGE[0], 60, 2000));

        // Level 2
        settings.add(
                new GameSettings(5, ROWS_PER_STAGE[1], 50, 2500));

        // Level 3
        settings.add(
                new GameSettings(5, ROWS_PER_STAGE[2], 40, 2000, true));

        // Level 4
        settings.add(
                new GameSettings(6, ROWS_PER_STAGE[3], 30, 1500));

        // Level 5
        settings.add(
                new GameSettings(5, ROWS_PER_STAGE[4], 5, 1000));

        // Level 6
        settings.add(
                new GameSettings(
                        6, ROWS_PER_STAGE[5], 10, 1000,
                        Math.round(6 * 3 * 0.20f)));

        // Level 7
        settings.add(
                new GameSettings(6, ROWS_PER_STAGE[6], 5, 500));

        // Level 8
        settings.add(
                new GameSettings(7, ROWS_PER_STAGE[7], 3, 500));

        // Level 9
        settings.add(
                new GameSettings(
                        7, ROWS_PER_STAGE[8], 1, 300, true,
                        Math.round(7 * 5 * 0.20f)));

        // Level 10
        // AI Warship boss (GOG-74); the boss has its own numbers.
        settings.add(
                new GameSettings(5, 4, 8, 500));

        // ======================================
        // CHAPTERS 2-4: LEVELS 11-40
        // ======================================

        for (int level = 11;
             level <= TOTAL_LEVELS;
             level++) {

            int chapter = getChapter(level);
            int stage = getLevelInChapter(level);

            int width;
            int height;
            int speed;
            int shooting;

            switch (chapter) {

                // CHAPTER 2: LEVELS 11-20
                case 2:

                    width = 5;
                    height = 3;
                    speed = 30;
                    shooting = 1600;

                    break;

                // CHAPTER 3: LEVELS 21-30
                case 3:

                    width = 6;
                    height = 4;
                    speed = 25;
                    shooting = 1400;

                    break;

                // CHAPTER 4: LEVELS 31-40
                case 4:

                    width = 7;
                    height = 4;
                    speed = 20;
                    shooting = 1200;

                    break;

                default:

                    throw new IllegalStateException(
                            "Unexpected chapter: "
                                    + chapter);
            }

            // Rows follow the game design draft pattern.
            height = ROWS_PER_STAGE[stage - 1];

            // Increase difficulty each stage.
            speed = Math.max(
                    8,
                    speed - (stage - 1) * 2);

            shooting = Math.max(
                    500,
                    shooting - (stage - 1) * 70);

            // Every third stage uses diagonal movement.
            boolean diagonalMovement =
                    stage % 3 == 0;

            // GOG-84: Act 2 levels 11-19 use the Game Design Draft row
            // counts and the Jupiter enemies.
            if (chapter == 2 && stage <= ACT2_ROWS.length) {
                EnemyType[] rows = ACT2_ROWS[stage - 1];
                settings.add(
                        new GameSettings(
                                width,
                                rows.length,
                                speed,
                                shooting,
                                diagonalMovement)
                                .withRowTypes(rows));
                continue;
            }

            settings.add(
                    new GameSettings(
                            width,
                            height,
                            speed,
                            shooting,
                            diagonalMovement));
        }

        return Collections.unmodifiableList(settings);
    }
}
