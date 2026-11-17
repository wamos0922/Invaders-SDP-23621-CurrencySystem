package screen;

import engine.StorySaveManager;
import story.StoryFont;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/** GOG-37: In-window Story submenu and save-slot picker. */
public final class StoryMenuPanel extends JPanel {
    public static final class Choice {
        public final boolean newGame;
        public final StorySaveManager.Slot slot;
        private Choice(boolean newGame, StorySaveManager.Slot slot) {
            this.newGame = newGame; this.slot = slot;
        }
        public static Choice newGame() { return new Choice(true, null); }
        public static Choice load(StorySaveManager.Slot slot) { return new Choice(false, slot); }
    }

    private static final Color BG = new Color(8,24,13);
    private static final Color GREEN = new Color(125,255,145);
    private static final Color WHITE = new Color(235,255,235);
    private static final Color DARK = new Color(5,38,17);
    private final Consumer<Choice> done;

    public StoryMenuPanel(Consumer<Choice> done) {
        this.done = done;
        setBackground(BG);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(448,520));
        showHome();
    }

    private JButton button(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setFont(StoryFont.bold(13));
        b.setForeground(GREEN);
        b.setBackground(DARK);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(340, 49));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50,120,65)),
                new EmptyBorder(11,12,11,12)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private JPanel base(String title) {
        removeAll();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column,BoxLayout.Y_AXIS));
        column.setBorder(new EmptyBorder(60,38,30,38));
        JLabel heading = new JLabel(title);
        heading.setFont(StoryFont.bold(21));
        heading.setForeground(GREEN);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(heading);
        column.add(Box.createVerticalStrut(52));
        add(column,BorderLayout.CENTER);
        return column;
    }

    private void addButton(JPanel column, String label, Runnable action) {
        column.add(button(label,action));
        column.add(Box.createVerticalStrut(15));
    }

    private void refresh() { revalidate(); repaint(); }

    private void showHome() {
        JPanel column = base("STORY MODE");
        addButton(column,"NEW GAME",() -> done.accept(Choice.newGame()));
        addButton(column,"LOAD FILE",this::showSaves);
        addButton(column,"BACK",() -> done.accept(null));
        refresh();
    }

    private void showSaves() {
        JPanel column = base("LOAD FILE");
        List<StorySaveManager.Slot> saves;
        try { saves = StorySaveManager.list(); }
        catch (IOException ex) {
            JOptionPane.showMessageDialog(this,"Cannot read saves: " + ex.getMessage());
            return;
        }
        if (saves.isEmpty()) {
            JLabel empty = new JLabel("No saved games yet.");
            empty.setFont(StoryFont.regular(11));
            empty.setForeground(WHITE);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            column.add(empty);
            column.add(Box.createVerticalStrut(22));
        }
        JPanel slots = new JPanel();
        slots.setOpaque(false);
        slots.setLayout(new BoxLayout(slots,BoxLayout.Y_AXIS));
        for (StorySaveManager.Slot slot : saves) {
            String label = slot.name + "  -  " + (slot.completed ? "COMPLETE" : "LV " + slot.level);
            addButton(slots,label,() -> done.accept(Choice.load(slot)));
        }
        JScrollPane scroll = new JScrollPane(slots);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setMaximumSize(new Dimension(370,250));
        scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(scroll);
        column.add(Box.createVerticalStrut(12));
        addButton(column,"BACK",this::showHome);
        refresh();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(40,105,55));
        g.drawRoundRect(10,10,getWidth()-21,getHeight()-21,12,12);
    }
}
