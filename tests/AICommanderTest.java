import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameState;
import engine.StoryMode;
import entity.AICommander;
import entity.Bullet;
import entity.Entity;
import entity.FinalBoss;
import entity.Ship;
import screen.GameScreen;

/**
 * GOG-77: Level 40 boss, AI Commander - laser that locks onto the player
 * (phase 1 and 2), Claw Slam at phase 2 start then every 30 s, invisible
 * after each phase 2 laser (no shooting, still hittable). Standalone checks
 * like the other tests; run with an isolated build directory and res on the
 * classpath, from a folder that is not your real profile folder.
 */
public final class AICommanderTest {
    private static int checks;

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
        findField(AICommander.class, "lastRealUpdate").setLong(boss, System.currentTimeMillis());
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

    /** Advances to the given time into the current action. */
    private static void advanceTo(AICommander boss, long t) throws Exception {
        long d = t - actionTime(boss);
        if (d > 0) advance(boss, d);
    }

    private static AICommander started() throws Exception {
        AICommander boss = new AICommander(200, 96);
        boss.moveBoss(448, 496, null);
        boss.startFight();
        advance(boss, AICommander.INTRO_MILLIS + 1);
        return boss;
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

    /** Draws both her hooks into an image; returns the image. */
    private static BufferedImage render(AICommander boss) throws Exception {
        BufferedImage img = new BufferedImage(448, 496, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Field bb = findField(DrawManager.class, "backBufferGraphics");
        Object old = bb.get(null);
        bb.set(null, g);
        try {
            boss.drawWarnings(Core.getDrawManager());
            boss.drawOverlay(Core.getDrawManager());
        } finally {
            bb.set(null, old);
            g.dispose();
        }
        return img;
    }

    private static boolean isViolet(int rgb) {
        Color c = new Color(rgb);
        return c.getBlue() > 150 && c.getRed() > 90 && c.getGreen() < c.getBlue() - 40;
    }

    private static int countViolet(BufferedImage img, Rectangle area) {
        int n = 0;
        for (int x = Math.max(0, area.x); x < Math.min(img.getWidth(), area.x + area.width); x++)
            for (int y = Math.max(0, area.y); y < Math.min(img.getHeight(), area.y + area.height); y++)
                if (isViolet(img.getRGB(x, y))) n++;
        return n;
    }

    private static Entity shipAt(int centerX, int y) {
        return new Ship(centerX - 13, y);
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        Set<Bullet> bullets = new HashSet<Bullet>();

        // Sprites load at the right size and match the art pack grid.
        @SuppressWarnings("unchecked")
        Map<SpriteType, boolean[][]> sprites =
                (Map<SpriteType, boolean[][]>) get(Core.getDrawManager(), "spriteMap");
        boolean[][] a = sprites.get(SpriteType.Commander1);
        boolean[][] b = sprites.get(SpriteType.Commander2);
        check(a.length == 24 && a[0].length == 16 && b.length == 24 && b[0].length == 16,
                "Commander sprites are 24 x 16");
        check(a[0][0] && a[23][0] && !a[1][0] && a[10][14] && !a[0][15], "Commander1 matches the grid");
        check(!b[0][0] && b[0][1] && b[11][15] && b[12][15] && !b[10][15], "Commander2 matches the grid");

        // Hidden and harmless during the countdown and the intro warning.
        AICommander boss = new AICommander(200, 96);
        check(!boss.isStarted() && !boss.isVisible() && boss.getWarningText() == null, "Waits for the countdown");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == AICommander.MAX_HP, "Can't be hit before");
        boss.startFight();
        check(AICommander.WARNING_TEXT.equals(boss.getWarningText()) && !boss.isVisible(), "Warning first");
        advance(boss, AICommander.INTRO_MILLIS + 1);
        check(boss.isVisible() && boss.getWarningText() == null, "Appears after the warning");
        check(boss.getColor().equals(AICommander.GALAXY), "Phase 1 is galaxy violet");
        check(!boss.shoot(bullets, null) && bullets.isEmpty(), "No normal bullets, ever");

        // Phase 1: rest, then laser. Locks the player's x and glides there.
        boss = started();
        boss.setTarget(100, 474);
        advance(boss, AICommander.IDLE_MILLIS + 1);
        check(action(boss).equals("LASER"), "Laser after the rest");
        check(boss.getLockX() == 100, "Locks the player's x");
        frames(boss, 40);
        check(boss.getPositionX() + 24 == 100, "Glides until her jaw is over the locked column");
        boss.setTarget(300, 474); // player moves away during the charge
        frames(boss, 10);
        check(boss.getLockX() == 100 && boss.getPositionX() + 24 == 100, "Lock doesn't follow during the charge");

        // Can't hurt while charging, even standing right under her.
        check(boss.takePlayerHits(shipAt(100, 467)) == 0, "Charging doesn't hurt");
        // Fires at 2.1 s: standing in the column costs one hit, once.
        advanceTo(boss, AICommander.LASER_GROW_END + 50);
        Rectangle beam = (Rectangle) invoke(boss, "beamBounds", actionTime(boss));
        check(beam != null && beam.width == AICommander.BEAM_WIDTH && beam.x == 100 - 12, "24 px beam on the column");
        check(beam.y + beam.height == boss.groundY(), "Beam reaches the ground");
        check(boss.takePlayerHits(shipAt(300, 467)) == 0, "Moved away: no hit");
        check(boss.takePlayerHits(shipAt(100, 467)) == 1, "In the beam: hit");
        check(boss.takePlayerHits(shipAt(100, 467)) == 0, "One hit per laser");
        // Fading beam can't hurt.
        AICommander fade = started();
        fade.setTarget(224, 474);
        advance(fade, AICommander.IDLE_MILLIS + 1);
        advanceTo(fade, AICommander.LASER_FIRE_END + 20);
        check(fade.takePlayerHits(shipAt(224, 467)) == 0, "Fading beam doesn't hurt");
        // Edge of the screen: lock is clamped so she stays on screen.
        AICommander edge = started();
        edge.setTarget(2, 474);
        advance(edge, AICommander.IDLE_MILLIS + 1);
        frames(edge, 60);
        check(edge.getPositionX() >= 0 && edge.getLockX() == 4 + 24, "Lock clamped at the edge");

        // Cycle: laser 3.4 s -> rest -> laser again; no claw in phase 1.
        advanceTo(boss, AICommander.LASER_CYCLE_END);
        check(action(boss).equals("IDLE"), "Rest after the laser in phase 1");
        advance(boss, AICommander.IDLE_MILLIS);
        check(action(boss).equals("LASER"), "Laser again");
        for (int i = 0; i < 20; i++) advance(boss, 2000);
        check(!action(boss).equals("CLAW") && !action(boss).equals("INVISIBLE"), "Phase 1 has no claw or invisibility");

        // Hits: 300 points each; phase 2 at half health starts a claw at once.
        AICommander p2 = started();
        p2.setTarget(224, 474);
        advance(p2, AICommander.IDLE_MILLIS + 1); // in a laser
        int points = 0;
        for (int i = 0; i < AICommander.MAX_HP - AICommander.PHASE_2_HP - 1; i++)
            points += p2.manageCollisionsBoss(null);
        check(points == AICommander.POINTS_PER_HIT * 14 && !p2.isPhase2(), "Still phase 1 above half");
        p2.manageCollisionsBoss(null);
        check(p2.isPhase2() && p2.getHp() == AICommander.PHASE_2_HP, "Phase 2 at half health");
        check(action(p2).equals("CLAW"), "Claw Slam right when phase 2 starts (cancels the laser)");
        check(p2.takePlayerHits(shipAt(224, 467)) == 0, "Laser cancelled");
        advance(p2, 1100);
        check(p2.getColor().equals(AICommander.STARLIGHT), "Phase 2 is starlight after the flash");

        // Claw: no hit before impact, exactly one at impact, wherever the player is.
        int beforeX = p2.getPositionX();
        advanceTo(p2, AICommander.CLAW_IMPACT - 50);
        p2.moveBoss(448, 496, null);
        check(p2.getPositionX() == beforeX, "Holds still during the claw");
        check(p2.takePlayerHits(shipAt(20, 467)) == 0, "No hit before impact");
        check(p2.getShakeOffset() == null, "No shake before impact");
        advanceTo(p2, AICommander.CLAW_IMPACT + 10);
        check(p2.takePlayerHits(shipAt(20, 467)) == 1, "Impact: unavoidable hit, even in the corner");
        Point shake = p2.getShakeOffset();
        check(shake != null && shake.x == -9 && shake.y == 7, "Shake follows the handoff table");
        advanceTo(p2, AICommander.CLAW_IMPACT + 410);
        check(p2.getShakeOffset() == null, "Shake over after 0.4 s");
        check(p2.takePlayerHits(shipAt(20, 467)) == 0, "Only one hit per claw");
        check(Double.isNaN(invokeStatic("clawAngle", 0L)) && invokeStatic("clawAngle", 1840L) == 0
                && Math.abs(invokeStatic("clawAngle", 1600L) - 3) < 0.01
                && Double.isNaN(invokeStatic("clawAngle", 3700L)), "Claw swing timeline");
        long clawStart = (Long) get(p2, "actionStart");
        check((Long) get(p2, "nextClawAt") == clawStart + AICommander.CLAW_EVERY_MILLIS, "Next claw in 30 s");

        // After the claw: rest, laser, then invisible (no shooting, still hittable).
        advanceTo(p2, AICommander.CLAW_END);
        check(action(p2).equals("IDLE"), "Rest after the claw");
        advance(p2, AICommander.IDLE_PHASE_2_MILLIS);
        check(action(p2).equals("LASER") && p2.isVisible(), "Laser while visible in phase 2");
        advanceTo(p2, AICommander.LASER_CYCLE_END);
        check(action(p2).equals("INVISIBLE") && !p2.isVisible(), "Invisible after the laser");
        check(!p2.shoot(bullets, null) && bullets.isEmpty(), "Doesn't shoot while invisible");
        check(p2.takePlayerHits(shipAt(224, 467)) == 0, "No laser while invisible");
        int hpBefore = p2.getHp();
        check(p2.manageCollisionsBoss(null) == AICommander.POINTS_PER_HIT && p2.getHp() == hpBefore - 1,
                "Bullets still hit her while invisible");
        check(p2.isVisible(), "Short flash when hit while invisible");
        advance(p2, AICommander.HIT_FLASH_MILLIS + 5);
        check(!p2.isVisible(), "Hidden again after the flash");
        advanceTo(p2, AICommander.INVISIBLE_MILLIS);
        check(action(p2).equals("IDLE") && p2.isVisible(), "Visible again after 2 s");

        // Every 30 s another claw (starts at the next rest).
        int claws = 0;
        String last = action(p2);
        long elapsed = 0;
        while (elapsed < 65000) {
            advance(p2, 50);
            elapsed += 50;
            String now = action(p2);
            if (now.equals("CLAW") && !last.equals("CLAW")) claws++;
            last = now;
        }
        check(claws == 2, "Two more claws in ~65 s, got " + claws);

        // Pause doesn't eat attack time.
        AICommander paused = started();
        advance(paused, AICommander.IDLE_MILLIS + 1);
        long tBefore = actionTime(paused);
        paused.resumeAfterPause(5000);
        long tAfter = actionTime(paused);
        // (no real time passed here, so a 5 s pause moves her clock back 5 s)
        check(Math.abs(tBefore - tAfter - 5000) < 50, "Pause stops her clock");

        // Shield: blocked claw shows SHIELD BROKEN; drawing doesn't throw and is violet.
        AICommander drawn = started();
        drawn.setTarget(224, 474);
        advance(drawn, AICommander.IDLE_MILLIS + 1);
        frames(drawn, 5);
        advanceTo(drawn, 700); // charging: banner (bright half of the blink) + aim line
        BufferedImage charge = render(drawn);
        check(countViolet(charge, new Rectangle(44, 218, 360, 76)) > 500, "WARNING banner is violet");
        check(countViolet(charge, new Rectangle(220, 150, 8, 300)) > 30, "Aim line under the jaw");
        advanceTo(drawn, 2500); // firing
        BufferedImage fire = render(drawn);
        check(countViolet(fire, new Rectangle(212, 140, 24, 340)) > 1500, "Beam is violet");
        check(countViolet(fire, new Rectangle(44, 218, 40, 76)) < 50, "Banner gone once the beam fires");
        for (int i = 0; i < AICommander.PHASE_2_HP; i++) drawn.manageCollisionsBoss(null);
        advanceTo(drawn, 1000);
        BufferedImage swing = render(drawn);
        check(countViolet(swing, new Rectangle(0, 0, 448, 120)) > 2000, "Claws swinging in from the top corners");
        advanceTo(drawn, AICommander.CLAW_IMPACT + 1);
        drawn.takePlayerHits(shipAt(224, 467));
        drawn.onPlayerHitBlocked();
        BufferedImage flash = render(drawn);
        Color top = new Color(flash.getRGB(10, 10));
        check(top.getRed() > 150 && top.getGreen() > 150, "Screen flash at impact");
        advanceTo(drawn, 2200);
        BufferedImage rest = render(drawn);
        check(countViolet(rest, new Rectangle(0, 380, 448, 116)) > 8000, "Claws cover the bottom at rest");
        check(countViolet(rest, new Rectangle(30, 0, 388, 300)) < 200, "Nothing left at the top at rest");
        advance(drawn, 400);
        render(drawn);
        for (long t = 0; t < 4000; t += 97) {
            advance(drawn, 97);
            render(drawn);
        }

        // Destroy: kill bonus, explosion, inactive after.
        AICommander kill = started();
        int last2 = 0;
        for (int i = 0; i < AICommander.MAX_HP; i++) last2 = kill.manageCollisionsBoss(null);
        check(kill.isDestroyed() && last2 == AICommander.POINTS_PER_HIT + AICommander.POINTS_DESTROYED, "Kill bonus");
        check(kill.getSpriteType() == SpriteType.Explosion && kill.isVisible(), "Explodes");
        check(kill.manageCollisionsBoss(null) == 0 && kill.takePlayerHits(shipAt(224, 467)) == 0, "Inactive once destroyed");

        // In game: level 40 spawns her and the hit costs a life (or the shield).
        GameScreen level40 = new GameScreen(new GameState(40, 0, 3, 0, 0, 0),
                StoryMode.getSettings(40), false, 448, 520, 60);
        level40.initialize();
        check(get(level40, "midBoss") instanceof AICommander, "Level 40 is the AI Commander");
        GameScreen level30 = new GameScreen(new GameState(30, 0, 3, 0, 0, 0),
                StoryMode.getSettings(30), false, 448, 520, 60);
        level30.initialize();
        check(get(level30, "midBoss") instanceof FinalBoss, "Level 30 keeps the placeholder");

        AICommander inGame = (AICommander) get(level40, "midBoss");
        Field inputDelay = findField(GameScreen.class, "inputDelay");
        engine.Cooldown countdown = (engine.Cooldown) inputDelay.get(level40);
        findField(countdown.getClass(), "time").setLong(countdown, System.currentTimeMillis() - 7000);
        Method update = GameScreen.class.getDeclaredMethod("update");
        update.setAccessible(true);
        runGameLogic(update, level40);
        check(inGame.isStarted(), "Starts after the countdown");
        check(AICommander.WARNING_TEXT.equals(inGame.getWarningText()), "Warning shows after the countdown");
        advance(inGame, AICommander.INTRO_MILLIS + 1);
        for (int i = 0; i < AICommander.PHASE_2_HP; i++) inGame.manageCollisionsBoss(null);
        check(action(inGame).equals("CLAW"), "Claw in game");
        Field paused2 = findField(AICommander.class, "pausedTotal");
        paused2.setLong(inGame, paused2.getLong(inGame) - (AICommander.CLAW_IMPACT + 20));
        findField(AICommander.class, "lastRealUpdate").setLong(inGame, System.currentTimeMillis());
        int livesBefore = (Integer) get(level40, "lives");
        runGameLogic(update, level40);
        check((Integer) get(level40, "lives") == livesBefore - 1, "Claw costs a life in game");
        check(level40Ship(level40).isDestroyed(), "Ship hit");

        System.out.println("AICommanderTest: " + checks + " checks passed");
    }

    private static Ship level40Ship(GameScreen screen) throws Exception {
        return (Ship) get(screen, "ship");
    }

    private static Object invoke(Object target, String name, long arg) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, long.class);
        m.setAccessible(true);
        return m.invoke(target, arg);
    }

    private static double invokeStatic(String name, long arg) throws Exception {
        Method m = AICommander.class.getDeclaredMethod(name, long.class);
        m.setAccessible(true);
        return (Double) m.invoke(null, arg);
    }
}
