package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public final class HookProjectileRenderer extends EntityRenderer<HookProjectileEntity> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/arrow.png");
    private static final double SHAFT_BACK_LENGTH = 0.55D;
    private static final double TIP_LENGTH = 0.12D;
    private static final double TIP_WIDTH = 0.08D;

    public HookProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(HookProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        HookRopeRenderer.render(entity, tickDelta, matrices, vertexConsumers, light);

        Vec3d forward = getForwardVector(entity, tickDelta);
        Vec3d up = getPerpendicular(forward);
        Vec3d side = forward.crossProduct(up).normalize();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        Vec3d tail = forward.multiply(-SHAFT_BACK_LENGTH);
        Vec3d tip = forward.multiply(TIP_LENGTH);
        Vec3d barbBase = forward.multiply(-0.08D);

        drawLine(vertexConsumer, positionMatrix, normalMatrix, tail, tip, 235, 235, 235);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, tip, barbBase.add(up.multiply(TIP_WIDTH)), 210, 210, 210);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, tip, barbBase.subtract(up.multiply(TIP_WIDTH)), 210, 210, 210);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, tip, barbBase.add(side.multiply(TIP_WIDTH)), 210, 210, 210);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, tip, barbBase.subtract(side.multiply(TIP_WIDTH)), 210, 210, 210);

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(HookProjectileEntity entity) {
        return TEXTURE;
    }

    private static Vec3d getForwardVector(HookProjectileEntity entity, float tickDelta) {
        Vec3d velocity = entity.getVelocity();

        if (velocity.lengthSquared() > 1.0E-7D) {
            return velocity.normalize();
        }

        float yaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) * MathHelper.RADIANS_PER_DEGREE;
        float pitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch()) * MathHelper.RADIANS_PER_DEGREE;
        float horizontal = MathHelper.cos(pitch);
        return new Vec3d(-MathHelper.sin(yaw) * horizontal, -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
    }

    private static Vec3d getPerpendicular(Vec3d forward) {
        Vec3d up = new Vec3d(0.0D, 1.0D, 0.0D);

        if (Math.abs(forward.dotProduct(up)) > 0.9D) {
            up = new Vec3d(1.0D, 0.0D, 0.0D);
        }

        return up.subtract(forward.multiply(forward.dotProduct(up))).normalize();
    }

    private static void drawLine(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d start, Vec3d end, int red, int green, int blue) {
        Vec3d normal = end.subtract(start).normalize();
        vertex(vertexConsumer, positionMatrix, normalMatrix, start, normal, red, green, blue);
        vertex(vertexConsumer, positionMatrix, normalMatrix, end, normal, red, green, blue);
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d position, Vec3d normal, int red, int green, int blue) {
        vertexConsumer.vertex(positionMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, 255)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .next();
    }
}
