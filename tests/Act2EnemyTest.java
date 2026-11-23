import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.StoryMode;
import entity.Bullet;
import entity.BulletPool;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.EnemyType;
import entity.Ship;
import screen.GameScreen;

/**
 * GOG-84: Act 2 (levels 11-19) enemy gameplay - Polar freeze bullet, Jelly
 * double shot, Beetle shield. Standalone checks like TwoHitEnemyTest; run
 * with an isolated build directory and res on the classpath, from a folder
 * that is not your real profile folder.
 */
public final class Act2EnemyTest {
    private static int checks;

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Fires once, ignoring the formation's random shooting delay. */
    private static Set<Bullet> shootOnce(EnemyShipFormation formation) throws Exception {
        set(formation, "shootingCooldown", Core.getCooldown(0));
        Set<Bullet> bullets = new HashSet<Bullet>();
        formation.shoot(bullets);
        return bullets;
    }

    /** Destroys every living ship of a type, so the next row starts shooting. */
    private static void destroyAll(EnemyShipFormation formation, EnemyType type) {
        List<EnemyShip> targets = new ArrayList<EnemyShip>();
        for (EnemyShip enemy : formation)
            if (enemy.getType() == type && !enemy.isDestroyed())
                targets.add(enemy);
        for (EnemyShip enemy : targets)
            formation.destroy(enemy);
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);

        // Sprites load from res/graphics in the right order and orientation.
        @SuppressWarnings("unchecked")
        java.util.Map<engine.DrawManager.SpriteType, boolean[][]> sprites =
                (java.util.Map<engine.DrawManager.SpriteType, boolean[][]>)
                        get(Core.getDrawManager(), "spriteMap");
        String[] polarA = {".#...##...#.", "..#.####.#..", "..########..", ".###.##.###.",
                ".##########.", "..########..", "..#.####.#..", ".#...##...#."};
        String[] beetleB = {".##......##.", "#..#....#..#", "...######...", "..##.##.##..",
                ".##########.", ".#.######.#.", ".#.#....#.#.", "#..##..##..#"};
        String[] kingA = {"#..#.##.#..#", ".##########.", "##..####..##", "##.#....#.##",
                "##.#.##.#.##", ".###....###.", "..#.####.#..", ".#........#."};
        String[] rayB = {"............", "#..........#", "##.######.##", "############",
                "..##.##.##..", "...######...", ".....##.....", "......#....."};
        for (Object[] c : new Object[][] {
                {engine.DrawManager.SpriteType.Polar1, polarA},
                {engine.DrawManager.SpriteType.Beetle2, beetleB},
                {engine.DrawManager.SpriteType.StormKing1, kingA},
                {engine.DrawManager.SpriteType.CloudRay2, rayB}}) {
            boolean[][] image = sprites.get(c[0]);
            String[] grid = (String[]) c[1];
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 12; x++)
                    check(image[x][y] == (grid[y].charAt(x) == '#'),
                            c[0] + " pixel " + x + "," + y);
        }

        // Levels 11-19: draft row counts, 5 per row, the planned row types.
        EnemyType K = EnemyType.STORM_KING, R = EnemyType.CLOUD_RAY,
                P = EnemyType.POLAR, J = EnemyType.JELLY, B = EnemyType.BEETLE;
        EnemyType[][] plan = {
                {K, B}, {K, J}, {P, K, R}, {P, R, B}, {K, J, R, B},
                {P, K, J, B}, {P, J, J, B}, {P, K, J, J, B}, {P, J, J, B, B}};
        int[] draftRows = {2, 2, 3, 3, 4, 4, 4, 5, 5};
        int previousSpecialRows = 0;
        for (int level = 11; level <= 19; level++) {
            GameSettings settings = StoryMode.getSettings(level);
            EnemyType[] rows = plan[level - 11];
            check(settings.getFormationHeight() == draftRows[level - 11],
                    "Level " + level + " has the draft row count");
            check(settings.getFormationWidth() == 5, "Level " + level + " has 5 per row");
            check(settings.isDiagonalMovement() == (level == 13 || level == 16 || level == 19),
                    "Level " + level + " diagonal setting kept");
            check(settings.getTwoHitEnemyCount() == 0, "No two-hit enemies yet");
            int specialRows = 0;
            for (int r = 0; r < rows.length; r++) {
                check(settings.getRowType(r) == rows[r], "Level " + level + " row " + r);
                if (rows[r].hasAbility()) specialRows++;
            }
            check(specialRows >= previousSpecialRows, "Special rows never decrease");
            check(level > 13 || specialRows == 1, "Levels 11-13 have one special row");
            previousSpecialRows = specialRows;
            check(rows[rows.length - 1] != P && (rows[0] == P || !java.util.Arrays.asList(rows).contains(P)),
                    "Polar only at the back");

            // Formation builds the planned types row by row, top to bottom.
            java.util.TreeMap<Integer, java.util.Set<EnemyType>> byY = new java.util.TreeMap<>();
            int count = 0;
            for (EnemyShip e : new EnemyShipFormation(settings)) {
                byY.computeIfAbsent(e.getPositionY(), y -> new java.util.HashSet<>()).add(e.getType());
                check(e.getSpriteType() == e.getType().getFrameA(), "Starts on frame A");
                check(e.getPointValue() == e.getType().getPointValue(), "Point value");
                count++;
            }
            check(count == 5 * rows.length, "Level " + level + " enemy count");
            int r = 0;
            for (java.util.Set<EnemyType> types : byY.values())
                check(types.size() == 1 && types.contains(rows[r++]), "Level " + level + " row order");
        }
        check(previousSpecialRows == 5, "Level 19 is all special");
        // Other acts keep their old formations until their owners change them.
        for (int level : new int[] {1, 9, 21, 31}) {
            for (EnemyShip e : new EnemyShipFormation(StoryMode.getSettings(level)))
                check(e.getType() == EnemyType.NORMAL, "Level " + level + " stays normal");
        }
        try {
            new GameSettings(5, 2, 30, 1600).withRowTypes(P);
            check(false, "Row types must match the row count");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        check(!K.hasAbility() && !R.hasAbility() && P.hasAbility() && J.hasAbility()
                && B.hasAbility(), "Only Polar, Jelly, Beetle have abilities");

        // Beetle shield: first hit only breaks the shield, colour shows it.
        EnemyShip beetle = new EnemyShip(0, 0, EnemyType.BEETLE, 1);
        check(beetle.getShield() == 1, "Beetle starts shielded");
        check(EnemyType.SHIELD_COLOR.equals(beetle.getColor()), "Shielded colour");
        check(!beetle.takeDamage(1), "Shield absorbs the first hit");
        check(beetle.getShield() == 0 && beetle.getHealth() == 1, "Health untouched by shield hit");
        check(EnemyType.BEETLE.getColor().equals(beetle.getColor()), "Back to beetle brown");
        check(beetle.takeDamage(1), "Second hit kills");
        EnemyShip eliteBeetle = new EnemyShip(0, 0, EnemyType.BEETLE, 2);
        check(!eliteBeetle.takeDamage(1) && Color.YELLOW.equals(eliteBeetle.getColor()),
                "Two-hit beetle: shield, then yellow");
        check(!eliteBeetle.takeDamage(1) && EnemyType.BEETLE.getColor().equals(eliteBeetle.getColor()),
                "Two-hit beetle: then brown");
        check(eliteBeetle.takeDamage(1), "Two-hit beetle dies on the third hit");
        EnemyShip jelly = new EnemyShip(0, 0, EnemyType.JELLY, 1);
        check(jelly.getShield() == 0 && jelly.takeDamage(1), "Jelly has no shield");
        EnemyShip king = new EnemyShip(0, 0, EnemyType.STORM_KING, 1);
        check(EnemyType.STORM_KING.getColor().equals(king.getColor()) && king.takeDamage(1),
                "Storm King: own colour, one hit");

        // Animation swaps between the type's own two frames.
        EnemyShip polar = new EnemyShip(0, 0, EnemyType.POLAR, 1);
        Thread.sleep(520);
        polar.update();
        check(polar.getSpriteType() == EnemyType.POLAR.getFrameB(), "Animates to frame B");
        try {
            new EnemyShip(0, 0, EnemyType.NORMAL, 1);
            check(false, "NORMAL must use the SpriteType constructor");
        } catch (IllegalArgumentException expected) {
            checks++;
        }

        // Shooting: front Beetles fire normal bullets, then Jelly, then Polar.
        EnemyShipFormation formation = new EnemyShipFormation(
                new GameSettings(5, 3, 30, 1600).withRowTypes(P, J, B));
        Set<Bullet> shot = shootOnce(formation);
        check(shot.size() == 1 && !shot.iterator().next().isFreezing(), "Beetle: one normal bullet");
        destroyAll(formation, EnemyType.BEETLE);
        shot = shootOnce(formation);
        check(shot.size() == 2, "Jelly: double shot");
        java.util.List<Integer> drift = new java.util.ArrayList<Integer>();
        int startX = -1;
        for (Bullet b : shot) {
            check(!b.isFreezing(), "Jelly bullets don't freeze");
            check(startX < 0 || startX == b.getPositionX(), "Both leave from the same point");
            startX = b.getPositionX();
            drift.add(b.getSpeedX());
        }
        java.util.Collections.sort(drift);
        check(drift.get(0) == -EnemyType.DOUBLE_SHOT_SPREAD
                && drift.get(1) == EnemyType.DOUBLE_SHOT_SPREAD, "Cone: one left, one right");
        for (int frame = 0; frame < 30; frame++)
            for (Bullet b : shot) b.update();
        int[] xs = new int[2];
        int n = 0;
        for (Bullet b : shot) xs[n++] = b.getPositionX();
        check(Math.abs(xs[0] - xs[1]) == 60, "Cone spreads 60 px after 30 frames");
        destroyAll(formation, EnemyType.JELLY);
        shot = shootOnce(formation);
        check(shot.size() == 1, "Polar: one bullet");
        Bullet freezeBullet = shot.iterator().next();
        check(freezeBullet.isFreezing(), "Polar bullet freezes");
        check(EnemyType.FREEZE_BULLET_COLOR.equals(freezeBullet.getColor()), "Freeze bullet colour");
        check(freezeBullet.getSpeedX() == 0, "Freeze bullet goes straight down");
        check(new Color(0x4AB9E8).equals(EnemyType.FREEZE_BULLET_COLOR), "Freeze colour is #4AB9E8");
        BulletPool.recycle(shot);
        Bullet reused = BulletPool.getBullet(0, 0, 4);
        check(!reused.isFreezing() && Color.WHITE.equals(reused.getColor()),
                "Recycled bullet is back to normal");

        // Ship freeze: no moving or shooting for 3 s, no stacking.
        Ship ship = new Ship(100, 400);
        Set<Bullet> playerBullets = new HashSet<Bullet>();
        check(ship.freeze() && ship.isFrozen(), "Freeze starts");
        check(!ship.freeze(), "A second freeze while frozen is ignored");
        check(!ship.shoot(playerBullets) && playerBullets.isEmpty(), "Can't shoot while frozen");
        ship.update();
        check(EnemyType.FREEZE_BULLET_COLOR.equals(ship.getColor()), "Frozen tint");
        Thread.sleep(EnemyType.FREEZE_MILLIS - 1000);
        ship.resumeAfterPause(1000); // a 1 s pause pushes the end back
        Thread.sleep(500);
        check(ship.isFrozen(), "Pause time doesn't count toward the freeze");
        Thread.sleep(1600); // 3 s freeze + 1 s pause has passed
        check(!ship.isFrozen(), "Freeze ends");
        ship.update();
        check(Color.GREEN.equals(ship.getColor()), "Colour restored");
        check(ship.shoot(playerBullets), "Can shoot again");

        // In game: a freeze bullet costs no life, a normal one still does.
        GameScreen screen = new GameScreen(new GameState(11, 0, 3, 0, 0, 0),
                new GameSettings(3, 3, 10, 1000).withRowTypes(P, J, B), false, 448, 520, 60);
        screen.initialize();
        Ship player = (Ship) get(screen, "ship");
        @SuppressWarnings("unchecked")
        Set<Bullet> bullets = (Set<Bullet>) get(screen, "bullets");
        Bullet hit = new Bullet(player.getPositionX(), player.getPositionY(), 4);
        hit.setFreezing(true);
        bullets.add(hit);
        call(screen, "manageCollisions");
        check((Integer) get(screen, "lives") == 3, "Freeze bullet takes no life");
        check(player.isFrozen() && !player.isDestroyed(), "Freeze bullet freezes the ship");
        check(bullets.isEmpty(), "Freeze bullet consumed");
        bullets.add(new Bullet(player.getPositionX(), player.getPositionY(), 4));
        call(screen, "manageCollisions");
        check((Integer) get(screen, "lives") == 2, "Normal bullet still takes a life while frozen");

        // Level 15 is a normal level with Act 2 enemies (no mid boss).
        GameScreen level15 = new GameScreen(new GameState(15, 0, 3, 0, 0, 0),
                StoryMode.getSettings(15), false, 448, 520, 60);
        level15.initialize();
        check(get(level15, "midBoss") == null && get(level15, "enemyShipFormation") != null,
                "Level 15 has no mid boss");
        EnemyShipFormation level15Formation =
                (EnemyShipFormation) get(level15, "enemyShipFormation");
        check(level15Formation.iterator().next().getType().hasOwnSprites(),
                "Level 15 uses the Act 2 enemies");

        System.out.println("PASS: " + checks + " checks");
    }
}
