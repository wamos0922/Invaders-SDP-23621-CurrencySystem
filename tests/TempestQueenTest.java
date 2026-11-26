import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import engine.Core;
import engine.DrawManager.SpriteType;
import engine.GameState;
import engine.StoryMode;
import entity.Boss;
import entity.Bullet;
import entity.FinalBoss;
import entity.TempestQueen;
import screen.GameScreen;

/**
 * GOG-75: Level 20 boss, Tempest Queen - warning, on/off shield, straight
 * shots only while closed, phase 2 at half health. Standalone checks like
 * the other tests; run with an isolated build directory and res on the
 * classpath, from a folder that is not your real profile folder.
 */
public final class TempestQueenTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void setLong(Object target, String name, long value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.setLong(target, value);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Starts the fight and skips the warning: she appears, shield closed. */
    private static void skipIntro(TempestQueen boss) throws Exception {
        boss.startFight();
        long t = now() - 1;
        setLong(boss, "introEndsAt", t);
        setLong(boss, "shieldChangesAt", t + TempestQueen.SHIELD_CLOSED_MILLIS);
        setLong(boss, "nextShotAt", t);
    }

    /** Makes the shield change state on the next update. */
    private static void flipShield(TempestQueen boss) throws Exception {
        setLong(boss, "shieldChangesAt", now() - 1);
        boss.update();
    }

    /**
     * Runs GameScreen.update(). Its game logic runs first; the draw() call
     * at its end needs a real window, so in a headless test only that draw
     * is allowed to fail.
     */
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

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        Set<Bullet> bullets = new HashSet<Bullet>();

        // Sprites load at the right size.
        @SuppressWarnings("unchecked")
        Map<SpriteType, boolean[][]> sprites =
                (Map<SpriteType, boolean[][]>) get(Core.getDrawManager(), "spriteMap");
        boolean[][] queen = sprites.get(SpriteType.TempestQueen1);
        boolean[][] gas = sprites.get(SpriteType.GasShield);
        check(queen.length == 24 && queen[0].length == 16, "Tempest Queen is 24 x 16");
        check(gas.length == 28 && gas[0].length == 20, "Gas Shield is 28 x 20");
        check(queen[8][0] && !queen[7][0] && queen[0][6], "Tempest Queen pixels");
        check(gas[9][1] && !gas[0][0] && gas[26][8], "Gas Shield pixels");

        // Level start countdown: her clock doesn't run until startFight().
        TempestQueen boss = new TempestQueen(200, 100);
        check(!boss.isStarted() && boss.isIntro() && !boss.isVisible(), "Hidden during the countdown");
        check(boss.getWarningText() == null, "No warning during the countdown");
        Thread.sleep(TempestQueen.INTRO_MILLIS + 200);
        boss.update();
        check(boss.isIntro() && boss.getWarningText() == null && !boss.isVisible(),
                "Waiting longer than the warning doesn't use it up");

        // Countdown ends: the warning shows for 2 s, she's hidden and harmless.
        boss.startFight();
        check(boss.isStarted() && boss.isIntro() && !boss.isVisible(), "Hidden during the warning");
        check(TempestQueen.WARNING_TEXT.equals(boss.getWarningText()), "Warning text after the countdown");
        long introEnd = (Long) get(boss, "introEndsAt");
        check(Math.abs(introEnd - now() - TempestQueen.INTRO_MILLIS) < 50, "Warning lasts 2 s from start");
        long shieldEnd = (Long) get(boss, "shieldChangesAt");
        check(shieldEnd - introEnd == TempestQueen.SHIELD_CLOSED_MILLIS, "Shield timer starts after the warning");
        boss.startFight();
        check((Long) get(boss, "introEndsAt") == introEnd, "Calling startFight again changes nothing");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == 15, "No damage during warning");
        check(!boss.shoot(bullets, null) && bullets.isEmpty(), "No shots during warning");
        boss.moveBoss(448, 520, null);
        check(boss.getPositionX() == 200, "Doesn't move during warning");
        check(boss.getMaxHp() == 15 && TempestQueen.PHASE_2_HP == 7, "15 HP, phase 2 at 7");

        // Appears with the shield closed: bullets just disappear.
        skipIntro(boss);
        boss.update();
        check(boss.isVisible() && boss.getWarningText() == null, "Appears after the warning");
        check(!boss.isShieldOpen(), "Shield starts closed");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == 15, "Closed shield blocks hits");
        check(boss.getAliveMobCount() == 0, "Phase 1: blocked hit calls no mob");

        // Shoots straight down while closed, one shot per second.
        check(boss.shoot(bullets, null), "Shoots while closed");
        check(bullets.size() == 1, "One bullet");
        Bullet shot = bullets.iterator().next();
        check(shot.getSpeedX() == 0 && shot.getSpeed() > 0, "Straight down");
        check(shot.getPositionY() >= boss.getPositionY() + boss.getHeight() - 1, "From her bottom");
        check(!boss.shoot(bullets, null), "Waits between shots");
        long next = (Long) get(boss, "nextShotAt");
        check(Math.abs(next - now() - TempestQueen.SHOT_MILLIS) < 50, "Phase 1: every 1 s");

        // Moves left and right only, bouncing at the edges.
        int y = boss.getPositionY();
        boss.moveBoss(448, 520, null);
        check(boss.getPositionX() == 202 && boss.getPositionY() == y, "Moves right, stays at the top");
        for (int i = 0; i < 200; i++) boss.moveBoss(448, 520, null);
        check(boss.getPositionX() + boss.getWidth() <= 448 && boss.getPositionX() >= 0, "Stays on screen");

        // Shield opens: she rests and can be hurt.
        flipShield(boss);
        check(boss.isShieldOpen(), "Shield opens");
        long openEnds = (Long) get(boss, "shieldChangesAt");
        check(Math.abs(openEnds - now() - TempestQueen.SHIELD_OPEN_MILLIS) < 50, "Phase 1: open 2 s");
        setLong(boss, "nextShotAt", 0);
        check(!boss.shoot(bullets, null), "Doesn't shoot while open");
        check(boss.manageCollisionsBoss(null) == TempestQueen.POINTS_PER_HIT && boss.getHp() == 14,
                "Open: hit lands for 300");
        check(boss.isVisible(), "Visible while open");

        // Shield closes again for 4 s.
        flipShield(boss);
        check(!boss.isShieldOpen(), "Shield closes");
        long closedEnds = (Long) get(boss, "shieldChangesAt");
        check(Math.abs(closedEnds - now() - TempestQueen.SHIELD_CLOSED_MILLIS) < 50, "Closed 4 s");

        // Phase 2 at half health: flashes, turns cream, shorter open, faster shots.
        flipShield(boss);
        for (int hp = 14; hp > 8; hp--) boss.manageCollisionsBoss(null);
        check(boss.getHp() == 8 && !boss.isPhase2(), "Still phase 1 at 8 HP");
        boss.manageCollisionsBoss(null);
        check(boss.getHp() == 7 && boss.isPhase2(), "Phase 2 at 7 HP");
        boolean sawRed = false, sawCream = false;
        long flashUntil = now() + 400;
        while (now() < flashUntil) {
            boss.update();
            sawRed |= TempestQueen.PHASE_1_COLOR.equals(boss.getColor());
            sawCream |= TempestQueen.PHASE_2_COLOR.equals(boss.getColor());
            Thread.sleep(20);
        }
        check(sawRed && sawCream, "Flashes between both colours");
        setLong(boss, "flashEndsAt", now() - 1);
        boss.update();
        check(TempestQueen.PHASE_2_COLOR.equals(boss.getColor()), "Then stays cream");
        flipShield(boss);
        check(!boss.isShieldOpen(), "Closed again");
        long firstShot = (Long) get(boss, "nextShotAt");
        check(Math.abs(firstShot - now() - TempestQueen.SHOT_PHASE_2_MILLIS) < 50,
                "Phase 2: every 0.5 s");
        flipShield(boss);
        openEnds = (Long) get(boss, "shieldChangesAt");
        check(Math.abs(openEnds - now() - TempestQueen.SHIELD_OPEN_PHASE_2_MILLIS) < 50,
                "Phase 2: open 1.5 s");

        // Phase 2 mobs: none from the hit that started phase 2, one per hit after.
        check(boss.getAliveMobCount() == 0, "No mob from the hit that starts phase 2");
        check(boss.manageCollisionsBoss(null) == TempestQueen.POINTS_PER_HIT, "Phase 2 hit lands");
        check(boss.getHp() == 6 && boss.getAliveMobCount() == 1, "Hit in phase 2 calls a mob");
        boss.manageCollisionsBoss(null);
        check(boss.getAliveMobCount() == 2, "Mobs stack");
        java.util.List<entity.EnemyShip> mobs = boss.getMinions();
        java.util.Set<entity.EnemyType> allowed = new java.util.HashSet<entity.EnemyType>(
                java.util.Arrays.asList(TempestQueen.MOB_TYPES));
        for (entity.EnemyShip mob : mobs) {
            check(allowed.contains(mob.getType()), "Mob is an Act 2 type: " + mob.getType());
            check(mob.getPositionY() > boss.getPositionY() + boss.getHeight(), "Mob below her");
        }
        check(mobs.get(0).getPositionX() != mobs.get(1).getPositionX(), "Mobs take different spots");

        // Mobs move with her.
        int mobX = mobs.get(0).getPositionX(), bossX = boss.getPositionX();
        boss.moveBoss(448, 520, null);
        int moved = boss.getPositionX() - bossX;
        check(moved != 0, "She moved");
        int mobMoved = mobs.get(0).getPositionX() - mobX;
        check(mobMoved == moved || mobs.get(0).getPositionX() == 0
                || mobs.get(0).getPositionX() == 448 - mobs.get(0).getWidth(), "Mob follows her");

        // Mobs shoot all the time, shield open or closed, with their abilities.
        check(boss.isShieldOpen(), "Shield open for the mob shot check");
        Field mobList = findField(TempestQueen.class, "mobs");
        mobList.setAccessible(true);
        for (Object mob : (java.util.List<?>) mobList.get(boss)) {
            Field mobNext = mob.getClass().getDeclaredField("nextShotAt");
            mobNext.setAccessible(true);
            mobNext.setLong(mob, 0);
        }
        bullets.clear();
        check(boss.shoot(bullets, null), "Mobs shoot while the shield is open");
        int expected = 0;
        boolean expectFreeze = false;
        for (entity.EnemyShip mob : mobs) {
            expected += mob.getType() == entity.EnemyType.JELLY ? 2 : 1;
            expectFreeze |= mob.getType() == entity.EnemyType.POLAR;
        }
        check(bullets.size() == expected, "Each mob fires its own shot (Jelly = 2)");
        boolean sawFreeze = false;
        for (Bullet b : bullets) sawFreeze |= b.isFreezing();
        check(sawFreeze == expectFreeze, "Polar mobs fire freeze bullets");
        for (Object mob : (java.util.List<?>) mobList.get(boss)) {
            Field mobNext = mob.getClass().getDeclaredField("nextShotAt");
            mobNext.setAccessible(true);
            long wait = mobNext.getLong(mob) - now();
            check(wait >= TempestQueen.MOB_SHOT_MIN_MILLIS - 50
                    && wait <= TempestQueen.MOB_SHOT_MAX_MILLIS + 50, "Mob shot timer 1.5-2.5 s");
        }
        bullets.clear();
        check(!boss.shoot(bullets, null) && bullets.isEmpty(), "Mobs wait between shots");

        // Phase 2: a bullet blocked by the closed shield also calls a mob.
        flipShield(boss);
        check(!boss.isShieldOpen(), "Shield closed for the blocked-hit check");
        int hpNow = boss.getHp(), mobsNow = boss.getAliveMobCount();
        check(boss.manageCollisionsBoss(null) == 0, "Blocked hit gives no points");
        check(boss.getHp() == hpNow && boss.getAliveMobCount() == mobsNow + 1,
                "Blocked hit in phase 2 calls a mob, no damage");
        boss.manageCollisionsBoss(null);
        boss.manageCollisionsBoss(null);
        check(boss.getAliveMobCount() == mobsNow + 3, "Every blocked hit calls one");
        flipShield(boss);
        check(boss.isShieldOpen(), "Open again");

        // No limit: 20 more mobs all stay.
        Method spawn = TempestQueen.class.getDeclaredMethod("spawnMob");
        spawn.setAccessible(true);
        int beforeMany = boss.getAliveMobCount();
        for (int i = 0; i < 20; i++) spawn.invoke(boss);
        check(boss.getAliveMobCount() == beforeMany + 20, "No mob limit");
        java.util.Set<String> spots = new java.util.HashSet<String>();
        for (entity.EnemyShip mob : boss.getMinions())
            spots.add(mob.getPositionX() + "," + mob.getPositionY());
        java.util.List<entity.EnemyShip> all = boss.getMinions();
        check(all.get(all.size() - 1).getPositionY() > all.get(0).getPositionY(),
                "Extra mobs stack in rows below");

        // A killed mob frees its spot for the next one.
        entity.EnemyShip first = boss.getMinions().get(0);
        int firstX = first.getPositionX(), firstY = first.getPositionY();
        first.destroy();
        spawn.invoke(boss);
        entity.EnemyShip newest = boss.getMinions().get(boss.getMinions().size() - 1);
        check(newest.getPositionX() == firstX && newest.getPositionY() == firstY,
                "New mob takes the freed spot");

        // Pausing keeps her timers still.
        long before = (Long) get(boss, "shieldChangesAt");
        boss.resumeAfterPause(5000);
        check((Long) get(boss, "shieldChangesAt") == before + 5000, "Pause shifts timers");

        // Destroyed on the last hit, with the kill bonus.
        int points = 0;
        while (boss.getHp() > 1) points += boss.manageCollisionsBoss(null);
        int last = boss.manageCollisionsBoss(null);
        check(boss.isDestroyed() && boss.getHp() == 0, "Destroyed at 0 HP");
        check(boss.getAliveMobCount() == 0, "All mobs die with her");
        check(last == TempestQueen.POINTS_PER_HIT + TempestQueen.POINTS_DESTROYED, "Kill bonus");
        check(boss.getSpriteType() == SpriteType.Explosion, "Explodes");
        check(!boss.shoot(bullets, null) && boss.manageCollisionsBoss(null) == 0, "Inactive once destroyed");

        // In game: level 20 spawns her; other boss levels keep the placeholder.
        GameScreen level20 = new GameScreen(new GameState(20, 0, 3, 0, 0, 0),
                StoryMode.getSettings(20), false, 448, 520, 60);
        level20.initialize();
        check(get(level20, "midBoss") instanceof TempestQueen, "Level 20 is the Tempest Queen");

        // Reviewer's case: the warning still shows after the 6 s countdown.
        TempestQueen loaded = (TempestQueen) get(level20, "midBoss");
        check(!loaded.isStarted(), "Not started while the countdown runs");
        Field inputDelay = findField(GameScreen.class, "inputDelay");
        inputDelay.setAccessible(true);
        engine.Cooldown countdown = (engine.Cooldown) inputDelay.get(level20);
        check(!countdown.checkFinished(), "Countdown is running");
        Field startTime = findField(countdown.getClass(), "time");
        startTime.setAccessible(true);
        startTime.setLong(countdown, now() - 7000); // as if 7 s passed
        check(countdown.checkFinished(), "Countdown over");
        Method update = GameScreen.class.getDeclaredMethod("update");
        update.setAccessible(true);
        runGameLogic(update, level20);
        check(loaded.isStarted(), "First update after the countdown starts her");
        check(TempestQueen.WARNING_TEXT.equals(loaded.getWarningText()), "Warning shows after the countdown");
        long firstIntroEnd = (Long) get(loaded, "introEndsAt");
        runGameLogic(update, level20);
        check((Long) get(loaded, "introEndsAt") == firstIntroEnd, "Started only once");
        // Levels 10 and 30 have their own bosses (GOG-74, GOG-76); level 40
        // keeps the placeholder.
        GameScreen level40 = new GameScreen(new GameState(40, 0, 3, 0, 0, 0),
                StoryMode.getSettings(40), false, 448, 520, 60);
        level40.initialize();
        check(get(level40, "midBoss") instanceof FinalBoss, "Level 40 keeps the placeholder");

        // Collisions through GameScreen: closed shield eats the bullet, no damage.
        TempestQueen inGame = (TempestQueen) get(level20, "midBoss");
        skipIntro(inGame);
        inGame.update();
        @SuppressWarnings("unchecked")
        Set<Bullet> screenBullets = (Set<Bullet>) get(level20, "bullets");
        screenBullets.add(new Bullet(inGame.getPositionX() + 20, inGame.getPositionY() + 10, -6));
        Method collisions = GameScreen.class.getDeclaredMethod("manageCollisions");
        collisions.setAccessible(true);
        collisions.invoke(level20);
        check(screenBullets.isEmpty() && inGame.getHp() == 15, "Closed: bullet gone, no damage");
        flipShield(inGame);
        screenBullets.add(new Bullet(inGame.getPositionX() + 20, inGame.getPositionY() + 10, -6));
        int scoreBefore = (Integer) get(level20, "score");
        collisions.invoke(level20);
        check(inGame.getHp() == 14 && (Integer) get(level20, "score") == scoreBefore + 300,
                "Open: hit lands through GameScreen");

        // Player bullets hit mobs first, with normal enemy rewards.
        for (int i = 0; i < 7; i++) inGame.manageCollisionsBoss(null);
        check(inGame.isPhase2() && inGame.getAliveMobCount() == 0, "In-game phase 2");
        inGame.manageCollisionsBoss(null);
        check(inGame.getAliveMobCount() == 1, "In-game mob called");
        entity.EnemyShip inGameMob = inGame.getMinions().get(0);
        int before2 = (Integer) get(level20, "score");
        int hpBefore = inGame.getHp();
        screenBullets.add(new Bullet(inGameMob.getPositionX() + 8, inGameMob.getPositionY() + 4, -6));
        collisions.invoke(level20);
        if (inGameMob.getType() == entity.EnemyType.BEETLE) {
            check(!inGameMob.isDestroyed(), "Beetle mob: shield takes the first hit");
            screenBullets.add(new Bullet(inGameMob.getPositionX() + 8, inGameMob.getPositionY() + 4, -6));
            collisions.invoke(level20);
        }
        check(inGameMob.isDestroyed(), "Mob killed by a player bullet");
        check(inGame.getHp() == hpBefore, "Bullet hit the mob, not her");
        check((Integer) get(level20, "score") > before2, "Mob kill gives points");

        // Pause in GameScreen reaches her timers through the Boss interface.
        Boss asBoss = inGame;
        long t = (Long) get(inGame, "shieldChangesAt");
        asBoss.resumeAfterPause(1000);
        check((Long) get(inGame, "shieldChangesAt") == t + 1000, "Boss interface pause");

        System.out.println("PASS: " + checks + " checks");
    }
}
