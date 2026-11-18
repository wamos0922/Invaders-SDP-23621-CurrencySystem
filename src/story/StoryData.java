
package story;

import engine.StoryMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StoryData {

    private static final String DIALOGUE_FOLDER =
            "res/story/Dialogue/";

    private static String getFileName(int panelNumber) {
        if (panelNumber >= 1 && panelNumber <= 5) {
            return "prologue.properties";
        } else if (panelNumber <= 11) {
            return "act1.properties";
        } else if (panelNumber <= 13) {
            return "act2.properties";
        } else if (panelNumber <= 18) {
            return "act3.properties";
        } else if (panelNumber <= 25) {
            return "act4.properties";
        } else if (panelNumber <= 27) {
            return "act5.properties";
        } else if (panelNumber <= 33) {
            return "ending.properties";
        }

        throw new IllegalArgumentException(
                "Invalid panel number: " + panelNumber
        );
    }

    public static List<Dialogue> loadPanel(int panelNumber)
            throws IOException {

        if (panelNumber < 1 || panelNumber > 33) {
            throw new IllegalArgumentException(
                    "Panel number must be between 1 and 33."
            );
        }

        Properties properties = new Properties();

        String filePath =
                DIALOGUE_FOLDER + getFileName(panelNumber);

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filePath),
                StandardCharsets.UTF_8)) {

            properties.load(reader);
        }

        List<Dialogue> dialogues = new ArrayList<>();

        for (int i = 1; ; i++) {

            String key = "panel." + panelNumber + "." + i;
            String value = properties.getProperty(key);

            if (value == null) {
                break;
            }

            String[] parts = value.split("\\|", 2);

            if (parts.length != 2) {
                throw new IOException(
                        "Invalid dialogue format: " + key
                );
            }

            String originalSpeaker = parts[0].trim();
            String speaker = originalSpeaker;
            String text = parts[1].trim();

            // Preserve the character identity for portraits,
            // even when the player chooses a custom name.
            if (speaker.equalsIgnoreCase("Protagonist")) {
                speaker = StoryMode.getPlayerName();
            }

            Dialogue.DialogueType type =
                    originalSpeaker.equalsIgnoreCase("Narrator")
                            ? Dialogue.DialogueType.NARRATION
                            : Dialogue.DialogueType.CHARACTER;

            String emotion =
                    StoryEmotions.getEmotion(panelNumber, i);

            dialogues.add(
                    new Dialogue(
                            speaker,
                            text,
                            type,
                            originalSpeaker,
                            emotion
                    )
            );
        }

        if (dialogues.isEmpty()) {
            throw new IOException(
                    "No dialogue found for panel " + panelNumber
            );
        }

        return dialogues;
    }
}
