package hookshot.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class HookshotItem extends Item {
    public static final int MAX_DURABILITY = 768;

    public HookshotItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.CROSSBOW) || super.canRepair(stack, ingredient);
    }
}
