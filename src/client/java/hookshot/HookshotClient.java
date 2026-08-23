package hookshot;

import hookshot.client.HookProjectileRenderer;
import hookshot.client.HookReticleRenderer;
import hookshot.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class HookshotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.HOOK_PROJECTILE, HookProjectileRenderer::new);
        HudRenderCallback.EVENT.register(HookReticleRenderer::render);
        HookshotMod.LOGGER.info("Hookshot MOD client initialized.");
    }
}
