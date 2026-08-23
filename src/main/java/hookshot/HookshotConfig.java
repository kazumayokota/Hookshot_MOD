package hookshot;

public final class HookshotConfig {
    public static final double MAX_RANGE = 80.0D;
    public static final int MAX_GRAPPLE_TICKS = 100;
    public static final int FALL_PROTECTION_TICKS = 100;
    public static final double PULL_FORCE = 0.16D;
    public static final double ENTITY_PULL_FORCE = 0.18D;
    public static final double GROUNDED_PULL_FORCE = 0.12D;
    public static final double ENTITY_GROUNDED_PULL_FORCE = 0.16D;
    public static final double SIDE_FORCE = 0.04D;
    public static final double LOOK_SWING_FORCE = 0.035D;
    public static final double JUMP_SWING_FORCE = 0.09D;
    public static final double SWING_JUMP_FORCE = 0.32D;
    public static final double SWING_JUMP_TANGENTIAL_BOOST = 0.18D;
    public static final double MAX_SPEED = 1.8D;
    public static final double ENTITY_MAX_SPEED = 1.5D;
    public static final double GRAPPLE_GRAVITY_DAMPING = 0.10D;
    public static final double GRAPPLE_MAX_DOWNWARD_SPEED = -0.25D;
    public static final double GRAPPLE_RELEASE_DISTANCE = 2.0D;
    public static final double GRAPPLE_VIEW_RELEASE_DEGREES = 75.0D;
    public static final double SWING_LOOK_ALIGNMENT = 0.82D;
    public static final double SWING_MIN_TANGENTIAL_SPEED = 0.08D;
    public static final double SWING_DISTANCE_REFERENCE = 30.0D;
    public static final double SWING_MAX_DISTANCE_SCALE = 3.0D;
    public static final int GRAPPLE_STUCK_TICKS = 7;
    public static final double GRAPPLE_MIN_PROGRESS_PER_TICK = 0.06D;
    public static final double GRAPPLE_STUCK_MAX_SPEED = 0.18D;
    public static final double HOOK_RETURN_SPEED = 4.0D;
    public static final double HOOK_RETURN_FINISH_DISTANCE = 1.2D;
    public static final double INSTANT_HIT_DISTANCE = 30.0D;

    private HookshotConfig() {
    }
}
