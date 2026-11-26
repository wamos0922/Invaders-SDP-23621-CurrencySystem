import java.io.File;
import story.StoryMusic;

/** GOG-35: Headless tests for Story Mode gameplay soundtrack routing. */
public final class StoryGameplayMusicTest {
    private static int checks = 0;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void track(int level, String expected) {
        String actual = StoryMusic.trackForLevel(level);
        check(expected == null ? actual == null : expected.equals(actual),
                "Level " + level + ": expected " + expected + ", got " + actual);
    }

    private static void file(String filename) {
        check(new File("res/sounds", filename).isFile(),
                "Missing audio file: " + filename);
    }

    public static void main(String[] args) {
        for (int level = 1; level <= 9; level++) track(level, "FirstPhase.wav");
        track(10, "Boss10.wav");
        for (int level = 11; level <= 19; level++) track(level, "SecondPhase.wav");
        track(20, "Boss20.wav");
        for (int level = 21; level <= 29; level++) track(level, "ThirdPhase.wav");
        track(30, "Boss30.wav");
        for (int level = 31; level <= 35; level++) track(level, "FourthPhase.wav");
        for (int level = 36; level <= 39; level++) track(level, "backup.wav");
        track(0, null);
        track(40, null); // playForLevel(40) handles finalBoss.wav separately.
        track(41, null);

        for (String filename : new String[] {
                "FirstPhase.wav", "SecondPhase.wav", "ThirdPhase.wav", "FourthPhase.wav",
                "Boss10.wav", "Boss20.wav", "Boss30.wav", "backup.wav",
                "finalBoss.wav", "wrench.wav"
        }) file(filename);

        System.out.println("StoryGameplayMusicTest passed (" + checks + " checks).");
    }
}
