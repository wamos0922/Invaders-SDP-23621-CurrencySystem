import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.Core;
import engine.DrawManager.SpriteType;
import engine.GameState;
import engine.StoryMode;
import entity.AICommander;
import entity.Bullet;
import entity.EnemyShip;
import entity.RingDreadnought;
import screen.GameScreen;

/**
 * GOG-76: Level 30 boss, Ring Dreadnought - fly-in, left/right with a bob,
 * wing fans and a spreading burst that always leave gaps, phase 2 shotgun
 * swoop with short-range pellets. Standalone checks like the other tests;
 * run with an isolated build directory and res on the classpath, from a
 * folder that is not your real profile folder.
 */
public final class RingDreadnoughtTest {
    private static final int WIDTH = 448;
    /** Player ship: 26 px wide; its top sits at 490 on a 520 px screen. */
    private static final int SHIP_WIDTH = 26;
    private static final int PLAYER_TOP = 490;
    private static final int PLAYER_CENTER_Y = PLAYER_TOP + 8;
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

    private static void setLong(Object target, String name, long value) throws Exception {
        field(target.getClass(), name).setLong(target, value);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Starts the fight and skips the fly-in; no volley is due yet. */
    private static RingDreadnought inPlace() throws Exception {
        RingDreadnought boss = new RingDreadnought(200, 100);
        boss.startFight();
        setLong(boss, "introEndsAt", now() - 1);
        setLong(boss, "nextFanAt", now() + 60_000);
        setLong(boss, "nextBurstAt", now() + 60_000);
        boss.setTarget(213, PLAYER_CENTER_Y);
        boss.moveBoss(WIDTH, 520, null);
        return boss;
    }

    /** Left-edge x of each bullet when it reaches the player's row. */
    private static List<Integer> xAtPlayerRow(Set<Bullet> volley) {
        List<Integer> xs = new ArrayList<Integer>();
        for (Bullet b : volley) {
            int x = b.getPositionX(), y = b.getPositionY();
            while (y + b.getHeight() < PLAYER_TOP) {
                y += b.getSpeed();
                x += b.getSpeedX();
            }
            xs.add(x);
        }
        return xs;
    }

    /** Smallest empty space between two bullets of a volley at the player row. */
    private static int smallestGap(Set<Bullet> volley) {
        List<Integer> xs = xAtPlayerRow(volley);
        java.util.Collections.sort(xs);
        int smallest = Integer.MAX_VALUE;
        for (int i = 1; i < xs.size(); i++)
            smallest = Math.min(smallest, xs.get(i) - xs.get(i - 1) - Bullet.DEFAULT_WIDTH);
        return smallest;
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        Set<Bullet> bullets = new HashSet<Bullet>();
        int minGap = (int) Math.ceil(1.5 * SHIP_WIDTH);

        // Numbers agreed for Level 30.
        check(RingDreadnought.MAX_HP == 20 && RingDreadnought.PHASE_2_HP == 10, "20 HP, phase 2 at 10");
        check(RingDreadnought.FAN_BULLETS == 3, "3 bullets per wing");
        check(RingDreadnought.FAN_MILLIS == 2000 && RingDreadnought.FAN_PHASE_2_MILLIS == 1200,
                "Fans every 2 s, then 1.2 s");
        check(RingDreadnought.SHOTGUN_PELLETS == 5, "5-pellet shotgun");

        // Countdown: hidden above the screen, harmless.
        RingDreadnought boss = new RingDreadnought(200, 100);
        check(!boss.isVisible() && boss.getPositionY() < 0 && boss.getWarningText() == null,
                "Hidden during the countdown");
        check(boss.getSpriteType() == SpriteType.RingDreadnought1, "Uses the GOG-81 sprite");
        check(!boss.shoot(bullets, null) && boss.manageCollisionsBoss(null) == 0, "Harmless before the fight");

        // Fly-in: warning, comes down, still harmless.
        boss.startFight();
        check(RingDreadnought.WARNING_TEXT.equals(boss.getWarningText()) && boss.isVisible(),
                "Warning while it flies in");
        setLong(boss, "introEndsAt", now() + RingDreadnought.INTRO_MILLIS / 2);
        boss.moveBoss(WIDTH, 520, null);
        check(boss.getPositionY() > -32 && boss.getPositionY() < 100, "Halfway down");
        check(boss.manageCollisionsBoss(null) == 0, "Can't be hit while flying in");
        setLong(boss, "introEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null);
        check(boss.getWarningText() == null && !boss.isIntro(), "In place");

        // Moves left/right and bobs a little, staying on screen.
        boss = inPlace();
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE, x0 = boss.getPositionX();
        boolean moved = false;
        for (int i = 0; i < 600; i++) {
            setLong(boss, "bobStart", now() - i * 10L);
            boss.moveBoss(WIDTH, 520, null);
            minY = Math.min(minY, boss.getPositionY());
            maxY = Math.max(maxY, boss.getPositionY());
            moved |= boss.getPositionX() != x0;
            check(boss.getPositionX() >= 4 && boss.getPositionX() + 48 <= WIDTH - 4, "On screen");
        }
        check(moved, "Moves sideways");
        check(minY >= 100 && maxY <= 100 + RingDreadnought.BOB_RANGE && maxY > minY,
                "Bobs up and down a little (12 px)");

        // Wing fans: 3 per wing, spreading outwards, gaps at the player row.
        boss = inPlace();
        setLong(boss, "nextFanAt", now() - 1);
        bullets.clear();
        check(boss.shoot(bullets, null) && bullets.size() == 6, "Two fans of 3");
        int left = 0, right = 0;
        for (Bullet b : bullets) {
            check(b.getSpeed() == RingDreadnought.FAN_SPEED_Y && b.getMaxY() == Integer.MAX_VALUE,
                    "Fan bullets fly the whole screen");
            if (b.getSpeedX() < 0) left++;
            if (b.getSpeedX() > 0) right++;
        }
        check(left == 2 && right == 2, "Each wing spreads outwards");
        check(smallestGap(bullets) >= minGap, "Fan gaps >= 1.5 ship widths at the player row");
        long nextFan = (Long) get(boss, "nextFanAt");
        check(Math.abs(nextFan - now() - RingDreadnought.FAN_MILLIS) < 100, "Next fan in 2 s");

        // Gaps hold at the bob's lowest point and at the screen edges too.
        for (int bob : new int[] {0, RingDreadnought.BOB_RANGE}) {
            for (int x : new int[] {4, 200, WIDTH - 52}) {
                RingDreadnought b2 = inPlace();
                field(RingDreadnought.class, "exactX").setDouble(b2, x);
                b2.setPositionX(x);
                b2.setPositionY(100 + bob);
                setLong(b2, "nextFanAt", now() - 1);
                setLong(b2, "nextBurstAt", now() - 1);
                Set<Bullet> fan = new HashSet<Bullet>();
                Set<Bullet> burst = new HashSet<Bullet>();
                field(RingDreadnought.class, "nextBurstAt").setLong(b2, now() + 60_000);
                b2.shoot(fan, null);
                setLong(b2, "nextBurstAt", now() - 1);
                b2.shoot(burst, null);
                check(fan.size() == 6 && burst.size() == 4, "Fan and burst at x " + x);
                check(smallestGap(fan) >= minGap, "Fan gap at x " + x + ", bob " + bob);
                check(smallestGap(burst) >= minGap, "Burst gap at x " + x + ", bob " + bob);
            }
        }

        // Spreading burst: tight at the cannon, wide at the player row.
        boss = inPlace();
        setLong(boss, "nextBurstAt", now() - 1);
        bullets.clear();
        boss.shoot(bullets, null);
        check(bullets.size() == 4, "Burst of 4");
        int startSpread = 0;
        for (Bullet a : bullets) for (Bullet b : bullets)
            startSpread = Math.max(startSpread, Math.abs(a.getPositionX() - b.getPositionX()));
        List<Integer> end = xAtPlayerRow(bullets);
        int endSpread = java.util.Collections.max(end) - java.util.Collections.min(end);
        check(startSpread == 0 && endSpread > 200, "Starts together, opens wide as it falls");

        // Phase 1 hits: 300 points, no shotgun.
        boss = inPlace();
        check(boss.manageCollisionsBoss(null) == RingDreadnought.POINTS_PER_HIT && boss.getHp() == 19, "Hit lands");
        while (boss.getHp() > 11) boss.manageCollisionsBoss(null);
        boss.update();
        check(!boss.isPhase2() && !boss.isSwooping(), "Phase 1 down to 11 HP");
        check(boss.getColor().equals(RingDreadnought.PHASE_1_COLOR), "saturn-gold");

        // Half health: phase 2, faster fans, butterscotch.
        boss.manageCollisionsBoss(null);
        check(boss.getHp() == 10 && boss.isPhase2(), "Phase 2 at 10 HP");
        setLong(boss, "flashEndsAt", now() - 1);
        setLong(boss, "nextShotgunAt", now() + 60_000);
        boss.update();
        check(boss.getColor().equals(RingDreadnought.PHASE_2_COLOR), "saturn-butterscotch");
        setLong(boss, "nextFanAt", now() - 1);
        bullets.clear();
        boss.shoot(bullets, null);
        nextFan = (Long) get(boss, "nextFanAt");
        check(Math.abs(nextFan - now() - RingDreadnought.FAN_PHASE_2_MILLIS) < 100, "Fans every 1.2 s now");

        // Shotgun swoop: warning flash, dive, 5 short pellets, back up.
        setLong(boss, "nextShotgunAt", now() - 1);
        boss.update();
        check(boss.isShotgunWarning(), "Flashes before the shotgun");
        check(boss.getColor().equals(RingDreadnought.WARNING_COLOR)
                || boss.getColor().equals(RingDreadnought.PHASE_2_COLOR), "Warning flash colours");
        setLong(boss, "nextFanAt", now() - 1);
        setLong(boss, "nextBurstAt", now() - 1);
        bullets.clear();
        check(!boss.shoot(bullets, null) && bullets.isEmpty(), "No fans or bursts during the swoop");
        int rowY = boss.getPositionY();
        setLong(boss, "swoopStepEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null); // warning -> down
        setLong(boss, "swoopStepEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null); // down -> hold
        int lowY = boss.getPositionY();
        check(lowY > rowY + 100, "Swoops down towards the player");
        check(PLAYER_CENTER_Y - (lowY + boss.getHeight()) <= RingDreadnought.SWOOP_GAP + 10,
                "Close above the player");
        check(lowY + boss.getHeight() < PLAYER_TOP, "Never touches the player's row");
        bullets.clear();
        check(boss.shoot(bullets, null) && bullets.size() == 5, "Shotgun: 5 pellets");
        for (Bullet b : bullets)
            check(b.getMaxY() == lowY + boss.getHeight() + RingDreadnought.SHOTGUN_RANGE,
                    "Pellets vanish after a short distance");
        check(lowY + boss.getHeight() + RingDreadnought.SHOTGUN_RANGE > PLAYER_TOP,
                "Short range still reaches the player");
        bullets.clear();
        check(!boss.shoot(bullets, null), "Fires once per swoop");
        setLong(boss, "swoopStepEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null); // hold -> up
        setLong(boss, "swoopStepEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null); // up -> done
        check(!boss.isSwooping() && boss.getPositionY() <= 100 + RingDreadnought.BOB_RANGE,
                "Back up on its row");
        long nextShotgun = (Long) get(boss, "nextShotgunAt");
        check(Math.abs(nextShotgun - now() - RingDreadnought.SHOTGUN_MILLIS) < 100, "Next shotgun in 4 s");
        check(boss.manageCollisionsBoss(null) == 300, "Hittable in phase 2");

        // Finish it: kill bonus, explosion.
        while (boss.getHp() > 1) boss.manageCollisionsBoss(null);
        int last = boss.manageCollisionsBoss(null);
        check(boss.isDestroyed() && last == 300 + RingDreadnought.POINTS_DESTROYED, "Kill bonus");
        check(!boss.isVisible() && !boss.shoot(bullets, null) && boss.manageCollisionsBoss(null) == 0,
                "Explodes, inactive");

        // Pause keeps timers still.
        RingDreadnought paused = inPlace();
        long fan = (Long) get(paused, "nextFanAt");
        long shotgun = (Long) get(paused, "nextShotgunAt");
        paused.resumeAfterPause(1000);
        check((Long) get(paused, "nextFanAt") == fan + 1000
                && (Long) get(paused, "nextShotgunAt") == shotgun + 1000, "Pause keeps timers");

        // Pooled bullets don't keep a short range.
        Bullet pellet = entity.BulletPool.getBullet(0, 0, 5);
        pellet.setMaxY(300);
        Set<Bullet> back = new HashSet<Bullet>();
        back.add(pellet);
        entity.BulletPool.recycle(back);
        check(entity.BulletPool.getBullet(0, 0, 5).getMaxY() == Integer.MAX_VALUE,
                "Reused bullet has no range limit");

        // In game: level 30 spawns it, level 40 keeps the placeholder.
        GameScreen level30 = new GameScreen(new GameState(30, 0, 3, 0, 0, 0),
                StoryMode.getSettings(30), false, WIDTH, 520, 60);
        level30.initialize();
        check(get(level30, "midBoss") instanceof RingDreadnought, "Level 30 is the Ring Dreadnought");
        GameScreen level40 = new GameScreen(new GameState(40, 0, 3, 0, 0, 0),
                StoryMode.getSettings(40), false, WIDTH, 520, 60);
        level40.initialize();
        check(get(level40, "midBoss") instanceof AICommander, "Level 40 is the AI Commander (GOG-77)");

        // GameScreen removes pellets once they pass their range.
        @SuppressWarnings("unchecked")
        Set<Bullet> live = (Set<Bullet>) get(level30, "bullets");
        Bullet shortShot = new Bullet(200, 300, 5);
        shortShot.setMaxY(302);
        Bullet longShot = new Bullet(260, 300, 5);
        live.add(shortShot);
        live.add(longShot);
        Method clean = GameScreen.class.getDeclaredMethod("cleanBullets");
        clean.setAccessible(true);
        clean.invoke(level30);
        check(!live.contains(shortShot) && live.contains(longShot), "Short-range pellet removed, normal bullet kept");

        // Player bullets hit it through GameScreen.
        RingDreadnought inGame = (RingDreadnought) get(level30, "midBoss");
        inGame.startFight();
        setLong(inGame, "introEndsAt", now() - 1);
        inGame.moveBoss(WIDTH, 520, null);
        live.clear();
        live.add(new Bullet(inGame.getPositionX() + 24, inGame.getPositionY() + 10, -6));
        int score = (Integer) get(level30, "score");
        Method collisions = GameScreen.class.getDeclaredMethod("manageCollisions");
        collisions.setAccessible(true);
        collisions.invoke(level30);
        check(inGame.getHp() == 19 && (Integer) get(level30, "score") == score + 300,
                "Hit lands through GameScreen");
        check(EnemyShip.SATURN_GOLD.equals(RingDreadnought.PHASE_1_COLOR), "Design colour");

        System.out.println("PASS: " + checks + " checks");
        System.exit(0);
    }
}
