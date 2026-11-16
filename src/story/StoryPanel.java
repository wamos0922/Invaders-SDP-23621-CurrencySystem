
package story;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class StoryPanel extends JPanel {

    private static final int TYPING_DELAY = 35;
    private static final int SOUND_INTERVAL = 2;

    private final int lastPanel;
    private final Runnable onFinished;
    private final Timer typingTimer;

    private int panelNumber;
    private DialogueManager dialogueManager;
    private int visibleCharacters;
    private int lettersSinceSound;
    private boolean finished;
    private Clip typingSound;

    public StoryPanel() throws IOException {
        this(1, 5, () -> {});
    }

    public StoryPanel(int firstPanel, int lastPanel,
                      Runnable onFinished) throws IOException {
        if (firstPanel < 1 || lastPanel > 33
                || firstPanel > lastPanel) {
            throw new IllegalArgumentException(
                    "Invalid story panel range");
        }

        this.lastPanel = lastPanel;
        this.onFinished = onFinished;

        setPreferredSize(new Dimension(448, 520));
        setBackground(new Color(15, 20, 35));
        setFocusable(true);

        typingTimer = new Timer(TYPING_DELAY,
                e -> updateTyping());

        loadTypingSound();
        loadPanel(firstPanel);

        bindKey("SPACE", "nextSpace");
        bindKey("ENTER", "nextEnter");
    }

    private void loadTypingSound() {
        try (AudioInputStream stream =
                     AudioSystem.getAudioInputStream(
                             new File("res/sounds/Typing_Sound.wav"))) {
            typingSound = AudioSystem.getClip();
            typingSound.open(stream);
        } catch (Exception e) {
            System.err.println(
                    "Typing sound unavailable: " + e.getMessage());
        }
    }

    private void playSound() {
        if (typingSound == null) return;
        typingSound.stop();
        typingSound.setFramePosition(0);
        typingSound.start();
    }

    private void stopSound() {
        if (typingSound != null) typingSound.stop();
    }

    private void bindKey(String key, String name) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                advance();
            }
        });
    }

    private void loadPanel(int number) throws IOException {
        List<Dialogue> dialogues = StoryData.loadPanel(number);
        panelNumber = number;
        dialogueManager = new DialogueManager(dialogues);
        startTyping();
    }

    private void startTyping() {
        typingTimer.stop();
        stopSound();
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
            stopSound();
            return;
        }

        String text = current.getText();

        if (visibleCharacters < text.length()) {
            char letter = text.charAt(visibleCharacters++);
            if (Character.isLetterOrDigit(letter)) {
                lettersSinceSound++;
                if (lettersSinceSound >= SOUND_INTERVAL) {
                    playSound();
                    lettersSinceSound = 0;
                }
            }
        }

        if (visibleCharacters >= text.length()) {
            typingTimer.stop();
            stopSound();
        }

        repaint();
    }

    private void advance() {
        if (finished) return;

        Dialogue current = dialogueManager.getCurrentDialogue();
        if (current == null) return;

        // First press reveals the complete sentence.
        if (visibleCharacters < current.getText().length()) {
            visibleCharacters = current.getText().length();
            typingTimer.stop();
            stopSound();
            repaint();
            return;
        }

        // Next press advances the dialogue.
        dialogueManager.advanceDialogue();

        if (!dialogueManager.isFinished()) {
            startTyping();
            return;
        }

        // Advance to the next story panel.
        if (panelNumber < lastPanel) {
            try {
                loadPanel(panelNumber + 1);
            } catch (IOException e) {
                typingTimer.stop();
                stopSound();
                JOptionPane.showMessageDialog(this,
                        "Story loading error: " + e.getMessage());
            }
            return;
        }

        // Entire panel range is complete.
        finished = true;
        close();
        if (onFinished != null) onFinished.run();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        int w = getWidth();
        int h = getHeight();

        // Placeholder background for future artwork.
        g.setColor(new Color(22, 30, 50));
        g.fillRect(0, 0, w, h);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("STORY MODE", 20, 40);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("Panel " + panelNumber + " / 33", 20, 68);

        int boxY = h - 210;
        g.setColor(new Color(5, 10, 20));
        g.fillRoundRect(15, boxY, w - 30, 195, 15, 15);

        Dialogue current = dialogueManager.getCurrentDialogue();

        if (current != null) {
            boolean narration = current.getType()
                    == Dialogue.DialogueType.NARRATION;

            int textX = 30;

            if (!narration) {
                // Placeholder character portrait.
                g.setColor(new Color(40, 55, 75));
                g.fillRoundRect(25, boxY + 20, 90, 110, 8, 8);
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString("Portrait", 43, boxY + 75);
                textX = 130;
            }

            g.setColor(Color.CYAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString(current.getSpeaker(), textX, boxY + 30);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));

            String fullText = current.getText();
            String visibleText = fullText.substring(
                    0, Math.min(visibleCharacters, fullText.length()));

            drawWrappedText(g, visibleText, textX,
                    boxY + 55, w - textX - 25);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("SPACE / ENTER - Continue", 25, h - 25);

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

    // Allows testing the prologue independently.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame window = new JFrame("Story Test");
                StoryPanel panel = new StoryPanel(
                        1, 5, window::dispose);

                window.setDefaultCloseOperation(
                        JFrame.DISPOSE_ON_CLOSE);
                window.setContentPane(panel);
                window.pack();
                window.setLocationRelativeTo(null);
                window.addWindowListener(
                        new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(
                                    java.awt.event.WindowEvent e) {
                                panel.close();
                            }
                        });
                window.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
