package hookshot.grapple;

import hookshot.HookshotConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PlayerPullBehavior {
    private PlayerPullBehavior() {
    }

    public static void tick(ServerPlayerEntity player, Vec3d anchorPosition) {
        Vec3d toAnchor = anchorPosition.subtract(player.getPos());

        if (isCloseEnough(player, anchorPosition)) {
            return;
        }

        Vec3d pullDirection = toAnchor.normalize();
        Vec3d currentVelocity = dampGravity(player.getVelocity());
        Vec3d nextVelocity = currentVelocity.add(pullDirection.multiply(HookshotConfig.PULL_FORCE));

        if (nextVelocity.length() > HookshotConfig.MAX_SPEED) {
            nextVelocity = nextVelocity.normalize().multiply(HookshotConfig.MAX_SPEED);
        }

        player.setVelocity(nextVelocity);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
    }

    private static Vec3d dampGravity(Vec3d velocity) {
        if (velocity.y >= 0.0D) {
            return velocity;
        }

        double dampedY = velocity.y * HookshotConfig.GRAPPLE_GRAVITY_DAMPING;
        return new Vec3d(velocity.x, Math.max(dampedY, HookshotConfig.GRAPPLE_MAX_DOWNWARD_SPEED), velocity.z);
    }

    public static boolean isCloseEnough(ServerPlayerEntity player, Vec3d anchorPosition) {
        return player.getPos().squaredDistanceTo(anchorPosition) <= HookshotConfig.GRAPPLE_RELEASE_DISTANCE * HookshotConfig.GRAPPLE_RELEASE_DISTANCE;
    }
}
