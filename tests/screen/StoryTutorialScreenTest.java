package screen;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import engine.Core;
import engine.DrawManager.SpriteType;
import engine.soundEngine.Sound;
import entity.Entity;

/**
 * Deterministic checks for the story tutorial, Level 0.5 (GOG-31).
 *
 * Covers the tutorial's rules (power cell placement, drone formation
 * bounce, launch close-up path, collision) and the resources it needs
 * (new sprites, intro sounds, story config).
 *
 * Run from the project root after compiling src and tests into bin:
 *   java -cp "bin;res" screen.StoryTutorialScreenTest     (Windows)
 *   java -cp bin:res screen.StoryTutorialScreenTest       (macOS / Linux)
 */
public final class StoryTutorialScreenTest {

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;

    private static int checks;

    private static void check(final boolean condition, final String message) {
        checks++;
        if (!condition)
            throw new AssertionError(message);
    }

    public static void main(final String[] args) throws Exception {
        checkPowerCellPlacement();
        checkFormationBounce();
        checkLaunchCloseUp();
        checkOverlap();
        checkSprites();
        checkIntroSounds();
        checkStoryConfig();
        System.out.println("StoryTutorialScreenTest: " + checks
                + " checks passed");
    }

    /** The power cell must drop on the side away from the player. */
    private static void checkPowerCellPlacement() {
        check(StoryTutorialScreen.cellSpawnX(10, WIDTH) == WIDTH * 3 / 4,
                "ship on the left: power cell should drop on the right");
        check(StoryTutorialScreen.cellSpawnX(WIDTH - 40, WIDTH) == WIDTH / 4,
                "ship on the right: power cell should drop on the left");
        check(StoryTutorialScreen.cellSpawnX(WIDTH / 2, WIDTH) == WIDTH / 4,
                "ship exactly in the middle should count as the right side");
    }

    /** Drones turn around before the screen edges and keep going otherwise. */
    private static void checkFormationBounce() {
        check(StoryTutorialScreen.nextFormationDirection(100, 300, WIDTH, 1) == 1,
                "formation in the middle should keep moving right");
        check(StoryTutorialScreen.nextFormationDirection(100, 300, WIDTH, -1) == -1,
                "formation in the middle should keep moving left");
        check(StoryTutorialScreen.nextFormationDirection(200, WIDTH - 10, WIDTH, 1) == -1,
                "formation at the right edge should turn left");
        check(StoryTutorialScreen.nextFormationDirection(10, 200, WIDTH, -1) == 1,
                "formation at the left edge should turn right");
    }

    /** The big ship starts below the screen, climbs, and ends above it. */
    private static void checkLaunchCloseUp() {
        int shipH = 80;
        int start = StoryTutorialScreen.launchCloseUpY(0f, HEIGHT, shipH);
        int middle = StoryTutorialScreen.launchCloseUpY(0.5f, HEIGHT, shipH);
        int end = StoryTutorialScreen.launchCloseUpY(1f, HEIGHT, shipH);
        check(start >= HEIGHT, "launch should start below the screen");
        check(end + shipH < 0, "launch should end fully above the screen");
        check(start > middle && middle > end, "ship should keep climbing");

        int previous = start;
        int previousStep = 0;
        for (int i = 1; i <= 10; i++) {
            int y = StoryTutorialScreen.launchCloseUpY(i / 10f, HEIGHT, shipH);
            int step = previous - y;
            check(step >= previousStep,
                    "launch should speed up (slow lift-off, fast climb)");
            previous = y;
            previousStep = step;
        }
    }

    /** Collision boxes: touching edges do not count, any overlap does. */
    private static void checkOverlap() {
        Entity a = new Entity(0, 0, 10, 10, Color.WHITE);
        Entity inside = new Entity(5, 5, 10, 10, Color.WHITE);
        Entity touching = new Entity(10, 0, 10, 10, Color.WHITE);
        Entity far = new Entity(100, 100, 10, 10, Color.WHITE);
        check(StoryTutorialScreen.overlaps(a, inside), "overlapping boxes should collide");
        check(StoryTutorialScreen.overlaps(inside, a), "collision should work both ways");
        check(!StoryTutorialScreen.overlaps(a, touching), "touching edges should not collide");
        check(!StoryTutorialScreen.overlaps(a, far), "distant boxes should not collide");
    }

    /** The tutorial-only sprites load from res/graphics at the right sizes. */
    private static void checkSprites() {
        int[] drone1 = Core.getDrawManager().getSpriteSize(SpriteType.TutorialDrone1);
        int[] drone2 = Core.getDrawManager().getSpriteSize(SpriteType.TutorialDrone2);
        int[] cell = Core.getDrawManager().getSpriteSize(SpriteType.TutorialCell);
        check(drone1[0] == 12 && drone1[1] == 8, "drone frame 1 should be 12x8");
        check(drone2[0] == 12 && drone2[1] == 8, "drone frame 2 should be 12x8");
        check(cell[0] == 8 && cell[1] == 8, "power cell should be 8x8");
    }

    /** The intro sounds exist and use the same format as the game's effects. */
    private static void checkIntroSounds() throws Exception {
        for (Sound sound : new Sound[] { Sound.SHIP_POWER_UP, Sound.SHIP_LAUNCH }) {
            check(sound.getCategory() == Sound.Category.SFX,
                    sound + " should be a sound effect");
            File file = new File("res/sounds/" + sound.name().toLowerCase() + ".wav");
            check(file.isFile(), file + " should exist");
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            check(format.getSampleRate() == 44100f && format.getChannels() == 1
                    && format.getSampleSizeInBits() == 16,
                    file + " should be 44.1 kHz, mono, 16-bit like the other effects");
            check(stream.getFrameLength() > 0, file + " should not be empty");
            stream.close();
        }
    }

    /** The tutorial's story config is filled in. */
    private static void checkStoryConfig() throws Exception {
        Properties levels = load("res/story/levels.properties");
        check("tutorial".equals(levels.getProperty("level.tutorial.type")),
                "level.tutorial.type should be tutorial");
        check(!"TODO".equals(levels.getProperty("level.tutorial.enemySetId")),
                "tutorial enemy set should be filled in");
        Properties locations = load("res/story/locations.properties");
        check(!"TODO".equals(locations.getProperty("location.human-prison.background")),
                "Human Prison background should be filled in");
    }

    private static Properties load(final String path) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            properties.load(in);
        }
        return properties;
    }

    private StoryTutorialScreenTest() { }
}
