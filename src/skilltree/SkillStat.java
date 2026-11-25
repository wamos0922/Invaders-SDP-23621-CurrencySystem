package skilltree;

/**
 * Ship stats a skill node can raise, by a percentage (GOG-88). Nodes with
 * a special effect instead (pierce, shields, ...) have no stat and are
 * recognised by their id.
 *
 * @author GoG - Progression (GOG-70)
 */
public enum SkillStat {
	/** Movement speed. */
	SPEED,
	/** Shots per second. */
	FIRE_RATE,
	/** How fast the ship's bullets fly. */
	BULLET_SPEED,
	/** Size (hitbox) of the ship's bullets. */
	BULLET_SIZE
}
