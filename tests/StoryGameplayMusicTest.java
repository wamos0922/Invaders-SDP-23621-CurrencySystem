import java.io.File;
import story.StoryMusic;

/** Headless checks for GOG-35 Story Mode gameplay audio routing. */
public final class StoryGameplayMusicTest {
    private static int checks = 0;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void track(int level, String expected) {
        String actual = StoryMusic.trackForLevel(level);
        check(expected == null ? actual == null : expected.equals(actual),
                "Level " + level + ": expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        for (int level = 1; level <= 9; level++) track(level, "FirstPhase.wav");
        for (int level = 11; level <= 19; level++) track(level, "SecondPhase.wav");
        for (int level : new int[] {0, 10, 20, 21, 29, 30, 31, 35, 36, 39, 40, 41}) {
            track(level, null);
        }
        check(new File("res/sounds/FirstPhase.wav").isFile(),
                "Missing FirstPhase.wav in res/sounds");
        check(new File("res/sounds/SecondPhase.wav").isFile(),
                "Missing SecondPhase.wav in res/sounds");
        System.out.println("StoryGameplayMusicTest passed (" + checks + " checks).");
    }
}
