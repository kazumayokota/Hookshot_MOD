package hookshot.entity;

import hookshot.HookshotConfig;
import hookshot.registry.ModEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class HookProjectileEntity extends Entity {
    private static final TrackedData<Integer> HOOK_STATE = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final double SPEED = 3.2D;
    private static final int ATTACHED_LIFETIME_TICKS = 60;
    private static final double START_OFFSET = 0.25D;
    private static final double BLOCK_SURFACE_OFFSET = 0.08D;

    private UUID ownerUuid;
    private int ownerEntityId;
    private int attachedTicks;
    private double traveledDistance;

    public HookProjectileEntity(EntityType<? extends HookProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = false;
    }

    public HookProjectileEntity(World world, Entity owner) {
        this(ModEntities.HOOK_PROJECTILE, world);
        setOwner(owner);
        setNoGravity(true);
    }

    public void shootFrom(Entity owner) {
        Vec3d direction = owner.getRotationVec(1.0F).normalize();
        Vec3d start = owner.getEyePos().add(direction.multiply(START_OFFSET));

        refreshPositionAndAngles(start.x, start.y, start.z, getYaw(direction), getPitch(direction));
        setVelocity(direction.multiply(SPEED));
        setRotationFromDirection(direction);
    }

    public HookState getHookState() {
        return HookState.values()[MathHelper.clamp(dataTracker.get(HOOK_STATE), 0, HookState.values().length - 1)];
    }

    public void setOwner(Entity owner) {
        ownerUuid = owner.getUuid();
        ownerEntityId = owner.getId();
    }

    public Entity getOwner() {
        if (ownerUuid != null && world instanceof ServerWorld serverWorld) {
            return serverWorld.getEntity(ownerUuid);
        }

        if (ownerEntityId != 0) {
            return world.getEntityById(ownerEntityId);
        }

        return null;
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(HOOK_STATE, HookState.FLYING.ordinal());
    }

    @Override
    public void tick() {
        super.tick();

        if (getHookState() != HookState.FLYING) {
            tickAttached();
            return;
        }

        Vec3d start = getPos();
        Vec3d velocity = getVelocity();
        Vec3d end = start.add(velocity);
        HitResult hitResult = getCollision(start, end);

        if (hitResult.getType() != HitResult.Type.MISS) {
            end = hitResult.getPos();
        }

        move(MovementType.SELF, end.subtract(start));
        traveledDistance += start.distanceTo(getPos());
        setRotationFromDirection(getVelocity());

        if (hitResult.getType() != HitResult.Type.MISS) {
            onCollision(hitResult);
            return;
        }

        if (traveledDistance >= HookshotConfig.MAX_RANGE || age > 100) {
            removeHook();
        }
    }

    private HitResult getCollision(Vec3d start, Vec3d end) {
        HitResult blockHit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this));

        Vec3d collisionEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();
        Box searchBox = getBoundingBox().stretch(getVelocity()).expand(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                world,
                this,
                start,
                collisionEnd,
                searchBox,
                entity -> entity.canHit() && !isOwner(entity));

        return entityHit != null ? entityHit : blockHit;
    }

    private boolean isOwner(Entity entity) {
        Entity owner = getOwner();
        return owner != null && entity.getUuid().equals(owner.getUuid());
    }

    private void onCollision(HitResult hitResult) {
        if (world.isClient) {
            return;
        }

        if (hitResult instanceof EntityHitResult entityHitResult) {
            onEntityHit(entityHitResult);
        } else if (hitResult instanceof BlockHitResult blockHitResult) {
            onBlockHit(blockHitResult);
        }
    }

    private void onEntityHit(EntityHitResult hitResult) {
        setHookState(HookState.ATTACHED_ENTITY);
        setRotationFromDirection(getVelocity());
        setVelocity(Vec3d.ZERO);
        setPosition(hitResult.getPos());
        playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F, 1.2F);
    }

    private void onBlockHit(BlockHitResult hitResult) {
        setHookState(HookState.ATTACHED_BLOCK);
        setRotationFromDirection(getVelocity());
        setVelocity(Vec3d.ZERO);

        Vec3d normal = Vec3d.of(hitResult.getSide().getVector());
        Vec3d attachedPos = hitResult.getPos().add(normal.multiply(BLOCK_SURFACE_OFFSET));
        setPosition(attachedPos);
        playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F, 1.0F);
    }

    private void tickAttached() {
        setVelocity(Vec3d.ZERO);

        if (!world.isClient && ++attachedTicks >= ATTACHED_LIFETIME_TICKS) {
            removeHook();
        }
    }

    private void removeHook() {
        setHookState(HookState.REMOVED);
        discard();
    }

    private void setHookState(HookState state) {
        dataTracker.set(HOOK_STATE, state.ordinal());
    }

    private void setRotationFromDirection(Vec3d direction) {
        if (direction.lengthSquared() < 1.0E-7D) {
            return;
        }

        Vec3d normalized = direction.normalize();
        float yaw = getYaw(normalized);
        float pitch = getPitch(normalized);

        setYaw(yaw);
        setPitch(pitch);
        prevYaw = yaw;
        prevPitch = pitch;
    }

    private static float getYaw(Vec3d direction) {
        return (float) (MathHelper.atan2(direction.z, direction.x) * 57.2957763671875D) - 90.0F;
    }

    private static float getPitch(Vec3d direction) {
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) (-(MathHelper.atan2(direction.y, horizontalLength) * 57.2957763671875D));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setHookState(readState(nbt.getString("HookState")));
        ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        attachedTicks = nbt.getInt("AttachedTicks");
        traveledDistance = nbt.getDouble("TraveledDistance");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("HookState", getHookState().name());
        Optional.ofNullable(ownerUuid).ifPresent(uuid -> nbt.putUuid("Owner", uuid));
        nbt.putInt("AttachedTicks", attachedTicks);
        nbt.putDouble("TraveledDistance", traveledDistance);
    }

    private static HookState readState(String value) {
        try {
            return HookState.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return HookState.FLYING;
        }
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, ownerEntityId);
    }
}
