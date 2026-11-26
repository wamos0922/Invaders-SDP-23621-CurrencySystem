import java.io.File;
import story.StoryMusic;

/** GOG-35: Headless tests for Story Mode gameplay and cutscene music routing. */
public final class StoryGameplayMusicTest {
    private static int checks = 0;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void track(int level, String expected, int seconds) {
        String actual = StoryMusic.trackForLevel(level);
        check(expected == null ? actual == null : expected.equals(actual),
                "Level " + level + ": expected " + expected + ", got " + actual);
        int actualSeconds = StoryMusic.startSecondsForLevel(level);
        check(seconds == actualSeconds,
                "Level " + level + ": expected offset " + seconds + ", got " + actualSeconds);
    }

    private static void panel(int panel, int line, String expected, int seconds) {
        String actual = StoryMusic.trackForPanel(panel, line);
        check(expected == null ? actual == null : expected.equals(actual),
                "Panel " + panel + ", line " + line + ": expected " + expected + ", got " + actual);
        int actualSeconds = StoryMusic.startSecondsForPanel(panel, line);
        check(seconds == actualSeconds,
                "Panel " + panel + ", line " + line + ": expected offset " + seconds + ", got " + actualSeconds);
    }

    private static void handoff(int level, boolean storyMode, boolean clearedNormally,
                                boolean expected) {
        check(StoryMusic.shouldContinueAfterLevel(level, storyMode, clearedNormally) == expected,
                "Level " + level + ": wrong handoff decision (storyMode=" + storyMode
                        + ", clearedNormally=" + clearedNormally + ")");
    }

    private static void sameCue(int panel, int level) {
        check(StoryMusic.trackForPanel(panel, 1).equals(StoryMusic.trackForLevel(level)),
                "Panel " + panel + " and Level " + level + " must use the same soundtrack");
        check(StoryMusic.startSecondsForPanel(panel, 1) == StoryMusic.startSecondsForLevel(level),
                "Panel " + panel + " and Level " + level + " must use the same offset");
    }

    private static void file(String filename) {
        check(new File("res/sounds", filename).isFile(),
                "Missing audio file: " + filename);
    }

    public static void main(String[] args) {
        for (int level = 1; level <= 9; level++) track(level, "FirstPhase.wav", 0);
        track(10, "Boss10.wav", 0);
        for (int level = 11; level <= 19; level++) track(level, "SecondPhase.wav", 0);
        track(20, "Boss20.wav", 0);
        for (int level = 21; level <= 29; level++) track(level, "ThirdPhase.wav", 0);
        track(30, "Boss30.wav", 0);
        for (int level = 31; level <= 35; level++) track(level, "FourthPhase.wav", 0);
        for (int level = 36; level <= 39; level++) track(level, "backup.wav", 53);
        track(40, "finalBoss.wav", 0);
        track(0, null, 0);
        track(41, null, 0);

        // Cutscene offsets, silent panels and the dialogue-dependent cue.
        for (int panel : new int[] {21, 22, 23, 26}) {
            panel(panel, 1, "backup.wav", 53);
        }
        panel(26, 2, "backup.wav", 53);
        panel(27, 1, "finalBoss.wav", 0);
        panel(32, 1, "ending.wav", 71);
        panel(32, 3, "ending.wav", 71);
        panel(33, 1, "ending.wav", 71);
        panel(6, 1, null, 0);
        panel(13, 2, "base.wav", 0);
        panel(13, 3, "alarm.wav", 0);
        panel(0, 1, null, 0);
        panel(34, 1, null, 0);

        // Matching filename and offset prevent the live cue from restarting.
        for (int level = 36; level <= 39; level++) {
            sameCue(26, level);
            handoff(level, true, true, true);
            handoff(level, true, false, false); // Death, quit or abnormal exit.
            handoff(level, false, true, false); // Endless or Daily Challenge.
            handoff(level, false, false, false);
        }
        sameCue(27, 40);
        handoff(35, true, true, false);
        handoff(40, true, true, false);
        handoff(40, true, false, false);
        handoff(0, true, true, false);
        handoff(41, true, true, false);

        for (String filename : new String[] {
                "FirstPhase.wav", "SecondPhase.wav", "ThirdPhase.wav", "FourthPhase.wav",
                "Boss10.wav", "Boss20.wav", "Boss30.wav", "backup.wav",
                "finalBoss.wav", "ending.wav", "wrench.wav"
        }) file(filename);

        System.out.println("StoryGameplayMusicTest passed (" + checks + " checks).");
    }
}
