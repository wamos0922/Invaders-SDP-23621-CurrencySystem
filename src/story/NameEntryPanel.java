
package story;

import screen.Planet;
import screen.Starfield;
import engine.soundEngine.MenuSounds;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Main-menu styled protagonist name entry.
 */
public class NameEntryPanel extends JPanel {

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int MAX_NAME_LENGTH = 20;

    private static final Color GREEN = Color.GREEN;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY = Color.GRAY;

    private static final Font TITLE_FONT =
            StoryFont.bold(24);

    private static final Font HEADING_FONT =
            StoryFont.regular(16);

    private static final Font INPUT_FONT =
            StoryFont.regular(18);

    private static final Font MENU_FONT =
            StoryFont.regular(16);

    private static final Font HINT_FONT =
            StoryFont.regular(10);

    private final JTextField nameField;
    private final Consumer<String> onConfirmed;
    private final Starfield starfield;
    private final Timer animationTimer;

    private boolean continueSelected = false;
    private boolean finished = false;

    public NameEntryPanel(Consumer<String> onConfirmed) {

        this.onConfirmed = onConfirmed;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setLayout(null);
        setFocusable(true);
        setDoubleBuffered(true);

        starfield = new Starfield(WIDTH, HEIGHT);

        // Name input field.
        nameField = new JTextField();

        nameField.setText("");
        nameField.setFont(INPUT_FONT);
        nameField.setForeground(WHITE);
        nameField.setBackground(new Color(8, 8, 17));

        nameField.setCaretColor(GREEN);
        nameField.setSelectionColor(new Color(35, 105, 45));

        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setBorder(
                BorderFactory.createLineBorder(GREEN, 2)
        );

        nameField.setOpaque(true);

        ((AbstractDocument) nameField.getDocument())
                .setDocumentFilter(new NameLimitFilter());

        nameField.addActionListener(e -> confirmName());

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                continueSelected = false;
                repaint();
            }
        });

        add(nameField);

        setupKeyboard();
        setupMouse();

        animationTimer = new Timer(33, e -> {
            if (!finished) {
                starfield.update();
                repaint();
            }
        });

        animationTimer.start();

        SwingUtilities.invokeLater(
                nameField::requestFocusInWindow
        );
    }

    // =========================================
    // LAYOUT
    // =========================================

    @Override
    public void doLayout() {
        super.doLayout();

        double scaleX = getWidth() / (double) WIDTH;
        double scaleY = getHeight() / (double) HEIGHT;

        nameField.setBounds(
                (int) (74 * scaleX),
                (int) (269 * scaleY),
                (int) (300 * scaleX),
                (int) (48 * scaleY)
        );
    }

    // =========================================
    // KEYBOARD
    // =========================================

    private void setupKeyboard() {

        bindKey("ESCAPE", "focusName", () -> {
            continueSelected = false;
            nameField.requestFocusInWindow();
            repaint();
        });

        bindKey("DOWN", "selectContinue",
                this::selectContinue);

        bindKey("UP", "selectName",
                this::selectName);

        bindKey("W", "selectNameW",
                this::selectName);

        bindKey("S", "selectContinueS",
                this::selectContinue);
    }

    private void bindKey(
            String key,
            String actionName,
            Runnable action) {

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(key),
                actionName
        );

        getActionMap().put(
                actionName,
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        if (!finished) {
                            action.run();
                        }
                    }
                }
        );
    }

    private void selectContinue() {
        if (!continueSelected) {
            continueSelected = true;
            MenuSounds.playMove();
        }

        requestFocusInWindow();
        repaint();
    }

    private void selectName() {
        if (continueSelected) {
            MenuSounds.playMove();
        }

        continueSelected = false;
        nameField.requestFocusInWindow();
        repaint();
    }

    // =========================================
    // MOUSE
    // =========================================

    private void setupMouse() {

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {

                int x = logicalX(event.getX());
                int y = logicalY(event.getY());

                if (isOverContinue(x, y)) {
                    if (!continueSelected) {
                        continueSelected = true;
                        MenuSounds.playMove();
                        repaint();
                    }
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {

                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }

                int x = logicalX(event.getX());
                int y = logicalY(event.getY());

                if (isOverContinue(x, y)) {
                    confirmName();
                }
            }
        });
    }

    private boolean isOverContinue(int x, int y) {
        return x >= 90 && x <= WIDTH - 90
                && y >= 355 && y <= 410;
    }

    private int logicalX(int x) {
        if (getWidth() <= 0) {
            return x;
        }

        return (int) (
                x * WIDTH / (double) getWidth()
        );
    }

    private int logicalY(int y) {
        if (getHeight() <= 0) {
            return y;
        }

        return (int) (
                y * HEIGHT / (double) getHeight()
        );
    }

    // =========================================
    // CONFIRM NAME
    // =========================================

    private void confirmName() {

        if (finished) {
            return;
        }

        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            name = "Protagonist";
        }

        finished = true;
        animationTimer.stop();

        if (onConfirmed != null) {
            onConfirmed.accept(name);
        }
    }

    // =========================================
    // DRAWING
    // =========================================

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

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
        drawTitle(g);
        drawContent(g);
        drawContinue(g);
        drawHints(g);

        g.dispose();
    }

    private void drawBackground(Graphics2D g) {

        BufferedImage nebula = starfield.getNebula();

        g.drawImage(
                nebula,
                0,
                0,
                WIDTH,
                HEIGHT,
                null
        );

        for (Planet planet : starfield.getPlanets()) {
            g.drawImage(
                    planet.getFrame(),
                    planet.getX(),
                    planet.getY(),
                    null
            );
        }

        int unit = starfield.getStarPixel();

        for (int i = 0; i < starfield.getCount(); i++) {

            int x = starfield.getX(i);
            int y = starfield.getY(i);
            int arm = starfield.getArm(i);

            g.setColor(starfield.getColor(i));
            g.fillRect(x, y, unit, unit);

            for (int d = 1; d <= arm; d++) {

                if (d > 2 && d % 2 == 1) {
                    continue;
                }

                g.fillRect(x + d * unit, y, unit, unit);
                g.fillRect(x - d * unit, y, unit, unit);
                g.fillRect(x, y + d * unit, unit, unit);
                g.fillRect(x, y - d * unit, unit, unit);
            }
        }
    }

    private void drawTitle(Graphics2D g) {

        g.setColor(GREEN);
        g.setFont(TITLE_FONT);

        drawCentered(g, "STORY MODE", 90);

        g.setFont(HEADING_FONT);
        g.setColor(GRAY);

        drawCentered(g, "NEW GAME", 126);

        g.setColor(new Color(65, 125, 75));
        g.drawLine(65, 145, WIDTH - 65, 145);
    }

    private void drawContent(Graphics2D g) {

        g.setColor(WHITE);
        g.setFont(HEADING_FONT);

        drawCentered(g, "YOUR IDENTITY", 210);

        g.setColor(GRAY);
        g.setFont(HINT_FONT);

        drawCentered(
                g,
                "WHAT SHOULD WE CALL YOU, PILOT?",
                239
        );

        drawCentered(
                g,
                "BLANK = PROTAGONIST",
                339
        );
    }

    private void drawContinue(Graphics2D g) {

        g.setFont(MENU_FONT);

        if (continueSelected) {
            g.setColor(GREEN);
            drawCentered(g, "> CONTINUE <", 388);
        } else {
            g.setColor(WHITE);
            drawCentered(g, "CONTINUE", 388);
        }
    }

    private void drawHints(Graphics2D g) {

        g.setColor(GRAY);
        g.setFont(HINT_FONT);

        drawCentered(g, "TYPE YOUR NAME", HEIGHT - 45);
        drawCentered(g, "ENTER TO CONTINUE", HEIGHT - 27);
    }

    private void drawCentered(
            Graphics2D g,
            String text,
            int baseline) {

        FontMetrics metrics = g.getFontMetrics();

        int x = (WIDTH - metrics.stringWidth(text)) / 2;

        g.drawString(text, x, baseline);
    }

    // =========================================
    // 20-CHARACTER LIMIT
    // =========================================

    private static class NameLimitFilter
            extends DocumentFilter {

        @Override
        public void insertString(
                FilterBypass fb,
                int offset,
                String text,
                AttributeSet attributes)
                throws BadLocationException {

            if (text == null) {
                return;
            }

            if (fb.getDocument().getLength()
                    + text.length() <= MAX_NAME_LENGTH) {

                super.insertString(
                        fb,
                        offset,
                        text,
                        attributes
                );
            }
        }

        @Override
        public void replace(
                FilterBypass fb,
                int offset,
                int length,
                String text,
                AttributeSet attributes)
                throws BadLocationException {

            if (text == null) {
                return;
            }

            int newLength =
                    fb.getDocument().getLength()
                            - length
                            + text.length();

            if (newLength <= MAX_NAME_LENGTH) {
                super.replace(
                        fb,
                        offset,
                        length,
                        text,
                        attributes
                );
            }
        }
    }

    // =========================================
    // CLEANUP
    // =========================================

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }
}
