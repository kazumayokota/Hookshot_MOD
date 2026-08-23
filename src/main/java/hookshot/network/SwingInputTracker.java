package hookshot.network;

import hookshot.HookshotMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class SwingInputTracker {
    public static final Identifier SWING_INPUT_PACKET_ID = new Identifier(HookshotMod.MOD_ID, "swing_input");

    private static final Map<UUID, SwingInput> INPUTS = new HashMap<>();

    private SwingInputTracker() {
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(SWING_INPUT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            double sidewaysInput = buf.readDouble();
            boolean jumpPressed = buf.readBoolean();
            boolean jumpHeld = buf.readBoolean();
            boolean sneakHeld = buf.readBoolean();
            server.execute(() -> setInput(player, sidewaysInput, jumpPressed, jumpHeld, sneakHeld));
        });
    }

    public static double getSidewaysInput(ServerPlayerEntity player) {
        return INPUTS.getOrDefault(player.getUuid(), SwingInput.NONE).sidewaysInput();
    }

    public static boolean consumeJumpPressed(ServerPlayerEntity player) {
        SwingInput input = INPUTS.getOrDefault(player.getUuid(), SwingInput.NONE);
        if (!input.jumpPressed()) {
            return false;
        }

        INPUTS.put(player.getUuid(), new SwingInput(input.sidewaysInput(), false, input.jumpHeld(), input.sneakHeld()));
        return true;
    }

    public static boolean isJumpHeld(ServerPlayerEntity player) {
        return INPUTS.getOrDefault(player.getUuid(), SwingInput.NONE).jumpHeld();
    }

    public static boolean isSneakHeld(ServerPlayerEntity player) {
        return INPUTS.getOrDefault(player.getUuid(), SwingInput.NONE).sneakHeld();
    }

    public static void clear(ServerPlayerEntity player) {
        INPUTS.remove(player.getUuid());
    }

    private static void setInput(ServerPlayerEntity player, double sidewaysInput, boolean jumpPressed, boolean jumpHeld, boolean sneakHeld) {
        SwingInput existing = INPUTS.getOrDefault(player.getUuid(), SwingInput.NONE);
        double clampedSidewaysInput = Math.max(-1.0D, Math.min(1.0D, sidewaysInput));
        INPUTS.put(player.getUuid(), new SwingInput(clampedSidewaysInput, jumpPressed || existing.jumpPressed(), jumpHeld, sneakHeld));
    }

    private record SwingInput(double sidewaysInput, boolean jumpPressed, boolean jumpHeld, boolean sneakHeld) {
        private static final SwingInput NONE = new SwingInput(0.0D, false, false, false);
    }
}
