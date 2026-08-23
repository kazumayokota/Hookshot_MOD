package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public final class HookRopeRenderer {
    private static final int SEGMENTS = 24;

    private HookRopeRenderer() {
    }

    public static void render(HookProjectileEntity hook, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Entity owner = hook.getOwner();

        if (owner == null) {
            return;
        }

        Vec3d hookPos = hook.getLerpedPos(tickDelta);
        Vec3d startPos = owner.getLeashPos(tickDelta);
        Vec3d delta = startPos.subtract(hookPos);

        matrices.push();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLeash());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        for (int segment = 0; segment < SEGMENTS; segment++) {
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, false, light);
            addSegment(vertexConsumer, positionMatrix, normalMatrix, delta, segment, true, light);
        }

        matrices.pop();
    }

    private static void addSegment(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d delta, int segment, boolean alternate, int light) {
        float start = (float) segment / SEGMENTS;
        float end = (float) (segment + 1) / SEGMENTS;
        float sagStart = getSag(start);
        float sagEnd = getSag(end);
        float offset = alternate ? 0.018F : -0.018F;
        int color = alternate ? 0x7A6241 : 0xA98A58;

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
