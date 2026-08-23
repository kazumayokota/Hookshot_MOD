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

        Vec3d nextVelocity = target.getVelocity().add(toPlayer.normalize().multiply(HookshotConfig.ENTITY_PULL_FORCE));

        if (nextVelocity.length() > HookshotConfig.ENTITY_MAX_SPEED) {
            nextVelocity = nextVelocity.normalize().multiply(HookshotConfig.ENTITY_MAX_SPEED);
        }

        target.setVelocity(nextVelocity);
        target.velocityModified = true;
        target.fallDistance = 0.0F;
    }

    public static boolean isCloseEnough(ServerPlayerEntity player, Entity target) {
        return player.getPos().squaredDistanceTo(target.getPos()) <= HookshotConfig.GRAPPLE_RELEASE_DISTANCE * HookshotConfig.GRAPPLE_RELEASE_DISTANCE;
    }
}
