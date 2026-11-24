
package story;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Story Mode cutscene player.
 *
 * - Background images for Panels 1-33
 * - Backgrounds fill the scene rectangle without stretching
 * - Center-crops images to remove black gaps
 * - Pixel-art nearest-neighbor rendering
 * - Emotion-based character portraits
 * - Typing sound
 * - SKIP button and ESC shortcut
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

    // =========================================
    // PORTRAIT CONFIGURATION
    // =========================================

    private static final int PORTRAIT_OFFSET_X = 8;
    private static final int PORTRAIT_OFFSET_Y = 8;

    // =========================================
    // SCENE BACKGROUND CONFIGURATION
    // =========================================

    private static final String PANEL_FOLDER =
            "res/panel/";

    // The rectangle above the dialogue box.
    private static final int SCENE_X = 14;
    private static final int SCENE_Y = 82;
    private static final int SCENE_WIDTH = WIDTH - 28;
    private static final int SCENE_HEIGHT = 194;

    private final Map<Integer, BufferedImage> sceneCache =
            new HashMap<>();

    private BufferedImage currentSceneImage;

    // =========================================
    // STORY STATE
    // =========================================

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

        if (firstPanel < 1 ||
                lastPanel > 33 ||
                firstPanel > lastPanel) {

            throw new IllegalArgumentException(
                    "Invalid story panel range"
            );
        }

        this.lastPanel = lastPanel;
        this.onFinished = onFinished;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
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

        bindKey("SPACE", "nextSpace", this::advance);
        bindKey("ENTER", "nextEnter", this::advance);
        bindKey("ESCAPE", "skipCutscene", this::skipCutscene);

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
    // LOAD STORY PANEL
    // =========================================

    private void loadPanel(int number) throws IOException {

        List<Dialogue> dialogues =
                StoryData.loadPanel(number);

        panelNumber = number;

        dialogueManager =
                new DialogueManager(dialogues);

        // Load background corresponding to this panel.
        currentSceneImage = loadSceneImage(number);

        startTyping();
    }

    // =========================================
    // LOAD BACKGROUND IMAGE
    // =========================================

    private BufferedImage loadSceneImage(int number) {

        // Load background images for panels 1-33.
        // Missing images fall back to a black background.
        if (number < 1 || number > 33) {
            return null;
        }

        if (sceneCache.containsKey(number)) {
            return sceneCache.get(number);
        }

        String path =
                PANEL_FOLDER + "panel" + number + ".png";

        BufferedImage image = null;

        try {

            File file = new File(path);

            if (file.isFile()) {

                image = ImageIO.read(file);

                if (image != null) {

                    System.out.println(
                            "[STORY] Loaded background: " + path
                    );

                } else {

                    System.err.println(
                            "[STORY] Unsupported background: " + path
                    );
                }

            } else {

                System.err.println(
                        "[STORY] Missing background: " + path
                );
            }

        } catch (IOException e) {

            System.err.println(
                    "[STORY] Failed to load: "
                            + path
                            + " - "
                            + e.getMessage()
            );
        }

        sceneCache.put(number, image);

        return image;
    }

    // =========================================
    // TYPING ANIMATION
    // =========================================

    private void startTyping() {

        typingTimer.stop();
        stopSound();

        visibleCharacters = 0;
        lettersSinceSound = 0;

        if (!finished &&
                dialogueManager.getCurrentDialogue() != null) {

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

        // Reveal the current sentence on first click.
        if (visibleCharacters < current.getText().length()) {

            visibleCharacters = current.getText().length();

            typingTimer.stop();
            stopSound();

            repaint();
            return;
        }

        // Move to next dialogue.
        dialogueManager.advanceDialogue();

        if (!dialogueManager.isFinished()) {
            startTyping();
            return;
        }

        // Move to next panel.
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

        // Keep pixel art sharp.
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
    // BLACK BACKGROUND
    // =========================================

    private void drawBackground(Graphics2D g) {

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    // =========================================
    // HEADER
    // =========================================

    private void drawHeader(Graphics2D g) {

        g.setColor(GREEN);
        g.setFont(TITLE_FONT);

        g.drawString("STORY MODE", 20, 35);

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        g.drawString(
                "PANEL " + panelNumber + " / 33",
                20,
                57
        );

        int progressWidth =
                (int) (WIDTH * panelNumber / 33.0);

        g.setColor(new Color(45, 65, 50));
        g.fillRect(0, 72, WIDTH, 3);

        g.setColor(GREEN);
        g.fillRect(0, 72, progressWidth, 3);
    }

    // =========================================
    // SCENE IMAGE — FILL WITHOUT STRETCHING
    // =========================================

    private void drawScene(Graphics2D g) {

        if (currentSceneImage == null) {
            return;
        }

        int imageWidth = currentSceneImage.getWidth();
        int imageHeight = currentSceneImage.getHeight();

        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        // Center-crop the image without stretching it.
        int cropX = 0;
        int cropY = 0;
        int cropWidth = imageWidth;
        int cropHeight = imageHeight;

        double imageRatio =
                imageWidth / (double) imageHeight;

        double sceneRatio =
                SCENE_WIDTH / (double) SCENE_HEIGHT;

        if (imageRatio > sceneRatio) {

            // Image is wider than the scene rectangle.
            cropWidth = Math.max(
                    1,
                    (int) Math.round(
                            imageHeight * sceneRatio
                    )
            );

            cropWidth = Math.min(cropWidth, imageWidth);

            cropX = (imageWidth - cropWidth) / 2;

        } else if (imageRatio < sceneRatio) {

            // Image is taller than the scene rectangle.
            cropHeight = Math.max(
                    1,
                    (int) Math.round(
                            imageWidth / sceneRatio
                    )
            );

            cropHeight = Math.min(cropHeight, imageHeight);

            cropY = (imageHeight - cropHeight) / 2;
        }

        // Destination: fills the entire scene rectangle.
        int destX1 = SCENE_X;
        int destY1 = SCENE_Y;
        int destX2 = SCENE_X + SCENE_WIDTH;
        int destY2 = SCENE_Y + SCENE_HEIGHT;

        // Source: center-cropped portion of the image.
        int srcX1 = cropX;
        int srcY1 = cropY;
        int srcX2 = cropX + cropWidth;
        int srcY2 = cropY + cropHeight;

        // Draw with nearest-neighbor interpolation.
        g.drawImage(
                currentSceneImage,
                destX1,
                destY1,
                destX2,
                destY2,
                srcX1,
                srcY1,
                srcX2,
                srcY2,
                null
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
                skipHovered ? GREEN : BOX_BORDER
        );

        g.drawRect(x, y, w, h);

        // Pixel-style corners.
        g.fillRect(x, y, 9, 3);
        g.fillRect(x, y, 3, 9);

        g.fillRect(x + w - 9, y, 9, 3);
        g.fillRect(x + w - 3, y, 3, 9);

        g.setFont(SMALL_FONT);
        g.setColor(skipHovered ? WHITE : GREEN);

        String label = "SKIP >";

        FontMetrics metrics = g.getFontMetrics();

        int textX =
                x + (w - metrics.stringWidth(label)) / 2;

        int textY =
                y + (h - metrics.getHeight()) / 2
                        + metrics.getAscent();

        g.drawString(label, textX, textY);
    }

    // =========================================
    // DIALOGUE BOX
    // =========================================

    private void drawDialogueBox(Graphics2D g) {

        final int boxX = 14;
        final int boxY = 284;
        final int boxW = WIDTH - 28;
        final int boxH = 220;

        // Main dialogue background.
        g.setColor(BOX_BG);
        g.fillRect(boxX, boxY, boxW, boxH);

        // Main dialogue border.
        g.setColor(BOX_BORDER);
        g.drawRect(boxX, boxY, boxW, boxH);

        // Decorative corners.
        g.setColor(GREEN);

        g.fillRect(boxX, boxY, 16, 3);
        g.fillRect(boxX, boxY, 3, 16);

        g.fillRect(boxX + boxW - 16, boxY, 16, 3);
        g.fillRect(boxX + boxW - 3, boxY, 3, 16);

        g.fillRect(boxX, boxY + boxH - 3, 16, 3);
        g.fillRect(boxX, boxY + boxH - 16, 3, 16);

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
        // CHARACTER PORTRAITS
        // =====================================

        if (!narration &&
                StoryPortraits.hasPortrait(current)) {

            StoryPortraits.draw(
                    g,
                    current,
                    boxX + PORTRAIT_OFFSET_X,
                    boxY + PORTRAIT_OFFSET_Y
            );

            textX = boxX + 125;

        } else {
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

        g.setColor(new Color(45, 90, 55));

        g.drawLine(
                textX,
                boxY + 45,
                boxX + boxW - 20,
                boxY + 45
        );

        // =====================================
        // DIALOGUE TEXT
        // =====================================

        String fullText = current.getText();

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

        Shape oldClip = g.getClip();

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

        FontMetrics metrics = g.getFontMetrics();

        String[] paragraphs =
                text.split("\n", -1);

        for (String paragraph : paragraphs) {

            StringBuilder line = new StringBuilder();

            for (String word : paragraph.split("\\s+")) {

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
                    line = new StringBuilder(word);

                } else {

                    line = new StringBuilder(candidate);
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
                                18,
                                18,
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
