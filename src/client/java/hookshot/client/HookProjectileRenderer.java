package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import hookshot.entity.HookState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public final class HookProjectileRenderer extends EntityRenderer<HookProjectileEntity> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/arrow.png");
    private static final double SHAFT_BACK_LENGTH = 0.76D;
    private static final double SHAFT_FRONT_LENGTH = 0.03D;
    private static final double SHAFT_RADIUS = 0.035D;
    private static final double TIP_LENGTH = 0.22D;
    private static final double TIP_RADIUS = 0.105D;
    private static final double BARB_BACK = 0.20D;
    private static final double BARB_SPREAD = 0.17D;

    public HookProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HookProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (shouldRenderRope(entity)) {
            HookRopeRenderer.render(entity, tickDelta, matrices, vertexConsumers, light);
        }

        Vec3d forward = entity.getAimDirection();
        Vec3d up = getPerpendicular(forward);
        Vec3d side = forward.crossProduct(up).normalize();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(getTexture(entity)));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        renderShaft(vertexConsumer, positionMatrix, normalMatrix, forward, up, side, light);
        renderTip(vertexConsumer, positionMatrix, normalMatrix, forward, up, side, light);
        renderBarbs(vertexConsumer, positionMatrix, normalMatrix, forward, up, side, light);

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static boolean shouldRenderRope(HookProjectileEntity entity) {
        HookState state = entity.getHookState();
        return state == HookState.FLYING || state == HookState.ATTACHED_BLOCK || state == HookState.ATTACHED_ENTITY;
    }

    @Override
    public Identifier getTexture(HookProjectileEntity entity) {
        return TEXTURE;
    }

    private static void renderShaft(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d forward, Vec3d up, Vec3d side, int light) {
        Vec3d back = forward.multiply(-SHAFT_BACK_LENGTH);
        Vec3d front = forward.multiply(SHAFT_FRONT_LENGTH);
        Vec3d upOffset = up.multiply(SHAFT_RADIUS);
        Vec3d sideOffset = side.multiply(SHAFT_RADIUS);

        Vec3d b0 = back.add(upOffset);
        Vec3d b1 = back.add(sideOffset);
        Vec3d b2 = back.subtract(upOffset);
        Vec3d b3 = back.subtract(sideOffset);
        Vec3d f0 = front.add(upOffset);
        Vec3d f1 = front.add(sideOffset);
        Vec3d f2 = front.subtract(upOffset);
        Vec3d f3 = front.subtract(sideOffset);

        quad(vertexConsumer, positionMatrix, normalMatrix, b0, f0, f1, b1, up.add(side).normalize(), light, 0.00F, 0.00F, 0.50F, 0.15F);
        quad(vertexConsumer, positionMatrix, normalMatrix, b1, f1, f2, b2, side.subtract(up).normalize(), light, 0.00F, 0.15F, 0.50F, 0.30F);
        quad(vertexConsumer, positionMatrix, normalMatrix, b2, f2, f3, b3, up.add(side).multiply(-1.0D).normalize(), light, 0.00F, 0.30F, 0.50F, 0.45F);
        quad(vertexConsumer, positionMatrix, normalMatrix, b3, f3, f0, b0, up.subtract(side).normalize(), light, 0.00F, 0.45F, 0.50F, 0.60F);
    }

    private static void renderTip(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d forward, Vec3d up, Vec3d side, int light) {
        Vec3d base = forward.multiply(SHAFT_FRONT_LENGTH);
        Vec3d tip = forward.multiply(SHAFT_FRONT_LENGTH + TIP_LENGTH);
        Vec3d upOffset = up.multiply(TIP_RADIUS);
        Vec3d sideOffset = side.multiply(TIP_RADIUS);

        Vec3d p0 = base.add(upOffset);
        Vec3d p1 = base.add(sideOffset);
        Vec3d p2 = base.subtract(upOffset);
        Vec3d p3 = base.subtract(sideOffset);

        triangle(vertexConsumer, positionMatrix, normalMatrix, p0, tip, p1, p0.subtract(tip).crossProduct(p1.subtract(tip)).normalize(), light, 0.50F, 0.00F, 0.75F, 0.15F);
        triangle(vertexConsumer, positionMatrix, normalMatrix, p1, tip, p2, p1.subtract(tip).crossProduct(p2.subtract(tip)).normalize(), light, 0.50F, 0.15F, 0.75F, 0.30F);
        triangle(vertexConsumer, positionMatrix, normalMatrix, p2, tip, p3, p2.subtract(tip).crossProduct(p3.subtract(tip)).normalize(), light, 0.50F, 0.30F, 0.75F, 0.45F);
        triangle(vertexConsumer, positionMatrix, normalMatrix, p3, tip, p0, p3.subtract(tip).crossProduct(p0.subtract(tip)).normalize(), light, 0.50F, 0.45F, 0.75F, 0.60F);
    }

    private static void renderBarbs(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d forward, Vec3d up, Vec3d side, int light) {
        Vec3d root = forward.multiply(0.0D);
        Vec3d back = forward.multiply(-BARB_BACK);

        renderBarb(vertexConsumer, positionMatrix, normalMatrix, root, back.add(up.multiply(BARB_SPREAD)), side, light);
        renderBarb(vertexConsumer, positionMatrix, normalMatrix, root, back.subtract(up.multiply(BARB_SPREAD)), side, light);
        renderBarb(vertexConsumer, positionMatrix, normalMatrix, root, back.add(side.multiply(BARB_SPREAD)), up, light);
        renderBarb(vertexConsumer, positionMatrix, normalMatrix, root, back.subtract(side.multiply(BARB_SPREAD)), up, light);
    }

    private static void renderBarb(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d root, Vec3d point, Vec3d widthAxis, int light) {
        Vec3d width = widthAxis.multiply(0.026D);
        Vec3d normal = point.subtract(root).crossProduct(widthAxis).normalize();
        quad(vertexConsumer, positionMatrix, normalMatrix, root.add(width), point.add(width), point.subtract(width), root.subtract(width), normal, light, 0.75F, 0.00F, 1.00F, 0.25F);
    }

    private static Vec3d getPerpendicular(Vec3d forward) {
        Vec3d up = new Vec3d(0.0D, 1.0D, 0.0D);

        if (Math.abs(forward.dotProduct(up)) > 0.9D) {
            up = new Vec3d(1.0D, 0.0D, 0.0D);
        }

        return up.subtract(forward.multiply(forward.dotProduct(up))).normalize();
    }

    private static void quad(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d a, Vec3d b, Vec3d c, Vec3d d, Vec3d normal, int light, float minU, float minV, float maxU, float maxV) {
        vertex(vertexConsumer, positionMatrix, normalMatrix, a, normal, minU, minV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, b, normal, maxU, minV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, c, normal, maxU, maxV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, d, normal, minU, maxV, light);
    }

    private static void triangle(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d a, Vec3d b, Vec3d c, Vec3d normal, int light, float minU, float minV, float maxU, float maxV) {
        vertex(vertexConsumer, positionMatrix, normalMatrix, a, normal, minU, maxV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, b, normal, (minU + maxU) * 0.5F, minV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, c, normal, maxU, maxV, light);
        vertex(vertexConsumer, positionMatrix, normalMatrix, c, normal, maxU, maxV, light);
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, Vec3d position, Vec3d normal, float u, float v, int light) {
        vertexConsumer.vertex(positionMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .next();
    }
}
