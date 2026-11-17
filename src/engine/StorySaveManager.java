package engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** GOG-37: Independent, persistent Story Mode checkpoints. */
public final class StorySaveManager {
    private static final Path DIR = Paths.get("saves", "story");

    public static final class Slot {
        public final String id, name;
        public final int level, score, lives, bullets, kills, diamonds, cleared, coins;
        public final boolean completed;
        public final long updated;

        private Slot(String id, String name, int level, int score, int lives,
                     int bullets, int kills, int diamonds, int cleared, int coins,
                     boolean completed, long updated) {
            this.id = id; this.name = name; this.level = level; this.score = score;
            this.lives = lives; this.bullets = bullets; this.kills = kills;
            this.diamonds = diamonds; this.cleared = cleared; this.coins = coins;
            this.completed = completed; this.updated = updated;
        }
        public GameState toGameState() {
            return new GameState(level, score, Math.max(1, lives), bullets, kills,
                    diamonds, cleared, coins);
        }
    }

    private StorySaveManager() {}

    public static Slot create(String name) throws IOException {
        String clean = name == null || name.trim().isEmpty() ? "Protagonist" : name.trim();
        Slot slot = new Slot(UUID.randomUUID().toString(), clean, 1, 0, 3,
                0, 0, 0, 0, 0, false, System.currentTimeMillis());
        write(slot);
        return slot;
    }

    public static Slot checkpoint(Slot previous, GameState state, boolean completed)
            throws IOException {
        int level = Math.min(StoryMode.TOTAL_LEVELS + 1, Math.max(1, state.getLevel()));
        Slot slot = new Slot(previous.id, previous.name, level, state.getScore(),
                Math.max(1, state.getLivesRemaining()), state.getBulletsShot(),
                state.getShipsDestroyed(), state.getPendingDiamonds(),
                state.getLevelsCompletedRun(), state.getCoinsCollected(),
                completed, System.currentTimeMillis());
        write(slot);
        return slot;
    }

    public static List<Slot> list() throws IOException {
        Files.createDirectories(DIR);
        List<Slot> slots = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(DIR, "*.properties")) {
            for (Path path : files) {
                try { slots.add(read(path)); }
                catch (Exception ignored) { /* Skip malformed save; never crash the menu. */ }
            }
        }
        slots.sort(Comparator.comparingLong((Slot s) -> s.updated).reversed());
        return slots;
    }

    private static Slot read(Path path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        String id = p.getProperty("id", "");
        if (!id.matches("[a-fA-F0-9-]{36}") || !path.getFileName().toString().equals(id + ".properties"))
            throw new IOException("Invalid save identifier");
        int level = number(p, "level", 1);
        if (level < 1 || level > StoryMode.TOTAL_LEVELS + 1)
            throw new IOException("Invalid level");
        return new Slot(id, p.getProperty("name", "Protagonist"), level,
                number(p,"score",0), number(p,"lives",3), number(p,"bullets",0),
                number(p,"kills",0), number(p,"diamonds",0), number(p,"cleared",0),
                number(p,"coins",0), Boolean.parseBoolean(p.getProperty("completed","false")),
                Long.parseLong(p.getProperty("updated","0")));
    }

    private static int number(Properties p, String key, int fallback) {
        return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)));
    }

    private static void write(Slot s) throws IOException {
        Files.createDirectories(DIR);
        Properties p = new Properties();
        p.setProperty("id",s.id); p.setProperty("name",s.name);
        p.setProperty("level",Integer.toString(s.level));
        p.setProperty("score",Integer.toString(s.score));
        p.setProperty("lives",Integer.toString(s.lives));
        p.setProperty("bullets",Integer.toString(s.bullets));
        p.setProperty("kills",Integer.toString(s.kills));
        p.setProperty("diamonds",Integer.toString(s.diamonds));
        p.setProperty("cleared",Integer.toString(s.cleared));
        p.setProperty("coins",Integer.toString(s.coins));
        p.setProperty("completed",Boolean.toString(s.completed));
        p.setProperty("updated",Long.toString(s.updated));
        Path target = DIR.resolve(s.id + ".properties");
        Path tmp = Files.createTempFile(DIR, "checkpoint-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "Space Invaders Story Mode save " + Instant.ofEpochMilli(s.updated));
            }
            try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }
}
