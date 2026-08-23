package hookshot.grapple;

import hookshot.HookshotConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PlayerPullBehavior {
    private PlayerPullBehavior() {
    }

    public static void tick(ServerPlayerEntity player, Vec3d anchorPosition) {
        Vec3d toAnchor = anchorPosition.subtract(player.getPos());

        if (toAnchor.lengthSquared() < 0.25D) {
            return;
        }

        Vec3d pullDirection = toAnchor.normalize();
        Vec3d nextVelocity = player.getVelocity().add(pullDirection.multiply(HookshotConfig.PULL_FORCE));

        if (nextVelocity.length() > HookshotConfig.MAX_SPEED) {
            nextVelocity = nextVelocity.normalize().multiply(HookshotConfig.MAX_SPEED);
        }

        player.setVelocity(nextVelocity);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
    }
}
