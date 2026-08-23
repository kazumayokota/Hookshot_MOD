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

    private static final Map<UUID, Double> SIDEWAYS_INPUTS = new HashMap<>();

    private SwingInputTracker() {
    }

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(SWING_INPUT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            double sidewaysInput = buf.readDouble();
            server.execute(() -> setSidewaysInput(player, sidewaysInput));
        });
    }

    public static double getSidewaysInput(ServerPlayerEntity player) {
        return SIDEWAYS_INPUTS.getOrDefault(player.getUuid(), 0.0D);
    }

    public static void clear(ServerPlayerEntity player) {
        SIDEWAYS_INPUTS.remove(player.getUuid());
    }

    private static void setSidewaysInput(ServerPlayerEntity player, double sidewaysInput) {
        SIDEWAYS_INPUTS.put(player.getUuid(), Math.max(-1.0D, Math.min(1.0D, sidewaysInput)));
    }
}
