
package story;

import javax.sound.sampled.*;
import java.io.File;

public class DialogueSoundTest {

    public static void main(String[] args) {

        String soundPath = "res/sounds/Typing_Sound.wav";

        try {
            File soundFile = new File(soundPath);

            AudioInputStream audioStream =
                    AudioSystem.getAudioInputStream(soundFile);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            audioStream.close();

            System.out.println("Playing dialogue typing sound...");

            // Simulate 20 characters being typed.
            for (int i = 0; i < 20; i++) {

                // Restart the sound for each blip.
                clip.stop();
                clip.setFramePosition(0);
                clip.start();

                // 80 milliseconds between blips.
                Thread.sleep(80);
            }

            clip.stop();
            clip.close();

            System.out.println("Dialogue sound test completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
