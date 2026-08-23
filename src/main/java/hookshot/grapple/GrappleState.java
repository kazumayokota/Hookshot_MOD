package hookshot.grapple;

import java.util.UUID;
import net.minecraft.util.math.Vec3d;

public final class GrappleState {
    private UUID hookUuid;
    private UUID hookedEntityUuid;
    private Vec3d anchorPosition;
    private GrappleMode mode = GrappleMode.NONE;
    private int activeTicks;
    private int fallProtectionTicks;
    private int stuckTicks;
    private double lastDistanceToAnchor = -1.0D;
    private boolean active;

    public UUID getHookUuid() {
        return hookUuid;
    }

    public void setHookUuid(UUID hookUuid) {
        this.hookUuid = hookUuid;
    }

    public UUID getHookedEntityUuid() {
        return hookedEntityUuid;
    }

    public void setHookedEntityUuid(UUID hookedEntityUuid) {
        this.hookedEntityUuid = hookedEntityUuid;
    }

    public Vec3d getAnchorPosition() {
        return anchorPosition;
    }

    public void setAnchorPosition(Vec3d anchorPosition) {
        this.anchorPosition = anchorPosition;
    }

    public GrappleMode getMode() {
        return mode;
    }

    public void setMode(GrappleMode mode) {
        this.mode = mode;
    }

    public int getActiveTicks() {
        return activeTicks;
    }

    public void setActiveTicks(int activeTicks) {
        this.activeTicks = activeTicks;
    }

    public int getFallProtectionTicks() {
        return fallProtectionTicks;
    }

    public void setFallProtectionTicks(int fallProtectionTicks) {
        this.fallProtectionTicks = fallProtectionTicks;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public void setStuckTicks(int stuckTicks) {
        this.stuckTicks = stuckTicks;
    }

    public double getLastDistanceToAnchor() {
        return lastDistanceToAnchor;
    }

    public void setLastDistanceToAnchor(double lastDistanceToAnchor) {
        this.lastDistanceToAnchor = lastDistanceToAnchor;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
