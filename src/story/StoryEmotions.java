
package story;

import java.util.HashMap;
import java.util.Map;

/**
 * Emotion assignments for the 33-panel story.
 *
 * Dialogue entries are identified by:
 * panel number + dialogue entry number.
 *
 * Anything not specified defaults to Normal.
 */
public final class StoryEmotions {

    private static final Map<String, String> EMOTIONS =
            new HashMap<>();

    private StoryEmotions() {}

    private static void set(
            int panel,
            int entry,
            String emotion) {

        EMOTIONS.put(panel + "." + entry, emotion);
    }

    static {
        // Panel 7 - Unexpected Passenger
        set(7, 1, "Angry");
        set(7, 2, "Angry");

        // Panel 10 - Meet Blorp
        set(10, 1, "Shocked");
        set(10, 2, "Panic");
        set(10, 3, "Panic");

        // Panel 11 - Merchant
        set(11, 4, "Yummy");

        // Panel 12 - Unexpected Destination
        set(12, 4, "Happy");
        set(12, 5, "Happy");

        // Panel 13 - Unwelcome Visitors
        set(13, 4, "Happy");
        set(13, 5, "Panicking");

        // Panel 16 - Another Planet
        set(16, 5, "Happy");

        // Panel 19 - Uncontrolled Descent
        set(19, 1, "Panic");
        set(19, 2, "Happy");

        // Panel 20 - Surrounded
        set(20, 3, "Sad");

        // Panel 21 - Unexpected Rescue
        set(21, 1, "Shocked");
        set(21, 2, "Shocked");
        set(21, 3, "Shocked");

        // Panel 22 - Reinforcements
        set(22, 1, "Shocked");

        // Panel 24 - They're Human
        set(24, 1, "Shocked");
        set(24, 2, "Happy");
        set(24, 4, "Confused");

        // Panel 25 - Truth About Planet 3
        set(25, 6, "Confused");
        set(25, 10, "Happy");
        set(25, 12, "Energetic");
        set(25, 13, "Happy");

        // Panel 32 - Last Goodbye
        set(32, 4, "Happy");
        set(32, 5, "Happy");
    }

    public static String getEmotion(int panel, int entry) {
        return EMOTIONS.getOrDefault(
                panel + "." + entry,
                "Normal"
        );
    }
}
