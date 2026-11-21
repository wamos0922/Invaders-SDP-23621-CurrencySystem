package story;

import engine.ProfileSession;
import engine.StorySaveManager;
import engine.soundEngine.MenuSounds;
import screen.Planet;
import screen.Starfield;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Name entry at profile creation.
 * When StoryMode invokes this class after a profile was selected, it
 * immediately reuses the chosen name instead of asking for it again.
 */
public final class NameEntryPanel extends JPanel {
    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int MAX_NAME_LENGTH = 20;
    private static final Color GREEN = Color.GREEN;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY = Color.GRAY;
    private static final Font TITLE_FONT = StoryFont.bold(24);
    private static final Font HEADING_FONT = StoryFont.regular(16);
    private static final Font INPUT_FONT = StoryFont.regular(18);
    private static final Font MENU_FONT = StoryFont.regular(16);
    private static final Font HINT_FONT = StoryFont.regular(10);

    private final JTextField nameField;
    private final Consumer<String> onConfirmed;
    private final Starfield starfield;
    private final Timer timer;
    private boolean continueSelected;
    private boolean finished;

    public NameEntryPanel(Consumer<String> callback) {
        this.onConfirmed = callback;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setLayout(null);
        setFocusable(true);
        setDoubleBuffered(true);
        starfield = new Starfield(WIDTH, HEIGHT);

        nameField = new JTextField();
        nameField.setFont(INPUT_FONT);
        nameField.setForeground(WHITE);
        nameField.setBackground(new Color(8, 8, 17));
        nameField.setCaretColor(GREEN);
        nameField.setSelectionColor(new Color(35, 105, 45));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setOpaque(true);
        nameField.setBorder(BorderFactory.createLineBorder(GREEN, 2));
        ((AbstractDocument) nameField.getDocument()).setDocumentFilter(
                new NameLimitFilter());
        nameField.addActionListener(e -> confirmName());
        nameField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                continueSelected = false;
                repaint();
            }
        });
        add(nameField);
        setupKeyboard();
        setupMouse();

        timer = new Timer(33, e -> {
            if (!finished) {
                starfield.update();
                repaint();
            }
        });
        timer.start();

        StorySaveManager.Slot active = ProfileSession.getActive();
        if (active != null) {
            // StoryMode's existing name-entry stage becomes a no-op.
            SwingUtilities.invokeLater(() -> finish(active.name));
        } else {
            SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        }
    }

    @Override public void doLayout() {
        super.doLayout();
        double sx = getWidth() / (double) WIDTH;
        double sy = getHeight() / (double) HEIGHT;
        nameField.setBounds((int) (74 * sx), (int) (269 * sy),
                (int) (300 * sx), (int) (48 * sy));
    }

    private void setupKeyboard() {
        bind("ESCAPE", "cancel", () -> {
            if (ProfileSession.getActive() == null) finish(null);
            else nameField.requestFocusInWindow();
        });
        bind("DOWN", "selectContinue", this::selectContinue);
        bind("S", "selectContinueW", this::selectContinue);
        bind("UP", "selectName", this::selectName);
        bind("W", "selectNameW", this::selectName);
        bind("ENTER", "confirm", this::confirmName);
    }

    private void bind(String key, String id, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!finished) action.run();
            }
        });
    }

    private void selectContinue() {
        continueSelected = true;
        MenuSounds.playMove();
        requestFocusInWindow();
        repaint();
    }

    private void selectName() {
        continueSelected = false;
        MenuSounds.playMove();
        nameField.requestFocusInWindow();
        repaint();
    }

    private void setupMouse() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (finished || !SwingUtilities.isLeftMouseButton(e)) return;
                int x = (int) (e.getX() * WIDTH / (double) Math.max(1, getWidth()));
                int y = (int) (e.getY() * HEIGHT / (double) Math.max(1, getHeight()));
                if (x >= 90 && x <= WIDTH - 90 && y >= 355 && y <= 410)
                    confirmName();
            }
        });
    }

    private void confirmName() {
        if (finished) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Protagonist";
        finish(name);
    }

    private void finish(String name) {
        if (finished) return;
        finished = true;
        timer.stop();
        if (onConfirmed != null) onConfirmed.accept(name);
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
        drawBackground(g);
        g.setColor(GREEN);
        g.setFont(TITLE_FONT);
        center(g, "NEW PROFILE", 90);
        g.setFont(HEADING_FONT);
        g.setColor(GRAY);
        center(g, "PILOT NAME", 126);
        g.setColor(new Color(65, 125, 75));
        g.drawLine(65, 145, WIDTH - 65, 145);
        g.setColor(WHITE);
        g.setFont(HEADING_FONT);
        center(g, "YOUR IDENTITY", 210);
        g.setFont(HINT_FONT);
        g.setColor(GRAY);
        center(g, "WHAT SHOULD WE CALL YOU, PILOT?", 239);
        center(g, "BLANK = PROTAGONIST", 339);
        g.setFont(MENU_FONT);
        g.setColor(continueSelected ? GREEN : WHITE);
        center(g, continueSelected ? "> CONTINUE <" : "CONTINUE", 388);
        g.setColor(GRAY);
        g.setFont(HINT_FONT);
        center(g, "TYPE YOUR NAME", HEIGHT - 45);
        center(g, "ENTER TO CONTINUE  |  ESC CANCEL", HEIGHT - 27);
        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        BufferedImage nebula = starfield.getNebula();
        g.drawImage(nebula, 0, 0, WIDTH, HEIGHT, null);
        for (Planet p : starfield.getPlanets())
            g.drawImage(p.getFrame(), p.getX(), p.getY(), null);
        int unit = starfield.getStarPixel();
        for (int i = 0; i < starfield.getCount(); i++) {
            int x = starfield.getX(i), y = starfield.getY(i);
            g.setColor(starfield.getColor(i));
            g.fillRect(x, y, unit, unit);
            for (int d = 1; d <= starfield.getArm(i); d++) {
                if (d > 2 && d % 2 == 1) continue;
                g.fillRect(x + d * unit, y, unit, unit);
                g.fillRect(x - d * unit, y, unit, unit);
                g.fillRect(x, y + d * unit, unit, unit);
                g.fillRect(x, y - d * unit, unit, unit);
            }
        }
    }

    private void center(Graphics2D g, String text, int baseline) {
        FontMetrics m = g.getFontMetrics();
        g.drawString(text, (WIDTH - m.stringWidth(text)) / 2, baseline);
    }

    private static final class NameLimitFilter extends DocumentFilter {
        @Override public void insertString(FilterBypass fb, int offset, String text,
                                           AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            if (fb.getDocument().getLength() + text.length() <= MAX_NAME_LENGTH)
                super.insertString(fb, offset, text, attrs);
        }
        @Override public void replace(FilterBypass fb, int offset, int length, String text,
                                      AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            if (fb.getDocument().getLength() - length + text.length() <= MAX_NAME_LENGTH)
                super.replace(fb, offset, length, text, attrs);
        }
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
