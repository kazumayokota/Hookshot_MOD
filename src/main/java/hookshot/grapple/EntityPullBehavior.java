package hookshot.grapple;

import hookshot.HookshotConfig;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class EntityPullBehavior {
    private EntityPullBehavior() {
    }

    public static void tick(ServerPlayerEntity player, Entity target) {
        Vec3d toPlayer = player.getPos().subtract(target.getPos());

        if (isCloseEnough(player, target)) {
            return;
        }

        Vec3d pullDirection = toPlayer.normalize();
        Vec3d nextVelocity = target.getVelocity().add(pullDirection.multiply(HookshotConfig.ENTITY_PULL_FORCE));
        nextVelocity = compensateGroundFriction(target, nextVelocity, pullDirection);

        if (nextVelocity.length() > HookshotConfig.ENTITY_MAX_SPEED) {
            nextVelocity = nextVelocity.normalize().multiply(HookshotConfig.ENTITY_MAX_SPEED);
        }

        target.setVelocity(nextVelocity);
        target.velocityModified = true;
        target.fallDistance = 0.0F;
    }

    private static Vec3d compensateGroundFriction(Entity target, Vec3d velocity, Vec3d pullDirection) {
        if (!target.isOnGround()) {
            return velocity;
        }

        Vec3d horizontalPull = new Vec3d(pullDirection.x, 0.0D, pullDirection.z);
        if (horizontalPull.lengthSquared() < 1.0E-7D) {
            return velocity;
        }

        return velocity.add(horizontalPull.normalize().multiply(HookshotConfig.ENTITY_GROUNDED_PULL_FORCE));
    }

    public static boolean isCloseEnough(ServerPlayerEntity player, Entity target) {
        return player.getPos().squaredDistanceTo(target.getPos()) <= HookshotConfig.GRAPPLE_RELEASE_DISTANCE * HookshotConfig.GRAPPLE_RELEASE_DISTANCE;
    }
}
