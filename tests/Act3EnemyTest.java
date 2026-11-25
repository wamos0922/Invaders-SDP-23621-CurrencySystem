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
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.EnemyType;
import entity.Entity;
import screen.GameScreen;

/**
 * GOG-85: Act 3 (Saturn) enemy abilities - Halo Lancer sniper, Orbit Fighter
 * drop and Hex Sentinel reflecting shield - plus the level rows from GOG-28.
 * Standalone checks like the other tests; run with an isolated build
 * directory and res on the classpath, from a folder that is not your real
 * profile folder.
 */
public final class Act3EnemyTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Object get(Object target, String name) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object call(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static List<EnemyShip> ofType(EnemyShipFormation f, EnemyType type) {
        List<EnemyShip> out = new ArrayList<EnemyShip>();
        for (EnemyShip e : f)
            if (e.getType() == type && !e.isDestroyed())
                out.add(e);
        return out;
    }

    private static int total(Bullet b) {
        return (int) Math.round(Math.sqrt(b.getSpeed() * b.getSpeed()
                + b.getSpeedX() * b.getSpeedX()));
    }

    public static void main(String[] args) throws Exception {
        Core.getLogger().setUseParentHandlers(false);
        EnemyType R = EnemyType.RING_COMMANDER, M = EnemyType.MOONLET_POD,
                L = EnemyType.HALO_LANCER, F = EnemyType.ORBIT_FIGHTER,
                H = EnemyType.HEX_SENTINEL;

        // Abilities: only Lancer, Fighter and Hex; Hex has a 1-hit shield.
        check(L.hasAbility() && F.hasAbility() && H.hasAbility(), "Act 3 abilities");
        check(!R.hasAbility() && !M.hasAbility(), "Ring Commander and Moonlet Pod only shoot");
        check(H.getShieldHits() == 1 && L.getShieldHits() == 0 && F.getShieldHits() == 0,
                "Only Hex Sentinel has a shield");

        // Rows (GOG-28): first N of the order, 2,2,3,3,4,4,4,5,5; no two-hit.
        EnemyType[] order = {R, M, L, F, H};
        int[] counts = {2, 2, 3, 3, 4, 4, 4, 5, 5};
        for (int level = 21; level <= 29; level++) {
            GameSettings s = StoryMode.getSettings(level);
            check(s.getFormationHeight() == counts[level - 21], "Level " + level + " rows");
            check(s.getTwoHitEnemyCount() == 0, "Level " + level + " no two-hit enemies");
            for (int r = 0; r < s.getFormationHeight(); r++)
                check(s.getRowType(r) == order[r], "Level " + level + " row " + (r + 1));
        }
        check(StoryMode.getSettings(30).getRowType(0) == EnemyType.NORMAL, "Level 30 boss untouched");

        // Sniper: fast and aimed at the player.
        Set<Bullet> bullets = new HashSet<Bullet>();
        EnemyShip lancer = new EnemyShip(200, 100, L, 1);
        EnemyShipFormation.fireShot(lancer, bullets, 60, 480);
        check(bullets.size() == 1, "Lancer fires one bullet");
        Bullet snipe = bullets.iterator().next();
        check(snipe.getSpeed() > 0 && snipe.getSpeedX() < 0, "Sniper aims down-left at the player");
        check(Math.abs(total(snipe) - EnemyType.SNIPER_BULLET_SPEED) <= 1, "Sniper bullet is fast (8)");
        bullets.clear();
        EnemyShipFormation.fireShot(lancer, bullets);
        snipe = bullets.iterator().next();
        check(snipe.getSpeedX() == 0 && snipe.getSpeed() == EnemyType.SNIPER_BULLET_SPEED,
                "No target known: fast shot straight down");
        bullets.clear();
        EnemyShipFormation.fireShot(new EnemyShip(200, 100, R, 1), bullets, 60, 480);
        Bullet plain = bullets.iterator().next();
        check(plain.getSpeedX() == 0 && plain.getSpeed() == 4, "Ring Commander shoots straight down");

        // Drop: level 25 has a bottom row of Orbit Fighters.
        EnemyShipFormation level25 = new EnemyShipFormation(StoryMode.getSettings(25));
        List<EnemyShip> fighters = ofType(level25, F);
        check(fighters.size() == 6, "Level 25: a row of 6 Orbit Fighters");
        Entity player = new Entity(0, 480, 26, 16, java.awt.Color.GREEN);
        EnemyShip fighter = fighters.get(0);
        int x0 = fighter.getPositionX(), y0 = fighter.getPositionY();
        check(level25.startDive(fighter, player), "First hit makes it drop");
        check(level25.isDropping(fighter) && !fighter.isDestroyed(), "Dropping, still alive");
        check(!level25.isCometDiving(), "A drop is not a Comet dive");
        check(!level25.startDive(fighter, player), "Second start does nothing");
        level25.updateDives();
        check(fighter.getPositionY() == y0 + EnemyType.FIGHTER_DROP_SPEED
                && fighter.getPositionX() == x0, "Falls straight down, 3 px per frame");

        // It falls onto a player standing under it: one crash, it's gone.
        player = new Entity(fighter.getPositionX(), fighter.getPositionY() + 60, 26, 16, java.awt.Color.GREEN);
        check(level25.takeDropHits(player) == 0, "Not touching yet");
        // Like the game: move, then check, every frame.
        int crashes = 0;
        for (int i = 0; i < 40 && !fighter.isDestroyed(); i++) {
            level25.updateDives();
            crashes += level25.takeDropHits(player);
        }
        check(crashes == 1 && fighter.isDestroyed(), "Crashes into the player");
        check(level25.takeDropHits(player) == 0, "Crashes only once");

        // One that misses falls off the bottom and is removed, no explosion.
        EnemyShip faller = fighters.get(1);
        level25.startDive(faller, player);
        for (int i = 0; i < 500 && !faller.isDestroyed(); i++) level25.updateDives();
        check(faller.isDestroyed(), "Falls off the bottom of the screen");
        check(level25.takeDetonations().isEmpty(), "No Comet blast for a fighter");

        // A second hit while falling kills it with normal rules.
        EnemyShip shot = fighters.get(2);
        level25.startDive(shot, player);
        check(shot.takeDamage(1), "Second hit kills it");
        level25.destroy(shot);
        check(shot.isDestroyed(), "Destroyed mid-fall");
        // 24 ships, 3 fighters gone.
        int alive = 0;
        for (EnemyShip e : level25) if (!e.isDestroyed()) alive++;
        check(alive == 21 && !level25.isEmpty(), "Formation count stays right");

        // Other types don't drop; Comets still dive (Act 4 unchanged).
        EnemyShipFormation level28 = new EnemyShipFormation(StoryMode.getSettings(28));
        check(!level28.startDive(ofType(level28, L).get(0), player), "Lancer doesn't drop");
        check(!level28.startDive(ofType(level28, H).get(0), player), "Hex doesn't drop");
        EnemyShipFormation level31 = new EnemyShipFormation(StoryMode.getSettings(31));
        EnemyShip comet = ofType(level31, EnemyType.COMET).get(0);
        check(level31.startDive(comet, player) && level31.isCometDiving()
                && !level31.isDropping(comet), "Comet still dives at the player");

        // Reflecting shield: first hit breaks it and reflects at the player.
        EnemyShip hex = ofType(level28, H).get(0);
        check(hex.getShield() == 1 && hex.getColor().equals(EnemyType.SHIELD_COLOR), "Hex shield up");
        check(!hex.takeDamage(1) && hex.getShield() == 0 && !hex.isDestroyed(), "Shield takes the first hit");
        check(hex.getColor().equals(H.getColor()), "Back to saturn-hexagon once broken");
        level28.setTarget(100, 480);
        Bullet incoming = new Bullet(hex.getPositionX() + 12, hex.getPositionY() + 8, -6);
        bullets.clear();
        level28.reflectShot(incoming, bullets);
        Bullet back = bullets.iterator().next();
        check(back.getSpeed() > 0, "Reflected bullet comes down (enemy bullet)");
        check((back.getSpeedX() < 0) == (100 < incoming.getPositionX()), "Aimed at the player");
        check(Math.abs(total(back) - EnemyType.REFLECT_BULLET_SPEED) <= 1, "Normal speed");
        check(hex.takeDamage(1), "Second hit kills the Hex");

        // In game (level 28): collisions go through GameScreen.
        GameScreen screen = new GameScreen(new GameState(28, 0, 3, 0, 0, 0),
                StoryMode.getSettings(28), false, 448, 520, 60);
        screen.initialize();
        EnemyShipFormation formation = (EnemyShipFormation) get(screen, "enemyShipFormation");
        @SuppressWarnings("unchecked")
        Set<Bullet> live = (Set<Bullet>) get(screen, "bullets");
        Entity ship = (Entity) get(screen, "ship");
        formation.setTarget(ship.getPositionX() + 13, ship.getPositionY() + 8);

        // Player bullet on a Hex: shield breaks, an enemy bullet comes back.
        EnemyShip gameHex = ofType(formation, H).get(0);
        live.clear();
        live.add(new Bullet(gameHex.getPositionX() + 12, gameHex.getPositionY() + 4, -6));
        call(screen, "manageCollisions");
        check(!gameHex.isDestroyed() && gameHex.getShield() == 0, "In game: Hex shield broke");
        check(live.size() == 1 && live.iterator().next().getSpeed() > 0, "In game: bullet reflected");
        live.clear();
        live.add(new Bullet(gameHex.getPositionX() + 12, gameHex.getPositionY() + 4, -6));
        int score = (Integer) get(screen, "score");
        call(screen, "manageCollisions");
        check(gameHex.isDestroyed() && (Integer) get(screen, "score") > score, "In game: second hit kills it");
        check(live.isEmpty(), "No reflection once the shield is gone");

        // Player bullet on an Orbit Fighter: it drops, no points yet.
        EnemyShip gameFighter = ofType(formation, F).get(0);
        live.add(new Bullet(gameFighter.getPositionX() + 12, gameFighter.getPositionY() + 4, -6));
        score = (Integer) get(screen, "score");
        call(screen, "manageCollisions");
        check(formation.isDropping(gameFighter) && !gameFighter.isDestroyed(), "In game: first hit drops it");
        check((Integer) get(screen, "score") == score, "No points for the first hit");

        // It lands on the player: one life lost.
        gameFighter.setPositionX(ship.getPositionX());
        gameFighter.setPositionY(ship.getPositionY());
        int lives = (Integer) get(screen, "lives");
        call(screen, "handleFighterDrops");
        check((Integer) get(screen, "lives") == lives - 1 && gameFighter.isDestroyed(),
                "In game: falling fighter costs a life");

        System.out.println("PASS: " + checks + " checks");
        System.exit(0);
    }
}
