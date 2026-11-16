
package story;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.*;
import java.util.function.Consumer;

/**
 * GOG-24: In-game protagonist name entry.
 *
 * Uses the same font as the main menu.
 * Displays inside the existing game window.
 */
public class NameEntryPanel extends JPanel {

    private static final Color BACKGROUND =
            new Color(8, 24, 13);

    private static final Color BOX =
            new Color(5, 38, 17);

    private static final Color ACCENT =
            new Color(125, 255, 145);

    private static final Color TEXT =
            new Color(235, 255, 235);

    private static final Color SECONDARY =
            new Color(175, 210, 180);

    private final JTextField nameField;
    private final Consumer<String> onConfirmed;

    public NameEntryPanel(
            Consumer<String> onConfirmed) {

        this.onConfirmed = onConfirmed;

        setPreferredSize(
                new Dimension(448, 520));

        setBackground(BACKGROUND);
        setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS));

        content.setBorder(
                new EmptyBorder(
                        20, 25, 20, 25));

        // ======================================
        // TITLE
        // ======================================

        JLabel title =
                new JLabel("STORY MODE");

        title.setFont(
                StoryFont.bold(23));

        title.setForeground(ACCENT);

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        // ======================================
        // HEADING
        // ======================================

        JLabel heading =
                new JLabel("YOUR IDENTITY");

        heading.setFont(
                StoryFont.bold(16));

        heading.setForeground(TEXT);

        heading.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        // ======================================
        // DESCRIPTION
        // ======================================

        JLabel description =
                new JLabel(
                        "What should we call you, pilot?");

        description.setFont(
                StoryFont.regular(10));

        description.setForeground(SECONDARY);

        description.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        // ======================================
        // PLAYER NAME FIELD
        // ======================================

        nameField =
                new JTextField(18);

        nameField.setText("");

        nameField.setFont(
                StoryFont.regular(17));

        nameField.setForeground(TEXT);
        nameField.setBackground(BOX);
        nameField.setCaretColor(ACCENT);

        nameField.setSelectionColor(
                new Color(40, 105, 55));

        nameField.setHorizontalAlignment(
                JTextField.CENTER);

        nameField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ACCENT, 2),
                        new EmptyBorder(
                                10, 12, 10, 12)
                ));

        nameField.setMaximumSize(
                new Dimension(310, 52));

        // ======================================
        // CHARACTER LIMIT
        // ======================================

        ((AbstractDocument)
                nameField.getDocument())
                .setDocumentFilter(
                        new DocumentFilter() {

                            @Override
                            public void insertString(
                                    FilterBypass fb,
                                    int offset,
                                    String string,
                                    AttributeSet attr)
                                    throws BadLocationException {

                                if (string != null
                                        && fb.getDocument()
                                        .getLength()
                                        + string.length() <= 20) {

                                    super.insertString(
                                            fb,
                                            offset,
                                            string,
                                            attr);
                                }
                            }

                            @Override
                            public void replace(
                                    FilterBypass fb,
                                    int offset,
                                    int length,
                                    String text,
                                    AttributeSet attrs)
                                    throws BadLocationException {

                                if (text != null
                                        && fb.getDocument()
                                        .getLength()
                                        - length
                                        + text.length() <= 20) {

                                    super.replace(
                                            fb,
                                            offset,
                                            length,
                                            text,
                                            attrs);
                                }
                            }
                        });

        // ======================================
        // HINT
        // ======================================

        JLabel hint =
                new JLabel(
                        "Blank = Protagonist");

        hint.setFont(
                StoryFont.regular(10));

        hint.setForeground(SECONDARY);

        hint.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        // ======================================
        // CONTINUE BUTTON
        // ======================================

        JButton continueButton =
                new JButton("CONTINUE >");

        continueButton.setFont(
                StoryFont.bold(13));

        continueButton.setForeground(BACKGROUND);
        continueButton.setBackground(ACCENT);
        continueButton.setOpaque(true);
        continueButton.setFocusPainted(false);

        continueButton.setBorder(
                new EmptyBorder(
                        12, 22, 12, 22));

        continueButton.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        continueButton.addActionListener(
                e -> confirmName());

        nameField.addActionListener(
                e -> confirmName());

        // ======================================
        // LAYOUT
        // ======================================

        content.add(title);

        content.add(
                Box.createVerticalStrut(55));

        content.add(heading);

        content.add(
                Box.createVerticalStrut(18));

        content.add(description);

        content.add(
                Box.createVerticalStrut(28));

        content.add(nameField);

        content.add(
                Box.createVerticalStrut(12));

        content.add(hint);

        content.add(
                Box.createVerticalStrut(38));

        content.add(continueButton);

        add(content);

        // Focus the text field automatically.
        SwingUtilities.invokeLater(
                () -> nameField.requestFocusInWindow());
    }

    /**
     * Confirms the protagonist's name.
     */
    private void confirmName() {
        String name =
                nameField.getText().trim();

        if (name.isEmpty()) {
            name = "Protagonist";
        }

        if (onConfirmed != null) {
            onConfirmed.accept(name);
        }
    }

    /**
     * Draws the green decorative border.
     */
    @Override
    protected void paintComponent(
            Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        g.setColor(
                new Color(40, 105, 55));

        g.drawRoundRect(
                10, 10,
                getWidth() - 21,
                getHeight() - 21,
                12, 12);

        g.dispose();
    }
}
