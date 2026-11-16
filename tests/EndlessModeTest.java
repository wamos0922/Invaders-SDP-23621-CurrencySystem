import engine.EndlessMode;
import engine.GameSettings;
import engine.GameState;

/** Standalone checks for endless mode (GOG-7). Run with java -cp. */
public final class EndlessModeTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // Waves never shrink and stay inside the caps.
        GameSettings previous = EndlessMode.getSettings(1);
        for (int wave = 2; wave <= 100; wave++) {
            GameSettings s = EndlessMode.getSettings(wave);
            check(s.getFormationWidth() >= previous.getFormationWidth(),
                    "width shrank at wave " + wave);
            check(s.getFormationWidth() <= 7, "width over cap at " + wave);
            check(s.getFormationHeight() <= 5, "height over cap at " + wave);
            check(s.getBaseSpeed() >= 5, "speed under cap at " + wave);
            check(s.getShootingFrecuency() >= 400, "shooting under cap at " + wave);
            previous = s;
        }

        // Hooks run in order, pass the state along, and get the wave number.
        final int[] seenWave = {0};
        EndlessMode.LevelClearedListener addLife =
                new EndlessMode.LevelClearedListener() {
            public GameState onLevelCleared(GameState state, int wave) {
                seenWave[0] = wave;
                return new GameState(state.getLevel(), state.getScore(),
                        state.getLivesRemaining() + 1, state.getBulletsShot(),
                        state.getShipsDestroyed(), state.getCoinsCollected());
            }
        };
        EndlessMode.LevelClearedListener returnsNull =
                new EndlessMode.LevelClearedListener() {
            public GameState onLevelCleared(GameState state, int wave) {
                return null;
            }
        };
        EndlessMode.addLevelClearedListener(addLife);
        EndlessMode.addLevelClearedListener(addLife); // duplicate is ignored
        EndlessMode.addLevelClearedListener(returnsNull);
        GameState out = EndlessMode.fireLevelCleared(
                new GameState(4, 100, 2, 0, 0, 0), 3);
        check(seenWave[0] == 3, "hook got the wrong wave");
        check(out.getLivesRemaining() == 3, "hook ran twice or state lost");

        EndlessMode.removeLevelClearedListener(addLife);
        EndlessMode.removeLevelClearedListener(returnsNull);
        out = EndlessMode.fireLevelCleared(new GameState(4, 100, 2, 0, 0, 0), 3);
        check(out.getLivesRemaining() == 2, "removed hook still ran");

        // Mode switch.
        EndlessMode.setEnabled(true);
        check(EndlessMode.isEnabled(), "setEnabled(true) failed");
        EndlessMode.setEnabled(false);
        check(!EndlessMode.isEnabled(), "setEnabled(false) failed");

        System.out.println("EndlessModeTest: " + checks + " checks passed");
    }
}
