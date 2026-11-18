
package story;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Story Mode cutscene player.
 *
 * - Black background
 * - Emotion-based character portraits
 * - Portraits aligned to top-left of dialogue box
 * - No additional portrait borders
 * - Typing sound
 * - Skip button and ESC shortcut
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

    private static final Rectangle SKIP_BUTTON =
            new Rectangle(345, 17, 83, 34);

    // Portrait positioning inside the dialogue box.
    private static final int PORTRAIT_OFFSET_X = 8;
    private static final int PORTRAIT_OFFSET_Y = 8;

    private final int lastPanel;
    private final Runnable onFinished;

    private final Timer typingTimer;
    private final Timer animationTimer;

    private int panelNumber;
    private DialogueManager dialogueManager;

    private int visibleCharacters;
    private int lettersSinceSound;

    private boolean finished;
    private boolean skipHovered;

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
                    "Invalid story panel range"
            );
        }

        this.lastPanel = lastPanel;
        this.onFinished = onFinished;

        setPreferredSize(
                new Dimension(WIDTH, HEIGHT)
        );

        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        typingTimer = new Timer(
                TYPING_DELAY,
                e -> updateTyping()
        );

        animationTimer = new Timer(
                100,
                e -> {
                    if (!finished) {
                        repaint();
                    }
                }
        );

        loadTypingSound();
        loadPanel(firstPanel);
        setupControls();

        animationTimer.start();

        SwingUtilities.invokeLater(
                this::requestFocusInWindow
        );
    }

    // =========================================
    // INPUT
    // =========================================

    private void setupControls() {

        bindKey(
                "SPACE",
                "nextSpace",
                this::advance
        );

        bindKey(
                "ENTER",
                "nextEnter",
                this::advance
        );

        bindKey(
                "ESCAPE",
                "skipCutscene",
                this::skipCutscene
        );

        MouseAdapter mouse = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent event) {

                if (!SwingUtilities.isLeftMouseButton(event)
                        || finished) {
                    return;
                }

                Point point = toLogicalPoint(event);

                if (SKIP_BUTTON.contains(point)) {
                    skipCutscene();
                } else {
                    advance();
                }
            }

            @Override
            public void mouseMoved(MouseEvent event) {

                Point point = toLogicalPoint(event);

                boolean hovered =
                        SKIP_BUTTON.contains(point);

                if (hovered != skipHovered) {
                    skipHovered = hovered;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                skipHovered = false;
                repaint();
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private Point toLogicalPoint(MouseEvent event) {

        double scaleX =
                WIDTH / (double) Math.max(1, getWidth());

        double scaleY =
                HEIGHT / (double) Math.max(1, getHeight());

        return new Point(
                (int) (event.getX() * scaleX),
                (int) (event.getY() * scaleY)
        );
    }

    private void bindKey(
            String key,
            String actionName,
            Runnable action) {

        getInputMap(
                WHEN_IN_FOCUSED_WINDOW
        ).put(
                KeyStroke.getKeyStroke(key),
                actionName
        );

        getActionMap().put(
                actionName,
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        action.run();
                    }
                }
        );
    }

    // =========================================
    // SKIP CUTSCENE
    // =========================================

    private void skipCutscene() {

        if (finished) {
            return;
        }

        System.out.println(
                "[STORY] Skipping panels "
                        + panelNumber
                        + " to "
                        + lastPanel
        );

        finishCutscene();
    }

    private void finishCutscene() {

        if (finished) {
            return;
        }

        finished = true;

        close();

        if (onFinished != null) {
            onFinished.run();
        }
    }

    // =========================================
    // TYPING SOUND
    // =========================================

    private void loadTypingSound() {

        try (AudioInputStream stream =
                     AudioSystem.getAudioInputStream(
                             new File(
                                     "res/sounds/Typing_Sound.wav"
                             )
                     )) {

            typingSound = AudioSystem.getClip();
            typingSound.open(stream);

        } catch (Exception e) {

            System.err.println(
                    "Typing sound unavailable: "
                            + e.getMessage()
            );
        }
    }

    private void playSound() {

        if (typingSound == null || finished) {
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

        if (!finished
                && dialogueManager.getCurrentDialogue()
                != null) {

            typingTimer.start();
        }

        repaint();
    }

    private void updateTyping() {

        if (finished) {

            typingTimer.stop();
            return;
        }

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

        // First click reveals the whole line.
        if (visibleCharacters < current.getText().length()) {

            visibleCharacters =
                    current.getText().length();

            typingTimer.stop();
            stopSound();

            repaint();
            return;
        }

        // Move to the next dialogue.
        dialogueManager.advanceDialogue();

        if (!dialogueManager.isFinished()) {

            startTyping();
            return;
        }

        // Move to the next panel.
        if (panelNumber < lastPanel) {

            try {

                loadPanel(panelNumber + 1);

            } catch (IOException e) {

                typingTimer.stop();
                stopSound();

                JOptionPane.showMessageDialog(
                        this,
                        "Story loading error: "
                                + e.getMessage()
                );
            }

            return;
        }

        finishCutscene();
    }

    // =========================================
    // PAINTING
    // =========================================

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        g.scale(
                getWidth() / (double) WIDTH,
                getHeight() / (double) HEIGHT
        );

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        );

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        drawBackground(g);
        drawHeader(g);
        drawScene(g);
        drawDialogueBox(g);
        drawSkipButton(g);

        g.dispose();
    }

    // =========================================
    // BACKGROUND
    // =========================================

    private void drawBackground(Graphics2D g) {

        g.setColor(Color.BLACK);

        g.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );
    }

    // =========================================
    // HEADER
    // =========================================

    private void drawHeader(Graphics2D g) {

        g.setColor(GREEN);
        g.setFont(TITLE_FONT);

        g.drawString(
                "STORY MODE",
                20,
                35
        );

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "PANEL " + panelNumber + " / 33",
                20,
                57
        );

        int progressWidth =
                (int) (
                        WIDTH * panelNumber / 33.0
                );

        g.setColor(
                new Color(45, 65, 50)
        );

        g.fillRect(
                0,
                72,
                WIDTH,
                3
        );

        g.setColor(GREEN);

        g.fillRect(
                0,
                72,
                progressWidth,
                3
        );
    }

    // =========================================
    // SKIP BUTTON
    // =========================================

    private void drawSkipButton(Graphics2D g) {

        int x = SKIP_BUTTON.x;
        int y = SKIP_BUTTON.y;
        int w = SKIP_BUTTON.width;
        int h = SKIP_BUTTON.height;

        g.setColor(
                skipHovered
                        ? new Color(25, 75, 35)
                        : new Color(8, 20, 12)
        );

        g.fillRect(x, y, w, h);

        g.setColor(
                skipHovered
                        ? GREEN
                        : BOX_BORDER
        );

        g.drawRect(x, y, w, h);

        // Pixel-style corners.
        g.fillRect(x, y, 9, 3);
        g.fillRect(x, y, 3, 9);

        g.fillRect(
                x + w - 9,
                y,
                9,
                3
        );

        g.fillRect(
                x + w - 3,
                y,
                3,
                9
        );

        g.setFont(SMALL_FONT);

        g.setColor(
                skipHovered ? WHITE : GREEN
        );

        String label = "SKIP >";

        FontMetrics metrics =
                g.getFontMetrics();

        int textX =
                x + (w - metrics.stringWidth(label)) / 2;

        int textY =
                y + (h - metrics.getHeight()) / 2
                        + metrics.getAscent();

        g.drawString(
                label,
                textX,
                textY
        );
    }

    // =========================================
    // SCENE AREA
    // =========================================

    private void drawScene(Graphics2D g) {

        // Reserved for future background illustrations,
        // full-body character sprites and story scenes.
    }

    // =========================================
    // DIALOGUE BOX
    // =========================================

    private void drawDialogueBox(Graphics2D g) {

        final int boxX = 14;
        final int boxY = 284;
        final int boxW = WIDTH - 28;
        final int boxH = 220;

        // Main dialogue box background.
        g.setColor(BOX_BG);

        g.fillRect(
                boxX,
                boxY,
                boxW,
                boxH
        );

        // Main dialogue box border.
        g.setColor(BOX_BORDER);

        g.drawRect(
                boxX,
                boxY,
                boxW,
                boxH
        );

        // Decorative corners.
        g.setColor(GREEN);

        g.fillRect(boxX, boxY, 16, 3);
        g.fillRect(boxX, boxY, 3, 16);

        g.fillRect(
                boxX + boxW - 16,
                boxY,
                16,
                3
        );

        g.fillRect(
                boxX + boxW - 3,
                boxY,
                3,
                16
        );

        g.fillRect(
                boxX,
                boxY + boxH - 3,
                16,
                3
        );

        g.fillRect(
                boxX,
                boxY + boxH - 16,
                3,
                16
        );

        g.fillRect(
                boxX + boxW - 16,
                boxY + boxH - 3,
                16,
                3
        );

        g.fillRect(
                boxX + boxW - 3,
                boxY + boxH - 16,
                3,
                16
        );

        Dialogue current =
                dialogueManager.getCurrentDialogue();

        if (current == null) {
            return;
        }

        boolean narration =
                current.getType()
                        == Dialogue.DialogueType.NARRATION;

        int textX;

        // =====================================
        // TOP-LEFT CHARACTER PORTRAIT
        // =====================================

        if (!narration &&
                StoryPortraits.hasPortrait(current)) {

            // Image begins near the top-left
            // corner of the dialogue box.
            //
            // The PNG already contains its border,
            // so no additional portrait frame is drawn.
            StoryPortraits.draw(
                    g,
                    current,
                    boxX + PORTRAIT_OFFSET_X,
                    boxY + PORTRAIT_OFFSET_Y
            );

            // Reserve space for the portrait.
            textX = boxX + 125;

        } else {

            // Narration and characters without portraits
            // use the entire dialogue width.
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
                boxY + 32
        );

        // Divider under speaker name.
        g.setColor(
                new Color(45, 90, 55)
        );

        g.drawLine(
                textX,
                boxY + 45,
                boxX + boxW - 20,
                boxY + 45
        );

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
                                fullText.length()
                        )
                );

        g.setFont(DIALOGUE_FONT);
        g.setColor(WHITE);

        int availableWidth =
                boxX + boxW - textX - 20;

        Shape oldClip =
                g.getClip();

        g.clipRect(
                textX,
                boxY + 53,
                availableWidth,
                118
        );

        drawWrappedText(
                g,
                visibleText,
                textX,
                boxY + 75,
                availableWidth
        );

        g.setClip(oldClip);

        // =====================================
        // TYPING / CONTINUE INDICATOR
        // =====================================

        boolean typingComplete =
                visibleCharacters >= fullText.length();

        g.setFont(SMALL_FONT);

        if (typingComplete) {

            boolean visible =
                    (System.currentTimeMillis() / 450)
                            % 2 == 0;

            if (visible) {

                g.setColor(GREEN);

                g.drawString(
                        "CONTINUE  >",
                        boxX + boxW - 125,
                        boxY + boxH - 18
                );
            }

        } else {

            g.setColor(GRAY);

            g.drawString(
                    "TYPING...",
                    boxX + boxW - 120,
                    boxY + boxH - 18
            );
        }

        // Controls.
        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "SPACE / ENTER / CLICK",
                boxX + 18,
                boxY + boxH - 18
        );
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
                            y
                    );

                    y += LINE_SPACING;

                    line =
                            new StringBuilder(word);

                } else {

                    line =
                            new StringBuilder(candidate);
                }
            }

            if (line.length() > 0) {

                g.drawString(
                        line.toString(),
                        x,
                        y
                );
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
    // STANDALONE PREVIEW
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
                                window::dispose
                        );

                window.setDefaultCloseOperation(
                        JFrame.DISPOSE_ON_CLOSE
                );

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
                        }
                );

                window.setVisible(true);

            } catch (IOException e) {

                e.printStackTrace();
            }
        });
    }
}
