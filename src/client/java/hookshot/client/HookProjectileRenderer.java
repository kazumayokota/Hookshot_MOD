package hookshot.client;

import hookshot.entity.HookProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
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
import net.minecraft.util.math.Vec3f;

public final class HookProjectileRenderer extends EntityRenderer<HookProjectileEntity> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/arrow.png");

    public HookProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(HookProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        HookRopeRenderer.render(entity, tickDelta, matrices, vertexConsumers, light);

        matrices.push();
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0F));
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
        matrices.scale(0.05625F, 0.05625F, 0.05625F);
        matrices.translate(-4.0D, 0.0D, 0.0D);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(getTexture(entity)));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, light);

        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
        vertex(positionMatrix, normalMatrix, vertexConsumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, light);

        for (int i = 0; i < 4; i++) {
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
            MatrixStack.Entry finEntry = matrices.peek();
            vertex(finEntry.getPositionMatrix(), finEntry.getNormalMatrix(), vertexConsumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light);
            vertex(finEntry.getPositionMatrix(), finEntry.getNormalMatrix(), vertexConsumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, light);
            vertex(finEntry.getPositionMatrix(), finEntry.getNormalMatrix(), vertexConsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, light);
            vertex(finEntry.getPositionMatrix(), finEntry.getNormalMatrix(), vertexConsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, light);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(HookProjectileEntity entity) {
        return TEXTURE;
    }

    private static void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertexConsumer, int x, int y, int z, float u, float v, int normalX, int normalY, int normalZ, int light) {
        vertexConsumer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .next();
    }
}
