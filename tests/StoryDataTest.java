
package story;

import java.util.List;

public class StoryDataTest {

    public static void main(String[] args) {
        try {
            List<Dialogue> dialogues = StoryData.loadPanel(7);

            for (Dialogue dialogue : dialogues) {
                System.out.println(
                        dialogue.getSpeaker() + ": " + dialogue.getText()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
