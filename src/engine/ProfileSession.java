package engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Selects the active player before any persisted game managers are used.
 * The current game supports one profile per launch. Quit and restart to switch.
 */
public final class ProfileSession {
    private static StorySaveManager.Slot active;
    private static Path saveDirectory;
    private static String priorSaveDirectory;

    private ProfileSession() { }

    public static synchronized void activate(StorySaveManager.Slot slot) throws IOException {
        if (slot == null) throw new IllegalArgumentException("Missing profile");
        if (active != null) throw new IllegalStateException("A profile is already active");
        if (!slot.id.matches("[a-fA-F0-9-]{36}")) {
            throw new IOException("Invalid profile identifier");
        }
        Path root = Paths.get("saves", "profiles").toAbsolutePath().normalize();
        Path folder = root.resolve(slot.id).normalize();
        if (!folder.startsWith(root)) throw new IOException("Invalid profile path");
        Files.createDirectories(folder);

        // FileManager (coins, diamonds, stamina) and LevelRewards already honor
        // game.saveDirectory. Shop and item equipment require separate edits.
        priorSaveDirectory = System.getProperty("game.saveDirectory");
        System.setProperty("game.saveDirectory", folder.toString());
        saveDirectory = folder;
        active = slot;
    }

    public static synchronized StorySaveManager.Slot getActive() {
        return active;
    }

    public static synchronized Path getSaveDirectory() {
        return saveDirectory;
    }

    public static synchronized void updateCheckpoint(StorySaveManager.Slot updated) {
        if (active == null || updated == null || !active.id.equals(updated.id)) {
            throw new IllegalArgumentException("Checkpoint does not match the active profile");
        }
        active = updated;
    }

    /** Intended for clean shutdown / tests; not for switching profiles in-game. */
    public static synchronized void resetForShutdown() {
        if (priorSaveDirectory == null) System.clearProperty("game.saveDirectory");
        else System.setProperty("game.saveDirectory", priorSaveDirectory);
        active = null;
        saveDirectory = null;
        priorSaveDirectory = null;
    }
}
