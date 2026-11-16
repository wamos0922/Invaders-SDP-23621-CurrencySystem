
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

    private static final String DIALOGUE_FOLDER = "res/story/Dialogue/";

    // Determines which file contains the requested panel.
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

    // Loads all dialogue entries for one story panel.
    public static List<Dialogue> loadPanel(int panelNumber) throws IOException {

        if (panelNumber < 1 || panelNumber > 33) {
            throw new IllegalArgumentException(
                    "Panel number must be between 1 and 33."
            );
        }

        Properties properties = new Properties();
        String filePath = DIALOGUE_FOLDER + getFileName(panelNumber);

        // Read the properties file using UTF-8.
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filePath),
                StandardCharsets.UTF_8)) {

            properties.load(reader);
        }

        List<Dialogue> dialogues = new ArrayList<>();

        // Read panel entries in numerical order.
        for (int i = 1; ; i++) {

            String key = "panel." + panelNumber + "." + i;
            String value = properties.getProperty(key);

            // No more dialogue entries for this panel.
            if (value == null) {
                break;
            }

            // Separate the speaker from the dialogue text.
            String[] parts = value.split("\\|", 2);

            if (parts.length != 2) {
                throw new IOException("Invalid dialogue format: " + key);
            }

            String speaker = parts[0].trim();
            String text = parts[1].trim();

            // Replace Protagonist with the player's chosen name.
            if (speaker.equalsIgnoreCase("Protagonist")) {
                speaker = StoryMode.getPlayerName();
            }

            // Narration is displayed differently from character dialogue.
            Dialogue.DialogueType type =
                    speaker.equalsIgnoreCase("Narrator")
                            ? Dialogue.DialogueType.NARRATION
                            : Dialogue.DialogueType.CHARACTER;

            dialogues.add(new Dialogue(speaker, text, type));
        }

        if (dialogues.isEmpty()) {
            throw new IOException(
                    "No dialogue found for panel " + panelNumber
            );
        }

        return dialogues;
    }
}
