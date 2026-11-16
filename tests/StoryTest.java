
package test;

import engine.Core;
import engine.EndlessMode;
import engine.Frame;
import engine.GameSettings;
import engine.GameState;
import engine.StoryMode;

import screen.GameScreen;
import screen.TitleScreen;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GOG-24: Playable Story Mode Test
 *
 * Runs the real Story Mode with real gameplay.
 *
 * TEST CONTROLS:
 * F8 = Skip the current gameplay level.
 *
 * Normal gameplay remains unchanged.
 * No changes to Core.java or GameScreen.java.
 */
public final class StoryTest {

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int FPS = 60;

    private static final int MAX_LIVES = 3;

    private static Frame frame;

    private StoryTest() {
    }

    public static void main(String[] args) {

        // Story Mode only.
        EndlessMode.setEnabled(false);

        frame = new Frame(
                WIDTH,
                HEIGHT,
                Core.SCALE
        );

        // Initialize the real drawing manager.
        Core.getDrawManager().setFrame(frame);

        frame.setTitle(
                "SPACE INVADERS - STORY TEST"
        );

        int width = frame.getWidth();
        int height = frame.getHeight();

        System.out.println(
                "================================");
        System.out.println(
                "       STORY MODE TEST");
        System.out.println(
                "================================");
        System.out.println(
                "F8 = Skip current level");
        System.out.println(
                "All story panels are enabled.");
        System.out.println(
                "================================");

        // Show the actual game's main menu.
        while (frame.isDisplayable()) {

            TitleScreen title =
                    new TitleScreen(width, height, FPS);

            int selection = frame.setScreen(title);

            if (selection == 0) {
                break;
            }

            if (selection == 2) {

                runStoryMode(width, height);

            } else {

                System.out.println(
                        "[TEST] Select Story Mode "
                                + "from the main menu.");
            }
        }

        frame.dispose();
    }

    /**
     * Runs the real Story Mode introduction,
     * followed by all 40 gameplay levels.
     */
    private static void runStoryMode(
            final int width,
            final int height) {

        EndlessMode.setEnabled(false);

        // =====================================
        // STORY INTRODUCTION
        // =====================================

        // Real sequence:
        // Panels 1-5
        // Tutorial
        // Panels 6-8

        boolean started = StoryMode.startStory(
                frame,
                width,
                height,
                FPS
        );

        if (!started) {

            System.out.println(
                    "[TEST] Tutorial cancelled.");

            return;
        }

        // =====================================
        // INITIAL PLAYER STATE
        // =====================================

        GameState gameState = new GameState(
                1,
                0,
                MAX_LIVES,
                0,
                0,
                0
        );

        // =====================================
        // LEVEL LOOP
        // =====================================

        for (int level = 1;
             level <= StoryMode.TOTAL_LEVELS;
             level++) {

            if (!frame.isDisplayable()) {
                return;
            }

            int chapter =
                    StoryMode.getChapter(level);

            int chapterLevel =
                    StoryMode.getLevelInChapter(level);

            System.out.println();
            System.out.println(
                    "================================");

            System.out.println(
                    "[TEST] CHAPTER " + chapter);

            System.out.println(
                    "[TEST] LEVEL " + level
                            + " / 40");

            System.out.println(
                    "[TEST] Chapter level "
                            + chapterLevel + " / 10");

            System.out.println(
                    "[TEST] Press F8 to skip.");

            System.out.println(
                    "================================");

            frame.setTitle(
                    "STORY TEST | LEVEL "
                            + level
                            + "/40 | F8 SKIP"
            );

            // Get the real gameplay settings.
            GameSettings settings =
                    StoryMode.getSettings(level);

            // Create the actual GameScreen.
            GameScreen gameScreen =
                    new GameScreen(
                            gameState,
                            settings,
                            false,
                            width,
                            height,
                            FPS
                    );

            // Run the actual level with
            // a test-only F8 skip shortcut.
            boolean skipped =
                    playSkippableLevel(gameScreen);

            if (!frame.isDisplayable()) {
                return;
            }

            // Retrieve the actual gameplay state.
            GameState result =
                    gameScreen.getGameState();

            if (skipped) {

                System.out.println(
                        "[TEST] SKIPPED LEVEL "
                                + level);

            } else if (
                    result.getLivesRemaining() <= 0) {

                System.out.println(
                        "[TEST] GAME OVER.");

                return;

            } else {

                System.out.println(
                        "[TEST] CLEARED LEVEL "
                                + level);
            }

            // =================================
            // PLAY STORY CHECKPOINTS
            // =================================

            // Uses the real StoryMode schedule.
            // For example, completing Level 5
            // automatically plays Panels 9-11.

            StoryMode.onLevelCompleted(
                    frame,
                    level
            );

            // =================================
            // PREPARE NEXT LEVEL
            // =================================

            // In test mode, skipping restores
            // the player's lives to three.
            //
            // This prevents skipped levels from
            // stopping the story progression.

            int nextLives = skipped
                    ? MAX_LIVES
                    : result.getLivesRemaining();

            gameState = new GameState(
                    level + 1,
                    result.getScore(),
                    nextLives,
                    result.getBulletsShot(),
                    result.getShipsDestroyed(),

                    // Keep test rewards isolated
                    // from the normal cash-out flow.
                    0,

                    result.getLevelsCompletedRun(),
                    result.getCoinsCollected()
            );
        }

        // =====================================
        // STORY FINISHED
        // =====================================

        System.out.println();
        System.out.println(
                "================================");

        System.out.println(
                "[TEST] ALL 40 LEVELS FINISHED!");

        System.out.println(
                "[TEST] STORY TEST COMPLETE.");

        System.out.println(
                "================================");

        frame.setTitle(
                "STORY TEST - COMPLETED"
        );
    }

    /**
     * Runs a normal GameScreen on a dedicated
     * gameplay thread.
     *
     * Pressing F8 interrupts that thread.
     *
     * The existing Screen.run() method already
     * handles InterruptedException and exits
     * its gameplay loop.
     *
     * Therefore, we do not need to modify
     * GameScreen.java.
     *
     * @return true if the level was skipped.
     */
    private static boolean playSkippableLevel(
            final GameScreen gameScreen) {

        AtomicBoolean skipped =
                new AtomicBoolean(false);

        AtomicBoolean running =
                new AtomicBoolean(true);

        // The actual game runs on this thread.
        Thread gameplayThread = new Thread(
                () -> {

                    try {

                        frame.setScreen(gameScreen);

                    } finally {

                        running.set(false);
                    }
                },
                "StoryTest-Gameplay"
        );

        // F8 listener, enabled only during
        // gameplay levels.
        KeyEventDispatcher skipListener =
                event -> {

                    if (event.getKeyCode()
                            != KeyEvent.VK_F8) {
                        return false;
                    }

                    if (event.getID()
                            == KeyEvent.KEY_PRESSED
                            && running.get()
                            && gameplayThread.isAlive()) {

                        if (skipped.compareAndSet(
                                false, true)) {

                            System.out.println(
                                    "[TEST] F8 pressed."
                                            + " Skipping level.");

                            gameplayThread.interrupt();
                        }
                    }

                    // Consume F8 so the regular
                    // game does not process it.
                    return true;
                };

        KeyboardFocusManager manager =
                KeyboardFocusManager
                        .getCurrentKeyboardFocusManager();

        // Enable the test shortcut.
        manager.addKeyEventDispatcher(
                skipListener
        );

        try {

            // Start the real gameplay.
            gameplayThread.start();

            // Wait until gameplay finishes,
            // either normally or by F8.
            while (gameplayThread.isAlive()) {

                try {

                    gameplayThread.join(100);

                } catch (InterruptedException e) {

                    Thread.currentThread()
                            .interrupt();

                    gameplayThread.interrupt();

                    return false;
                }
            }

        } finally {

            // Remove F8 when gameplay ends.
            // It will not affect story panels,
            // the tutorial, or the main menu.
            manager.removeKeyEventDispatcher(
                    skipListener
            );
        }

        return skipped.get();
    }
}
