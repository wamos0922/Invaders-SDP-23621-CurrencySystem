
package story;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class StoryPanel extends JPanel {

    private static final int FIRST_PANEL = 1;
    private static final int LAST_PANEL = 5;

    // Time between each character appearing.
    private static final int TYPING_DELAY = 35;

    // Play a sound every 2 letters or digits.
    private static final int SOUND_INTERVAL = 2;

    private int panelNumber;
    private DialogueManager dialogueManager;

    private int visibleCharacters = 0;
    private int lettersSinceSound = 0;

    private final Timer typingTimer;
    private Clip typingSound;

    public StoryPanel() throws IOException {
        setPreferredSize(new Dimension(448, 520));
        setBackground(new Color(15, 20, 35));
        setFocusable(true);

        typingTimer = new Timer(TYPING_DELAY, e -> updateTyping());

        loadTypingSound();
        loadStoryPanel(FIRST_PANEL);

        bindAdvanceKey("SPACE", "advanceSpace");
        bindAdvanceKey("ENTER", "advanceEnter");
    }

    // Load the sound once, then reuse it.
    private void loadTypingSound() {
        String path = "res/sounds/Typing_Sound.wav";

        try (AudioInputStream stream =
                     AudioSystem.getAudioInputStream(new File(path))) {

            typingSound = AudioSystem.getClip();
            typingSound.open(stream);

            System.out.println("Typing sound loaded successfully.");

        } catch (Exception e) {
            System.err.println("Could not load typing sound: "
                    + e.getMessage());
        }
    }

    private void playTypingSound() {
        if (typingSound == null) {
            return;
        }

        // Restart the short sound from the beginning.
        typingSound.stop();
        typingSound.setFramePosition(0);
        typingSound.start();
    }

    private void stopTypingSound() {
        if (typingSound != null) {
            typingSound.stop();
        }
    }

    private void bindAdvanceKey(String key, String actionName) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), actionName);

        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                advance();
            }
        });
    }

    private void loadStoryPanel(int number) throws IOException {
        List<Dialogue> dialogues = StoryData.loadPanel(number);

        panelNumber = number;
        dialogueManager = new DialogueManager(dialogues);

        startTyping();
    }

    private void startTyping() {
        typingTimer.stop();
        stopTypingSound();

        visibleCharacters = 0;
        lettersSinceSound = 0;

        if (dialogueManager.getCurrentDialogue() != null) {
            typingTimer.start();
        }

        repaint();
    }


    private void updateTyping() {
        Dialogue current = dialogueManager.getCurrentDialogue();

        if (current == null) {
            typingTimer.stop();
            stopTypingSound();
            return;
        }

        String text = current.getText();

        if (visibleCharacters < text.length()) {
            char letter = text.charAt(visibleCharacters);
            visibleCharacters++;

            if (Character.isLetterOrDigit(letter)) {
                lettersSinceSound++;

                if (lettersSinceSound >= SOUND_INTERVAL) {
                    playTypingSound();
                    lettersSinceSound = 0;
                }
            }

            // Stop both animation and sound
            // immediately after the last character.
            if (visibleCharacters >= text.length()) {
                typingTimer.stop();
                stopTypingSound();
            }

        } else {
            typingTimer.stop();
            stopTypingSound();
        }

        repaint();
    }


    private void advance() {
        Dialogue current = dialogueManager.getCurrentDialogue();

        if (current == null) {
            return;
        }

        // First press: reveal the entire sentence.
        if (visibleCharacters < current.getText().length()) {
            visibleCharacters = current.getText().length();

            typingTimer.stop();
            stopTypingSound();

            repaint();
            return;
        }

        // Second press: move to the next dialogue.
        stopTypingSound();
        dialogueManager.advanceDialogue();

        if (dialogueManager.isFinished()) {
            if (panelNumber < LAST_PANEL) {
                try {
                    loadStoryPanel(panelNumber + 1);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                            "Could not load story: " + e.getMessage());
                }
            }
        } else {
            startTyping();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        int width = getWidth();
        int height = getHeight();

        // Temporary background.
        g.setColor(new Color(22, 30, 50));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("STORY MODE", 20, 40);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("Panel " + panelNumber, 20, 68);

        int boxY = height - 190;

        // Dialogue box.
        g.setColor(new Color(5, 10, 20));
        g.fillRoundRect(15, boxY, width - 30, 175, 15, 15);

        Dialogue current = dialogueManager.getCurrentDialogue();

        if (current != null) {
            boolean isNarration =
                    current.getType() == Dialogue.DialogueType.NARRATION;

            int textX = 30;

            if (!isNarration) {
                // Placeholder for character artwork.
                g.setColor(new Color(40, 55, 75));
                g.fillRoundRect(25, boxY + 20, 90, 110, 8, 8);

                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString("Portrait", 43, boxY + 75);

                textX = 130;
            }

            // Speaker name.
            g.setColor(Color.CYAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString(current.getSpeaker(), textX, boxY + 30);

            // Dialogue text.
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));

            String fullText = current.getText();
            String displayedText = fullText.substring(
                    0, Math.min(visibleCharacters, fullText.length())
            );

            drawWrappedText(g, displayedText,
                    textX, boxY + 55, width - textX - 25);

        } else {
            g.setColor(Color.WHITE);
            g.drawString("End of prologue.", 30, boxY + 60);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("SPACE / ENTER - Skip typing or continue",
                25, height - 25);

        g.dispose();
    }

    private void drawWrappedText(Graphics2D g, String text,
                                 int x, int y, int maxWidth) {

        FontMetrics metrics = g.getFontMetrics();
        StringBuilder line = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = line.length() == 0
                    ? word : line + " " + word;

            if (metrics.stringWidth(candidate) > maxWidth
                    && line.length() > 0) {

                g.drawString(line.toString(), x, y);
                y += 19;
                line = new StringBuilder(word);

            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            g.drawString(line.toString(), x, y);
        }
    }

    public void close() {
        typingTimer.stop();

        if (typingSound != null) {
            typingSound.stop();
            typingSound.close();
            typingSound = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame =
                        new JFrame("Space Invader - Story Test");

                StoryPanel panel = new StoryPanel();

                frame.setDefaultCloseOperation(
                        JFrame.DISPOSE_ON_CLOSE
                );

                frame.setContentPane(panel);
                frame.pack();
                frame.setLocationRelativeTo(null);

                frame.addWindowListener(
                        new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(
                                    java.awt.event.WindowEvent e) {
                                panel.close();
                            }
                        }
                );

                frame.setVisible(true);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

