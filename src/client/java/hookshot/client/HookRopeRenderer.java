package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public final class HookRopeRenderer {
    private static final int SEGMENTS = 24;
    private static final float ROPE_HALF_WIDTH = 0.034F;
    private static final float ROPE_INNER_WIDTH = 0.014F;
    private static final double HAND_SIDE_OFFSET = 0.36D;
    private static final double HAND_FORWARD_OFFSET = 0.16D;
    private static final double HAND_HEIGHT_RATIO = 0.72D;

    private HookRopeRenderer() {
    }

    public static void render(HookProjectileEntity hook, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Entity owner = hook.getOwner();

        if (owner == null) {
            return;
        }

        Vec3d hookPos = hook.getLerpedPos(tickDelta);
        Vec3d startPos = getHandPos(hook, owner, tickDelta);
        Vec3d delta = startPos.subtract(hookPos);

        matrices.push();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLeash());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        for (int segment = 0; segment < SEGMENTS; segment++) {
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, -ROPE_HALF_WIDTH, 0x6E5739, light);
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, -ROPE_INNER_WIDTH, 0xA98A58, light);
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, ROPE_INNER_WIDTH, 0x8A7048, light);
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, ROPE_HALF_WIDTH, 0xC0A06A, light);
        }

        matrices.pop();
    }

    private static Vec3d getHandPos(HookProjectileEntity hook, Entity owner, float tickDelta) {
        Vec3d base = owner.getLerpedPos(tickDelta);
        float bodyYaw = owner instanceof LivingEntity livingEntity
                ? MathHelper.lerp(tickDelta, livingEntity.prevBodyYaw, livingEntity.bodyYaw)
                : MathHelper.lerp(tickDelta, owner.prevYaw, owner.getYaw());
        float yawRadians = bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0D, MathHelper.cos(yawRadians));
        Vec3d right = new Vec3d(-MathHelper.cos(yawRadians), 0.0D, -MathHelper.sin(yawRadians));
        Arm sourceArm = hook.getSourceArm(owner);
        double sideSign = sourceArm == Arm.RIGHT ? 1.0D : -1.0D;

        return base
                .add(0.0D, owner.getHeight() * HAND_HEIGHT_RATIO, 0.0D)
                .add(forward.multiply(HAND_FORWARD_OFFSET))
                .add(right.multiply(HAND_SIDE_OFFSET * sideSign));
    }

    private static void addSegment(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d delta, int segment, float offset, int color, int light) {
        float start = (float) segment / SEGMENTS;
        float end = (float) (segment + 1) / SEGMENTS;
        float sagStart = getSag(start);
        float sagEnd = getSag(end);

        vertex(vertexConsumer, positionMatrix, normalMatrix, delta, start, sagStart, offset, color, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, delta, end, sagEnd, offset, color, light);
    }

    private static float getSag(float progress) {
        return MathHelper.sin(progress * MathHelper.PI) * 0.12F;
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d delta, float progress, float sag, float offset, int color, int light) {
        float x = (float) (delta.x * progress);
        float y = (float) (delta.y * progress) - sag;
        float z = (float) (delta.z * progress);
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;

        vertexConsumer.vertex(positionMatrix, x + offset, y, z)
                .color(red, green, blue, 255)
                .light(light)
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .next();
    }
}
