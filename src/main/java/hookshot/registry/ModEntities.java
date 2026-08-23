package hookshot.registry;

import hookshot.HookshotMod;
import hookshot.entity.HookProjectileEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class ModEntities {
    public static final EntityType<HookProjectileEntity> HOOK_PROJECTILE = EntityType.Builder
            .<HookProjectileEntity>create(HookProjectileEntity::new, SpawnGroup.MISC)
            .setDimensions(0.5F, 0.5F)
            .maxTrackingRange(80)
            .trackingTickInterval(1)
            .build(id("hook_projectile").toString());

    private ModEntities() {
    }

    public static void register() {
        Registry.register(Registry.ENTITY_TYPE, id("hook_projectile"), HOOK_PROJECTILE);
    }

    private static Identifier id(String path) {
        return new Identifier(HookshotMod.MOD_ID, path);
    }
}
