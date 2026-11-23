import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import engine.ItemLoadout;
import engine.ItemLoadout.Mode;

/**
 * Standalone checks for the per-mode item loadout (GOG-50). Uses a
 * temporary file, so the player's real save is never touched.
 */
public final class ItemLoadoutTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("item-loadout-test").toFile();
        File file = new File(dir, "item-loadout");

        // Arrange: a new loadout is empty, 3 slots Endless, 1 slot Story.
        ItemLoadout loadout = ItemLoadout.forFile(file);
        check(Mode.ENDLESS.getSlots() == 3, "Endless should have 3 slots");
        check(Mode.STORY.getSlots() == 1, "Story should have 1 slot");
        check(loadout.getEquipped(Mode.ENDLESS).isEmpty(), "new loadout not empty");

        // Act + assert: equip into slots.
        check(loadout.equip(Mode.ENDLESS, 0, "magnet"), "equip failed");
        check(loadout.equip(Mode.ENDLESS, 2, "long_lasting"), "equip failed");
        check(loadout.getEquipped(Mode.ENDLESS).equals(Arrays.asList("magnet", "long_lasting")),
                "wrong Endless items: " + loadout.getEquipped(Mode.ENDLESS));

        // The same item can't be twice in one mode: it moves.
        check(loadout.equip(Mode.ENDLESS, 1, "magnet"), "move failed");
        check(loadout.get(Mode.ENDLESS, 0) == null, "old slot should be empty");
        check("magnet".equals(loadout.get(Mode.ENDLESS, 1)), "magnet should be in slot 2");

        // ...but it can be in both modes.
        check(loadout.equip(Mode.STORY, 0, "magnet"), "story equip failed");
        check(loadout.contains(Mode.STORY, "magnet") && loadout.contains(Mode.ENDLESS, "magnet"),
                "item should be in both modes");

        // Story has only one slot.
        check(!loadout.equip(Mode.STORY, 1, "max_life"), "story slot 2 should not exist");

        // Clearing a slot.
        check(loadout.clear(Mode.ENDLESS, 2), "clear failed");
        check(!loadout.contains(Mode.ENDLESS, "long_lasting"), "clear did nothing");

        // Saved: a new loadout from the same file sees the same items.
        ItemLoadout reloaded = ItemLoadout.forFile(file);
        check(reloaded.getEquipped(Mode.ENDLESS).equals(Arrays.asList("magnet")),
                "Endless not saved: " + reloaded.getEquipped(Mode.ENDLESS));
        check(reloaded.getEquipped(Mode.STORY).equals(Arrays.asList("magnet")),
                "Story not saved: " + reloaded.getEquipped(Mode.STORY));

        System.out.println("ItemLoadoutTest passed (" + checks + " checks).");
    }
}
