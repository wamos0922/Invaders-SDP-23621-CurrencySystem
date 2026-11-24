import java.awt.Color;
import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import engine.Core;
import engine.DrawManager.SpriteType;
import engine.GameSettings;
import engine.GameState;
import engine.StoryMode;
import entity.Bullet;
import entity.BulletPool;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.EnemyType;
import entity.Entity;
import entity.Ship;
import screen.GameScreen;

/**
 * GOG-86: Act 4 (levels 31-39) enemy gameplay - Comet dives when shot and
 * explodes where the player was, Mite boom (shrapnel + neighbours, Mites
 * never next to each other), Stinger
 * vulnerable bullet (1 life for 5 s, can be shot down). Standalone checks
 * like the other tests; run with an isolated build directory and res on the
 * classpath, from a folder that is not your real profile folder.
 */
public final class Act4EnemyTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object get(Object target, String name) throws Exception {
        return field(target.getClass(), name).get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    private static void call(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static int count(EnemyShipFormation formation, boolean alive) {
        int n = 0;
        for (EnemyShip e : formation)
            if (e.isDestroyed() != alive) n++;
        return n;
    }

    private static GameScreen screen(int level, GameSettings settings) throws Exception {
        GameScreen s = new GameScreen(new GameState(level, 0, 3, 0, 0, 0),
                settings, false, 448, 520, 60);
        s.initialize();
        return s;
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        EnemyType K = EnemyType.STAR_EATER, W = EnemyType.NEBULA_WRAITH,
                C = EnemyType.COMET, M = EnemyType.MITE, S = EnemyType.STINGER;

        // Sprites load from res/graphics.
        @SuppressWarnings("unchecked")
        Map<SpriteType, boolean[][]> sprites =
                (Map<SpriteType, boolean[][]>) get(Core.getDrawManager(), "spriteMap");
        String[] cometA = {".#.#.##.#.#.", "..#.#..#.#..", ".#.#.##.#.#.", "..########..",
                ".###.##.###.", ".##########.", "..########..", "....####...."};
        String[] miteB = {"............", "..#.#..#.#..", "..########..", ".##.####.##.",
                ".##########.", "..##....##..", "..#.#..#.#..", ".#........#."};
        for (Object[] c : new Object[][] {{SpriteType.Comet1, cometA}, {SpriteType.Mite2, miteB}}) {
            boolean[][] image = sprites.get(c[0]);
            String[] grid = (String[]) c[1];
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 12; x++)
                    check(image[x][y] == (grid[y].charAt(x) == '#'), c[0] + " pixel " + x + "," + y);
        }

        // Levels 31-39: draft row counts, 7 per row, the planned row types.
        EnemyType[][] plan = {
                {K, C}, {K, M}, {S, K, W}, {S, W, C}, {K, M, W, C},
                {S, K, M, C}, {S, M, M, C}, {S, K, M, M, C}, {S, M, M, C, C}};
        int[] draftRows = {2, 2, 3, 3, 4, 4, 4, 5, 5};
        int previous = 0;
        for (int level = 31; level <= 39; level++) {
            GameSettings settings = StoryMode.getSettings(level);
            EnemyType[] rows = plan[level - 31];
            check(settings.getFormationHeight() == draftRows[level - 31], "Level " + level + " rows");
            check(settings.getFormationWidth() == 7, "Level " + level + " has 7 per row");
            int special = 0;
            for (int r = 0; r < rows.length; r++) {
                check(settings.getRowType(r) == rows[r], "Level " + level + " row " + r);
                if (rows[r].hasAbility()) special++;
            }
            check(level > 33 || special == 1, "31-33: one special row");
            check(special >= previous, "Special rows never decrease");
            previous = special;
            int built = 0;
            java.util.TreeMap<Integer, java.util.List<EnemyShip>> byRow = new java.util.TreeMap<>();
            List<EnemyShip> mites = new java.util.ArrayList<EnemyShip>();
            for (EnemyShip e : new EnemyShipFormation(settings)) {
                byRow.computeIfAbsent(e.getPositionY(), y -> new java.util.ArrayList<>()).add(e);
                if (e.getType() == M) mites.add(e);
                built++;
            }
            int r = 0;
            for (List<EnemyShip> row : byRow.values()) {
                EnemyType planned = rows[r++];
                for (EnemyShip e : row)
                    check(e.getType() == planned
                            || (planned == M && e.getType() == EnemyType.MITE_ROW_FILLER),
                            "Level " + level + " row " + (r - 1) + " type");
                if (planned == M) {
                    int mitesInRow = 0;
                    for (EnemyShip e : row) if (e.getType() == M) mitesInRow++;
                    check(mitesInRow >= 3, "Mite row still has Mites");
                }
            }
            for (EnemyShip a : mites)
                for (EnemyShip b : mites)
                    if (a != b)
                        check(Math.hypot(a.getPositionX() - b.getPositionX(),
                                a.getPositionY() - b.getPositionY()) > EnemyType.MITE_BLAST_RADIUS,
                                "Level " + level + ": no Mite within blast reach of another");
            check(built == 7 * rows.length, "Level " + level + " enemy count");
        }
        check(previous == 5, "Level 39 is all special");
        for (EnemyShip e : new EnemyShipFormation(StoryMode.getSettings(21)))
            check(e.getType() == EnemyType.NORMAL, "Act 3 untouched");
        check(C.hasAbility() && M.hasAbility() && S.hasAbility() && !K.hasAbility()
                && !W.hasAbility(), "Abilities");

        // Stinger shot: violet vulnerable bullet; recycled bullets are normal again.
        EnemyShip stinger = new EnemyShip(100, 100, S, 1);
        Set<Bullet> shots = new HashSet<Bullet>();
        EnemyShipFormation.fireShot(stinger, shots);
        Bullet vb = shots.iterator().next();
        check(shots.size() == 1 && vb.isVulnerable() && !vb.isFreezing(), "Stinger fires a vulnerable bullet");
        check(EnemyType.VULNERABLE_BULLET_COLOR.equals(vb.getColor()), "Violet bullet");
        BulletPool.recycle(shots);
        Bullet reused = BulletPool.getBullet(0, 0, 4);
        check(!reused.isVulnerable() && Color.WHITE.equals(reused.getColor()), "Pool resets it");
        Set<Bullet> mshots = new HashSet<Bullet>();
        EnemyShipFormation.fireShot(new EnemyShip(0, 0, M, 1), mshots);
        EnemyShipFormation.fireShot(new EnemyShip(0, 0, C, 1), mshots);
        for (Bullet b : mshots) check(!b.isVulnerable() && !b.isFreezing(), "Mite/Comet shoot normal bullets");

        // Comet: a normal enemy until shot; the first hit makes it dive.
        GameScreen diveScreen = screen(31, new GameSettings(3, 2, 30, 1600).withRowTypes(K, C));
        EnemyShipFormation formation = (EnemyShipFormation) get(diveScreen, "enemyShipFormation");
        Entity target = new Ship(200, 450);
        int targetX = target.getPositionX() + target.getWidth() / 2;
        int targetY = target.getPositionY() + target.getHeight() / 2;
        for (int i = 0; i < 300; i++) formation.updateDives();
        check(!formation.isCometDiving(), "No Comet dives on its own");
        @SuppressWarnings("unchecked")
        List<List<EnemyShip>> columns = (List<List<EnemyShip>>) get(formation, "enemyShips");
        EnemyShip diver = null, starEater = null;
        for (EnemyShip e : formation) {
            if (e.getType() == C && diver == null) diver = e;
            if (e.getType() == K && starEater == null) starEater = e;
        }
        check(!formation.startDive(starEater, target), "Only Comets dive");
        check(formation.startDive(diver, target), "Hit Comet dives");
        check(!diver.isDestroyed() && formation.isDiving(diver), "Diving, not dead");
        check(!formation.startDive(diver, target), "A diving Comet doesn't restart its dive");
        check(count(formation, true) == 6, "Diving Comet still counts as an enemy");
        for (List<EnemyShip> col : columns) check(!col.contains(diver), "Diver left its column");
        @SuppressWarnings("unchecked")
        List<EnemyShip> shooters = (List<EnemyShip>) get(formation, "shooters");
        check(!shooters.contains(diver), "Diver no longer a formation shooter");
        int startY = diver.getPositionY();
        double before = Math.hypot(diver.getPositionX() + 12 - targetX, diver.getPositionY() + 8 - targetY);
        formation.updateDives();
        double after = Math.hypot(diver.getPositionX() + 12 - targetX, diver.getPositionY() + 8 - targetY);
        check(before - after > EnemyType.COMET_DIVE_SPEED - 1.5 && before - after < EnemyType.COMET_DIVE_SPEED + 1.5,
                "Moves 4 px per frame toward the locked spot");
        check(diver.getPositionY() > startY, "Dives downward");
        target.setPositionX(20); // player moves away: Comet keeps its locked spot
        int frames = 0;
        while (!diver.isDestroyed() && frames++ < 500) formation.updateDives();
        check(diver.isDestroyed(), "Comet explodes when it arrives");
        check(diver.getPositionX() + 12 == targetX && diver.getPositionY() + 8 == targetY,
                "It explodes at the spot it locked onto, not the moved player");
        List<Point> blasts = formation.takeDetonations();
        check(blasts.size() == 1 && blasts.get(0).x == targetX && blasts.get(0).y == targetY, "Detonation point");
        check(formation.takeDetonations().isEmpty(), "Detonations are taken once");
        check(count(formation, true) == 5, "Exploded Comet no longer counts");

        // Several hit Comets can dive at once.
        EnemyShip c2 = null, c3 = null;
        for (EnemyShip e : formation)
            if (e.getType() == C && !e.isDestroyed() && !formation.isDiving(e)) {
                if (c2 == null) c2 = e; else c3 = e;
            }
        check(formation.startDive(c2, target) && formation.startDive(c3, target), "Two Comets dive together");

        // Shooting a diving Comet: destroyed, counted once, no blast.
        formation.destroy(c2);
        formation.destroy(c2);
        check(c2.isDestroyed() && count(formation, true) == 4, "Shot down once");
        for (int i = 0; i < 400; i++) formation.updateDives();
        check(formation.takeDetonations().size() == 1, "Only the Comet that wasn't shot explodes");

        // Through GameScreen: 1st bullet = dive (no points), 2nd bullet = kill (points).
        GameScreen hitScreen = screen(31, new GameSettings(3, 2, 30, 1600).withRowTypes(K, C));
        EnemyShipFormation hitFormation = (EnemyShipFormation) get(hitScreen, "enemyShipFormation");
        @SuppressWarnings("unchecked")
        Set<Bullet> hitBullets = (Set<Bullet>) get(hitScreen, "bullets");
        EnemyShip comet = null;
        for (EnemyShip e : hitFormation) if (e.getType() == C && comet == null) comet = e;
        int score0 = (Integer) get(hitScreen, "score");
        hitBullets.add(new Bullet(comet.getPositionX() + 8, comet.getPositionY() + 4, -6));
        call(hitScreen, "manageCollisions");
        check(hitFormation.isDiving(comet) && !comet.isDestroyed(), "First hit: Comet dives");
        check((Integer) get(hitScreen, "score") == score0, "No points for the first hit");
        hitBullets.add(new Bullet(comet.getPositionX() + 8, comet.getPositionY() + 4, -6));
        call(hitScreen, "manageCollisions");
        check(comet.isDestroyed(), "Second hit kills the diving Comet");
        check((Integer) get(hitScreen, "score") > score0, "Kill gives points");

        // Comet blast vs player: inside the radius = lose a life, outside = safe.
        Ship player = (Ship) get(diveScreen, "ship");
        @SuppressWarnings("unchecked")
        List<Point> pending = (List<Point>) get(formation, "detonations");
        int px = player.getPositionX() + player.getWidth() / 2, py = player.getPositionY() + player.getHeight() / 2;
        pending.add(new Point(px + EnemyType.COMET_BLAST_RADIUS + 5, py));
        call(diveScreen, "handleCometBlasts");
        check((Integer) get(diveScreen, "lives") == 3, "Outside the blast: safe");
        pending.add(new Point(px + EnemyType.COMET_BLAST_RADIUS - 5, py));
        call(diveScreen, "handleCometBlasts");
        check((Integer) get(diveScreen, "lives") == 2 && player.isDestroyed(), "Inside the blast: lose a life");

        // Mite boom: shrapnel + neighbours, chain through other Mites, with rewards.
        check(EnemyType.MITE_BLAST_RADIUS > 40 && EnemyType.MITE_BLAST_RADIUS < Math.hypot(40, 40),
                "Blast reaches left/right/up/down neighbours, not diagonals");
        GameScreen boomScreen = screen(32, new GameSettings(3, 3, 30, 1600).withRowTypes(K, M, W));
        EnemyShipFormation boomFormation = (EnemyShipFormation) get(boomScreen, "enemyShipFormation");
        @SuppressWarnings("unchecked")
        Set<Bullet> screenBullets = (Set<Bullet>) get(boomScreen, "bullets");
        int mitesBuilt = 0;
        EnemyShip centerMite = null;
        for (EnemyShip e : boomFormation)
            if (e.getType() == M) { mitesBuilt++; centerMite = e; }
        check(mitesBuilt == 1, "3-wide Mite row is Star Eater, Mite, Star Eater");
        int scoreBefore = (Integer) get(boomScreen, "score");
        boomFormation.destroy(centerMite);
        call(boomScreen, "handleMiteBooms");
        check(count(boomFormation, true) == 4, "Boom kills the 4 neighbours, corners survive");
        int shrapnel = 0, left = 0, right = 0;
        for (Bullet b : screenBullets) {
            if (b.getSpeed() > 0) shrapnel++;
            if (b.getSpeedX() < 0) left++;
            if (b.getSpeedX() > 0) right++;
        }
        check(shrapnel == 3 && left == 1 && right == 1, "3 shrapnel bullets spreading down");
        check((Integer) get(boomScreen, "score") > scoreBefore, "Blast kills give points");
        check(boomFormation.takeMiteBooms().isEmpty(), "No chain: only one boom");

        // Level 37 (two Mite rows): killing any Mite makes exactly one boom.
        GameScreen l37 = screen(37, StoryMode.getSettings(37));
        EnemyShipFormation f37 = (EnemyShipFormation) get(l37, "enemyShipFormation");
        for (EnemyShip e : f37)
            if (e.getType() == M) { f37.destroy(e); break; }
        call(l37, "handleMiteBooms");
        @SuppressWarnings("unchecked")
        Set<Bullet> b37 = (Set<Bullet>) get(l37, "bullets");
        int s37 = 0;
        for (Bullet b : b37) if (b.getSpeed() > 0) s37++;
        check(s37 == 3, "Level 37: one boom, no chain");

        // Non-Mites don't boom.
        GameScreen quiet = screen(33, new GameSettings(3, 1, 30, 1600).withRowTypes(K));
        EnemyShipFormation quietFormation = (EnemyShipFormation) get(quiet, "enemyShipFormation");
        quietFormation.destroy(quietFormation.iterator().next());
        check(quietFormation.takeMiteBooms().isEmpty(), "Only Mites boom");

        // Stinger hit: 1 life for 5 s, second hit ignored, then lives come back.
        GameScreen vScreen = screen(33, new GameSettings(3, 1, 30, 1600).withRowTypes(S));
        Ship vShip = (Ship) get(vScreen, "ship");
        @SuppressWarnings("unchecked")
        Set<Bullet> vBullets = (Set<Bullet>) get(vScreen, "bullets");
        Bullet sting = new Bullet(vShip.getPositionX() + 4, vShip.getPositionY() + 2, 4);
        sting.setVulnerable(true);
        vBullets.add(sting);
        call(vScreen, "manageCollisions");
        check((Integer) get(vScreen, "lives") == 1 && (Boolean) get(vScreen, "vulnerableActive"),
                "Stinger hit: 1 life left");
        check(!vShip.isDestroyed() && vBullets.isEmpty(), "Not destroyed, bullet consumed");
        long until = (Long) get(vScreen, "vulnerableUntil");
        check(Math.abs(until - System.currentTimeMillis() - EnemyType.VULNERABLE_MILLIS) < 100, "Lasts 5 s");
        Bullet sting2 = new Bullet(vShip.getPositionX() + 4, vShip.getPositionY() + 2, 4);
        sting2.setVulnerable(true);
        vBullets.add(sting2);
        call(vScreen, "manageCollisions");
        check((Long) get(vScreen, "vulnerableUntil") == until && (Integer) get(vScreen, "lives") == 1,
                "Second Stinger hit ignored");
        check(((GameState) vScreen.getGameState()).getLivesRemaining() == 3,
                "Lives held back still count if the level ends now");
        set(vScreen, "vulnerableUntil", System.currentTimeMillis() - 1);
        call(vScreen, "updateVulnerable");
        check((Integer) get(vScreen, "lives") == 3 && !(Boolean) get(vScreen, "vulnerableActive"),
                "After 5 s the lives come back");

        // A life gained during the 5 s is kept.
        Bullet sting3 = new Bullet(vShip.getPositionX() + 4, vShip.getPositionY() + 2, 4);
        sting3.setVulnerable(true);
        vBullets.add(sting3);
        call(vScreen, "manageCollisions");
        set(vScreen, "lives", 2); // picked up a life
        set(vScreen, "vulnerableUntil", System.currentTimeMillis() - 1);
        call(vScreen, "updateVulnerable");
        check((Integer) get(vScreen, "lives") == 4, "Gained life kept on top");

        // Pause doesn't use up the 5 s.
        Bullet sting4 = new Bullet(vShip.getPositionX() + 4, vShip.getPositionY() + 2, 4);
        sting4.setVulnerable(true);
        vBullets.add(sting4);
        call(vScreen, "manageCollisions");
        long u = (Long) get(vScreen, "vulnerableUntil");
        set(vScreen, "paused", true);
        set(vScreen, "pauseStartedAt", System.currentTimeMillis() - 1000);
        call(vScreen, "resumeGameplay");
        check((Long) get(vScreen, "vulnerableUntil") - u >= 990, "Pause shifts the 5 s");

        // Hit by a normal bullet while at 1 life: game over (0 lives).
        Bullet normal = new Bullet(vShip.getPositionX() + 4, vShip.getPositionY() + 2, 4);
        vBullets.add(normal);
        call(vScreen, "manageCollisions");
        check((Integer) get(vScreen, "lives") == 0, "Normal hit at 1 life = game over");
        call(vScreen, "updateVulnerable");
        set(vScreen, "vulnerableUntil", System.currentTimeMillis() - 1);
        call(vScreen, "updateVulnerable");
        check((Integer) get(vScreen, "lives") == 0, "No lives come back after a game over");

        // Shooting a Stinger bullet down: both bullets vanish; normal bullets can't be.
        GameScreen sdScreen = screen(33, new GameSettings(3, 1, 30, 1600).withRowTypes(S));
        @SuppressWarnings("unchecked")
        Set<Bullet> sdBullets = (Set<Bullet>) get(sdScreen, "bullets");
        Bullet enemyV = new Bullet(200, 300, 4);
        enemyV.setVulnerable(true);
        Bullet mine = new Bullet(200, 302, -6);
        sdBullets.add(enemyV);
        sdBullets.add(mine);
        call(sdScreen, "manageCollisions");
        check(sdBullets.isEmpty(), "Stinger bullet shot down");
        check((Integer) get(sdScreen, "lives") == 3, "No effect on the player");
        Bullet enemyN = new Bullet(200, 300, 4);
        Bullet mine2 = new Bullet(200, 302, -6);
        sdBullets.add(enemyN);
        sdBullets.add(mine2);
        call(sdScreen, "manageCollisions");
        check(sdBullets.contains(enemyN), "Normal enemy bullets can't be shot down");

        System.out.println("PASS: " + checks + " checks");
    }
}
