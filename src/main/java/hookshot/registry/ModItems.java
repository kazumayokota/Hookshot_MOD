package hookshot.registry;

import hookshot.HookshotMod;
import hookshot.item.HookshotItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class ModItems {
    public static final Item HOOKSHOT = new HookshotItem(new Item.Settings()
            .group(ItemGroup.COMBAT)
            .maxDamage(HookshotItem.MAX_DURABILITY));

    private ModItems() {
    }

    public static void register() {
        Registry.register(Registry.ITEM, id("hookshot"), HOOKSHOT);
    }

    private static Identifier id(String path) {
        return new Identifier(HookshotMod.MOD_ID, path);
    }
}
