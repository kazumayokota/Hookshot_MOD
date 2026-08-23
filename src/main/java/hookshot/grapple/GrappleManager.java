package hookshot.grapple;

import hookshot.HookshotConfig;
import hookshot.entity.HookProjectileEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class GrappleManager {
    private static final Map<UUID, GrappleState> STATES = new HashMap<>();

    private GrappleManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GrappleManager::tick);
    }

    public static void startBlockGrapple(ServerPlayerEntity player, HookProjectileEntity hook, Vec3d anchorPosition) {
        GrappleState state = STATES.computeIfAbsent(player.getUuid(), ignored -> new GrappleState());
        state.setHookUuid(hook.getUuid());
        state.setAnchorPosition(anchorPosition);
        state.setMode(GrappleMode.PLAYER_PULL);
        state.setActiveTicks(0);
        state.setActive(true);
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, GrappleState>> iterator = STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, GrappleState> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null || player.isDead()) {
                iterator.remove();
                continue;
            }

            GrappleState state = entry.getValue();
            tickFallProtection(player, state);

            if (!state.isActive()) {
                if (state.getFallProtectionTicks() <= 0) {
                    iterator.remove();
                }
                continue;
            }

            if (shouldRelease(player, state)) {
                release(player, state);
                continue;
            }

            if (state.getMode() == GrappleMode.PLAYER_PULL && state.getAnchorPosition() != null) {
                PlayerPullBehavior.tick(player, state.getAnchorPosition());
            }

            state.setActiveTicks(state.getActiveTicks() + 1);
        }
    }

    private static void tickFallProtection(ServerPlayerEntity player, GrappleState state) {
        if (state.getFallProtectionTicks() <= 0) {
            return;
        }

        state.setFallProtectionTicks(state.getFallProtectionTicks() - 1);
        player.fallDistance = 0.0F;
    }

    private static boolean shouldRelease(ServerPlayerEntity player, GrappleState state) {
        if (state.getActiveTicks() >= HookshotConfig.MAX_GRAPPLE_TICKS || state.getHookUuid() == null) {
            return true;
        }

        if (state.getMode() == GrappleMode.PLAYER_PULL && state.getAnchorPosition() != null) {
            if (PlayerPullBehavior.isCloseEnough(player, state.getAnchorPosition())) {
                return true;
            }

            if (isLookingAway(player, state.getAnchorPosition())) {
                return true;
            }
        }

        Entity hook = player.world instanceof ServerWorld serverWorld ? serverWorld.getEntity(state.getHookUuid()) : null;
        return !(hook instanceof HookProjectileEntity) || hook.isRemoved();
    }

    private static boolean isLookingAway(ServerPlayerEntity player, Vec3d anchorPosition) {
        Vec3d toAnchor = anchorPosition.subtract(player.getEyePos());

        if (toAnchor.lengthSquared() < 1.0E-7D) {
            return false;
        }

        double dot = player.getRotationVec(1.0F).normalize().dotProduct(toAnchor.normalize());
        double minDot = Math.cos(Math.toRadians(HookshotConfig.GRAPPLE_VIEW_RELEASE_DEGREES));
        return dot < minDot;
    }

    private static void release(ServerPlayerEntity player, GrappleState state) {
        if (state.getHookUuid() != null && player.world instanceof ServerWorld serverWorld) {
            Entity hook = serverWorld.getEntity(state.getHookUuid());

            if (hook instanceof HookProjectileEntity hookProjectile) {
                hookProjectile.release();
            }
        }

        state.setActive(false);
        state.setMode(GrappleMode.NONE);
        state.setHookUuid(null);
        state.setAnchorPosition(null);
        state.setActiveTicks(0);
        state.setFallProtectionTicks(HookshotConfig.FALL_PROTECTION_TICKS);
    }
}
