package hookshot.grapple;

import hookshot.HookshotConfig;
import hookshot.network.SwingInputTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class SwingPhysics {
    private static final Vec3d UP = new Vec3d(0.0D, 1.0D, 0.0D);
    private static final double INPUT_DEAD_ZONE = 0.05D;

    private SwingPhysics() {
    }

    public static Vec3d apply(ServerPlayerEntity player, Vec3d anchorPosition, Vec3d velocity) {
        Vec3d radialToAnchor = anchorPosition.subtract(player.getPos());
        if (radialToAnchor.lengthSquared() < 1.0E-7D) {
            return velocity;
        }

        Vec3d radial = radialToAnchor.normalize();
        Vec3d tangent = getHorizontalTangent(radial);
        if (tangent.lengthSquared() < 1.0E-7D) {
            return velocity;
        }

        Vec3d nextVelocity = velocity;
        double sidewaysInput = SwingInputTracker.getSidewaysInput(player);

        if (Math.abs(sidewaysInput) > INPUT_DEAD_ZONE) {
            nextVelocity = nextVelocity.add(tangent.multiply(sidewaysInput * HookshotConfig.SIDE_FORCE));
        }

        if (isLookingTowardAnchor(player, radial)) {
            Vec3d tangentialVelocity = getTangentialVelocity(nextVelocity, radial);
            if (tangentialVelocity.lengthSquared() >= HookshotConfig.SWING_MIN_TANGENTIAL_SPEED * HookshotConfig.SWING_MIN_TANGENTIAL_SPEED) {
                nextVelocity = nextVelocity.add(tangentialVelocity.normalize().multiply(HookshotConfig.LOOK_SWING_FORCE));
            }
        }

        if (SwingInputTracker.consumeJumpPressed(player)) {
            nextVelocity = applyJump(player, radial, tangent, nextVelocity);
        }

        return nextVelocity;
    }

    public static boolean hasSwingIntent(ServerPlayerEntity player, Vec3d anchorPosition) {
        if (Math.abs(SwingInputTracker.getSidewaysInput(player)) > INPUT_DEAD_ZONE) {
            return true;
        }

        Vec3d radialToAnchor = anchorPosition.subtract(player.getPos());
        if (radialToAnchor.lengthSquared() < 1.0E-7D) {
            return false;
        }

        Vec3d radial = radialToAnchor.normalize();
        Vec3d tangentialVelocity = getTangentialVelocity(player.getVelocity(), radial);
        return isLookingTowardAnchor(player, radial)
                && tangentialVelocity.lengthSquared() >= HookshotConfig.SWING_MIN_TANGENTIAL_SPEED * HookshotConfig.SWING_MIN_TANGENTIAL_SPEED;
    }

    private static Vec3d getHorizontalTangent(Vec3d radial) {
        Vec3d tangent = radial.crossProduct(UP);
        return tangent.lengthSquared() < 1.0E-7D ? Vec3d.ZERO : tangent.normalize();
    }

    private static Vec3d getTangentialVelocity(Vec3d velocity, Vec3d radial) {
        return velocity.subtract(radial.multiply(velocity.dotProduct(radial)));
    }

    private static Vec3d applyJump(ServerPlayerEntity player, Vec3d radial, Vec3d tangent, Vec3d velocity) {
        Vec3d tangentialVelocity = getTangentialVelocity(velocity, radial);
        Vec3d boostDirection = tangentialVelocity.lengthSquared() >= 1.0E-7D
                ? tangentialVelocity.normalize()
                : getLookBasedTangent(player, radial, tangent);

        return velocity
                .add(UP.multiply(HookshotConfig.SWING_JUMP_FORCE))
                .add(boostDirection.multiply(HookshotConfig.SWING_JUMP_TANGENTIAL_BOOST));
    }

    private static Vec3d getLookBasedTangent(ServerPlayerEntity player, Vec3d radial, Vec3d fallbackTangent) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d lookTangent = look.subtract(radial.multiply(look.dotProduct(radial)));

        if (lookTangent.lengthSquared() >= 1.0E-7D) {
            return lookTangent.normalize();
        }

        return fallbackTangent;
    }

    private static boolean isLookingTowardAnchor(ServerPlayerEntity player, Vec3d radial) {
        double dot = player.getRotationVec(1.0F).normalize().dotProduct(radial);
        return dot >= HookshotConfig.SWING_LOOK_ALIGNMENT;
    }
}
