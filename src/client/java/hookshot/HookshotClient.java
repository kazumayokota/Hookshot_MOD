package hookshot;

import hookshot.client.HookReticleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class HookshotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(HookReticleRenderer::render);
        HookshotMod.LOGGER.info("Hookshot MOD client initialized.");
    }
}
