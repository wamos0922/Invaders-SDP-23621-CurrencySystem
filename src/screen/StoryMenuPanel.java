
package screen;

import engine.StorySaveManager;
import engine.soundEngine.MenuSounds;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;
import story.StoryFont;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GOG-37: Story Mode menu.
 *
 * Uses the main menu's starfield, planets, fonts,
 * selection style, keyboard controls and sounds.
 *
 * Works inside the existing game window.
 */
public final class StoryMenuPanel extends JPanel {

    public static final class Choice {
        public final boolean newGame;
        public final StorySaveManager.Slot slot;

        private Choice(boolean newGame,
                       StorySaveManager.Slot slot) {
            this.newGame = newGame;
            this.slot = slot;
        }

        public static Choice newGame() {
            return new Choice(true, null);
        }

        public static Choice load(StorySaveManager.Slot slot) {
            return new Choice(false, slot);
        }
    }

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;

    private static final Color GREEN = Color.GREEN;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY = Color.GRAY;

    private static final int ROW_HEIGHT = 37;
    private static final int VISIBLE_SAVES = 7;

    private static final Font NORMAL_FONT =
            StoryFont.regular(14);

    private static final Font SELECTED_FONT =
            StoryFont.regular(17);

    private static final Font TITLE_FONT =
            StoryFont.bold(24);

    private static final Font SMALL_FONT =
            StoryFont.regular(10);

    private final Consumer<Choice> done;
    private final Starfield starfield;

    private final List<String> options =
            new ArrayList<>();

    private final List<StorySaveManager.Slot> saves =
            new ArrayList<>();

    private boolean showingSaves = false;
    private boolean finished = false;

    private int selected = 0;
    private int scrollOffset = 0;

    private final Timer animationTimer;

    public StoryMenuPanel(Consumer<Choice> done) {
        this.done = done;

        setPreferredSize(
                new Dimension(WIDTH, HEIGHT));

        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        starfield = new Starfield(WIDTH, HEIGHT);

        // The original title screen stops its music
        // when Story is selected. Resume it here.
        SoundManager.getInstance().playBgm(
                Sound.BGM_TITLE);

        setupKeyboard();
        setupMouse();

        showHome();

        animationTimer = new Timer(33, e -> {
            if (!finished) {
                starfield.update();
                repaint();
            }
        });

        animationTimer.start();

        SwingUtilities.invokeLater(
                this::requestFocusInWindow);
    }

    // =========================================
    // MENU CONTENT
    // =========================================

    private void showHome() {
        showingSaves = false;

        options.clear();
        options.add("New Game");
        options.add("Load File");
        options.add("Back");

        selected = 0;
        scrollOffset = 0;

        repaint();
    }

    private void showSaves() {
        List<StorySaveManager.Slot> loaded;

        try {
            loaded = StorySaveManager.list();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot read saves:\n"
                            + ex.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        saves.clear();
        saves.addAll(loaded);

        showingSaves = true;

        options.clear();

        for (StorySaveManager.Slot slot : saves) {
            options.add(slot.name);
        }

        options.add("Back");

        selected = 0;
        scrollOffset = 0;

        repaint();
    }

    // =========================================
    // KEYBOARD INPUT
    // =========================================

    private void setupKeyboard() {
        bindKey("UP", "moveUp", () -> move(-1));
        bindKey("W", "moveUpW", () -> move(-1));

        bindKey("DOWN", "moveDown", () -> move(1));
        bindKey("S", "moveDownS", () -> move(1));

        bindKey("ENTER", "confirmEnter",
                this::confirm);

        bindKey("SPACE", "confirmSpace",
                this::confirm);

        bindKey("ESCAPE", "goBack",
                this::goBack);
    }

    private void bindKey(String key,
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
                    public void actionPerformed(
                            ActionEvent event) {
                        if (!finished) {
                            action.run();
                        }
                    }
                }
        );
    }

    private void move(int direction) {
        if (options.isEmpty()) {
            return;
        }

        selected = Math.floorMod(
                selected + direction,
                options.size()
        );

        keepSelectionVisible();

        MenuSounds.playMove();
        repaint();
    }

    private void keepSelectionVisible() {
        if (!showingSaves) {
            return;
        }

        if (selected < scrollOffset) {
            scrollOffset = selected;
        }

        if (selected >= scrollOffset
                + VISIBLE_SAVES) {
            scrollOffset =
                    selected - VISIBLE_SAVES + 1;
        }

        scrollOffset = Math.max(
                0,
                Math.min(
                        scrollOffset,
                        Math.max(
                                0,
                                options.size()
                                        - VISIBLE_SAVES
                        )
                )
        );
    }

    // =========================================
    // MENU ACTIONS
    // =========================================

    private void confirm() {
        if (finished || options.isEmpty()) {
            return;
        }

        if (!showingSaves) {
            switch (selected) {
                case 0:
                    finish(Choice.newGame());
                    break;

                case 1:
                    showSaves();
                    break;

                case 2:
                    finish(null);
                    break;

                default:
                    break;
            }

            return;
        }

        // Last option is always Back.
        if (selected == options.size() - 1) {
            showHome();
            return;
        }

        if (selected >= 0
                && selected < saves.size()) {

            StorySaveManager.Slot slot =
                    saves.get(selected);

            finish(Choice.load(slot));
        }
    }

    private void goBack() {
        if (showingSaves) {
            showHome();
        } else {
            finish(null);
        }
    }

    private void finish(Choice choice) {
        if (finished) {
            return;
        }

        finished = true;
        animationTimer.stop();

        // Keep music when returning to main menu.
        // Stop it only when entering gameplay.
        if (choice != null) {
            SoundManager.getInstance().stopBgm();
        }

        if (done != null) {
            done.accept(choice);
        }
    }

    // =========================================
    // MOUSE INPUT
    // =========================================

    private void setupMouse() {
        addMouseMotionListener(
                new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(
                            MouseEvent event) {

                        selectFromMouse(
                                event.getY(),
                                false
                        );
                    }
                }
        );

        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(
                            MouseEvent event) {

                        if (SwingUtilities
                                .isLeftMouseButton(event)) {

                            selectFromMouse(
                                    event.getY(),
                                    true
                            );
                        }
                    }
                }
        );

        addMouseWheelListener(event -> {
            if (!showingSaves || finished) {
                return;
            }

            int direction =
                    event.getWheelRotation();

            if (direction != 0) {
                move(direction > 0 ? 1 : -1);
            }
        });
    }

    private void selectFromMouse(
            int mouseY,
            boolean click) {

        if (finished) {
            return;
        }

        double scale =
                getHeight() / (double) HEIGHT;

        if (scale <= 0) {
            return;
        }

        int logicalY =
                (int) (mouseY / scale);

        int firstY = showingSaves ? 188 : 265;

        int visibleCount = showingSaves
                ? Math.min(VISIBLE_SAVES,
                options.size())
                : options.size();

        for (int row = 0;
             row < visibleCount;
             row++) {

            int baseline =
                    firstY + row * ROW_HEIGHT;

            if (logicalY >= baseline - 25
                    && logicalY < baseline + 12) {

                int index = showingSaves
                        ? scrollOffset + row
                        : row;

                if (index >= options.size()) {
                    return;
                }

                if (selected != index) {
                    selected = index;
                    MenuSounds.playMove();
                    repaint();
                }

                if (click) {
                    confirm();
                }

                return;
            }
        }
    }

    // =========================================
    // DRAWING
    // =========================================

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        double scaleX =
                getWidth() / (double) WIDTH;

        double scaleY =
                getHeight() / (double) HEIGHT;

        g.scale(scaleX, scaleY);

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
        drawHeading(g);

        if (showingSaves) {
            drawSaveMenu(g);
        } else {
            drawHomeMenu(g);
        }

        drawHints(g);

        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        BufferedImage nebula =
                starfield.getNebula();

        g.drawImage(
                nebula,
                0,
                0,
                WIDTH,
                HEIGHT,
                null
        );

        // Same animated planets as main menu.
        for (Planet planet : starfield.getPlanets()) {
            g.drawImage(
                    planet.getFrame(),
                    planet.getX(),
                    planet.getY(),
                    null
            );
        }

        // Same twinkling stars as main menu.
        int unit = starfield.getStarPixel();

        for (int i = 0;
             i < starfield.getCount();
             i++) {

            int x = starfield.getX(i);
            int y = starfield.getY(i);
            int arm = starfield.getArm(i);

            g.setColor(starfield.getColor(i));

            g.fillRect(x, y, unit, unit);

            for (int d = 1; d <= arm; d++) {
                if (d > 2 && d % 2 == 1) {
                    continue;
                }

                g.fillRect(
                        x + d * unit,
                        y,
                        unit,
                        unit
                );

                g.fillRect(
                        x - d * unit,
                        y,
                        unit,
                        unit
                );

                g.fillRect(
                        x,
                        y + d * unit,
                        unit,
                        unit
                );

                g.fillRect(
                        x,
                        y - d * unit,
                        unit,
                        unit
                );
            }
        }
    }

    private void drawHeading(Graphics2D g) {
        g.setColor(GREEN);
        g.setFont(TITLE_FONT);

        drawCentered(
                g,
                "STORY MODE",
                90
        );

        g.setFont(NORMAL_FONT);
        g.setColor(GRAY);

        drawCentered(
                g,
                showingSaves
                        ? "LOAD FILE"
                        : "SELECT AN OPTION",
                125
        );

        g.setColor(new Color(65, 125, 75));

        g.drawLine(
                65,
                145,
                WIDTH - 65,
                145
        );
    }

    private void drawHomeMenu(Graphics2D g) {
        for (int i = 0;
             i < options.size();
             i++) {

            drawMenuRow(
                    g,
                    options.get(i),
                    265 + i * ROW_HEIGHT,
                    i == selected
            );
        }
    }

    private void drawSaveMenu(Graphics2D g) {
        if (saves.isEmpty()) {
            g.setFont(NORMAL_FONT);
            g.setColor(WHITE);

            drawCentered(
                    g,
                    "NO SAVED GAMES",
                    170
            );
        }

        int visible = Math.min(
                VISIBLE_SAVES,
                options.size() - scrollOffset
        );

        for (int row = 0;
             row < visible;
             row++) {

            int index = scrollOffset + row;

            if (index >= options.size()) {
                break;
            }

            String label = options.get(index);

            if (index < saves.size()) {
                StorySaveManager.Slot slot =
                        saves.get(index);

                String progress =
                        slot.completed
                                ? "COMPLETE"
                                : "LV " + slot.level;

                label = slot.name + " - " + progress;
            }

            drawMenuRow(
                    g,
                    label,
                    188 + row * ROW_HEIGHT,
                    index == selected
            );
        }

        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        if (scrollOffset > 0) {
            drawCentered(
                    g,
                    "^ MORE ABOVE ^",
                    163
            );
        }

        if (scrollOffset + VISIBLE_SAVES
                < options.size()) {

            drawCentered(
                    g,
                    "v MORE BELOW v",
                    470
            );
        }
    }

    /**
     * Main menu style:
     * selected = green, larger, > text <
     * normal = white, smaller.
     */
    private void drawMenuRow(
            Graphics2D g,
            String text,
            int baseline,
            boolean isSelected) {

        if (isSelected) {
            g.setFont(SELECTED_FONT);
            g.setColor(GREEN);

            drawCentered(
                    g,
                    "> " + fitText(
                            g,
                            text,
                            WIDTH - 75
                    ) + " <",
                    baseline
            );

        } else {
            g.setFont(NORMAL_FONT);
            g.setColor(WHITE);

            drawCentered(
                    g,
                    fitText(
                            g,
                            text,
                            WIDTH - 45
                    ),
                    baseline
            );
        }
    }

    private String fitText(
            Graphics2D g,
            String text,
            int maxWidth) {

        FontMetrics metrics =
                g.getFontMetrics();

        if (metrics.stringWidth(text)
                <= maxWidth) {
            return text;
        }

        String shortened = text;

        while (!shortened.isEmpty()
                && metrics.stringWidth(
                shortened + "..."
        ) > maxWidth) {

            shortened = shortened.substring(
                    0,
                    shortened.length() - 1
            );
        }

        return shortened + "...";
    }

    private void drawHints(Graphics2D g) {
        g.setColor(GRAY);
        g.setFont(SMALL_FONT);

        drawCentered(
                g,
                "W/S OR ARROWS TO MOVE",
                HEIGHT - 45
        );

        drawCentered(
                g,
                "SPACE SELECT   ESC BACK",
                HEIGHT - 27
        );
    }

    private void drawCentered(
            Graphics2D g,
            String text,
            int baseline) {

        FontMetrics metrics =
                g.getFontMetrics();

        int x = (WIDTH
                - metrics.stringWidth(text)) / 2;

        g.drawString(text, x, baseline);
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }
}
