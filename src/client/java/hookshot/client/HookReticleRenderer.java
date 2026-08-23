package hookshot.client;

import hookshot.HookshotConfig;
import hookshot.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

public final class HookReticleRenderer {
    private static final int HIT_COLOR = 0xFFFFFFFF;
    private static final int MISS_COLOR = 0xFFFF3030;
    private static final int HALF_SIZE = 5;
    private static final int GAP = 2;
    private static final int THICKNESS = 1;

    private HookReticleRenderer() {
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.options.hudHidden || !isHookshotEquipped(client.player)) {
            return;
        }

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        int color = hasHookableTarget(client.player, tickDelta) ? HIT_COLOR : MISS_COLOR;

        DrawableHelper.fill(matrices, centerX - HALF_SIZE, centerY, centerX - GAP, centerY + THICKNESS, color);
        DrawableHelper.fill(matrices, centerX + GAP + 1, centerY, centerX + HALF_SIZE + 1, centerY + THICKNESS, color);
        DrawableHelper.fill(matrices, centerX, centerY - HALF_SIZE, centerX + THICKNESS, centerY - GAP, color);
        DrawableHelper.fill(matrices, centerX, centerY + GAP + 1, centerX + THICKNESS, centerY + HALF_SIZE + 1, color);
    }

    public static boolean isHookshotEquipped(PlayerEntity player) {
        return isHookshot(player.getStackInHand(Hand.MAIN_HAND)) || isHookshot(player.getStackInHand(Hand.OFF_HAND));
    }

    private static boolean hasHookableTarget(PlayerEntity player, float tickDelta) {
        HitResult hitResult = player.raycast(HookshotConfig.MAX_RANGE, tickDelta, false);
        return hitResult.getType() == HitResult.Type.BLOCK;
    }

    private static boolean isHookshot(ItemStack stack) {
        return stack.isOf(ModItems.HOOKSHOT);
    }
}
