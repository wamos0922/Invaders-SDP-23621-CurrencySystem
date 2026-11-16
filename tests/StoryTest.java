
package test;

import story.StoryPanel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * GOG-24 private story sequence tester.
 *
 * Run this class directly in IntelliJ.
 *
 * This does not launch Core.java, award currency,
 * modify save data, or affect normal gameplay.
 *
 * Each checkpoint simulates the end of a level
 * or tutorial and plays the appropriate cutscene.
 */
public final class StoryTest {

    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 780;

    private static final String[] CHECKPOINTS = {
            "Start - Prologue (Panels 1-5)",
            "Tutorial complete (Panels 6-8)",
            "Level 5 complete (Panels 9-11)",
            "Level 10 complete (Panels 12-13)",
            "Level 20 complete (Panels 14-17)",
            "Level 30 complete (Panel 18)",
            "Level 35 complete (Panels 19-26)",
            "Level 39 complete (Panel 27)",
            "Level 40 complete (Panels 28-33)"
    };

    private static final int[][] RANGES = {
            {1, 5},
            {6, 8},
            {9, 11},
            {12, 13},
            {14, 17},
            {18, 18},
            {19, 26},
            {27, 27},
            {28, 33}
    };

    private JFrame window;
    private JPanel root;
    private StoryPanel activeStory;
    private int checkpoint;

    private final JLabel status = new JLabel(
            "STORY MODE TEST",
            SwingConstants.CENTER);

    private StoryTest() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StoryTest().start();
        });
    }

    private void start() {
        window = new JFrame("Space Invaders - Story Test");
        window.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE);
        window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        window.setLocationRelativeTo(null);

        root = new JPanel(new BorderLayout());
        root.setBackground(new Color(8, 22, 12));

        window.setContentPane(root);

        window.addWindowListener(
                new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(
                            java.awt.event.WindowEvent e) {
                        closeActiveStory();
                    }
                });

        window.setVisible(true);
        showCheckpoint(0);
    }

    private void showCheckpoint(int index) {
        if (index < 0 || index >= RANGES.length) {
            showFinished();
            return;
        }

        closeActiveStory();

        checkpoint = index;
        int first = RANGES[index][0];
        int last = RANGES[index][1];

        root.removeAll();

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(12, 42, 23));

        status.setText(CHECKPOINTS[index]);
        status.setForeground(new Color(160, 255, 170));
        status.setFont(new Font(
                "SansSerif", Font.BOLD, 17));

        top.add(status, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);

        try {
            activeStory = new StoryPanel(
                    first,
                    last,
                    () -> showCheckpointComplete(index));

            root.add(activeStory, BorderLayout.CENTER);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    window,
                    "Could not load panels "
                            + first + "-" + last
                            + "\n" + e.getMessage(),
                    "Story Test Error",
                    JOptionPane.ERROR_MESSAGE);

            showCheckpointComplete(index);
            return;
        }

        root.add(createControls(), BorderLayout.SOUTH);

        root.revalidate();
        root.repaint();

        activeStory.requestFocusInWindow();
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 10));

        controls.setBackground(new Color(12, 42, 23));

        JButton previous = createButton(
                "Previous checkpoint");
        JButton skip = createButton(
                "Skip level / cutscene");
        JButton restart = createButton(
                "Restart checkpoint");

        previous.setEnabled(checkpoint > 0);

        previous.addActionListener(e ->
                showCheckpoint(checkpoint - 1));

        skip.addActionListener(e ->
                showCheckpoint(checkpoint + 1));

        restart.addActionListener(e ->
                showCheckpoint(checkpoint));

        controls.add(previous);
        controls.add(skip);
        controls.add(restart);

        return controls;
    }

    private JButton createButton(String title) {
        JButton button = new JButton(title);
        button.setFocusable(false);
        button.setBackground(new Color(25, 80, 42));
        button.setForeground(new Color(220, 255, 220));
        button.setFont(new Font(
                "SansSerif", Font.BOLD, 12));
        button.setOpaque(true);

        return button;
    }

    private void showCheckpointComplete(int index) {
        closeActiveStory();
        root.removeAll();

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(
                summary, BoxLayout.Y_AXIS));
        summary.setBackground(new Color(8, 22, 12));

        JLabel heading = new JLabel(
                "CHECKPOINT COMPLETED");
        heading.setForeground(new Color(160, 255, 170));
        heading.setFont(new Font(
                "SansSerif", Font.BOLD, 25));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel details = new JLabel(CHECKPOINTS[index]);
        details.setForeground(Color.WHITE);
        details.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel explanation = new JLabel(
                "Gameplay is simulated in this test.");
        explanation.setForeground(Color.LIGHT_GRAY);
        explanation.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton next = createButton(
                index == RANGES.length - 1
                        ? "Finish test"
                        : "Skip gameplay - Next cutscene");

        next.setAlignmentX(Component.CENTER_ALIGNMENT);
        next.addActionListener(e ->
                showCheckpoint(index + 1));

        summary.add(Box.createVerticalGlue());
        summary.add(heading);
        summary.add(Box.createVerticalStrut(18));
        summary.add(details);
        summary.add(Box.createVerticalStrut(10));
        summary.add(explanation);
        summary.add(Box.createVerticalStrut(30));
        summary.add(next);
        summary.add(Box.createVerticalGlue());

        root.add(summary, BorderLayout.CENTER);
        root.add(createControls(), BorderLayout.SOUTH);

        root.revalidate();
        root.repaint();
    }

    private void showFinished() {
        closeActiveStory();
        root.removeAll();

        JPanel end = new JPanel(new GridBagLayout());
        end.setBackground(new Color(8, 22, 12));

        JLabel message = new JLabel(
                "ALL 33 STORY PANELS TESTED");

        message.setFont(new Font(
                "SansSerif", Font.BOLD, 23));
        message.setForeground(new Color(160, 255, 170));

        end.add(message);
        root.add(end, BorderLayout.CENTER);

        JButton restart = createButton("Restart entire test");
        restart.addActionListener(e -> showCheckpoint(0));

        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(12, 42, 23));
        bottom.add(restart);

        root.add(bottom, BorderLayout.SOUTH);
        root.revalidate();
        root.repaint();
    }

    private void closeActiveStory() {
        if (activeStory != null) {
            activeStory.close();
            activeStory = null;
        }
    }
}
