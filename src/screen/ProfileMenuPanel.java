package screen;

import engine.StorySaveManager;
import engine.soundEngine.MenuSounds;
import story.StoryFont;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Startup profile selector (New Game / Load File / Exit). */
public final class ProfileMenuPanel extends JPanel {

    public static final class Choice {
        public final boolean newGame;
        public final StorySaveManager.Slot slot;
        private Choice(boolean create, StorySaveManager.Slot save) {
            this.newGame = create;
            this.slot = save;
        }
        public static Choice newGame() { return new Choice(true, null); }
        public static Choice load(StorySaveManager.Slot slot) { return new Choice(false, slot); }
    }

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int ROW_HEIGHT = 37;
    private static final int VISIBLE_SAVES = 7;
    private static final Color GREEN = Color.GREEN;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY = new Color(145, 150, 155);

    private static final Font TITLE_FONT = StoryFont.bold(24);
    private static final Font NORMAL_FONT = StoryFont.regular(14);
    private static final Font SELECTED_FONT = StoryFont.regular(17);
    private static final Font SMALL_FONT = StoryFont.regular(10);

    private final Consumer<Choice> onSelected;
    private final Starfield starfield;
    private final Timer timer;
    private final List<String> options = new ArrayList<>();
    private final List<StorySaveManager.Slot> saves = new ArrayList<>();

    private boolean showingSaves;
    private boolean finished;
    private int selected;
    private int scrollOffset;

    public ProfileMenuPanel(Consumer<Choice> callback) {
        this.onSelected = callback;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);
        starfield = new Starfield(WIDTH, HEIGHT);
        setupKeyboard();
        setupMouse();
        showHome();
        timer = new Timer(33, e -> {
            if (!finished) {
                starfield.update();
                repaint();
            }
        });
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void showHome() {
        showingSaves = false;
        saves.clear();
        options.clear();
        options.add("New Game");
        options.add("Load File");
        options.add("Exit");
        selected = 0;
        scrollOffset = 0;
        repaint();
    }

    private void showSaves() {
        List<StorySaveManager.Slot> loaded;
        try {
            loaded = StorySaveManager.list();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load saves:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        saves.clear();
        saves.addAll(loaded);
        showingSaves = true;
        options.clear();
        for (StorySaveManager.Slot slot : saves) options.add(slot.name);
        options.add("Back");
        selected = 0;
        scrollOffset = 0;
        repaint();
    }

    private void setupKeyboard() {
        bind("UP", "up", () -> move(-1));
        bind("W", "upW", () -> move(-1));
        bind("DOWN", "down", () -> move(1));
        bind("S", "downS", () -> move(1));
        bind("ENTER", "enter", this::confirm);
        bind("SPACE", "space", this::confirm);
        bind("ESCAPE", "back", this::back);
    }

    private void bind(String key, String id, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!finished) action.run();
            }
        });
    }

    private void move(int by) {
        if (finished || options.isEmpty()) return;
        selected = Math.floorMod(selected + by, options.size());
        if (showingSaves) {
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + VISIBLE_SAVES)
                scrollOffset = selected - VISIBLE_SAVES + 1;
        }
        MenuSounds.playMove();
        repaint();
    }

    private void confirm() {
        if (finished || options.isEmpty()) return;
        if (!showingSaves) {
            if (selected == 0) finish(Choice.newGame());
            else if (selected == 1) showSaves();
            else finish(null);
            return;
        }
        if (selected == options.size() - 1) {
            showHome();
        } else if (selected >= 0 && selected < saves.size()) {
            finish(Choice.load(saves.get(selected)));
        }
    }

    private void back() {
        if (showingSaves) showHome();
        else finish(null);
    }

    private void finish(Choice choice) {
        if (finished) return;
        finished = true;
        timer.stop();
        if (onSelected != null) onSelected.accept(choice);
    }

    private void setupMouse() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                selectMouse(e.getY(), false);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) selectMouse(e.getY(), true);
            }
        });
        addMouseWheelListener(e -> {
            if (showingSaves && !finished) {
                int wheel = e.getWheelRotation();
                if (wheel != 0) move(wheel > 0 ? 1 : -1);
            }
        });
    }

    private void selectMouse(int pixelY, boolean click) {
        if (finished || getHeight() == 0) return;
        int y = (int) (pixelY * HEIGHT / (double) getHeight());
        int firstY = showingSaves ? 188 : 265;
        int visible = showingSaves ? Math.min(VISIBLE_SAVES,
                options.size() - scrollOffset) : options.size();
        for (int row = 0; row < visible; row++) {
            int baseline = firstY + row * ROW_HEIGHT;
            if (y >= baseline - 25 && y < baseline + 12) {
                int index = showingSaves ? scrollOffset + row : row;
                if (index >= options.size()) return;
                if (selected != index) {
                    selected = index;
                    MenuSounds.playMove();
                    repaint();
                }
                if (click) confirm();
                return;
            }
        }
    }

    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original);
        Graphics2D g = (Graphics2D) original.create();
        g.scale(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        drawStarfield(g);
        drawHeading(g);
        if (showingSaves) drawSaves(g);
        else drawHome(g);
        g.setColor(GRAY);
        g.setFont(SMALL_FONT);
        center(g, "ARROWS / W S  |  ENTER  |  ESC BACK", HEIGHT - 24);
        g.dispose();
    }

    private void drawStarfield(Graphics2D g) {
        BufferedImage nebula = starfield.getNebula();
        g.drawImage(nebula, 0, 0, WIDTH, HEIGHT, null);
        for (Planet planet : starfield.getPlanets()) {
            g.drawImage(planet.getFrame(), planet.getX(), planet.getY(), null);
        }
        int unit = starfield.getStarPixel();
        for (int i = 0; i < starfield.getCount(); i++) {
            int x = starfield.getX(i), y = starfield.getY(i);
            g.setColor(starfield.getColor(i));
            g.fillRect(x, y, unit, unit);
            for (int j = 1; j <= starfield.getArm(i); j++) {
                if (j > 2 && j % 2 == 1) continue;
                g.fillRect(x + j * unit, y, unit, unit);
                g.fillRect(x - j * unit, y, unit, unit);
                g.fillRect(x, y + j * unit, unit, unit);
                g.fillRect(x, y - j * unit, unit, unit);
            }
        }
    }

    private void drawHeading(Graphics2D g) {
        g.setColor(GREEN);
        g.setFont(TITLE_FONT);
        center(g, "SELECT PROFILE", 90);
        g.setColor(GRAY);
        g.setFont(NORMAL_FONT);
        center(g, showingSaves ? "LOAD FILE" : "SELECT AN OPTION", 125);
        g.setColor(new Color(65, 125, 75));
        g.drawLine(65, 145, WIDTH - 65, 145);
    }

    private void drawHome(Graphics2D g) {
        for (int i = 0; i < options.size(); i++) {
            row(g, options.get(i), 265 + i * ROW_HEIGHT, i == selected);
        }
    }

    private void drawSaves(Graphics2D g) {
        if (saves.isEmpty()) {
            g.setFont(NORMAL_FONT);
            g.setColor(WHITE);
            center(g, "NO SAVED GAMES", 170);
        }
        int visible = Math.min(VISIBLE_SAVES, options.size() - scrollOffset);
        for (int i = 0; i < visible; i++) {
            int index = scrollOffset + i;
            String name = options.get(index);
            if (index < saves.size()) {
                StorySaveManager.Slot slot = saves.get(index);
                name += slot.completed ? " - COMPLETE" : " - LV " + slot.level;
            }
            row(g, name, 188 + i * ROW_HEIGHT, index == selected);
        }
        g.setColor(GRAY);
        g.setFont(SMALL_FONT);
        if (scrollOffset > 0) center(g, "^ MORE ABOVE ^", 163);
        if (scrollOffset + VISIBLE_SAVES < options.size())
            center(g, "v MORE BELOW v", 470);
    }

    private void row(Graphics2D g, String text, int y, boolean highlighted) {
        g.setFont(highlighted ? SELECTED_FONT : NORMAL_FONT);
        g.setColor(highlighted ? GREEN : WHITE);
        String label = highlighted ? "> " + text + " <" : text;
        FontMetrics fm = g.getFontMetrics();
        while (label.length() > 4 && fm.stringWidth(label) > WIDTH - 30)
            label = label.substring(0, label.length() - 4) + "...";
        center(g, label, y);
    }

    private void center(Graphics2D g, String text, int baseline) {
        FontMetrics m = g.getFontMetrics();
        g.drawString(text, (WIDTH - m.stringWidth(text)) / 2, baseline);
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
