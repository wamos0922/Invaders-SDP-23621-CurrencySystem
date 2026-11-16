
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

    // Sizes adjusted for the wider Silkscreen font.
    private static final int TITLE_FONT_SIZE = 23;
    private static final int PANEL_FONT_SIZE = 13;
    private static final int SPEAKER_FONT_SIZE = 15;
    private static final int DIALOGUE_FONT_SIZE = 12;
    private static final int HINT_FONT_SIZE = 10;
    private static final int LINE_SPACING = 19;

    // Space Invaders green theme.
    private static final Color BACKGROUND =
            new Color(8, 24, 13);
    private static final Color DIALOGUE_BOX =
            new Color(5, 38, 17);
    private static final Color PORTRAIT_BACKGROUND =
            new Color(24, 75, 38);
    private static final Color ACCENT =
            new Color(125, 255, 145);
    private static final Color TEXT =
            new Color(235, 255, 235);
    private static final Color SECONDARY_TEXT =
            new Color(175, 210, 180);

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

    public StoryPanel(
            int firstPanel,
            int lastPanel,
            Runnable onFinished) throws IOException {

        if (firstPanel < 1
                || lastPanel > 33
                || firstPanel > lastPanel) {

            throw new IllegalArgumentException(
                    "Invalid story panel range");
        }

        this.lastPanel = lastPanel;
        this.onFinished = onFinished;

        setPreferredSize(new Dimension(448, 520));
        setBackground(BACKGROUND);
        setFocusable(true);

        typingTimer = new Timer(
                TYPING_DELAY,
                e -> updateTyping());

        loadTypingSound();
        loadPanel(firstPanel);

        bindKey("SPACE", "nextSpace");
        bindKey("ENTER", "nextEnter");
    }

    private void loadTypingSound() {
        try (AudioInputStream stream =
                     AudioSystem.getAudioInputStream(
                             new File(
                                     "res/sounds/Typing_Sound.wav"))) {

            typingSound = AudioSystem.getClip();
            typingSound.open(stream);

        } catch (Exception e) {
            System.err.println(
                    "Typing sound unavailable: "
                            + e.getMessage());
        }
    }

    private void playSound() {
        if (typingSound == null) {
            return;
        }

        typingSound.stop();
        typingSound.setFramePosition(0);
        typingSound.start();
    }

    private void stopSound() {
        if (typingSound != null) {
            typingSound.stop();
        }
    }

    private void bindKey(String key, String name) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(
                name,
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            ActionEvent e) {
                        advance();
                    }
                });
    }

    private void loadPanel(int number)
            throws IOException {

        List<Dialogue> dialogues =
                StoryData.loadPanel(number);

        panelNumber = number;

        dialogueManager =
                new DialogueManager(dialogues);

        startTyping();
    }

    private void startTyping() {
        typingTimer.stop();
        stopSound();

        visibleCharacters = 0;
        lettersSinceSound = 0;

        if (dialogueManager.getCurrentDialogue()
                != null) {
            typingTimer.start();
        }

        repaint();
    }

    private void updateTyping() {
        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current == null) {
            typingTimer.stop();
            stopSound();
            return;
        }

        String text = current.getText();

        if (visibleCharacters < text.length()) {
            char letter =
                    text.charAt(visibleCharacters++);

            if (Character.isLetterOrDigit(letter)) {
                lettersSinceSound++;

                if (lettersSinceSound
                        >= SOUND_INTERVAL) {

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
        if (finished) {
            return;
        }

        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current == null) {
            return;
        }

        if (visibleCharacters
                < current.getText().length()) {

            visibleCharacters =
                    current.getText().length();

            typingTimer.stop();
            stopSound();
            repaint();
            return;
        }

        dialogueManager.advanceDialogue();

        if (!dialogueManager.isFinished()) {
            startTyping();
            return;
        }

        if (panelNumber < lastPanel) {
            try {
                loadPanel(panelNumber + 1);

            } catch (IOException e) {
                typingTimer.stop();
                stopSound();

                JOptionPane.showMessageDialog(
                        this,
                        "Story loading error: "
                                + e.getMessage());
            }

            return;
        }

        finished = true;
        close();

        if (onFinished != null) {
            onFinished.run();
        }
    }

    @Override
    protected void paintComponent(
            Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        // Keep pixel-font edges sharp.
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int w = getWidth();
        int h = getHeight();

        // ======================================
        // BACKGROUND
        // ======================================

        g.setColor(BACKGROUND);
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(40, 105, 55));
        g.drawRoundRect(
                10, 10,
                w - 21, h - 21,
                12, 12);

        // ======================================
        // STORY TITLE
        // ======================================

        g.setColor(ACCENT);
        g.setFont(
                StoryFont.bold(TITLE_FONT_SIZE));

        g.drawString(
                "STORY MODE",
                20, 43);

        // ======================================
        // PANEL NUMBER
        // ======================================

        g.setColor(SECONDARY_TEXT);
        g.setFont(
                StoryFont.regular(PANEL_FONT_SIZE));

        g.drawString(
                "Panel " + panelNumber + " / 33",
                20, 73);

        // ======================================
        // DIALOGUE BOX
        // ======================================

        int boxY = h - 230;
        int boxHeight = 215;

        g.setColor(DIALOGUE_BOX);
        g.fillRoundRect(
                15, boxY,
                w - 30, boxHeight,
                15, 15);

        g.setColor(new Color(50, 120, 65));
        g.drawRoundRect(
                15, boxY,
                w - 31, boxHeight - 1,
                15, 15);

        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current != null) {
            boolean narration =
                    current.getType()
                            == Dialogue.DialogueType.NARRATION;

            int textX = 30;

            // ==================================
            // CHARACTER PORTRAIT
            // ==================================

            if (!narration) {
                g.setColor(PORTRAIT_BACKGROUND);
                g.fillRoundRect(
                        25, boxY + 20,
                        90, 110,
                        8, 8);

                g.setColor(SECONDARY_TEXT);
                g.setFont(
                        StoryFont.regular(10));

                g.drawString(
                        "Portrait",
                        31, boxY + 75);

                textX = 130;
            }

            // ==================================
            // SPEAKER NAME
            // ==================================

            g.setColor(ACCENT);
            g.setFont(
                    StoryFont.bold(
                            SPEAKER_FONT_SIZE));

            g.drawString(
                    current.getSpeaker(),
                    textX,
                    boxY + 33);

            // ==================================
            // DIALOGUE TEXT
            // ==================================

            g.setColor(TEXT);
            g.setFont(
                    StoryFont.regular(
                            DIALOGUE_FONT_SIZE));

            String fullText =
                    current.getText();

            String visibleText =
                    fullText.substring(
                            0,
                            Math.min(
                                    visibleCharacters,
                                    fullText.length()));

            drawWrappedText(
                    g,
                    visibleText,
                    textX,
                    boxY + 63,
                    w - textX - 25);
        }

        // ======================================
        // CONTINUE HINT
        // ======================================

        g.setColor(SECONDARY_TEXT);
        g.setFont(
                StoryFont.regular(HINT_FONT_SIZE));

        g.drawString(
                "SPACE / ENTER - Continue",
                25,
                h - 25);

        g.dispose();
    }

    /**
     * Draws dialogue with word wrapping.
     */
    private void drawWrappedText(
            Graphics2D g,
            String text,
            int x,
            int y,
            int maxWidth) {

        FontMetrics metrics =
                g.getFontMetrics();

        StringBuilder line =
                new StringBuilder();

        for (String word : text.split("\\s+")) {

            String candidate =
                    line.length() == 0
                            ? word
                            : line + " " + word;

            if (metrics.stringWidth(candidate)
                    > maxWidth
                    && line.length() > 0) {

                g.drawString(
                        line.toString(), x, y);

                y += LINE_SPACING;

                line = new StringBuilder(word);

            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            g.drawString(
                    line.toString(), x, y);
        }
    }

    /**
     * Stops animations and releases sound.
     */
    public void close() {
        typingTimer.stop();

        if (typingSound != null) {
            typingSound.stop();
            typingSound.close();
            typingSound = null;
        }
    }

    /**
     * Standalone story panel test.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame window =
                        new JFrame("Story Test");

                StoryPanel panel =
                        new StoryPanel(
                                1, 5,
                                window::dispose);

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
