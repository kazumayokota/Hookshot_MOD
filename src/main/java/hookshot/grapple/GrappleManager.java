package hookshot.grapple;

import hookshot.HookshotConfig;
import hookshot.entity.HookProjectileEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
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
        state.setHookedEntityUuid(null);
        state.setAnchorPosition(anchorPosition);
        state.setMode(GrappleMode.PLAYER_PULL);
        state.setActiveTicks(0);
        state.setStuckTicks(0);
        state.setLastDistanceToAnchor(-1.0D);
        state.setActive(true);
    }

    public static void startEntityGrapple(ServerPlayerEntity player, HookProjectileEntity hook, Entity target) {
        GrappleState state = STATES.computeIfAbsent(player.getUuid(), ignored -> new GrappleState());
        state.setHookUuid(hook.getUuid());
        state.setHookedEntityUuid(target.getUuid());
        state.setAnchorPosition(getEntityAnchorPosition(target));
        state.setMode(isHumanTypeTarget(target) ? GrappleMode.ENTITY_PULL : GrappleMode.PLAYER_PULL);
        state.setActiveTicks(0);
        state.setStuckTicks(0);
        state.setLastDistanceToAnchor(-1.0D);
        state.setActive(true);
    }

    public static void clear(ServerPlayerEntity player) {
        GrappleState state = STATES.remove(player.getUuid());

        if (state != null && state.getHookUuid() != null && player.world instanceof ServerWorld serverWorld) {
            Entity hook = serverWorld.getEntity(state.getHookUuid());

            if (hook instanceof HookProjectileEntity hookProjectile) {
                hookProjectile.removeImmediately();
            }
        }

        if (player.world instanceof ServerWorld serverWorld) {
            for (Entity entity : serverWorld.iterateEntities()) {
                if (!(entity instanceof HookProjectileEntity hookProjectile)) {
                    continue;
                }

                Entity owner = hookProjectile.getOwner();
                if (owner != null && owner.getUuid().equals(player.getUuid())) {
                    hookProjectile.removeImmediately();
                }
            }
        }
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
                updateEntityAnchor(player, state);
                PlayerPullBehavior.tick(player, state.getAnchorPosition());
            } else if (state.getMode() == GrappleMode.ENTITY_PULL) {
                Entity target = getHookedEntity(player, state);
                if (target != null) {
                    EntityPullBehavior.tick(player, target);
                    state.setAnchorPosition(getEntityAnchorPosition(target));
                }
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
            if (state.getHookedEntityUuid() != null && getHookedEntity(player, state) == null) {
                return true;
            }

            updateEntityAnchor(player, state);

            if (PlayerPullBehavior.isCloseEnough(player, state.getAnchorPosition())) {
                return true;
            }

            if (isStuck(player, state)) {
                return true;
            }

            if (isLookingAway(player, state.getAnchorPosition())) {
                return true;
            }
        } else if (state.getMode() == GrappleMode.ENTITY_PULL) {
            Entity target = getHookedEntity(player, state);
            if (target == null || EntityPullBehavior.isCloseEnough(player, target)) {
                return true;
            }

            state.setAnchorPosition(getEntityAnchorPosition(target));

            if (isEntityStuck(player, target, state)) {
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

    private static boolean isStuck(ServerPlayerEntity player, GrappleState state) {
        double distance = player.getPos().distanceTo(state.getAnchorPosition());
        double lastDistance = state.getLastDistanceToAnchor();
        state.setLastDistanceToAnchor(distance);

        if (lastDistance < 0.0D) {
            state.setStuckTicks(0);
            return false;
        }

        double progress = lastDistance - distance;
        boolean barelyMovedTowardAnchor = progress < HookshotConfig.GRAPPLE_MIN_PROGRESS_PER_TICK;
        boolean notMovingMeaningfully = player.getVelocity().lengthSquared() < HookshotConfig.GRAPPLE_STUCK_MAX_SPEED * HookshotConfig.GRAPPLE_STUCK_MAX_SPEED;

        if (barelyMovedTowardAnchor && notMovingMeaningfully) {
            state.setStuckTicks(state.getStuckTicks() + 1);
        } else {
            state.setStuckTicks(0);
        }

        return state.getStuckTicks() >= HookshotConfig.GRAPPLE_STUCK_TICKS;
    }

    private static boolean isEntityStuck(ServerPlayerEntity player, Entity target, GrappleState state) {
        double distance = target.getPos().distanceTo(player.getPos());
        double lastDistance = state.getLastDistanceToAnchor();
        state.setLastDistanceToAnchor(distance);

        if (lastDistance < 0.0D) {
            state.setStuckTicks(0);
            return false;
        }

        double progress = lastDistance - distance;
        boolean barelyMovedTowardPlayer = progress < HookshotConfig.GRAPPLE_MIN_PROGRESS_PER_TICK;
        boolean notMovingMeaningfully = target.getVelocity().lengthSquared() < HookshotConfig.GRAPPLE_STUCK_MAX_SPEED * HookshotConfig.GRAPPLE_STUCK_MAX_SPEED;

        if (barelyMovedTowardPlayer && notMovingMeaningfully) {
            state.setStuckTicks(state.getStuckTicks() + 1);
        } else {
            state.setStuckTicks(0);
        }

        return state.getStuckTicks() >= HookshotConfig.GRAPPLE_STUCK_TICKS;
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
        state.setHookedEntityUuid(null);
        state.setAnchorPosition(null);
        state.setActiveTicks(0);
        state.setStuckTicks(0);
        state.setLastDistanceToAnchor(-1.0D);
        state.setFallProtectionTicks(HookshotConfig.FALL_PROTECTION_TICKS);
    }

    private static void updateEntityAnchor(ServerPlayerEntity player, GrappleState state) {
        Entity target = getHookedEntity(player, state);
        if (target != null) {
            state.setAnchorPosition(getEntityAnchorPosition(target));
        }
    }

    private static Entity getHookedEntity(ServerPlayerEntity player, GrappleState state) {
        if (state.getHookedEntityUuid() == null || !(player.world instanceof ServerWorld serverWorld)) {
            return null;
        }

        Entity target = serverWorld.getEntity(state.getHookedEntityUuid());
        if (target == null || target.isRemoved() || !target.isAlive()) {
            return null;
        }

        return target;
    }

    private static Vec3d getEntityAnchorPosition(Entity entity) {
        return entity.getPos().add(0.0D, entity.getHeight() * 0.5D, 0.0D);
    }

    private static boolean isHumanTypeTarget(Entity entity) {
        return entity instanceof VillagerEntity
                || entity instanceof ArmorStandEntity
                || entity instanceof CreeperEntity
                || entity instanceof IllagerEntity
                || entity instanceof ZombieEntity
                || entity instanceof ZombieVillagerEntity
                || entity instanceof ZombifiedPiglinEntity
                || entity instanceof WitchEntity
                || entity instanceof PiglinEntity
                || entity instanceof EndermanEntity;
    }
}
