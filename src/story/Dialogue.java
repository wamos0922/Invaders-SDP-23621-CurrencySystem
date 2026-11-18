
package story;

public class Dialogue {

    public enum DialogueType {
        NARRATION,
        CHARACTER
    }

    private final String speaker;
    private final String text;
    private final DialogueType type;
    private final String portraitCharacter;
    private final String emotion;

    // Keeps existing code compatible.
    public Dialogue(String speaker, String text, DialogueType type) {
        this(speaker, text, type, speaker, "Normal");
    }

    public Dialogue(
            String speaker,
            String text,
            DialogueType type,
            String portraitCharacter,
            String emotion) {

        if (speaker == null || text == null || type == null) {
            throw new IllegalArgumentException(
                    "Dialogue fields cannot be null.");
        }

        this.speaker = speaker;
        this.text = text;
        this.type = type;
        this.portraitCharacter = portraitCharacter;
        this.emotion = emotion;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public DialogueType getType() {
        return type;
    }

    public String getPortraitCharacter() {
        return portraitCharacter;
    }

    public String getEmotion() {
        return emotion;
    }
}
