package entity;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import screen.Screen;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;

/**
 * Groups enemy ships into a formation that moves together.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class EnemyShipFormation implements Iterable<EnemyShip> {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 20;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 100;
	/** Distance between ships. */
	private static final int SEPARATION_DISTANCE = 40;
	/** Proportion of C-type ships. */
	private static final double PROPORTION_C = 0.2;
	/** Proportion of B-type ships. */
	private static final double PROPORTION_B = 0.4;
	/** Lateral speed of the formation. */
	private static final int X_SPEED = 8;
	/** Downwards speed of the formation. */
	private static final int Y_SPEED = 4;
	/** Speed of the bullets shot by the members. */
	private static final int BULLET_SPEED = 4;
	/** Proportion of differences between shooting times. */
	private static final double SHOOTING_VARIANCE = .2;
	/** Margin on the sides of the screen. */
	private static final int SIDE_MARGIN = 20;
	/** Margin on the bottom of the screen. */
	private static final int BOTTOM_MARGIN = 80;
	/** Distance to go down each pass. */
	private static final int DESCENT_DISTANCE = 20;
	/** Minimum speed allowed. */
	private static final int MINIMUM_SPEED = 10;
	/** Vertical range of the diagonal zig-zag, in pixels (Team A1). */
	private static final int DIAGONAL_RANGE = 40;
	/** How far the diagonal formation sinks each time it touches a side. */
	private static final int DIAGONAL_DESCENT = 12;

	/** DrawManager instance. */
	private DrawManager drawManager;
	/** Application logger. */
	private Logger logger;
	/** Screen to draw ships on. */
	private Screen screen;

	/** List of enemy ships forming the formation. */
	private List<List<EnemyShip>> enemyShips;
	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Number of ships in the formation - horizontally. */
	private int nShipsWide;
	/** Number of ships in the formation - vertically. */
	private int nShipsHigh;
	/** Time between shots. */
	private int shootingInterval;
	/** Variance in the time between shots. */
	private int shootingVariance;
	/** Initial ship speed. */
	private int baseSpeed;
	/** Speed of the ships. */
	private int movementSpeed;
	/** Current direction the formation is moving on. */
	private Direction currentDirection;
	/** Direction the formation was moving previously. */
	private Direction previousDirection;
	/** Interval between movements, in frames. */
	private int movementInterval;
	/** Total width of the formation. */
	private int width;
	/** Total height of the formation. */
	private int height;
	/** Position in the x-axis of the upper left corner of the formation. */
	private int positionX;
	/** Position in the y-axis of the upper left corner of the formation. */
	private int positionY;
	/** Width of one ship. */
	private int shipWidth;
	/** Height of one ship. */
	private int shipHeight;
	/** List of ships that are able to shoot. */
	private List<EnemyShip> shooters;
	/** Number of not destroyed ships. */
	private int shipCount;

	/** Comets diving at the player, alive or fading out (GOG-86). */
	private final List<Dive> dives = new ArrayList<Dive>();
	/** Centres where diving Comets exploded, not yet handled by the game. */
	private final List<Point> detonations = new ArrayList<Point>();
	/** Centres where Mites died, not yet handled by the game. */
	private final List<Point> miteBooms = new ArrayList<Point>();
	/** Player centre for aimed shots (GOG-85); negative until known. */
	private int targetX = -1;
	/** Player centre for aimed shots (GOG-85); negative until known. */
	private int targetY = -1;

	/**
	 * A Comet flying in a straight line to where the player was, or an
	 * Orbit Fighter falling straight down (GOG-85).
	 */
	private static final class Dive {
		/** The Comet or Orbit Fighter. */
		private final EnemyShip ship;
		/** True for a falling Orbit Fighter, false for a Comet. */
		private final boolean drop;
		/** Exact position of its centre (sub-pixel). */
		private double centerX;
		/** Exact position of its centre (sub-pixel). */
		private double centerY;
		/** Where it explodes: the player's centre when it dove. */
		private final int targetX;
		/** Where it explodes: the player's centre when it dove. */
		private final int targetY;

		Dive(final EnemyShip ship, final int targetX, final int targetY,
				final boolean drop) {
			this.ship = ship;
			this.drop = drop;
			this.centerX = ship.getPositionX() + ship.getWidth() / 2.0;
			this.centerY = ship.getPositionY() + ship.getHeight() / 2.0;
			this.targetX = targetX;
			this.targetY = targetY;
		}
	}
	/** True if the whole formation moves diagonally (Team A1). */
	private boolean diagonalMovement;
	/** Horizontal step of the diagonal movement: positive is right. */
	private int diagonalStepX;
	/** Vertical step of the diagonal zig-zag: positive is down. */
	private int diagonalStepY;
	/** How far down the zig-zag currently is, from 0 to DIAGONAL_RANGE. */
	private int diagonalOffset;
	/** Pixels the formation still has to sink after touching a side. */
	private int pendingDescent;

	/** Directions the formation can move. */
	private enum Direction {
		/** Movement to the right side of the screen. */
		RIGHT,
		/** Movement to the left side of the screen. */
		LEFT,
		/** Movement to the bottom of the screen. */
		DOWN
	};

	/**
	 * Constructor, sets the initial conditions.
	 * 
	 * @param gameSettings
	 *            Current game settings.
	 */
	public EnemyShipFormation(final GameSettings gameSettings) {
		this(gameSettings, false);
	}

	/**
	 * Act 1 (Earth Escape) row designs, top (row 1, most valuable) to bottom
	 * (row 5, weakest).
	 */
	private static final SpriteType[] EARTH_ROWS = {
			SpriteType.Overseer1, SpriteType.HunterJet1,
			SpriteType.WardenBot1, SpriteType.Crawler1,
			SpriteType.SentryDrone1 };

	/**
	 * Constructor, sets the initial conditions.
	 *
	 * @param gameSettings
	 *            Current game settings.
	 * @param earthAct
	 *            True to use the Act 1 (Earth Escape) enemies. Rows fill from
	 *            the top in design order (Overseer, Hunter Jet, Warden Bot,
	 *            Crawler, Sentry Drone). Act 1 enemies have no special powers,
	 *            so the settings' two-hit enemies are ignored.
	 */
	public EnemyShipFormation(final GameSettings gameSettings,
			final boolean earthAct) {
		this.drawManager = Core.getDrawManager();
		this.logger = Core.getLogger();
		this.enemyShips = new ArrayList<List<EnemyShip>>();
		this.currentDirection = Direction.RIGHT;
		this.movementInterval = 0;
		this.diagonalMovement = gameSettings.isDiagonalMovement();
		this.diagonalStepX = X_SPEED;
		this.diagonalStepY = Y_SPEED;
		this.diagonalOffset = 0;
		this.pendingDescent = 0;
		this.nShipsWide = gameSettings.getFormationWidth();
		this.nShipsHigh = gameSettings.getFormationHeight();
		this.shootingInterval = gameSettings.getShootingFrecuency();
		this.shootingVariance = (int) (gameSettings.getShootingFrecuency()
				* SHOOTING_VARIANCE);
		this.baseSpeed = gameSettings.getBaseSpeed();
		this.movementSpeed = this.baseSpeed;
		this.positionX = INIT_POS_X;
		this.positionY = INIT_POS_Y;
		this.shooters = new ArrayList<EnemyShip>();
		SpriteType spriteType;

		this.logger.info("Initializing " + nShipsWide + "x" + nShipsHigh
				+ " ship formation in (" + positionX + "," + positionY + ")");

		// Each sub-list is a column on the formation.
		for (int i = 0; i < this.nShipsWide; i++)
			this.enemyShips.add(new ArrayList<EnemyShip>());

		// Sample without replacement so every position is equally eligible.
		List<Integer> positions = new ArrayList<Integer>();
		for (int i = 0; i < this.nShipsWide * this.nShipsHigh; i++)
			positions.add(i);
		Collections.shuffle(positions);
		int twoHitCount = earthAct ? 0 : gameSettings.getTwoHitEnemyCount();
		Set<Integer> elitePositions = new HashSet<Integer>(
				positions.subList(0, twoHitCount));

		for (List<EnemyShip> column : this.enemyShips) {
			for (int i = 0; i < this.nShipsHigh; i++) {
				int positionIndex = i * this.nShipsWide
						+ this.enemyShips.indexOf(column);
				int health = elitePositions.contains(positionIndex) ? 2 : 1;
				int shipX = (SEPARATION_DISTANCE
						* this.enemyShips.indexOf(column)) + positionX;
				int shipY = (SEPARATION_DISTANCE * i) + positionY;

				// Story levels can set a type per row (GOG-84).
				EnemyType type = gameSettings.getRowType(i);
				// Mites never sit next to each other, so one boom can't chain
				// (GOG-86): Mite rows alternate with Star Eaters in a
				// checkerboard, so Mites only meet diagonally.
				if (type == EnemyType.MITE
						&& (i + this.enemyShips.indexOf(column)) % 2 == 1)
					type = EnemyType.MITE_ROW_FILLER;
				if (type.hasOwnSprites()) {
					column.add(new EnemyShip(shipX, shipY, type, health));
					this.shipCount++;
					continue;
				}

				if (earthAct)
					spriteType = EARTH_ROWS[Math.min(i, EARTH_ROWS.length - 1)];
				else if (i / (float) this.nShipsHigh < PROPORTION_C)
					spriteType = SpriteType.EnemyShipC1;
				else if (i / (float) this.nShipsHigh < PROPORTION_B
						+ PROPORTION_C)
					spriteType = SpriteType.EnemyShipB1;
				else
					spriteType = SpriteType.EnemyShipA1;

				column.add(new EnemyShip(shipX, shipY, spriteType, health));
				this.shipCount++;
			}
		}

		this.shipWidth = this.enemyShips.get(0).get(0).getWidth();
		this.shipHeight = this.enemyShips.get(0).get(0).getHeight();

		this.width = (this.nShipsWide - 1) * SEPARATION_DISTANCE
				+ this.shipWidth;
		this.height = (this.nShipsHigh - 1) * SEPARATION_DISTANCE
				+ this.shipHeight;

		for (List<EnemyShip> column : this.enemyShips)
			this.shooters.add(column.get(column.size() - 1));
	}

	/**
	 * Associates the formation to a given screen.
	 * 
	 * @param newScreen
	 *            Screen to attach.
	 */
	public final void attach(final Screen newScreen) {
		screen = newScreen;
	}

	/**
	 * Draws every individual component of the formation.
	 */
	public final void draw() {
		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				drawManager.drawEntity(enemyShip, enemyShip.getPositionX(),
						enemyShip.getPositionY());
		for (Dive dive : this.dives)
			drawManager.drawEntity(dive.ship, dive.ship.getPositionX(),
					dive.ship.getPositionY());
	}

	/**
	 * Comet kamikaze (GOG-86): the first player bullet that hits a Comet
	 * doesn't kill it. Instead it leaves the formation, locks onto the
	 * player's centre and dives there in a straight line (see
	 * {@link #updateDives()}). A second hit while it dives kills it.
	 *
	 * @param enemy
	 *            Enemy the player's bullet hit.
	 * @param target
	 *            Player ship.
	 * @return True if the hit started a dive (the enemy is a Comet still in
	 *         the formation); false for every other case.
	 */
	public final boolean startDive(final EnemyShip enemy, final Entity target) {
		if (enemy.isDestroyed() || isDiving(enemy))
			return false;
		if (enemy.getType() == EnemyType.ORBIT_FIGHTER)
			return startDrop(enemy);
		if (enemy.getType() != EnemyType.COMET)
			return false;
		detach(enemy);
		this.dives.add(new Dive(enemy,
				target.getPositionX() + target.getWidth() / 2,
				target.getPositionY() + target.getHeight() / 2, false));
		this.logger.info("A Comet was hit and dives at the player.");
		return true;
	}

	/**
	 * Orbit Fighter drop (GOG-85): the first hit sends it out of the
	 * formation, falling straight down until it leaves the screen. The game
	 * checks it against the player with {@link #takeDropHits(Entity)}.
	 */
	private boolean startDrop(final EnemyShip enemy) {
		detach(enemy);
		int bottom = this.screen != null ? this.screen.getHeight() : 1000;
		this.dives.add(new Dive(enemy,
				enemy.getPositionX() + enemy.getWidth() / 2,
				bottom + enemy.getHeight(), true));
		this.logger.info("An Orbit Fighter was hit and drops.");
		return true;
	}

	/**
	 * Falling Orbit Fighters that touch the player crash into it (GOG-85):
	 * each one that overlaps the ship is destroyed. The game takes a life
	 * for each (a shield item can block it).
	 *
	 * @param player
	 *            Player ship.
	 * @return How many falling Orbit Fighters hit the ship.
	 */
	public final int takeDropHits(final Entity player) {
		int hits = 0;
		for (Dive dive : this.dives) {
			EnemyShip ship = dive.ship;
			if (!dive.drop || ship.isDestroyed())
				continue;
			boolean overlap = ship.getPositionX() < player.getPositionX()
					+ player.getWidth()
					&& player.getPositionX() < ship.getPositionX()
							+ ship.getWidth()
					&& ship.getPositionY() < player.getPositionY()
							+ player.getHeight()
					&& player.getPositionY() < ship.getPositionY()
							+ ship.getHeight();
			if (!overlap)
				continue;
			ship.destroy();
			this.shipCount--;
			hits++;
		}
		return hits;
	}

	/**
	 * @param enemy
	 *            Enemy to check.
	 * @return True if it is an Orbit Fighter falling out of the formation
	 *         (GOG-85).
	 */
	public final boolean isDropping(final EnemyShip enemy) {
		for (Dive dive : this.dives)
			if (dive.ship == enemy && dive.drop)
				return true;
		return false;
	}

	/**
	 * @param enemy
	 *            Enemy to check.
	 * @return True if it is a Comet diving at the player (GOG-86).
	 */
	public final boolean isDiving(final EnemyShip enemy) {
		for (Dive dive : this.dives)
			if (dive.ship == enemy)
				return true;
		return false;
	}

	/**
	 * Moves diving Comets, every frame (GOG-86). When one reaches the spot
	 * it locked onto it explodes; the game reads the spot with
	 * {@link #takeDetonations()} and hurts the player if the ship is still
	 * within {@link EnemyType#COMET_BLAST_RADIUS}.
	 */
	public final void updateDives() {
		java.util.Iterator<Dive> it = this.dives.iterator();
		while (it.hasNext()) {
			Dive dive = it.next();
			if (dive.ship.isDestroyed()) {
				dive.ship.update();
				if (dive.ship.isFadeFinished())
					it.remove();
				continue;
			}
			double speed = dive.drop ? EnemyType.FIGHTER_DROP_SPEED
					: EnemyType.COMET_DIVE_SPEED;
			double dx = dive.targetX - dive.centerX;
			double dy = dive.targetY - dive.centerY;
			double distance = Math.sqrt(dx * dx + dy * dy);
			if (distance <= speed) {
				dive.centerX = dive.targetX;
				dive.centerY = dive.targetY;
				placeDive(dive);
				dive.ship.destroy();
				this.shipCount--;
				// A Comet explodes there; an Orbit Fighter just fell off
				// the bottom of the screen (GOG-85).
				if (!dive.drop)
					this.detonations.add(new Point(dive.targetX, dive.targetY));
				continue;
			}
			dive.centerX += dx / distance * speed;
			dive.centerY += dy / distance * speed;
			placeDive(dive);
			dive.ship.update();
		}
	}

	/** Moves a diving Comet's sprite to its exact centre. */
	private void placeDive(final Dive dive) {
		dive.ship.setPositionX((int) Math.round(dive.centerX
				- dive.ship.getWidth() / 2.0));
		dive.ship.setPositionY((int) Math.round(dive.centerY
				- dive.ship.getHeight() / 2.0));
	}

	/**
	 * Takes a living ship out of its column without destroying it (a Comet
	 * starting its dive). It still counts as a remaining enemy.
	 */
	private void detach(final EnemyShip ship) {
		List<EnemyShip> ownColumn = null;
		for (List<EnemyShip> column : this.enemyShips)
			if (column.contains(ship))
				ownColumn = column;
		if (ownColumn == null)
			return;
		ownColumn.remove(ship);
		int shooterIndex = this.shooters.indexOf(ship);
		if (shooterIndex >= 0) {
			EnemyShip nextShooter = getNextShooter(ownColumn);
			if (nextShooter != null)
				this.shooters.set(shooterIndex, nextShooter);
			else
				this.shooters.remove(shooterIndex);
		}
	}

	/**
	 * @return Centres where diving Comets exploded since the last call
	 *         (GOG-86). The list is cleared.
	 */
	public final List<Point> takeDetonations() {
		List<Point> taken = new ArrayList<Point>(this.detonations);
		this.detonations.clear();
		return taken;
	}

	/**
	 * @return Centres where Mites died since the last call (GOG-86). The
	 *         game sprays shrapnel and hurts the neighbours. The list is
	 *         cleared.
	 */
	public final List<Point> takeMiteBooms() {
		List<Point> taken = new ArrayList<Point>(this.miteBooms);
		this.miteBooms.clear();
		return taken;
	}

	/** @return True while a Comet is diving (GOG-86). */
	public final boolean isCometDiving() {
		for (Dive dive : this.dives)
			if (!dive.drop && !dive.ship.isDestroyed())
				return true;
		return false;
	}

	/**
	 * Updates the position of the ships.
	 */
	public final void update() {
		if(this.shootingCooldown == null) {
			this.shootingCooldown = Core.getVariableCooldown(shootingInterval,
					shootingVariance);
			this.shootingCooldown.reset();
		}
		
		cleanUp();

		int movementX = 0;
		int movementY = 0;
		double remainingProportion = (double) this.shipCount
				/ (this.nShipsHigh * this.nShipsWide);
		this.movementSpeed = (int) (Math.pow(remainingProportion, 2)
				* this.baseSpeed);
		this.movementSpeed += MINIMUM_SPEED;
		
		movementInterval++;
		if (movementInterval >= this.movementSpeed) {
			movementInterval = 0;

			boolean isAtBottom = positionY
					+ this.height > screen.getHeight() - BOTTOM_MARGIN;
			boolean isAtRightSide = positionX
					+ this.width >= screen.getWidth() - SIDE_MARGIN;
			boolean isAtLeftSide = positionX <= SIDE_MARGIN;
			boolean isAtHorizontalAltitude = positionY % DESCENT_DISTANCE == 0;

			if (this.diagonalMovement) {
				advanceDiagonal(isAtLeftSide, isAtRightSide, isAtBottom);
			} else if (currentDirection == Direction.DOWN) {
				if (isAtHorizontalAltitude)
					if (previousDirection == Direction.RIGHT) {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 1");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 2");
					}
			} else if (currentDirection == Direction.LEFT) {
				if (isAtLeftSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 3");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 4");
					}
			} else {
				if (isAtRightSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 5");
					} else {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 6");
					}
			}

			if (this.diagonalMovement) {
				movementX = this.diagonalStepX;
				movementY = this.nextDiagonalStepY(isAtBottom);
			} 
			else if (currentDirection == Direction.RIGHT)
				movementX = X_SPEED;
			else if (currentDirection == Direction.LEFT)
				movementX = -X_SPEED;
			else
				movementY = Y_SPEED;

			positionX += movementX;
			positionY += movementY;

			// Cleans explosions.
			List<EnemyShip> destroyed;
			for (List<EnemyShip> column : this.enemyShips) {
				destroyed = new ArrayList<EnemyShip>();
				for (EnemyShip ship : column) {
					if (ship != null && ship.isFadeFinished()) {
						destroyed.add(ship);
						this.logger.info("Removed enemy "
								+ column.indexOf(ship) + " from column "
								+ this.enemyShips.indexOf(column));
					}
				}
				column.removeAll(destroyed);
			}

			for (List<EnemyShip> column : this.enemyShips)
				for (EnemyShip enemyShip : column) {
					enemyShip.move(movementX, movementY);
					enemyShip.update();
				}
		}
	}
	/**
	 * Diagonal movement (Team A1): the whole formation keeps its spacing and
	 * moves sideways and up and down at the same time. Touching a side
	 * reverses the horizontal direction and makes the formation sink a bit,
	 * like the classic movement does.
	 * 
	 * @param isAtLeftSide
	 *            True if the formation touches the left margin.
	 * @param isAtRightSide
	 *            True if the formation touches the right margin.
	 * @param isAtBottom
	 *            True if the formation reached the lower boundary.
	 */
	private void advanceDiagonal(final boolean isAtLeftSide,
			final boolean isAtRightSide, final boolean isAtBottom) {
		if (isAtLeftSide && this.diagonalStepX < 0) {
			this.diagonalStepX = X_SPEED;
			if (!isAtBottom)
				this.pendingDescent += DIAGONAL_DESCENT;
		} else if (isAtRightSide && this.diagonalStepX > 0) {
			this.diagonalStepX = -X_SPEED;
			if (!isAtBottom)
				this.pendingDescent += DIAGONAL_DESCENT;
		}
	}

	/**
	 * Vertical step of the diagonal movement: a zig-zag between the top of
	 * the range and DIAGONAL_RANGE pixels below it, plus the sinking after
	 * touching a side. The formation never moves down once it is at the
	 * lower boundary.
	 * 
	 * @param isAtBottom
	 *            True if the formation reached the lower boundary.
	 * @return Pixels to move down, negative to move up.
	 */
	private int nextDiagonalStepY(final boolean isAtBottom) {
		if (isAtBottom) {
			this.pendingDescent = 0;
			return 0;
		}
		int nextOffset = this.diagonalOffset + this.diagonalStepY;
		if (nextOffset > DIAGONAL_RANGE || nextOffset < 0)
			this.diagonalStepY = -this.diagonalStepY;
		this.diagonalOffset += this.diagonalStepY;
		int stepY = this.diagonalStepY;
		if (this.pendingDescent > 0) {
			stepY += Y_SPEED;
			this.pendingDescent -= Y_SPEED;
		}
		return stepY;
	}
	
	/**
	 * Removes destroyed ships from the formation. Used by the game over
	 * sequence, while the formation is no longer updated.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 */
	public final void removeDestroyed() {
		List<EnemyShip> destroyed;
		for (List<EnemyShip> column : this.enemyShips) {
			destroyed = new ArrayList<EnemyShip>();
			for (EnemyShip ship : column) {
				if (ship != null && ship.isDestroyed()) {
					destroyed.add(ship);
					this.logger.info("Removed enemy "
							+ column.indexOf(ship) + " from column "
							+ this.enemyShips.indexOf(column));
				}
			}
			column.removeAll(destroyed);
		}
	}

	/**
	 * Takes every remaining ship in the lowest row out of the formation, so
	 * the game over sequence can animate them on its own.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 *
	 * @return Ships of the lowest row, empty if no ships were left.
	 */
	public final List<EnemyShip> removeBottomRow() {
		List<EnemyShip> bottomRow = new ArrayList<EnemyShip>();
		int bottomY = Integer.MIN_VALUE;
		for (EnemyShip enemyShip : this)
			if (!enemyShip.isDestroyed())
				bottomY = Math.max(bottomY, enemyShip.getPositionY());
		if (bottomY == Integer.MIN_VALUE)
			return bottomRow;

		for (EnemyShip enemyShip : this)
			if (!enemyShip.isDestroyed()
					&& enemyShip.getPositionY() == bottomY)
				bottomRow.add(enemyShip);
		for (List<EnemyShip> column : this.enemyShips)
			column.removeAll(bottomRow);
		this.dives.removeIf(dive -> bottomRow.contains(dive.ship));
		this.shipCount -= bottomRow.size();
		return bottomRow;
	}

	/**
	 * Cleans empty columns, adjusts the width and height of the formation.
	 */
	private void cleanUp() {
		Set<Integer> emptyColumns = new HashSet<Integer>();
		int maxColumn = 0;
		int minPositionY = Integer.MAX_VALUE;
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				// Height of this column
				int columnSize = column.get(column.size() - 1).positionY
						- this.positionY + this.shipHeight;
				maxColumn = Math.max(maxColumn, columnSize);
				minPositionY = Math.min(minPositionY, column.get(0)
						.getPositionY());
			} else {
				// Empty column, we remove it.
				emptyColumns.add(this.enemyShips.indexOf(column));
			}
		}
		for (int index : emptyColumns) {
			this.enemyShips.remove(index);
			logger.info("Removed column " + index);
		}

		int leftMostPoint = 0;
		int rightMostPoint = 0;
		
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				if (leftMostPoint == 0)
					leftMostPoint = column.get(0).getPositionX();
				rightMostPoint = column.get(0).getPositionX();
			}
		}

		this.width = rightMostPoint - leftMostPoint + this.shipWidth;
		this.height = maxColumn;

		this.positionX = leftMostPoint;
		this.positionY = minPositionY;
	}

	/**
	 * Shoots a bullet downwards.
	 * 
	 * @param bullets
	 *            Bullets set to add the bullet being shot.
	 */
	public final void shoot(final Set<Bullet> bullets) {
		// For now, only ships in the bottom row are able to shoot.
		int index = (int) (Math.random() * this.shooters.size());
		EnemyShip shooter = this.shooters.get(index);

		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();
			fireShot(shooter, bullets, this.targetX, this.targetY);
		}
	}

	/**
	 * Tells the formation where the player is, for aimed shots (Halo
	 * Lancer sniper and Hex Sentinel reflections, GOG-85).
	 *
	 * @param x
	 *            Player ship centre.
	 * @param y
	 *            Player ship centre.
	 */
	public final void setTarget(final int x, final int y) {
		this.targetX = x;
		this.targetY = y;
	}

	/**
	 * Hex Sentinel reflecting shield (GOG-85): the player bullet that broke
	 * the shield comes back down as an enemy bullet aimed at the player.
	 *
	 * @param hitBy
	 *            Player bullet the shield blocked.
	 * @param bullets
	 *            Where the reflected bullet is added.
	 */
	public final void reflectShot(final Bullet hitBy,
			final Set<Bullet> bullets) {
		bullets.add(aimedBullet(hitBy.getPositionX() + hitBy.getWidth() / 2,
				hitBy.getPositionY() + hitBy.getHeight(), this.targetX,
				this.targetY, EnemyType.REFLECT_BULLET_SPEED));
	}

	/**
	 * @return An enemy bullet from (fromX, fromY) towards (toX, toY) at
	 *         the given total speed, always moving down. With no target
	 *         below it, the bullet goes straight down.
	 */
	private static Bullet aimedBullet(final int fromX, final int fromY,
			final int toX, final int toY, final int speed) {
		int speedX = 0;
		int speedY = speed;
		if (toX >= 0 && toY > fromY) {
			double dx = toX - fromX;
			double dy = toY - fromY;
			double length = Math.sqrt(dx * dx + dy * dy);
			speedX = (int) Math.round(dx / length * speed);
			speedY = Math.max(1, (int) Math.round(dy / length * speed));
		}
		Bullet bullet = BulletPool.getBullet(fromX, fromY, speedY);
		bullet.setSpeedX(speedX);
		return bullet;
	}

	/**
	 * Fires one shot from an enemy, using its type's ability (GOG-84): Jelly
	 * fires two bullets in a cone, Polar a freeze bullet, everyone else one
	 * straight bullet. Also used by the Tempest Queen's mobs (GOG-75).
	 *
	 * @param shooter
	 *            Enemy that shoots.
	 * @param bullets
	 *            Bullets in play, the new ones are added here.
	 */
	public static void fireShot(final EnemyShip shooter,
			final Set<Bullet> bullets) {
		fireShot(shooter, bullets, -1, -1);
	}

	/**
	 * Fires one shot from an enemy, aiming where the type needs it: a Halo
	 * Lancer's sniper bullet is fast and goes straight at the player
	 * (GOG-85). Without a target it fires straight down.
	 *
	 * @param shooter
	 *            Enemy that shoots.
	 * @param bullets
	 *            Bullets in play, the new ones are added here.
	 * @param targetX
	 *            Player centre, negative if unknown.
	 * @param targetY
	 *            Player centre, negative if unknown.
	 */
	public static void fireShot(final EnemyShip shooter,
			final Set<Bullet> bullets, final int targetX, final int targetY) {
		int centerX = shooter.getPositionX() + shooter.width / 2;
		int y = shooter.getPositionY();
		switch (shooter.getType()) {
		case HALO_LANCER:
			// Sniper: fast and aimed at the player, no warning (GOG-85).
			bullets.add(aimedBullet(centerX, y, targetX, targetY,
					EnemyType.SNIPER_BULLET_SPEED));
			break;
		case JELLY:
			// Double shot: two bullets spreading in a cone (GOG-84).
			Bullet left = BulletPool.getBullet(centerX, y, BULLET_SPEED);
			left.setSpeedX(-EnemyType.DOUBLE_SHOT_SPREAD);
			bullets.add(left);
			Bullet right = BulletPool.getBullet(centerX, y, BULLET_SPEED);
			right.setSpeedX(EnemyType.DOUBLE_SHOT_SPREAD);
			bullets.add(right);
			break;
		case POLAR:
			// Freeze bullet: locks the ship instead of taking a life.
			Bullet freezeBullet = BulletPool.getBullet(centerX, y,
					BULLET_SPEED);
			freezeBullet.setFreezing(true);
			bullets.add(freezeBullet);
			break;
		case STINGER:
			// Vulnerable bullet: leaves the player at 1 life (GOG-86).
			Bullet vulnerableBullet = BulletPool.getBullet(centerX, y,
					BULLET_SPEED);
			vulnerableBullet.setVulnerable(true);
			bullets.add(vulnerableBullet);
			break;
		default:
			bullets.add(BulletPool.getBullet(centerX, y, BULLET_SPEED));
			break;
		}
	}

	/**
	 * Destroys a ship.
	 * 
	 * @param destroyedShip
	 *            Ship to be destroyed.
	 */
	public final void destroy(final EnemyShip destroyedShip) {
		// A Mite that dies booms (GOG-86).
		if (destroyedShip.getType() == EnemyType.MITE
				&& !destroyedShip.isDestroyed())
			this.miteBooms.add(new Point(
					destroyedShip.getPositionX() + destroyedShip.getWidth() / 2,
					destroyedShip.getPositionY() + destroyedShip.getHeight() / 2));

		// A diving Comet is no longer in a column (GOG-86).
		for (Dive dive : this.dives)
			if (dive.ship == destroyedShip) {
				if (!destroyedShip.isDestroyed()) {
					destroyedShip.destroy();
					this.shipCount--;
				}
				return;
			}

		for (List<EnemyShip> column : this.enemyShips)
			for (int i = 0; i < column.size(); i++)
				if (column.get(i).equals(destroyedShip)) {
					column.get(i).destroy();
					this.logger.info("Destroyed ship in ("
							+ this.enemyShips.indexOf(column) + "," + i + ")");
				}

		// Updates the list of ships that can shoot the player.
		if (this.shooters.contains(destroyedShip)) {
			int destroyedShipIndex = this.shooters.indexOf(destroyedShip);
			int destroyedShipColumnIndex = -1;

			for (List<EnemyShip> column : this.enemyShips)
				if (column.contains(destroyedShip)) {
					destroyedShipColumnIndex = this.enemyShips.indexOf(column);
					break;
				}

			EnemyShip nextShooter = getNextShooter(this.enemyShips
					.get(destroyedShipColumnIndex));

			if (nextShooter != null)
				this.shooters.set(destroyedShipIndex, nextShooter);
			else {
				this.shooters.remove(destroyedShipIndex);
				this.logger.info("Shooters list reduced to "
						+ this.shooters.size() + " members.");
			}
		}

		this.shipCount--;
	}

	/**
	 * Gets the ship on a given column that will be in charge of shooting.
	 * 
	 * @param column
	 *            Column to search.
	 * @return New shooter ship.
	 */
	public final EnemyShip getNextShooter(final List<EnemyShip> column) {
		Iterator<EnemyShip> iterator = column.iterator();
		EnemyShip nextShooter = null;
		while (iterator.hasNext()) {
			EnemyShip checkShip = iterator.next();
			if (checkShip != null && !checkShip.isDestroyed())
				nextShooter = checkShip;
		}

		return nextShooter;
	}

	/**
	 * Returns an iterator over the ships in the formation.
	 * 
	 * @return Iterator over the enemy ships.
	 */
	@Override
	public final Iterator<EnemyShip> iterator() {
		Set<EnemyShip> enemyShipsList = new HashSet<EnemyShip>();

		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				enemyShipsList.add(enemyShip);
		for (Dive dive : this.dives)
			enemyShipsList.add(dive.ship);

		return enemyShipsList.iterator();
	}

	/**
	 * Checks if there are any ships remaining.
	 * 
	 * @return True when all ships have been destroyed.
	 */
	public final boolean isEmpty() {
		return this.shipCount <= 0;
	}
}
