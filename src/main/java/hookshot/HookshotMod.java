package hookshot;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HookshotMod implements ModInitializer {
    public static final String MOD_ID = "hookshot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hookshot MOD initialized.");
    }
}
