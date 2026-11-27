package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.GlitchEffect;
import engine.PlayerSounds;
import engine.soundEngine.ItemSounds;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;
import engine.DrawManager.SpriteType;
import entity.Bullet;
import entity.BulletPool;
import entity.Entity;
import entity.Ship;

/**
 * Story mode Level 0.5 (GOG-31): the escape from the Human Prison's
 * underground hangar.
 *
 * Opens with a short power-up transition (the stolen ship glitches on, then
 * a close-up of it launching), then teaches the basics in four steps inside
 * the hangar: move, shoot, collect an item, and destroy three security drones
 * that try to stop the player. After the last step the ship flies up out of
 * the hangar, leading into Act 1 (panel 6, "Finally, freedom!").
 * Practice only: the player cannot lose lives here and nothing affects the
 * score. Endless mode keeps using the original {@link TutorialScreen}.
 *
 * Sound: the intro has its own power-up and jet-thruster effects
 * (ship_power_up.wav, ship_launch.wav). Gameplay uses the same music and
 * effects as a normal stage: the game BGM once the intro ends, enemy hit and explosion sounds, the player damage
 * sound, the rapid-fire item sound and the victory jingle on completion.
 *
 * Return codes: 2 = finished or skipped (continue the story),
 * 1 = cancelled with ESC (back to the menu).
 */
public final class StoryTutorialScreen extends Screen {

    /** Tutorial steps, in order. */
    private enum Step { INTRO, MOVE, SHOOT, COLLECT, DEFEAT, COMPLETE, EXIT }

    /** Pixels the player must travel to finish the MOVE step. */
    private static final int MOVE_DISTANCE = 60;
    /** Number of security drones in the final step. */
    private static final int DRONE_COUNT = 3;
    /** Space between drones in the formation. */
    private static final int DRONE_SPACING = 80;
    /** Drone formation speed, in pixels per frame. */
    private static final int DRONE_SPEED = 1;
    /** Speed at which drones fly in from the top. */
    private static final int DRONE_ENTRY_SPEED = 2;
    /** Speed of drone shots (positive = downwards). */
    private static final int DRONE_BULLET_SPEED = 3;
    /** Time between drone shots, in milliseconds. */
    private static final int DRONE_FIRE_INTERVAL = 1400;
    /** How long the player blinks after being hit, in milliseconds. */
    private static final int HIT_INVULNERABILITY = 1200;
    /** How long an exploded drone stays visible, in milliseconds. */
    private static final int EXPLOSION_TIME = 300;
    /** How long a short notice stays on screen, in milliseconds. */
    private static final int NOTICE_TIME = 2000;
    /** Fall speed of the power cell. */
    private static final int CELL_FALL_SPEED = 2;
    /** Extra shots per second given by the power cell. */
    private static final double CELL_FIRE_RATE_BONUS = 2.0;

    /** Intro: glitch while the ship powers up (ms). */
    private static final int INTRO_GLITCH_END = 1000;
    /** Intro: first white flash (ms). */
    private static final int INTRO_FLASH_END = 1250;
    /** Intro: close-up of the ship launching (ms). */
    private static final int INTRO_LAUNCH_END = 2750;
    /** Intro: flash that fades into the tutorial (ms). */
    private static final int INTRO_END = 3100;
    /** Size of one ship pixel in the close-up. */
    private static final int CLOSE_UP_PIXEL = 10;
    /** Exit: ship flies out of the hangar (ms). */
    private static final int EXIT_TIME = 1100;

    /** Hangar wall colour. */
    private static final Color WALL = new Color(16, 18, 28);
    /** Hangar panel line colour. */
    private static final Color PANEL_LINE = new Color(26, 29, 42);
    /** Hangar pillar colour. */
    private static final Color PILLAR = new Color(30, 33, 48);
    /** Hangar pillar edge colour. */
    private static final Color PILLAR_EDGE = new Color(44, 48, 68);
    /** Warning lamp colour (on). */
    private static final Color LAMP_ON = new Color(255, 150, 40);
    /** Warning lamp colour (off). */
    private static final Color LAMP_OFF = new Color(80, 45, 20);
    /** Hazard stripe colour. */
    private static final Color HAZARD = new Color(200, 160, 20);

    /** Colour of the security drones. */
    private static final Color DRONE_COLOR = new Color(255, 96, 96);
    /** Colour of the power cell. */
    private static final Color CELL_COLOR = new Color(255, 220, 60);
    /** Colour of step instructions. */
    private static final Color TEXT_COLOR = new Color(255, 214, 64);
    /** Colour of secondary text. */
    private static final Color HINT_COLOR = new Color(170, 170, 190);
    /** Colour of notices such as "Rapid fire online!". */
    private static final Color NOTICE_COLOR = new Color(110, 220, 255);

    private Step step;
    private Ship ship;
    private final Set<Bullet> bullets = new HashSet<Bullet>();
    private final Set<Bullet> enemyBullets = new HashSet<Bullet>();
    private final List<Drone> drones = new ArrayList<Drone>();
    private final Random random = new Random();

    private int distanceMoved;
    private Pickup cell;
    private int formationDirection = 1;
    private Cooldown droneFireCooldown;
    private Cooldown invulnerability;
    private Cooldown noticeTimer;
    private String notice;
    private boolean enterWasDown;
    private boolean skipWasDown;
    private long phaseStart;
    private final GlitchEffect glitch = new GlitchEffect();
    private long nextGlitchBurst;
    private boolean launchSoundPlayed;

    /**
     * Creates the Level 0.5 tutorial.
     *
     * @param width  Screen width.
     * @param height Screen height.
     * @param fps    Frames per second.
     */
    public StoryTutorialScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
    }

    @Override
    public void initialize() {
        ship = new Ship(width / 2 - 13, height - 75);
        step = Step.INTRO;
        phaseStart = System.currentTimeMillis();
        nextGlitchBurst = 0;
        launchSoundPlayed = false;
        SoundManager.getInstance().playSfx(Sound.SHIP_POWER_UP);
        distanceMoved = 0;
        cell = null;
        drones.clear();
        formationDirection = 1;
        droneFireCooldown = Core.getCooldown(DRONE_FIRE_INTERVAL);
        invulnerability = Core.getCooldown(HIT_INVULNERABILITY);
        noticeTimer = Core.getCooldown(NOTICE_TIME);
        notice = null;
        returnCode = 1;
        enterWasDown = inputManager.isKeyDown(KeyEvent.VK_ENTER);
        skipWasDown = inputManager.isKeyDown(KeyEvent.VK_S);
        inputDelay.reset();
    }

    @Override
    public int run() {
        try {
            super.run();
            return returnCode;
        } finally {
            // Story panels follow, so the stage music stops with the tutorial.
            SoundManager.getInstance().stopBgm();
            BulletPool.recycle(bullets);
            BulletPool.recycle(enemyBullets);
            bullets.clear();
            enemyBullets.clear();
        }
    }

    @Override
    protected void update() {
        boolean enterDown = inputManager.isKeyDown(KeyEvent.VK_ENTER);
        boolean skipDown = inputManager.isKeyDown(KeyEvent.VK_S);

        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            returnCode = 1;
            isRunning = false;
            return;
        }
        // A fresh press of S skips the whole tutorial at any time.
        if (skipDown && !skipWasDown) {
            returnCode = 2;
            isRunning = false;
            return;
        }
        skipWasDown = skipDown;

        boolean enterPressed = enterDown && !enterWasDown;
        if (step == Step.INTRO) {
            // ENTER skips the intro only; S above skips the whole tutorial.
            if (enterPressed || elapsed() >= INTRO_END) {
                startStep(Step.MOVE);
                glitch.setEnabled(false);
                // Story tutorial: do not start generic BGM; SFX remain.
            }
        } else if (step == Step.EXIT) {
            if (enterPressed || elapsed() >= EXIT_TIME) {
                returnCode = 2;
                isRunning = false;
            }
        } else if (inputDelay.checkFinished()) {
            if (step == Step.COMPLETE) {
                if (enterPressed) {
                    startStep(Step.EXIT);
                    SoundManager.getInstance().playSfx(Sound.SHIP_LAUNCH);
                }
            } else {
                updatePlay();
            }
        }
        enterWasDown = enterDown;
        if (step == Step.INTRO) {
            drawIntro();
        } else {
            draw();
        }
    }

    // ------------------------------------------------------------------
    // Game logic
    // ------------------------------------------------------------------

    private void updatePlay() {
        updateShipMovement();

        if (step != Step.MOVE && inputManager.isKeyDown(KeyEvent.VK_SPACE)
                && ship.shoot(bullets) && step == Step.SHOOT) {
            startCollectStep();
        }
        ship.update();
        if (invulnerability.checkFinished()) {
            ship.setBlinking(false);
        }

        if (cell != null) {
            updateCell();
        }
        if (step == Step.DEFEAT) {
            updateDrones();
        }
        updateBullets();
    }

    private void updateShipMovement() {
        int oldX = ship.getPositionX();
        boolean left = inputManager.isKeyDown(KeyEvent.VK_LEFT)
                || inputManager.isKeyDown(KeyEvent.VK_A);
        boolean right = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
                || inputManager.isKeyDown(KeyEvent.VK_D);
        if (left && !right && oldX - ship.getSpeed() >= 1) {
            ship.moveLeft();
        } else if (right && !left
                && oldX + ship.getWidth() + ship.getSpeed() < width) {
            ship.moveRight();
        }
        if (step == Step.MOVE) {
            distanceMoved += Math.abs(ship.getPositionX() - oldX);
            if (distanceMoved >= MOVE_DISTANCE) {
                step = Step.SHOOT;
            }
        }
    }

    /** Drops the power cell on the side of the screen away from the player. */
    private void startCollectStep() {
        step = Step.COLLECT;
        cell = new Pickup(cellSpawnX(ship.getPositionX(), width) - 8, height / 4);
    }

    private void updateCell() {
        // The cell falls to the player's row and waits there, so it can't be missed.
        if (cell.getPositionY() < ship.getPositionY()) {
            cell.setPositionY(Math.min(ship.getPositionY(),
                    cell.getPositionY() + CELL_FALL_SPEED));
        }
        if (overlaps(cell, ship)) {
            cell = null;
            ship.setItemBonuses(CELL_FIRE_RATE_BONUS, 0);
            ItemSounds.play("rapid_fire");
            showNotice("Power cell collected: rapid fire!");
            startDefeatStep();
        }
    }

    /** Three security drones fly in from above, in a row. */
    private void startDefeatStep() {
        step = Step.DEFEAT;
        int startX = width / 2 - DRONE_SPACING * (DRONE_COUNT - 1) / 2 - 12;
        for (int i = 0; i < DRONE_COUNT; i++) {
            drones.add(new Drone(startX + i * DRONE_SPACING, -40 - i * 25,
                    height / 2 - 40));
        }
        droneFireCooldown.reset();
    }

    private void updateDrones() {
        boolean allArrived = true;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (Drone drone : drones) {
            drone.animate();
            if (drone.isExploded()) {
                continue;
            }
            if (!drone.hasArrived()) {
                drone.flyIn();
                allArrived = false;
            }
            minX = Math.min(minX, drone.getPositionX());
            maxX = Math.max(maxX, drone.getPositionX() + drone.getWidth());
        }

        // Once in place, the formation sweeps left and right and fires.
        if (allArrived && minX != Integer.MAX_VALUE) {
            formationDirection = nextFormationDirection(minX, maxX, width,
                    formationDirection);
            for (Drone drone : drones) {
                if (!drone.isExploded()) {
                    drone.setPositionX(drone.getPositionX()
                            + formationDirection * DRONE_SPEED);
                }
            }
            if (droneFireCooldown.checkFinished()) {
                droneFireCooldown.reset();
                fireFromRandomDrone();
            }
        }

        // Remove drones whose explosion has finished.
        Iterator<Drone> iterator = drones.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExplosionFinished()) {
                iterator.remove();
            }
        }
        if (drones.isEmpty()) {
            step = Step.COMPLETE;
            // Same as clearing a stage: music stops, victory jingle plays.
            SoundManager.getInstance().stopBgm();
            SoundManager.getInstance().playSfx(Sound.VICTORY);
            BulletPool.recycle(enemyBullets);
            enemyBullets.clear();
            BulletPool.recycle(bullets);
            bullets.clear();
            notice = null;
        }
    }

    private void fireFromRandomDrone() {
        List<Drone> alive = new ArrayList<Drone>();
        for (Drone drone : drones) {
            if (!drone.isExploded()) {
                alive.add(drone);
            }
        }
        if (alive.isEmpty()) {
            return;
        }
        Drone shooter = alive.get(random.nextInt(alive.size()));
        enemyBullets.add(BulletPool.getBullet(
                shooter.getPositionX() + shooter.getWidth() / 2,
                shooter.getPositionY() + shooter.getHeight(),
                DRONE_BULLET_SPEED));
    }

    private void updateBullets() {
        Set<Bullet> expired = new HashSet<Bullet>();

        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            boolean hit = false;
            for (Drone drone : drones) {
                if (!drone.isExploded() && drone.hasArrived()
                        && overlaps(bullet, drone)) {
                    drone.explode();
                    SoundManager.getInstance().playSfx(Sound.ENEMY_EXPLOSION);
                    hit = true;
                    break;
                }
            }
            if (hit || bullet.getPositionY() + bullet.getHeight() < 0) {
                iterator.remove();
                expired.add(bullet);
            }
        }

        iterator = enemyBullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            boolean hitShip = overlaps(bullet, ship);
            if (hitShip && invulnerability.checkFinished()) {
                // Practice only: no life is lost, the ship just blinks.
                invulnerability.reset();
                ship.setBlinking(true);
                PlayerSounds.playDamage();
                showNotice("Hit! Dodge the red shots.");
            }
            if (hitShip || bullet.getPositionY() > height) {
                iterator.remove();
                expired.add(bullet);
            }
        }
        BulletPool.recycle(expired);
    }

    private void startStep(final Step next) {
        step = next;
        phaseStart = System.currentTimeMillis();
    }

    /** @return Milliseconds since the current step or phase began. */
    private long elapsed() {
        return System.currentTimeMillis() - phaseStart;
    }

    private void showNotice(final String text) {
        notice = text;
        noticeTimer.reset();
    }

    // ------------------------------------------------------------------
    // Small rules kept separate so they can be unit tested
    // (tests/screen/StoryTutorialScreenTest.java)
    // ------------------------------------------------------------------

    /**
     * The power cell drops on the side of the screen away from the player,
     * so the player has to move to collect it.
     *
     * @param shipX Ship's left edge.
     * @param width Screen width.
     * @return Centre x of the power cell.
     */
    static int cellSpawnX(final int shipX, final int width) {
        return shipX < width / 2 ? width * 3 / 4 : width / 4;
    }

    /**
     * Drone formation direction for the next frame: it turns around before
     * touching either side of the screen (10 px margin).
     *
     * @param minX      Left edge of the formation.
     * @param maxX      Right edge of the formation.
     * @param width     Screen width.
     * @param direction Current direction (1 = right, -1 = left).
     * @return New direction.
     */
    static int nextFormationDirection(final int minX, final int maxX,
                                      final int width, final int direction) {
        if (maxX + DRONE_SPEED >= width - 10) {
            return -1;
        }
        if (minX - DRONE_SPEED <= 10) {
            return 1;
        }
        return direction;
    }

    /**
     * Top of the big ship in the launch close-up. Starts below the screen,
     * eases in (slow lift-off, fast climb) and ends fully above the top.
     *
     * @param k      Progress of the launch, 0 to 1.
     * @param height Screen height.
     * @param shipH  Close-up ship height.
     * @return Y position of the ship's top edge.
     */
    static int launchCloseUpY(final float k, final int height, final int shipH) {
        float eased = k * k;
        return (int) (height + 20 - eased * (height + shipH + 140));
    }

    /** @return True if the two entities' boxes overlap. */
    static boolean overlaps(final Entity first, final Entity second) {
        return first.getPositionX() < second.getPositionX() + second.getWidth()
                && first.getPositionX() + first.getWidth() > second.getPositionX()
                && first.getPositionY() < second.getPositionY() + second.getHeight()
                && first.getPositionY() + first.getHeight() > second.getPositionY();
    }

    private int dronesLeft() {
        int count = 0;
        for (Drone drone : drones) {
            if (!drone.isExploded()) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    private void draw() {
        drawManager.initDrawing(this);
        drawHangar(true);
        drawManager.drawScreenTitle(this, "LEVEL 0.5");
        drawManager.drawCenteredRegularString(this,
                "Escape the Human Prison", height / 8 + 24, HINT_COLOR);

        drawInstructions();

        // Hangar floor.
        drawManager.drawHorizontalLine(this, height - 57);

        if (step == Step.EXIT) {
            drawExitFlight();
        } else {
            drawManager.drawEntity(ship, ship.getPositionX(), ship.getPositionY());
        }
        if (cell != null) {
            drawManager.drawEntity(cell, cell.getPositionX(), cell.getPositionY());
        }
        for (Drone drone : drones) {
            drawManager.drawEntity(drone, drone.getPositionX(), drone.getPositionY());
        }
        for (Bullet bullet : bullets) {
            drawManager.drawEntity(bullet, bullet.getPositionX(), bullet.getPositionY());
        }
        for (Bullet bullet : enemyBullets) {
            drawManager.drawEntity(bullet, bullet.getPositionX(), bullet.getPositionY());
        }

        if (notice != null && !noticeTimer.checkFinished()) {
            drawManager.drawCenteredRegularString(this, notice,
                    height / 8 + 115, NOTICE_COLOR);
        }
        drawManager.drawKeyHints(this, "S: skip tutorial | ESC: menu");
        drawManager.completeDrawing(this);
    }

    // ------------------------------------------------------------------
    // Intro transition, hangar and exit flight
    // ------------------------------------------------------------------

    /**
     * Power-up intro: the stolen ship glitches on, a white flash, a close-up
     * of the ship launching from the bottom to the top, then a flash that
     * fades into the tutorial.
     */
    private void drawIntro() {
        long t = elapsed();
        drawManager.initDrawing(this);

        if (t < INTRO_GLITCH_END) {
            // Hangar lights flicker while the ship's systems wake up.
            boolean lightsOn = (t / 90) % 3 != 0;
            drawHangar(lightsOn);
            if (!lightsOn) {
                drawManager.drawFadeOverlay(this, 150);
            }
            String status = t < INTRO_GLITCH_END / 2
                    ? "POWERING UP..." : "ENGINES ONLINE";
            if ((t / 120) % 4 != 0) {
                // drawCenteredBigString uses the current colour; a zero-size
                // fillBox sets it to green without drawing anything.
                drawManager.fillBox(0, 0, 0, 0, Color.GREEN);
                drawManager.drawCenteredBigString(this, status, height / 2);
            }
            // Re-trigger the VFX team's glitch so bursts keep coming.
            if (t >= nextGlitchBurst) {
                glitch.setEnabled(false);
                glitch.setEnabled(true);
                nextGlitchBurst = t + 180;
            }
            drawManager.drawGlitch(this, glitch);
        } else if (t < INTRO_FLASH_END) {
            glitch.setEnabled(false);
            float k = (float) (t - INTRO_GLITCH_END)
                    / (INTRO_FLASH_END - INTRO_GLITCH_END);
            drawHangar(true);
            flash(1f - k * 0.6f);
        } else if (t < INTRO_LAUNCH_END) {
            if (!launchSoundPlayed) {
                // Jet thrusters roar as the ship climbs past the camera.
                launchSoundPlayed = true;
                SoundManager.getInstance().playSfx(Sound.SHIP_LAUNCH);
            }
            float k = (float) (t - INTRO_FLASH_END)
                    / (INTRO_LAUNCH_END - INTRO_FLASH_END);
            drawLaunchCloseUp(k);
        } else {
            float k = (float) (t - INTRO_LAUNCH_END) / (INTRO_END - INTRO_LAUNCH_END);
            drawHangar(true);
            flash(1f - k);
        }
        drawManager.drawKeyHints(this, "ENTER: skip intro | S: skip tutorial");
        drawManager.completeDrawing(this);
    }

    /** Big ship rising from the bottom to the top, with engine flames. */
    private void drawLaunchCloseUp(final float k) {
        // Camera shake that grows as the engines push harder.
        int shake = 1 + (int) (k * 4);
        drawManager.setWorldOffset(random.nextInt(shake * 2 + 1) - shake,
                random.nextInt(shake * 2 + 1) - shake);
        drawHangar(true);

        // Speed lines streaming past.
        for (int i = 0; i < 14; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            drawManager.fillBox(x, y, 1, 20 + random.nextInt(40),
                    new Color(255, 255, 255, 60 + random.nextInt(80)));
        }

        int[] size = drawManager.getSpriteSize(ship.getIdleSprite());
        int shipW = size[0] * CLOSE_UP_PIXEL;
        int shipH = size[1] * CLOSE_UP_PIXEL;
        // Ease in: slow lift-off, then fast climb past the top.
        int x = width / 2 - shipW / 2;
        int y = launchCloseUpY(k, height, shipH);

        drawFlame(x + shipW / 2, y + shipH, shipW / 3, 40 + (int) (k * 60));
        drawManager.drawSpriteScaled(ship.getIdleSprite(), x, y,
                CLOSE_UP_PIXEL, ship.getColor());
        drawManager.setWorldOffset(0, 0);
    }

    /** Ship flying up out of the hangar after the tutorial. */
    private void drawExitFlight() {
        float k = Math.min(1f, (float) elapsed() / EXIT_TIME);
        int y = (int) (ship.getPositionY() - k * k * (height + 60));
        int x = ship.getPositionX();
        drawFlame(x + ship.getWidth() / 2, y + ship.getHeight(), 5,
                10 + (int) (k * 30));
        drawManager.drawEntity(ship, x, y);
        if (k > 0.7f) {
            flash((k - 0.7f) / 0.3f);
        }
    }

    /** Flickering engine flame, centred on x, hanging below y. */
    private void drawFlame(final int x, final int y, final int halfWidth,
                           final int length) {
        int len = length + random.nextInt(Math.max(1, length / 3));
        drawManager.fillBox(x - halfWidth, y, halfWidth * 2, len / 2,
                new Color(255, 120, 30, 220));
        drawManager.fillBox(x - halfWidth * 2 / 3, y, halfWidth * 4 / 3,
                len * 3 / 4, new Color(255, 190, 60, 230));
        drawManager.fillBox(x - halfWidth / 3, y, Math.max(2, halfWidth * 2 / 3),
                len, new Color(255, 250, 210, 240));
    }

    /** Full-screen white flash; strength 1 = solid white, 0 = none. */
    private void flash(final float strength) {
        int a = Math.max(0, Math.min(255, (int) (strength * 255)));
        if (a > 0) {
            drawManager.fillBox(0, 0, width, height, new Color(255, 255, 255, a));
        }
    }

    /**
     * Underground hangar backdrop: dark wall panels, two pillars with
     * blinking warning lamps, and hazard stripes along the floor.
     */
    private void drawHangar(final boolean lightsOn) {
        drawManager.fillBox(0, 0, width, height, WALL);
        for (int y = 40; y < height - 57; y += 56) {
            drawManager.fillBox(0, y, width, 2, PANEL_LINE);
        }
        for (int x = 56; x < width; x += 112) {
            drawManager.fillBox(x, 0, 2, height - 57, PANEL_LINE);
        }
        int pillarW = 22;
        boolean blink = lightsOn && (System.currentTimeMillis() / 600) % 2 == 0;
        for (int px : new int[] { 0, width - pillarW }) {
            drawManager.fillBox(px, 0, pillarW, height - 57, PILLAR);
            drawManager.fillBox(px == 0 ? pillarW - 2 : px, 0, 2, height - 57,
                    PILLAR_EDGE);
            for (int ly = 90; ly < height - 80; ly += 120) {
                drawManager.fillBox(px + 7, ly, 8, 5, blink ? LAMP_ON : LAMP_OFF);
            }
        }
        // Hazard stripes under the floor line.
        for (int x = -8; x < width; x += 16) {
            drawManager.fillBox(x, height - 55, 8, 4, HAZARD);
        }
    }

    private void drawInstructions() {
        int y = height / 8 + 60;
        switch (step) {
            case MOVE:
                text("1/4  Move: A/D or LEFT/RIGHT", y);
                hint("The ship is powered up. Get moving!", y + 25);
                break;
            case SHOOT:
                text("2/4  Press SPACE to fire", y);
                hint("Test the ship's cannon.", y + 25);
                break;
            case COLLECT:
                text("3/4  Collect the power cell", y);
                hint("Fly into it to pick it up.", y + 25);
                break;
            case DEFEAT:
                text("4/4  Destroy the security drones  ("
                        + (DRONE_COUNT - dronesLeft()) + "/" + DRONE_COUNT + ")", y);
                hint("They are trying to stop your escape!", y + 25);
                break;
            default:
                text("Tutorial complete!", y);
                hint("Now get out of the hangar!", y + 25);
                if (step == Step.COMPLETE) {
                    text("Press ENTER to launch", y + 75);
                }
                break;
        }
    }

    private void text(final String string, final int y) {
        drawManager.drawCenteredRegularString(this, string, y, TEXT_COLOR);
    }

    private void hint(final String string, final int y) {
        drawManager.drawCenteredRegularString(this, string, y, HINT_COLOR);
    }

    // ------------------------------------------------------------------
    // Tutorial-only entities
    // ------------------------------------------------------------------

    /** Security drone: the simple enemy used only in this tutorial. */
    private static final class Drone extends Entity {
        private final int targetY;
        private final Cooldown animation = Core.getCooldown(400);
        private final Cooldown explosion = Core.getCooldown(EXPLOSION_TIME);
        private boolean exploded;

        Drone(final int x, final int startY, final int targetY) {
            super(x, startY, 12 * 2, 8 * 2, DRONE_COLOR);
            this.targetY = targetY;
            this.spriteType = SpriteType.TutorialDrone1;
        }

        boolean hasArrived() {
            return positionY >= targetY;
        }

        void flyIn() {
            positionY = Math.min(targetY, positionY + DRONE_ENTRY_SPEED);
        }

        void animate() {
            if (!exploded && animation.checkFinished()) {
                animation.reset();
                spriteType = spriteType == SpriteType.TutorialDrone1
                        ? SpriteType.TutorialDrone2 : SpriteType.TutorialDrone1;
            }
        }

        void explode() {
            exploded = true;
            spriteType = SpriteType.Explosion;
            explosion.reset();
        }

        boolean isExploded() {
            return exploded;
        }

        boolean isExplosionFinished() {
            return exploded && explosion.checkFinished();
        }
    }

    /** Power cell pickup: gives the ship rapid fire for the rest of the tutorial. */
    private static final class Pickup extends Entity {
        Pickup(final int x, final int y) {
            super(x, y, 8 * 2, 8 * 2, CELL_COLOR);
            this.spriteType = SpriteType.TutorialCell;
        }
    }
}
