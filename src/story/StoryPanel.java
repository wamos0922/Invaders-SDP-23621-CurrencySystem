
package story;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Story Mode cinematic cutscene player.
 *
 * Pure black scene background.
 * Character portraits appear beside dialogue.
 * Only the existing typing sound is played.
 *
 * Scene artwork and environmental audio
 * can be added later.
 */
public class StoryPanel extends JPanel {

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;

    private static final int TYPING_DELAY = 35;
    private static final int SOUND_INTERVAL = 2;

    private static final Color GREEN = Color.GREEN;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY =
            new Color(165, 170, 180);

    private static final Color BOX_BG =
            new Color(5, 8, 18);

    private static final Color BOX_BORDER =
            new Color(65, 150, 80);

    private static final Font TITLE_FONT =
            StoryFont.bold(18);

    private static final Font SMALL_FONT =
            StoryFont.regular(10);

    private static final Font SPEAKER_FONT =
            StoryFont.bold(15);

    private static final Font DIALOGUE_FONT =
            StoryFont.regular(12);

    private static final int LINE_SPACING = 20;

    private final int lastPanel;
    private final Runnable onFinished;

    private final Timer typingTimer;
    private final Timer animationTimer;

    private int panelNumber;
    private DialogueManager dialogueManager;

    private int visibleCharacters;
    private int lettersSinceSound;

    private boolean finished;
    private Clip typingSound;

    // =========================================
    // CONSTRUCTORS
    // =========================================

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

        setPreferredSize(
                new Dimension(WIDTH, HEIGHT));

        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        typingTimer = new Timer(
                TYPING_DELAY,
                e -> updateTyping());

        // Used only for the blinking Continue indicator.
        // No animated background or additional sound.
        animationTimer = new Timer(
                100,
                e -> {
                    if (!finished) {
                        repaint();
                    }
                });

        loadTypingSound();
        loadPanel(firstPanel);

        setupControls();

        animationTimer.start();

        SwingUtilities.invokeLater(
                this::requestFocusInWindow);
    }

    // =========================================
    // INPUT CONTROLS
    // =========================================

    private void setupControls() {

        bindKey("SPACE", "nextSpace");
        bindKey("ENTER", "nextEnter");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {

                if (SwingUtilities
                        .isLeftMouseButton(event)) {
                    advance();
                }
            }
        });
    }

    private void bindKey(
            String key,
            String actionName) {

        getInputMap(
                WHEN_IN_FOCUSED_WINDOW
        ).put(
                KeyStroke.getKeyStroke(key),
                actionName);

        getActionMap().put(
                actionName,
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            ActionEvent event) {
                        advance();
                    }
                });
    }

    // =========================================
    // TYPING SOUND ONLY
    // =========================================

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

    // =========================================
    // STORY LOADING
    // =========================================

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

    // =========================================
    // TYPEWRITER ANIMATION
    // =========================================

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

    // =========================================
    // DIALOGUE PROGRESSION
    // =========================================

    private void advance() {

        if (finished) {
            return;
        }

        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current == null) {
            return;
        }

        // First press reveals the entire dialogue.
        if (visibleCharacters
                < current.getText().length()) {

            visibleCharacters =
                    current.getText().length();

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

        // Continue to the next story panel.
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

        // All requested panels completed.
        finished = true;
        close();

        if (onFinished != null) {
            onFinished.run();
        }
    }

    // =========================================
    // MAIN DRAWING
    // =========================================

    @Override
    protected void paintComponent(
            Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        g.scale(
                getWidth() / (double) WIDTH,
                getHeight() / (double) HEIGHT);

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        drawBackground(g);
        drawHeader(g);
        drawScene(g);
        drawDialogueBox(g);

        g.dispose();
    }

    // =========================================
    // PURE BLACK BACKGROUND
    // =========================================

    private void drawBackground(Graphics2D g) {

        g.setColor(Color.BLACK);

        g.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT);

        // Intentionally empty.
        //
        // Future cutscene backgrounds and
        // illustrations can be drawn here.
        //
        // No stars, planets, nebula or effects.
    }

    // =========================================
    // STORY HEADER
    // =========================================

    private void drawHeader(Graphics2D g) {

        g.setColor(GREEN);
        g.setFont(TITLE_FONT);

        g.drawString(
                "STORY MODE",
                20,
                35);

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "PANEL "
                        + panelNumber
                        + " / 33",
                20,
                57);

        // Story progress indicator.
        int progressWidth =
                (int) (WIDTH
                        * panelNumber / 33.0);

        g.setColor(new Color(45, 65, 50));
        g.fillRect(0, 72, WIDTH, 3);

        g.setColor(GREEN);
        g.fillRect(0, 72, progressWidth, 3);
    }

    // =========================================
    // SCENE AREA
    // =========================================

    private void drawScene(Graphics2D g) {

        // Intentionally blank.
        //
        // This is the main cutscene artwork area.
        //
        // Future examples:
        // - Character illustrations
        // - Environment backgrounds
        // - Scene-specific PNG artwork
        //
        // The dialogue portrait is handled
        // separately inside drawDialogueBox().
    }

    // =========================================
    // DIALOGUE BOX
    // =========================================

    private void drawDialogueBox(Graphics2D g) {

        final int boxX = 14;
        final int boxY = 284;
        final int boxW = WIDTH - 28;
        final int boxH = 220;

        g.setColor(BOX_BG);

        g.fillRect(
                boxX,
                boxY,
                boxW,
                boxH);

        // Pixel-style border.
        g.setColor(BOX_BORDER);

        g.drawRect(
                boxX,
                boxY,
                boxW,
                boxH);

        // Decorative corners.
        g.setColor(GREEN);

        g.fillRect(boxX, boxY, 16, 3);
        g.fillRect(boxX, boxY, 3, 16);

        g.fillRect(
                boxX + boxW - 16,
                boxY,
                16,
                3);

        g.fillRect(
                boxX + boxW - 3,
                boxY,
                3,
                16);

        g.fillRect(
                boxX,
                boxY + boxH - 3,
                16,
                3);

        g.fillRect(
                boxX,
                boxY + boxH - 16,
                3,
                16);

        g.fillRect(
                boxX + boxW - 16,
                boxY + boxH - 3,
                16,
                3);

        g.fillRect(
                boxX + boxW - 3,
                boxY + boxH - 16,
                3,
                16);

        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current == null) {
            return;
        }

        boolean narration =
                current.getType()
                        == Dialogue.DialogueType.NARRATION;

        // =====================================
        // CHARACTER PORTRAIT ON LEFT
        // =====================================

        int textX;

        if (!narration) {

            drawPortrait(g, boxX + 15, boxY + 55);

            // Dialogue text starts to the
            // right of the portrait.
            textX = boxX + 125;

        } else {

            // Narration uses the full width.
            textX = boxX + 20;
        }

        // =====================================
        // SPEAKER NAME
        // =====================================

        g.setFont(SPEAKER_FONT);
        g.setColor(GREEN);

        g.drawString(
                current.getSpeaker(),
                textX,
                boxY + 32);

        // Divider under speaker name.
        g.setColor(new Color(45, 90, 55));

        g.drawLine(
                textX,
                boxY + 45,
                boxX + boxW - 20,
                boxY + 45);

        // =====================================
        // DIALOGUE TEXT
        // =====================================

        String fullText =
                current.getText();

        String visibleText =
                fullText.substring(
                        0,
                        Math.min(
                                visibleCharacters,
                                fullText.length()));

        g.setFont(DIALOGUE_FONT);
        g.setColor(WHITE);

        int availableWidth =
                boxX + boxW - textX - 20;

        Shape oldClip = g.getClip();

        g.clipRect(
                textX,
                boxY + 53,
                availableWidth,
                118);

        drawWrappedText(
                g,
                visibleText,
                textX,
                boxY + 75,
                availableWidth);

        g.setClip(oldClip);

        // =====================================
        // CONTINUE INDICATOR
        // =====================================

        boolean typingComplete =
                visibleCharacters
                        >= fullText.length();

        g.setFont(SMALL_FONT);

        if (typingComplete) {

            boolean visible =
                    (System.currentTimeMillis()
                            / 450) % 2 == 0;

            if (visible) {

                g.setColor(GREEN);

                g.drawString(
                        "CONTINUE  >",
                        boxX + boxW - 125,
                        boxY + boxH - 18);
            }

        } else {

            g.setColor(GRAY);

            g.drawString(
                    "TYPING...",
                    boxX + boxW - 120,
                    boxY + boxH - 18);
        }

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "SPACE / ENTER / CLICK",
                boxX + 18,
                boxY + boxH - 18);
    }

    // =========================================
    // LEFT-SIDE CHARACTER PORTRAIT
    // =========================================

    private void drawPortrait(
            Graphics2D g,
            int x,
            int y) {

        final int portraitW = 95;
        final int portraitH = 112;

        g.setColor(new Color(10, 15, 25));

        g.fillRect(
                x,
                y,
                portraitW,
                portraitH);

        g.setColor(BOX_BORDER);

        g.drawRect(
                x,
                y,
                portraitW,
                portraitH);

        // Temporary silhouette.
        // Replace with actual character PNG later.
        g.setColor(new Color(40, 85, 55));

        g.fillRect(x + 30, y + 20, 35, 34);
        g.fillRect(x + 20, y + 58, 55, 35);

        g.setColor(new Color(85, 140, 95));

        g.fillRect(x + 37, y + 32, 6, 5);
        g.fillRect(x + 53, y + 32, 6, 5);

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "PORTRAIT",
                x + 12,
                y + portraitH - 8);
    }

    // =========================================
    // TEXT WRAPPING
    // =========================================

    private void drawWrappedText(
            Graphics2D g,
            String text,
            int x,
            int y,
            int maxWidth) {

        FontMetrics metrics =
                g.getFontMetrics();

        String[] paragraphs =
                text.split("\n", -1);

        for (String paragraph : paragraphs) {

            StringBuilder line =
                    new StringBuilder();

            for (String word :
                    paragraph.split("\\s+")) {

                if (word.isEmpty()) {
                    continue;
                }

                String candidate =
                        line.length() == 0
                                ? word
                                : line + " " + word;

                if (metrics.stringWidth(candidate)
                        > maxWidth
                        && line.length() > 0) {

                    g.drawString(
                            line.toString(),
                            x,
                            y);

                    y += LINE_SPACING;

                    line = new StringBuilder(word);

                } else {

                    line = new StringBuilder(
                            candidate);
                }
            }

            if (line.length() > 0) {

                g.drawString(
                        line.toString(),
                        x,
                        y);
            }

            y += LINE_SPACING;
        }
    }

    // =========================================
    // CLEANUP
    // =========================================

    public void close() {

        typingTimer.stop();
        animationTimer.stop();

        if (typingSound != null) {
            typingSound.stop();
            typingSound.close();
            typingSound = null;
        }
    }

    @Override
    public void removeNotify() {
        close();
        super.removeNotify();
    }

    // =========================================
    // STANDALONE TEST
    // =========================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                JFrame window =
                        new JFrame("Story Test");

                StoryPanel panel =
                        new StoryPanel(
                                1,
                                5,
                                window::dispose);

                window.setDefaultCloseOperation(
                        JFrame.DISPOSE_ON_CLOSE);

                window.setContentPane(panel);
                window.pack();
                window.setLocationRelativeTo(null);

                window.addWindowListener(
                        new WindowAdapter() {
                            @Override
                            public void windowClosed(
                                    WindowEvent event) {
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
