import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.Core;
import engine.DrawManager;
import engine.GameState;
import engine.StoryMode;
import entity.AICommander;
import entity.Bullet;
import entity.EnemyShip;
import entity.Entity;
import entity.RingDreadnought;
import entity.Ship;
import screen.GameScreen;

/**
 * GOG-77: Level 40 boss, AI Commander. Two-line intro warning; glowing weak
 * point (only the core column hurts her, bullets fly through gaps in her
 * art); phase 1 wing turrets, laser and halo shockwave; phase 2 change of
 * form, shown / hidden loop, eye stream, triple quiet laser, halo shards,
 * and the escort wall with its unavoidable missile barrage on the
 * 15 / 10 / 5 / 3 s schedule. Standalone checks like the other tests; run
 * with an isolated build directory and res on the classpath, from a folder
 * that is not your real profile folder.
 */
public final class AICommanderTest {
    private static int checks;
    /** Her clock's real time is frozen here; tests move her clock themselves. */
    private static final long FROZEN_NOW = System.currentTimeMillis();

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Field findField(Class<?> type, String name) throws Exception {
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
        return findField(target.getClass(), name).get(target);
    }

    /** Moves her clock forward without waiting, then runs one update. */
    private static void advance(AICommander boss, long millis) throws Exception {
        Field paused = findField(AICommander.class, "pausedTotal");
        paused.setLong(boss, paused.getLong(boss) - millis);
        findField(AICommander.class, "lastRealUpdate").setLong(boss, FROZEN_NOW);
        boss.update();
    }

    /** Runs update + move a number of frames, ~16 ms of boss time each. */
    private static void frames(AICommander boss, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            advance(boss, 16);
            boss.moveBoss(448, 496, null);
        }
    }

    private static String action(AICommander boss) throws Exception {
        return String.valueOf(get(boss, "action"));
    }

    private static long actionTime(AICommander boss) throws Exception {
        Method m = AICommander.class.getDeclaredMethod("actionTime");
        m.setAccessible(true);
        return (Long) m.invoke(boss);
    }

    private static long clock(AICommander boss) throws Exception {
        Method m = AICommander.class.getDeclaredMethod("now");
        m.setAccessible(true);
        return (Long) m.invoke(boss);
    }

    /** Advances to the given time into the current action. */
    private static void advanceTo(AICommander boss, long t) throws Exception {
        long d = t - actionTime(boss);
        if (d > 0) advance(boss, d);
    }

    private static AICommander started() throws Exception {
        AICommander boss = new AICommander(224 - AICommander.WIDTH / 2, AICommander.SPAWN_Y);
        boss.moveBoss(448, 496, null);
        boss.startFight();
        advance(boss, AICommander.INTRO_MILLIS + 1);
        return boss;
    }

    private static AICommander phase2() throws Exception {
        AICommander boss = started();
        for (int i = 0; i < AICommander.MAX_HP - AICommander.PHASE_2_HP; i++)
            boss.manageCollisionsBoss(null);
        return boss;
    }

    /** Runs frames until she starts the given action (or fails after max ms). */
    private static void runUntil(AICommander boss, String wanted, long max) throws Exception {
        for (long t = 0; t < max; t += 16) {
            if (action(boss).equals(wanted)) return;
            frames(boss, 1);
        }
        throw new AssertionError("never reached " + wanted);
    }

    /** Runs GameScreen.update(); only its draw() may fail headless. */
    private static void runGameLogic(Method update, GameScreen screen) throws Exception {
        try {
            update.invoke(screen);
        } catch (java.lang.reflect.InvocationTargetException e) {
            boolean fromDraw = false;
            for (StackTraceElement frame : e.getCause().getStackTrace())
                fromDraw |= frame.getMethodName().equals("draw")
                        && frame.getClassName().equals("screen.GameScreen");
            if (!fromDraw) throw e;
        }
    }

    /** Draws her body (if visible), attacks and overlay into an image. */
    private static BufferedImage render(AICommander boss) throws Exception {
        BufferedImage img = new BufferedImage(448, 496, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Field bb = findField(DrawManager.class, "backBufferGraphics");
        Object old = bb.get(null);
        bb.set(null, g);
        try {
            if (boss.isVisible()) boss.drawBody(Core.getDrawManager());
            boss.drawWarnings(Core.getDrawManager());
            boss.drawOverlay(Core.getDrawManager());
        } finally {
            bb.set(null, old);
            g.dispose();
        }
        return img;
    }

    private static boolean isRed(int rgb) {
        Color c = new Color(rgb);
        return c.getRed() > 150 && c.getGreen() < 100 && c.getBlue() < 120;
    }

    private static boolean isIon(int rgb) {
        Color c = new Color(rgb);
        return c.getBlue() > 120 && c.getGreen() > 120 && c.getRed() < 120;
    }

    private static int count(BufferedImage img, Rectangle area, boolean ion) {
        int n = 0;
        for (int x = Math.max(0, area.x); x < Math.min(img.getWidth(), area.x + area.width); x++)
            for (int y = Math.max(0, area.y); y < Math.min(img.getHeight(), area.y + area.height); y++)
                if (ion ? isIon(img.getRGB(x, y)) : isRed(img.getRGB(x, y))) n++;
        return n;
    }

    private static int countRed(BufferedImage img, Rectangle area) {
        return count(img, area, false);
    }

    private static Entity shipAt(int centerX, int y) {
        return new Ship(centerX - 13, y);
    }

    /** A player bullet whose centre is at x. */
    private static Bullet playerBullet(int centerX, int y) {
        return new Bullet(centerX - Bullet.DEFAULT_WIDTH / 2, y, -6);
    }

    /** Counts hits over the rest of the current halo, ship standing still. */
    private static int haloHits(AICommander boss, Entity ship) throws Exception {
        int hits = 0;
        while (action(boss).equals("HALO")) {
            advance(boss, 16);
            hits += boss.takePlayerHits(ship);
        }
        return hits;
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        // Freeze her real-time clock: each step moves her clock by exactly the
        // time asked, however slow the machine is.
        findField(AICommander.class, "timeSource")
                .set(null, (java.util.function.LongSupplier) () -> FROZEN_NOW);
        Set<Bullet> bullets = new HashSet<Bullet>();

        // Size: drawn at 2x like every sprite.
        check(AICommander.WIDTH == 144 && AICommander.HEIGHT == 80, "144 x 80 on screen");

        // Hidden and harmless during the countdown and the intro warning.
        AICommander boss = new AICommander(152, AICommander.SPAWN_Y);
        check(!boss.isStarted() && !boss.isVisible() && boss.getWarningText() == null, "Waits for the countdown");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == AICommander.MAX_HP, "Can't be hit before");
        boss.moveBoss(448, 496, null);
        boss.startFight();
        String[] lines = boss.getWarningText().split("\n");
        check(lines.length == 2 && lines[0].equals(AICommander.WARNING_TEXT)
                && lines[1].equals("DON'T DRAG THIS FIGHT"), "Two-line warning");
        check(!boss.isVisible() && !boss.isHitBy(playerBullet(224, 140)), "Bullets fly through before she appears");
        advance(boss, AICommander.INTRO_MILLIS - 100);
        check(boss.getWarningText() != null, "Warning lasts 3 s");
        advance(boss, 101);
        check(boss.isVisible() && boss.getWarningText() == null, "Appears after the warning");

        // Her own art: red lights on a grey hull, inside her box.
        BufferedImage body = render(boss);
        Rectangle box = new Rectangle(boss.getPositionX(), boss.getPositionY(), 144, 80);
        check(countRed(body, box) > 300, "Red-lit hull drawn");
        check(countRed(body, new Rectangle(0, 0, 448, boss.getPositionY() - 4)) == 0, "Nothing drawn above her");

        // Weak point: only the core column hurts her; the hull sparks.
        AICommander weak = started();
        int center = weak.getPositionX() + AICommander.WIDTH / 2;
        Bullet wing = playerBullet(weak.getPositionX() + 30, weak.getPositionY() + 30);
        check(!weak.isOnWeakPoint(wing) && weak.manageCollisionsBoss(wing) == 0
                && weak.getHp() == AICommander.MAX_HP, "Hull hit: no damage");
        check(((List<?>) get(weak, "sparks")).size() == 1, "Hull hit sparks");
        Bullet core = playerBullet(center + AICommander.WEAK_POINT_HALF_WIDTH, weak.getPositionY() + 60);
        check(weak.isOnWeakPoint(core) && weak.manageCollisionsBoss(core) == AICommander.POINTS_PER_HIT
                && weak.getHp() == AICommander.MAX_HP - 1, "Core column hit: 1 damage, 300 points");
        check(!weak.isOnWeakPoint(playerBullet(center + AICommander.WEAK_POINT_HALF_WIDTH + 1, 150)),
                "Just outside the column is hull");
        // Bullets fly through gaps: under the wing tip there is nothing to hit.
        check(!weak.isHitBy(playerBullet(weak.getPositionX() + 4, weak.getPositionY() + 70)), "Gap under the wing");
        check(weak.isHitBy(playerBullet(weak.getPositionX() + 4, weak.getPositionY() + 34)), "Wing tip is solid");
        check(weak.isHitBy(playerBullet(center, weak.getPositionY() + 64)), "Spike is solid (and on the weak point)");

        // Phase 1 turrets: aimed 3-way spreads, wings take turns, red.
        AICommander guns = started();
        guns.setTarget(440, 474);
        check(!guns.shoot(bullets, null) && bullets.isEmpty(), "Turrets wait a moment after she appears");
        advance(guns, AICommander.TURRET_FIRST_MILLIS + 1);
        check(guns.shoot(bullets, null) && bullets.size() == 3, "3-bullet spread");
        List<Integer> speeds = new ArrayList<Integer>();
        int minX = Integer.MAX_VALUE;
        for (Bullet b : bullets) {
            speeds.add(b.getSpeedX());
            minX = Math.min(minX, b.getPositionX());
            check(b.getSpeed() == AICommander.TURRET_SPEED_Y && b.getColor().equals(AICommander.CORE_RED),
                    "Red bullet falling at turret speed");
        }
        Collections.sort(speeds);
        check(speeds.get(1) > 0 && speeds.get(0) == speeds.get(1) - 1 && speeds.get(2) == speeds.get(1) + 1,
                "Aimed at the player (right), spread by 1");
        check(minX < guns.getPositionX() + 40, "First volley from the left wing");
        bullets.clear();
        check(!guns.shoot(bullets, null), "Not again straight away");
        advance(guns, AICommander.TURRET_MILLIS);
        guns.shoot(bullets, null);
        int rightX = Integer.MAX_VALUE;
        for (Bullet b : bullets) rightX = Math.min(rightX, b.getPositionX());
        check(bullets.size() == 3 && rightX > guns.getPositionX() + 100, "Next volley from the right wing");
        bullets.clear();
        advance(guns, AICommander.TURRET_MILLIS);
        findField(AICommander.class, "lastRealUpdate").setLong(guns, FROZEN_NOW - 500);
        check(!guns.shoot(bullets, null) && bullets.isEmpty(), "No shots while frozen by an item");

        // Phase 1 laser: locks the player's x, glides there, one hit.
        boss = started();
        boss.setTarget(100, 474);
        advance(boss, AICommander.IDLE_MILLIS + 1);
        check(action(boss).equals("LASER") && boss.getLockX() == 100, "Laser first, locks the player's x");
        frames(boss, 40);
        check(boss.getPositionX() + 72 == 100, "Glides until her spike is over the column");
        boss.setTarget(300, 474);
        frames(boss, 10);
        check(boss.getLockX() == 100, "Lock doesn't follow during the charge");
        check(boss.takePlayerHits(shipAt(100, 467)) == 0, "Charging doesn't hurt");
        BufferedImage charge = render(boss);
        check(countRed(charge, new Rectangle(44, 218, 360, 76)) > 500, "WARNING banner is red");
        advanceTo(boss, AICommander.LASER_GROW_END + 50);
        Rectangle beam = (Rectangle) invoke(boss, "beamBounds", actionTime(boss));
        check(beam != null && beam.width == AICommander.BEAM_WIDTH && beam.x == 100 - 12, "24 px beam on the column");
        check(beam.y == boss.getPositionY() + 76 && beam.y + beam.height == boss.groundY(), "From the spike to the ground");
        check(((List<?>) invoke(boss, "beamRects", actionTime(boss))).size() == 1, "One beam in phase 1");
        check(boss.takePlayerHits(shipAt(300, 467)) == 0, "Moved away: no hit");
        check(boss.takePlayerHits(shipAt(100, 467)) == 1, "In the beam: hit");
        check(boss.takePlayerHits(shipAt(100, 467)) == 0, "One hit per laser");
        BufferedImage fire = render(boss);
        check(countRed(fire, new Rectangle(88, 200, 24, 280)) > 1000, "Beam is red");
        advanceTo(boss, AICommander.LASER_FIRE_END + 20);
        check(boss.takePlayerHits(shipAt(100, 467)) == 0, "Fading beam doesn't hurt");
        // Edges aren't safe: she leaves the screen partly to line up.
        AICommander edge = started();
        edge.setTarget(10, 474);
        advance(edge, AICommander.IDLE_MILLIS + 1);
        frames(edge, 60);
        check(edge.getLockX() == 12 && edge.getPositionX() == 12 - 72, "Laser reaches the corner");
        advanceTo(edge, AICommander.LASER_CYCLE_END);
        frames(edge, 30);
        check(edge.getPositionX() >= 4, "Glides back on screen afterwards");

        // Phase 1 halo shockwave comes after the laser, with one gap.
        advanceTo(boss, AICommander.LASER_CYCLE_END);
        check(action(boss).equals("IDLE"), "Rest after the laser");
        boss.setTarget(120, 474);
        advance(boss, AICommander.IDLE_MILLIS);
        check(action(boss).equals("HALO"), "Then the halo shockwave");
        int gapX = boss.getHaloGapX();
        check(Math.abs(gapX - 120) >= AICommander.HALO_GAP_MIN_DISTANCE, "Gap opens away from the player");
        check(boss.haloGapWidth() == AICommander.HALO_GAP, "Phase 1 gap width");
        check(boss.takePlayerHits(shipAt(120, 467)) == 0, "Harmless while the halo glows");
        check(haloHits(boss, shipAt(gapX, 467)) == 0, "In the gap: no hit");
        check(action(boss).equals("IDLE"), "Rest after the shockwave");
        advance(boss, AICommander.IDLE_MILLIS);
        check(action(boss).equals("LASER"), "Laser and shockwave take turns");
        advanceTo(boss, AICommander.LASER_CYCLE_END);
        boss.setTarget(300, 474);
        advance(boss, AICommander.IDLE_MILLIS);
        int away = boss.getHaloGapX() < 224 ? 420 : 30;
        check(haloHits(boss, shipAt(away, 467)) == 1, "Outside the gap: exactly one hit");
        // The far edge isn't safe: she is at one side, the player waits at the other.
        for (int side = 0; side < 2; side++) {
            AICommander far = started();
            findField(AICommander.class, "phase1Attacks").setInt(far, 1); // halo next
            far.setPositionX(side == 0 ? 4 : 448 - AICommander.WIDTH - 4);
            int shipX = side == 0 ? 435 : 13;
            far.setTarget(shipX, 474);
            advance(far, AICommander.IDLE_MILLIS + 1);
            check(action(far).equals("HALO"), "Halo for the edge check");
            Entity edgeShip = shipAt(shipX, 467);
            long hitAt = -1;
            int edgeHits = 0;
            while (action(far).equals("HALO")) {
                advance(far, 16);
                int h = far.takePlayerHits(edgeShip);
                if (h > 0 && hitAt < 0) hitAt = actionTime(far);
                edgeHits += h;
            }
            check(edgeHits == 1, "Far edge is hit too (side " + side + ")");
            check(hitAt > 0 && hitAt < AICommander.HALO_TELL + AICommander.HALO_FALL - 100,
                    "Wave reaches the far edge well before it ends, got " + hitAt);
        }

        // Phase 2 at half health: change of form, the barrage at once.
        AICommander p2 = started();
        p2.setTarget(224, 474);
        advance(p2, AICommander.IDLE_MILLIS + 1); // in a laser
        int points = 0;
        for (int i = 0; i < AICommander.MAX_HP - AICommander.PHASE_2_HP - 1; i++)
            points += p2.manageCollisionsBoss(null);
        check(points == AICommander.POINTS_PER_HIT * 14 && !p2.isPhase2(), "Still phase 1 above half");
        p2.manageCollisionsBoss(null);
        check(p2.isPhase2() && p2.getHp() == AICommander.PHASE_2_HP, "Phase 2 at half health");
        check(action(p2).equals("BARRAGE") && p2.takePlayerHits(shipAt(224, 467)) == 0,
                "Barrage right away (cancels the laser)");
        check(p2.getMinions().size() == AICommander.ESCORTS, "Six escorts");
        check(p2.isVisible(), "The change of form is seen");

        // The wall: lined up under her, no gaps, shielded while charging.
        frames(p2, AICommander.BARRAGE_LINED_UP / 16 + 2);
        List<Integer> xs = new ArrayList<Integer>();
        for (EnemyShip e : p2.getMinions()) {
            xs.add(e.getPositionX());
            check(e.getPositionY() == p2.getPositionY() + AICommander.ESCORT_OFFSET_Y, "Escort row under her");
        }
        Collections.sort(xs);
        boolean noGaps = xs.get(0) == p2.getPositionX();
        for (int i = 1; i < xs.size(); i++) noGaps &= xs.get(i) - xs.get(i - 1) == AICommander.ESCORT_WIDTH;
        check(noGaps && xs.get(5) + AICommander.ESCORT_WIDTH == p2.getPositionX() + AICommander.WIDTH,
                "Wall covers her whole width with no gaps");
        check(p2.areEscortsShielded(), "Shielded while charging");
        EnemyShip tough = p2.getMinions().get(2);
        boolean survived = true;
        for (int i = 0; i < 20; i++) survived &= !tough.takeDamage(1);
        check(survived && !tough.isDestroyed(), "Shielded escorts can't be shot down");
        BufferedImage shielded = render(p2);
        check(count(shielded, new Rectangle(p2.getPositionX() - 4, p2.getPositionY() + 80, 152, 26), true) > 100,
                "Shields drawn");
        advanceTo(p2, AICommander.BARRAGE_FIRE + 1);
        check(!p2.areEscortsShielded(), "Shields drop when the missiles launch");
        EnemyShip target = p2.getMinions().get(2);
        check(!target.takeDamage(1) && target.takeDamage(1), "Then 2 hits shoot one down");
        target.destroy();
        int alive = 0;
        for (EnemyShip e : p2.getMinions()) if (!e.isDestroyed()) alive++;
        check(alive == AICommander.ESCORTS - 1, "A gap opens in the wall");

        // Unavoidable barrage: one hit at impact, wherever the player is.
        advanceTo(p2, AICommander.BARRAGE_IMPACT - 30);
        check(p2.takePlayerHits(shipAt(20, 467)) == 0, "No hit before impact");
        check(p2.getShakeOffset() == null, "No shake before impact");
        advanceTo(p2, AICommander.BARRAGE_IMPACT + 10);
        check(p2.takePlayerHits(shipAt(20, 467)) == 1, "Impact: unavoidable hit, even in the corner");
        Point shake = p2.getShakeOffset();
        check(shake != null && shake.x == -9 && shake.y == 7, "Shake at impact");
        check(p2.takePlayerHits(shipAt(20, 467)) == 0, "Only one hit per barrage");
        p2.onPlayerHitBlocked();
        BufferedImage broken = render(p2);
        check(count(broken, new Rectangle(0, 230, 448, 50), true) > 50, "SHIELD BROKEN when the shield takes it");
        advanceTo(p2, AICommander.BARRAGE_END + 1);
        long end = clock(p2);
        check(action(p2).equals("IDLE"), "Barrage over");
        frames(p2, 1);
        for (EnemyShip e : p2.getMinions())
            check(e.isDestroyed(), "Surviving escorts flew off");
        check(Math.abs((Long) get(p2, "nextBarrageAt") - (end + 15000)) <= 20,
                "Next barrage 15 s after this one ends");

        // Schedule, visibility and hidden attacks over a long phase 2.
        AICommander longFight = phase2();
        longFight.setTarget(224, 474);
        List<long[]> barrages = new ArrayList<long[]>();
        long start = clock(longFight);
        String last = "BARRAGE";
        barrages.add(new long[] {0, -1});
        int lasers = 0, halos = 0, visibleFrames = 0, hiddenFrames = 0;
        boolean attackWhileShown = false, overlap = false, tripleSeen = false, quietSeen = true;
        for (long t = 0; t < 80000; t += 16) {
            frames(longFight, 1);
            long now = clock(longFight) - start;
            String a = action(longFight);
            if (!a.equals(last)) {
                if (last.equals("BARRAGE")) barrages.get(barrages.size() - 1)[1] = now;
                if (a.equals("BARRAGE")) {
                    barrages.add(new long[] {now, -1});
                    overlap |= last.equals("LASER") || last.equals("HALO");
                }
                if (a.equals("LASER")) {
                    lasers++;
                    tripleSeen |= ((List<?>) invoke(longFight, "beamXs")).size() == 3;
                    quietSeen &= (Boolean) get(longFight, "quietLaser");
                }
                if (a.equals("HALO")) halos++;
            }
            if ((a.equals("LASER") || a.equals("HALO")) && longFight.isVisible()) attackWhileShown = true;
            if (longFight.isVisible()) visibleFrames++; else hiddenFrames++;
            last = a;
        }
        int[] expected = {15000, 10000, 5000, 3000, 3000, 3000};
        check(barrages.size() >= 7, "Barrages keep coming, got " + barrages.size());
        for (int i = 1; i < expected.length + 1; i++) {
            long gap = barrages.get(i)[0] - barrages.get(i - 1)[1];
            check(Math.abs(gap - expected[i - 1]) <= 32, "Barrage gap " + i + " is " + expected[i - 1] + ", got " + gap);
        }
        check(!overlap, "A laser or shockwave never runs into a barrage");
        check(lasers > 0 && halos > 0, "Hidden lasers and halo shards both happen");
        check(tripleSeen && quietSeen, "Phase 2 lasers are triple and quiet");
        check(!attackWhileShown, "Phase 2 lasers and shockwaves only while hidden");
        double hiddenShare = hiddenFrames / (double) (hiddenFrames + visibleFrames);
        check(hiddenShare > 0.6 && hiddenShare < 0.75, "Hidden more than shown: " + hiddenShare);

        // Phase 2 laser details: three beams, no banner, one hit in total.
        AICommander tri = phase2();
        tri.setTarget(224, 474);
        advanceTo(tri, AICommander.BARRAGE_END + 1);
        tri.takePlayerHits(null); // collect the barrage hit queued by the jump
        runUntil(tri, "LASER", 20000);
        check(!tri.isVisible(), "Fires while hidden");
        List<?> beamXs = (List<?>) invoke(tri, "beamXs");
        check(beamXs.size() == 3 && beamXs.contains(224 - AICommander.SIDE_BEAM_OFFSET)
                && beamXs.contains(224 + AICommander.SIDE_BEAM_OFFSET), "Locked beam plus two side beams");
        advanceTo(tri, 300);
        BufferedImage tell = render(tri);
        check(countRed(tell, new Rectangle(44, 218, 360, 76)) < 50, "No banner in phase 2");
        check(tri.takePlayerHits(shipAt(224 + AICommander.SIDE_BEAM_OFFSET, 467)) == 0, "Tell doesn't hurt");
        advanceTo(tri, AICommander.LASER_TELL_PHASE_2 + 200);
        List<?> rects = (List<?>) invoke(tri, "beamRects", actionTime(tri));
        check(rects.size() == 3, "Three beams fire after the 0.6 s tell");
        check(tri.takePlayerHits(shipAt(224 - AICommander.SIDE_BEAM_OFFSET + 1, 467)) == 1, "Side beam hits");
        check(tri.takePlayerHits(shipAt(224, 467)) == 0, "One hit per laser");
        AICommander gapTest = phase2();
        gapTest.setTarget(224, 474);
        advanceTo(gapTest, AICommander.BARRAGE_END + 1);
        gapTest.takePlayerHits(null); // collect the barrage hit queued by the jump
        runUntil(gapTest, "LASER", 20000);
        advanceTo(gapTest, AICommander.LASER_TELL_PHASE_2 + 200);
        check(gapTest.takePlayerHits(shipAt(224 + AICommander.SIDE_BEAM_OFFSET / 2, 467)) == 0,
                "Safe between the beams");

        // Hidden hit flash.
        int hpBefore = tri.getHp();
        check(tri.manageCollisionsBoss(null) == AICommander.POINTS_PER_HIT && tri.getHp() == hpBefore - 1,
                "Bullets still hit her while hidden");
        check(tri.isVisible(), "Short flash when hit while hidden");
        advance(tri, AICommander.HIT_FLASH_MILLIS + 5);
        check(!tri.isVisible(), "Hidden again after the flash");

        // Phase 2 halo shards: faster, narrower gap.
        AICommander shards = phase2();
        shards.setTarget(224, 474);
        advanceTo(shards, AICommander.BARRAGE_END + 1);
        shards.takePlayerHits(null); // collect the barrage hit queued by the jump
        runUntil(shards, "HALO", 30000);
        check(shards.haloGapWidth() == AICommander.HALO_GAP_PHASE_2, "Narrower gap in phase 2");
        check(haloHits(shards, shipAt(shards.getHaloGapX(), 467)) == 0, "Shards: safe in the gap");

        // Phase 2 eye stream: only while shown, aimed, red.
        AICommander eye = phase2();
        eye.setTarget(60, 474);
        bullets.clear();
        int shots = 0;
        boolean shotWhileHidden = false;
        for (long t = 0; t < 12000; t += 16) {
            frames(eye, 1);
            int before = bullets.size();
            eye.shoot(bullets, null);
            if (bullets.size() > before && !eye.isVisible()) shotWhileHidden = true;
            shots += bullets.size() - before;
        }
        check(shots >= AICommander.STREAM_BULLETS * 2, "Streams in shown stretches, got " + shots);
        check(!shotWhileHidden, "No stream while hidden");
        for (Bullet b : bullets)
            check(b.getSpeed() == AICommander.STREAM_SPEED_Y && b.getSpeedX() < 0
                    && b.getColor().equals(AICommander.CORE_RED), "Stream aimed at the player (left)");

        // Dev switch: start straight in phase 2.
        System.setProperty(AICommander.PHASE_2_SWITCH, "true");
        try {
            AICommander dev = started();
            frames(dev, 1);
            check(dev.isPhase2() && dev.getHp() == AICommander.PHASE_2_HP && action(dev).equals("BARRAGE"),
                    "-Dcommander.phase2=true starts in phase 2");
        } finally {
            System.clearProperty(AICommander.PHASE_2_SWITCH);
        }
        check(!started().isPhase2(), "Normal start without the switch");

        // Pause doesn't eat attack time.
        AICommander paused = started();
        advance(paused, AICommander.IDLE_MILLIS + 1);
        long tBefore = actionTime(paused);
        paused.resumeAfterPause(5000);
        check(Math.abs(tBefore - actionTime(paused) - 5000) < 50, "Pause stops her clock");

        // Drawing through every state doesn't throw.
        AICommander drawn = started();
        drawn.setTarget(224, 474);
        for (long t = 0; t < 12000; t += 97) {
            frames(drawn, 6);
            render(drawn);
        }
        for (int i = 0; i < AICommander.PHASE_2_HP; i++) drawn.manageCollisionsBoss(null);
        for (long t = 0; t < 30000; t += 97) {
            frames(drawn, 6);
            render(drawn);
        }

        // Destroy: kill bonus, escorts die with her, explosion, inactive after.
        AICommander kill = phase2();
        int lastPoints = 0;
        for (int i = 0; i < AICommander.PHASE_2_HP; i++) lastPoints = kill.manageCollisionsBoss(null);
        check(kill.isDestroyed() && lastPoints == AICommander.POINTS_PER_HIT + AICommander.POINTS_DESTROYED,
                "Kill bonus");
        boolean allDead = true;
        for (EnemyShip e : kill.getMinions()) allDead &= e.isDestroyed();
        check(allDead && kill.isVisible(), "Escorts die with her; she explodes");
        check(countRed(render(kill), new Rectangle(0, 60, 448, 160)) > 0, "Explosion draws");
        check(kill.manageCollisionsBoss(null) == 0 && kill.takePlayerHits(shipAt(224, 467)) == 0
                && !kill.isHitBy(playerBullet(224, 150)), "Inactive once destroyed");

        // In game: level 40 spawns her centred; level 30 is the Ring Dreadnought.
        GameScreen level40 = new GameScreen(new GameState(40, 0, 3, 0, 0, 0),
                StoryMode.getSettings(40), false, 448, 520, 60);
        level40.initialize();
        check(get(level40, "midBoss") instanceof AICommander, "Level 40 is the AI Commander");
        AICommander inGame = (AICommander) get(level40, "midBoss");
        check(inGame.getPositionX() == 224 - 72 && inGame.getPositionY() == AICommander.SPAWN_Y, "Spawns centred");
        GameScreen level30 = new GameScreen(new GameState(30, 0, 3, 0, 0, 0),
                StoryMode.getSettings(30), false, 448, 520, 60);
        level30.initialize();
        check(get(level30, "midBoss") instanceof RingDreadnought, "Level 30 is the Ring Dreadnought");

        Field inputDelay = findField(GameScreen.class, "inputDelay");
        engine.Cooldown countdown = (engine.Cooldown) inputDelay.get(level40);
        findField(countdown.getClass(), "time").setLong(countdown, System.currentTimeMillis() - 7000);
        Method update = GameScreen.class.getDeclaredMethod("update");
        update.setAccessible(true);
        runGameLogic(update, level40);
        check(inGame.isStarted() && inGame.getWarningText().contains("\n"), "Warning shows after the countdown");
        advance(inGame, AICommander.INTRO_MILLIS + 1);

        // Bullets in game: through a gap untouched; on the core column, a hit.
        @SuppressWarnings("unchecked")
        Set<Bullet> live = (Set<Bullet>) get(level40, "bullets");
        Method collisions = GameScreen.class.getDeclaredMethod("manageCollisions");
        collisions.setAccessible(true);
        live.clear();
        Bullet throughGap = playerBullet(inGame.getPositionX() + 4, inGame.getPositionY() + 66);
        live.add(throughGap);
        collisions.invoke(level40);
        check(live.contains(throughGap) && inGame.getHp() == AICommander.MAX_HP, "Bullet flies through a gap");
        live.clear();
        Bullet onCore = playerBullet(inGame.getPositionX() + 72, inGame.getPositionY() + 64);
        live.add(onCore);
        collisions.invoke(level40);
        check(!live.contains(onCore) && inGame.getHp() == AICommander.MAX_HP - 1, "Core hit in game");
        live.clear();
        Bullet onHull = playerBullet(inGame.getPositionX() + 30, inGame.getPositionY() + 30);
        live.add(onHull);
        collisions.invoke(level40);
        check(!live.contains(onHull) && inGame.getHp() == AICommander.MAX_HP - 1, "Hull stops the bullet, no damage");

        // The barrage costs a life in game.
        for (int i = 0; i < AICommander.MAX_HP - 1 - AICommander.PHASE_2_HP; i++)
            inGame.manageCollisionsBoss(null);
        check(action(inGame).equals("BARRAGE"), "Barrage in game");
        Field paused2 = findField(AICommander.class, "pausedTotal");
        paused2.setLong(inGame, paused2.getLong(inGame) - (AICommander.BARRAGE_IMPACT + 20));
        findField(AICommander.class, "lastRealUpdate").setLong(inGame, FROZEN_NOW);
        int livesBefore = (Integer) get(level40, "lives");
        runGameLogic(update, level40);
        check((Integer) get(level40, "lives") == livesBefore - 1, "Barrage costs a life in game");
        check(((Ship) get(level40, "ship")).isDestroyed(), "Ship hit");

        System.out.println("AICommanderTest: " + checks + " checks passed");
    }

    private static Object invoke(Object target, String name, long arg) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, long.class);
        m.setAccessible(true);
        return m.invoke(target, arg);
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
