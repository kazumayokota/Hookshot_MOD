package hookshot.client;

import hookshot.network.SwingInputTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

public final class HookSwingInputSender {
    private HookSwingInputSender() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HookSwingInputSender::sendSwingInput);
    }

    private static void sendSwingInput(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        double sidewaysInput = 0.0D;
        if (client.options.leftKey.isPressed()) {
            sidewaysInput -= 1.0D;
        }
        if (client.options.rightKey.isPressed()) {
            sidewaysInput += 1.0D;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(sidewaysInput);
        ClientPlayNetworking.send(SwingInputTracker.SWING_INPUT_PACKET_ID, buf);
    }
}
