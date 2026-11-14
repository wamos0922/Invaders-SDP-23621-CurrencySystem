
package story;

public class Dialogue {

    public enum DialogueType {
        NARRATION,
        CHARACTER
    }

    private final String speaker;
    private final String text;
    private final DialogueType type;

    public Dialogue(String speaker, String text, DialogueType type) {
        if (speaker == null || text == null || type == null) {
            throw new IllegalArgumentException("Dialogue fields cannot be null.");
        }

        this.speaker = speaker;
        this.text = text;
        this.type = type;
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
}
