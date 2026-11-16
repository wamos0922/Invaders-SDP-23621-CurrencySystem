
package story;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

public class StoryPanel extends JPanel {

    private static final int FIRST_PANEL = 1;
    private static final int LAST_PANEL = 5;

    private int panelNumber;
    private DialogueManager dialogueManager;

    public StoryPanel() throws IOException {
        setPreferredSize(new Dimension(448, 520));
        setBackground(new Color(15, 20, 35));
        setFocusable(true);

        loadStoryPanel(FIRST_PANEL);

        // Space and Enter both advance the dialogue.
        bindAdvanceKey("SPACE", "advanceSpace");
        bindAdvanceKey("ENTER", "advanceEnter");
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
        repaint();
    }

    private void advance() {
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
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        int width = getWidth();
        int height = getHeight();

        // Temporary background until panel artwork is ready.
        g.setColor(new Color(22, 30, 50));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("STORY MODE", 20, 40);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("Panel " + panelNumber, 20, 68);

        // Dialogue box.
        int boxY = height - 190;
        g.setColor(new Color(5, 10, 20));
        g.fillRoundRect(15, boxY, width - 30, 175, 15, 15);

        Dialogue current = dialogueManager.getCurrentDialogue();

        if (current != null) {
            g.setColor(Color.CYAN);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(current.getSpeaker(), 30, boxY + 30);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));

            // Wrap long dialogue across multiple lines.
            drawWrappedText(g, current.getText(),
                    30, boxY + 60, width - 60);
        } else {
            g.setColor(Color.WHITE);
            g.drawString("End of prologue.", 30, boxY + 60);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("SPACE / ENTER - Continue",
                30, height - 25);

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
                y += 20;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            g.drawString(line.toString(), x, y);
        }
    }

    // Temporary entry point to test the visual story screen.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Space Invader - Story Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(new StoryPanel());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
