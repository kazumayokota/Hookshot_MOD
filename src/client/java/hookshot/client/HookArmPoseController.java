package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import hookshot.entity.HookState;
import hookshot.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class HookArmPoseController {
    private HookArmPoseController() {
    }

    public static void apply(LivingEntity entity, ModelPart rightArm, ModelPart leftArm) {
        HookProjectileEntity hook = findActiveHook(entity);

        if (hook != null) {
            applyAim(entity, hook.getAimDirection(), hook.getSourceArm(entity), rightArm, leftArm);
            return;
        }

        HeldHookshot heldHookshot = getHeldHookshot(entity);

        if (heldHookshot == null) {
            return;
        }

        applyAim(entity, entity.getRotationVec(1.0F), heldHookshot.arm(), rightArm, leftArm);
    }

    private static void applyAim(LivingEntity entity, Vec3d aim, Arm sourceArm, ModelPart rightArm, ModelPart leftArm) {
        ModelPart arm = sourceArm == Arm.RIGHT ? rightArm : leftArm;
        float aimYaw = (float) (MathHelper.atan2(aim.z, aim.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
        float relativeYaw = MathHelper.wrapDegrees(aimYaw - entity.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        float pitch = (float) (-(MathHelper.atan2(aim.y, horizontal) * MathHelper.DEGREES_PER_RADIAN)) * MathHelper.RADIANS_PER_DEGREE;

        arm.pitch = pitch - MathHelper.HALF_PI;
        arm.yaw = relativeYaw;
        arm.roll = 0.0F;
    }

    private static HookProjectileEntity findActiveHook(LivingEntity owner) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null) {
            return null;
        }

        HookProjectileEntity candidate = null;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof HookProjectileEntity hook)
                    || hook.getHookState() == HookState.REMOVED
                    || hook.getHookState() == HookState.RETURNING) {
                continue;
            }

            Entity hookOwner = hook.getOwner();
            if (hookOwner != null && hookOwner.getUuid().equals(owner.getUuid())) {
                candidate = hook;
            }
        }

        return candidate;
    }

    private static HeldHookshot getHeldHookshot(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return null;
        }

        if (player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.HOOKSHOT)) {
            return new HeldHookshot(player.getMainArm());
        }

        if (player.getStackInHand(Hand.OFF_HAND).isOf(ModItems.HOOKSHOT)) {
            return new HeldHookshot(player.getMainArm().getOpposite());
        }

        return null;
    }

    private record HeldHookshot(Arm arm) {
    }
}
