import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import engine.Core;
import engine.DrawManager.SpriteType;
import engine.GameState;
import engine.StoryMode;
import entity.AIWarship;
import entity.Bullet;
import entity.EnemyShip;
import entity.EnemyType;
import entity.Ship;
import screen.BlackHoleScene;
import screen.GameScreen;

/**
 * GOG-74: Level 10 boss, AI Warship - fly-in, double aimed shots, 2x normal
 * enemy speed, phase 2 Repair Drones that heal it and make it invincible,
 * and the black hole scene after it dies. Standalone checks like the other
 * tests; run with an isolated build directory and res on the classpath,
 * from a folder that is not your real profile folder.
 */
public final class AIWarshipTest {
    private static final int WIDTH = 448;
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
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

    private static Object get(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setLong(Object target, String name, long value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.setLong(target, value);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Starts the fight and skips the fly-in: it is in place and can shoot. */
    private static AIWarship inPlace(int x) throws Exception {
        AIWarship boss = new AIWarship(x, 100);
        boss.startFight();
        setLong(boss, "introEndsAt", now() - 1);
        setLong(boss, "nextShotAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null);
        return boss;
    }

    /** Hits the boss until it reaches the given health. */
    private static void hitDownTo(AIWarship boss, int hp) {
        while (boss.getHp() > hp)
            boss.manageCollisionsBoss(null);
    }

    /** Destroys a drone the way GameScreen does: damage, then destroy. */
    private static void kill(EnemyShip drone) {
        while (!drone.takeDamage(1)) { }
        drone.destroy();
    }

    /** Runs GameScreen.update(); only the final draw() may fail headless. */
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

        // Sprites (GOG-78) load at the right size.
        @SuppressWarnings("unchecked")
        Map<SpriteType, boolean[][]> sprites =
                (Map<SpriteType, boolean[][]>) get(Core.getDrawManager(), "spriteMap");
        check(sprites.get(SpriteType.AIWarship1).length == 24
                && sprites.get(SpriteType.AIWarship1)[0].length == 16, "Warship is 24 x 16");
        check(sprites.get(SpriteType.RepairDrone1).length == 12
                && sprites.get(SpriteType.RepairDrone1)[0].length == 8, "Repair Drone is 12 x 8");

        // Numbers agreed for Level 10.
        check(AIWarship.MAX_HP == 15 && AIWarship.PHASE_2_HP == 7, "15 HP, phase 2 at 7");
        check(AIWarship.DRONE_COUNT == 4 && AIWarship.DRONE_HP == 2, "4 drones, 2 hits each");
        check(AIWarship.HEAL_MILLIS == 3000 && AIWarship.HEAL_AMOUNT == 1, "1 HP every 3 s");
        check(Math.abs(AIWarship.SPEED_X - 1.6) < 1e-9, "2x the formation's top speed (0.8 px/frame)");

        // Countdown: hidden above the screen, harmless, no warning.
        AIWarship boss = new AIWarship(200, 100);
        check(!boss.isStarted() && !boss.isVisible() && boss.getPositionY() < 0, "Hidden above the screen");
        check(boss.getWarningText() == null, "No warning during the countdown");
        check(!boss.shoot(bullets, null) && bullets.isEmpty(), "No shots during the countdown");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == 15, "Can't be hit during the countdown");

        // Fly-in: warning shows, it comes down from the top, still harmless.
        boss.startFight();
        check(boss.isVisible() && boss.isIntro(), "Visible while flying in");
        check(AIWarship.WARNING_TEXT.equals(boss.getWarningText()), "Warning while flying in");
        int startY = boss.getPositionY();
        setLong(boss, "introEndsAt", now() + AIWarship.INTRO_MILLIS / 2);
        boss.moveBoss(WIDTH, 520, null);
        check(boss.getPositionY() > startY && boss.getPositionY() < 100, "Halfway down mid fly-in");
        check(boss.getPositionX() == 200, "Doesn't move sideways while flying in");
        check(boss.manageCollisionsBoss(null) == 0, "Can't be hit while flying in");
        setLong(boss, "introEndsAt", now() - 1);
        boss.moveBoss(WIDTH, 520, null);
        check(boss.getPositionY() == 100 && !boss.isIntro(), "In place at y = 100");
        check(boss.getWarningText() == null, "Warning gone once in place");

        // Moves left and right only, at 1.6 px per frame, bouncing.
        boss = inPlace(200);
        int x0 = boss.getPositionX();
        for (int i = 0; i < 10; i++) boss.moveBoss(WIDTH, 520, null);
        check(Math.abs(boss.getPositionX() - x0) == 16, "16 px in 10 frames");
        check(boss.getPositionY() == 100, "Never moves down");
        for (int i = 0; i < 400; i++) boss.moveBoss(WIDTH, 520, null);
        check(boss.getPositionX() >= 4 && boss.getPositionX() + boss.getWidth() <= WIDTH - 4,
                "Stays on screen");

        // Double shot, aimed at the player.
        boss = inPlace(200);
        boss.setTarget(100, 480);
        bullets.clear();
        check(boss.shoot(bullets, null) && bullets.size() == 2, "Two bullets per shot");
        for (Bullet b : bullets)
            check(b.getSpeed() > 0 && b.getSpeedX() < 0, "Both go down towards the player (left)");
        bullets.clear();
        check(!boss.shoot(bullets, null), "Waits between shots");
        setLong(boss, "nextShotAt", now() - 1);
        boss.setTarget(boss.getCenterX(), 480);
        check(boss.shoot(bullets, null) && bullets.size() == 2, "Shoots again after the interval");

        // Phase 1 hits: 300 points each, no drones yet.
        boss = inPlace(200);
        check(boss.manageCollisionsBoss(null) == AIWarship.POINTS_PER_HIT && boss.getHp() == 14, "Hit lands");
        hitDownTo(boss, 8);
        check(!boss.isPhase2() && boss.getMinions().isEmpty() && !boss.isInvincible(), "Phase 1 down to 8 HP");
        boss.update();
        check(boss.getColor().equals(AIWarship.PHASE_1_COLOR), "Phase 1 is earth-ocean");

        // Half health: phase 2, four drones fly in, boss invincible.
        boss.manageCollisionsBoss(null);
        check(boss.getHp() == 7 && boss.isPhase2(), "Phase 2 at 7 HP");
        List<EnemyShip> drones = boss.getMinions();
        check(drones.size() == 4 && boss.getAliveDroneCount() == 4, "Four drones");
        for (EnemyShip d : drones) {
            check(d.getType() == EnemyType.REPAIR_DRONE && d.getHealth() == 2, "Drone type, 2 HP");
            check(d.getPositionY() < 0, "Drone starts above the screen");
            check(d.getColor().equals(EnemyShip.EARTH_LAND), "Drone is earth-land, not yellow");
        }
        check(boss.isInvincible(), "Invincible while drones live");
        check(AIWarship.DRONE_TEXT.equals(boss.getWarningText()), "Drone message shows");
        check(boss.manageCollisionsBoss(null) == 0 && boss.getHp() == 7, "Bullets do nothing while drones live");
        boss.update();
        check(boss.getColor().equals(AIWarship.PHASE_1_COLOR)
                || boss.getColor().equals(AIWarship.PHASE_2_COLOR), "Flashing between both colours");
        setLong(boss, "flashEndsAt", now() - 1);
        boss.update();
        check(boss.getColor().equals(AIWarship.PHASE_2_COLOR), "Phase 2 is earth-magma");

        // Drones fly down to a row under the boss and sway near it.
        for (int i = 0; i < 200; i++) boss.moveBoss(WIDTH, 520, null);
        int rowY = drones.get(0).getPositionY();
        check(rowY > boss.getPositionY() + boss.getHeight(), "Drones sit under the boss");
        for (EnemyShip d : drones) {
            check(d.getPositionY() == rowY, "Drones in one row");
            check(Math.abs(d.getPositionX() + 12 - boss.getCenterX()) < 140, "Drones stay near the boss");
            check(d.getPositionX() >= 0 && d.getPositionX() + d.getWidth() <= WIDTH, "Drones on screen");
        }

        // Drones don't shoot: only the boss's two bullets come out.
        bullets.clear();
        setLong(boss, "nextShotAt", now() - 1);
        boss.shoot(bullets, null);
        check(bullets.size() == 2, "Boss keeps shooting in phase 2, drones never do");

        // Healing: each living drone heals 1 HP every 3 s.
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) get(boss, "drones");
        for (Object d : raw) {
            long next = (Long) get(d, "nextHealAt");
            check(Math.abs(next - now() - AIWarship.HEAL_MILLIS) < 200, "First heal 3 s after arriving");
            setLong(d, "nextHealAt", now() - 1);
        }
        boss.update();
        check(boss.getHp() == 11, "Four drones heal 4 HP");
        check(boss.getColor().equals(AIWarship.HEAL_COLOR), "Glows green when healed");
        boss.update();
        check(boss.getHp() == 11, "No heal until the next 3 s");

        // Heal goes up to full health, never above.
        for (int round = 0; round < 3; round++) {
            for (Object d : raw) setLong(d, "nextHealAt", now() - 1);
            boss.update();
        }
        check(boss.getHp() == AIWarship.MAX_HP, "Healed up to full, not above");

        // A drone takes two hits; first hit leaves it alive and green.
        EnemyShip first = drones.get(0);
        check(!first.takeDamage(1) && !first.isDestroyed(), "Drone survives one hit");
        check(first.getColor().equals(EnemyShip.EARTH_LAND), "Still green after one hit");
        check(first.takeDamage(1), "Second hit kills it");
        first.destroy();
        check(boss.getAliveDroneCount() == 3 && boss.isInvincible(), "Still invincible with 3 left");

        // A dead drone stops healing.
        Field hpField = findField(AIWarship.class, "hp");
        hpField.setAccessible(true);
        hpField.setInt(boss, 10);
        for (Object d : raw) setLong(d, "nextHealAt", now() - 1);
        boss.update();
        check(boss.getHp() == 13, "Only the 3 living drones heal");

        // Kill all drones: boss can be hurt again, drones never come back.
        for (EnemyShip d : boss.getMinions())
            if (!d.isDestroyed()) kill(d);
        check(!boss.isInvincible() && boss.getAliveDroneCount() == 0, "No drones left, vulnerable");
        check(boss.getWarningText() == null, "Drone message gone once they're dead");
        check(boss.manageCollisionsBoss(null) == 300 && boss.getHp() == 12, "Hits land again");
        hitDownTo(boss, 3);
        check(boss.getAliveDroneCount() == 0, "Drones come only once");

        // Dead drones are removed after their fade-out.
        Thread.sleep(500);
        boss.update();
        check(boss.getMinions().isEmpty(), "Faded drones removed");

        // Finish it: kill bonus, explosion, inactive.
        hitDownTo(boss, 1);
        int last = boss.manageCollisionsBoss(null);
        check(boss.isDestroyed() && boss.getHp() == 0, "Destroyed at 0 HP");
        check(last == AIWarship.POINTS_PER_HIT + AIWarship.POINTS_DESTROYED, "Kill bonus");
        check(!boss.isVisible(), "Replaced by the explosion");
        check(!boss.shoot(bullets, null) && boss.manageCollisionsBoss(null) == 0, "Inactive once destroyed");

        // Killing the boss with drones alive kills them too.
        AIWarship early = inPlace(200);
        hitDownTo(early, 7);
        early.destroy();
        for (EnemyShip d : early.getMinions())
            check(d.isDestroyed(), "Drones die with the boss");

        // Pause keeps every timer still.
        AIWarship paused = inPlace(200);
        hitDownTo(paused, 7);
        long shot = (Long) get(paused, "nextShotAt");
        @SuppressWarnings("unchecked")
        List<Object> pausedDrones = (List<Object>) get(paused, "drones");
        long heal = (Long) get(pausedDrones.get(0), "nextHealAt");
        paused.resumeAfterPause(1000);
        check((Long) get(paused, "nextShotAt") == shot + 1000, "Shot timer paused");
        check((Long) get(pausedDrones.get(0), "nextHealAt") == heal + 1000, "Heal timer paused");

        // Black hole scene timeline.
        long t0 = 1_000_000L;
        BlackHoleScene scene = new BlackHoleScene(224, 116, t0);
        check(scene.getRadius(t0 + 500) == 0, "Explosion plays first, no hole yet");
        check(scene.getRadius(t0 + BlackHoleScene.EXPLOSION_MILLIS + BlackHoleScene.OPEN_MILLIS / 2) > 0,
                "Hole opens");
        long pullAt = t0 + BlackHoleScene.EXPLOSION_MILLIS + BlackHoleScene.OPEN_MILLIS;
        check(scene.getRadius(pullAt) == BlackHoleScene.MAX_RADIUS, "Full size before the pull");
        Ship ship = new Ship(200, 480, Core.getSelectedShipType());
        scene.pull(ship, pullAt - 1);
        check(ship.getPositionY() == 480, "Ship not pulled before the hole is open");
        scene.pull(ship, pullAt + BlackHoleScene.PULL_MILLIS / 2);
        int midY = ship.getPositionY() + ship.getHeight() / 2;
        check(midY < 480 + ship.getHeight() / 2 && midY > 116, "Ship halfway to the hole");
        check(scene.getShipScale(pullAt + BlackHoleScene.PULL_MILLIS / 2) < 1, "Ship shrinking");
        long pullEnd = pullAt + BlackHoleScene.PULL_MILLIS;
        scene.pull(ship, pullEnd);
        check(Math.abs(ship.getPositionX() + ship.getWidth() / 2 - 224) <= 1
                && Math.abs(ship.getPositionY() + ship.getHeight() / 2 - 116) <= 1, "Ship swallowed at the centre");
        check(scene.getShipScale(pullEnd) == 0, "Ship gone");
        check(scene.getFadeAlpha(pullEnd) == 0 && !scene.isFinished(pullEnd), "Fade starts after the pull");
        long end = pullEnd + BlackHoleScene.FADE_MILLIS;
        check(scene.getFadeAlpha(end) == 255 && scene.isFinished(end), "Fades to black, then done");
        scene.resumeAfterPause(5000);
        check(!scene.isFinished(end) && scene.isFinished(end + 5000), "Pause delays the scene");

        // In game: level 10 spawns it; other boss levels are unchanged.
        GameScreen level10 = new GameScreen(new GameState(10, 0, 3, 0, 0, 0),
                StoryMode.getSettings(10), false, WIDTH, 520, 60);
        level10.initialize();
        check(get(level10, "midBoss") instanceof AIWarship, "Level 10 is the AI Warship");
        check(get(level10, "blackHole") == null, "No black hole before it dies");
        GameScreen level20 = new GameScreen(new GameState(20, 0, 3, 0, 0, 0),
                StoryMode.getSettings(20), false, WIDTH, 520, 60);
        level20.initialize();
        check(!(get(level20, "midBoss") instanceof AIWarship), "Level 20 is not the AI Warship");

        // Countdown ends: the first update starts the fly-in.
        AIWarship inGame = (AIWarship) get(level10, "midBoss");
        Field inputDelay = findField(GameScreen.class, "inputDelay");
        inputDelay.setAccessible(true);
        engine.Cooldown countdown = (engine.Cooldown) inputDelay.get(level10);
        Field startTime = findField(countdown.getClass(), "time");
        startTime.setAccessible(true);
        startTime.setLong(countdown, now() - 7000);
        Method update = GameScreen.class.getDeclaredMethod("update");
        update.setAccessible(true);
        runGameLogic(update, level10);
        check(inGame.isStarted() && inGame.isIntro(), "Fly-in starts after the countdown");

        // Bullets through GameScreen: drones are hit first, boss shielded.
        setLong(inGame, "introEndsAt", now() - 1);
        hitDownTo(inGame, 7);
        for (int i = 0; i < 200; i++) inGame.moveBoss(WIDTH, 520, null);
        @SuppressWarnings("unchecked")
        Set<Bullet> screenBullets = (Set<Bullet>) get(level10, "bullets");
        Method collisions = GameScreen.class.getDeclaredMethod("manageCollisions");
        collisions.setAccessible(true);
        screenBullets.add(new Bullet(inGame.getCenterX(), inGame.getPositionY() + 10, -6));
        collisions.invoke(level10);
        check(screenBullets.isEmpty() && inGame.getHp() == 7, "Boss shielded: bullet gone, no damage");
        int scoreBefore = (Integer) get(level10, "score");
        for (EnemyShip d : inGame.getMinions()) {
            for (int hit = 0; hit < 2; hit++) {
                screenBullets.add(new Bullet(d.getPositionX() + 12, d.getPositionY() + 8, -6));
                collisions.invoke(level10);
            }
            check(d.isDestroyed(), "Two bullets kill a drone in game");
        }
        check((Integer) get(level10, "score") >= scoreBefore + 4 * 100, "Drone kills give points");
        check(!inGame.isInvincible(), "All drones dead");

        // Last hit: level finishes, black hole starts, screen keeps running.
        // GameScreen checks for a cleared level after draw(), which needs a
        // window, so this part runs only where a display is available.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println("SKIP: black hole in GameScreen (no display)");
            System.out.println("PASS: " + checks + " checks");
            System.exit(0);
        }
        engine.Frame window = new engine.Frame(WIDTH, 520);
        Core.getDrawManager().setFrame(window);
        hitDownTo(inGame, 1);
        screenBullets.add(new Bullet(inGame.getCenterX(), inGame.getPositionY() + 10, -6));
        collisions.invoke(level10);
        check(inGame.isDestroyed(), "Boss destroyed in game");
        update.invoke(level10); // draws too: explosion, then the hole
        Thread.sleep(BlackHoleScene.EXPLOSION_MILLIS + BlackHoleScene.OPEN_MILLIS + 300);
        update.invoke(level10); // draws the hole and the shrinking ship
        BlackHoleScene inGameScene = (BlackHoleScene) get(level10, "blackHole");
        check(inGameScene != null, "Black hole starts when it dies");
        check(inGameScene.getCenterX() == inGame.getCenterX()
                && inGameScene.getCenterY() == inGame.getCenterY(), "Hole opens at the wreck");
        check((Boolean) get(level10, "levelFinished"), "Level counts as cleared");
        Field cooldownTime = findField(engine.Cooldown.class, "time");
        cooldownTime.setAccessible(true);
        cooldownTime.setLong(get(level10, "screenFinishedCooldown"), now() - 60_000);
        Field running = findField(GameScreen.class, "isRunning");
        running.setAccessible(true);
        running.setBoolean(level10, true); // Screen.run() sets this
        update.invoke(level10);
        check((Boolean) get(level10, "isRunning"), "Level waits for the black hole scene");
        check(inGameScene.isPulling(now()), "Ship is being pulled in");
        setLong(inGameScene, "startedAt", now() - 60_000);
        // A first boss kill may unlock an achievement; the level also waits
        // for its popup, so clear it to test only the black hole wait.
        Field popup = findField(GameScreen.class, "unlockedAchievement");
        popup.setAccessible(true);
        popup.set(level10, null);
        ((java.util.Queue<?>) get(level10, "achievementPopupQueue")).clear();
        update.invoke(level10);
        check(!(Boolean) get(level10, "isRunning"), "Level ends once the scene is over");
        window.dispose();

        // Other enemy types still turn yellow with extra health (Act 2).
        EnemyShip jelly = new EnemyShip(0, 0, EnemyType.JELLY, 2);
        check(jelly.getColor().equals(Color.YELLOW), "Act 2 two-hit enemies still yellow");

        System.out.println("PASS: " + checks + " checks");
        System.exit(0);
    }
}
