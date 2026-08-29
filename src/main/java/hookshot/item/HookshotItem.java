package hookshot.item;

import hookshot.entity.HookProjectileEntity;
import hookshot.grapple.GrappleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class HookshotItem extends Item {
    public static final int MAX_DURABILITY = 768;

    public HookshotItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.CROSSBOW) || super.canRepair(stack, ingredient);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.isDamageable() && !stack.hasEnchantments();
    }

    @Override
    public int getEnchantability() {
        return 1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        user.getItemCooldownManager().set(this, 8);

        if (!world.isClient) {
            if (user instanceof ServerPlayerEntity serverPlayer) {
                GrappleManager.clear(serverPlayer);
            }

            HookProjectileEntity hook = new HookProjectileEntity(world, user);
            hook.setSourceHand(hand);
            hook.shootFrom(user);
            world.spawnEntity(hook);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);

            stack.damage(1, user, player -> player.sendToolBreakStatus(hand));
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
