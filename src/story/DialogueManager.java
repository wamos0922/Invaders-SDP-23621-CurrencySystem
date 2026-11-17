
package story;

import java.util.List;

public class DialogueManager {

    private final List<Dialogue> dialogues;
    private int currentIndex;

    public DialogueManager(List<Dialogue> dialogues) {
        if (dialogues == null) {
            throw new IllegalArgumentException("Dialogue list cannot be null.");
        }

        this.dialogues = List.copyOf(dialogues);
        this.currentIndex = 0;
    }

    // Returns the dialogue currently being displayed.
    public Dialogue getCurrentDialogue() {
        if (isFinished()) {
            return null;
        }

        return dialogues.get(currentIndex);
    }

    // Moves to the next dialogue.
    public void advanceDialogue() {
        if (!isFinished()) {
            currentIndex++;
        }
    }

    // Checks whether another dialogue exists.
    public boolean hasNext() {
        return currentIndex < dialogues.size() - 1;
    }

    // Checks whether all dialogues have been completed.
    public boolean isFinished() {
        return currentIndex >= dialogues.size();
    }

    // Resets the sequence to the beginning.
    public void reset() {
        currentIndex = 0;
    }
}
