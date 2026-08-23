package hookshot;

import net.fabricmc.api.ClientModInitializer;

public final class HookshotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HookshotMod.LOGGER.info("Hookshot MOD client initialized.");
    }
}
