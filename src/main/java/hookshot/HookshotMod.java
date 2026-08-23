package hookshot;

import hookshot.grapple.GrappleManager;
import hookshot.network.SwingInputTracker;
import hookshot.registry.ModItems;
import hookshot.registry.ModEntities;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HookshotMod implements ModInitializer {
    public static final String MOD_ID = "hookshot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        GrappleManager.register();
        SwingInputTracker.registerServerReceiver();
        LOGGER.info("Hookshot MOD initialized.");
    }
}
