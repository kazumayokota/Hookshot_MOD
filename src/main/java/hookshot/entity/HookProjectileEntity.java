package hookshot.entity;

import hookshot.HookshotConfig;
import hookshot.grapple.GrappleManager;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class HookProjectileEntity extends Entity {
    private static final TrackedData<Integer> HOOK_STATE = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> AIM_X = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> AIM_Y = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> AIM_Z = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SOURCE_HAND = DataTracker.registerData(HookProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final double SPEED = 3.2D;
    private static final double START_OFFSET = 0.25D;
    private static final double BLOCK_SURFACE_OFFSET = 0.08D;

    private UUID ownerUuid;
    private UUID attachedEntityUuid;
    private int ownerEntityId;
    private int attachedTicks;
    private int returningTicks;
    private double traveledDistance;
    private boolean checkedInstantHit;

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
        setAimDirection(direction);
        setVelocity(direction.multiply(SPEED));
        setRotationFromDirection(direction);
    }

    public HookState getHookState() {
        return HookState.values()[MathHelper.clamp(dataTracker.get(HOOK_STATE), 0, HookState.values().length - 1)];
    }

    public void setSourceHand(Hand hand) {
        dataTracker.set(SOURCE_HAND, hand == Hand.OFF_HAND ? 1 : 0);
    }

    public Hand getSourceHand() {
        return dataTracker.get(SOURCE_HAND) == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public Arm getSourceArm(Entity owner) {
        Arm mainArm = owner instanceof net.minecraft.entity.player.PlayerEntity player ? player.getMainArm() : Arm.RIGHT;
        return getSourceHand() == Hand.MAIN_HAND ? mainArm : mainArm.getOpposite();
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
        dataTracker.startTracking(AIM_X, 0.0F);
        dataTracker.startTracking(AIM_Y, 0.0F);
        dataTracker.startTracking(AIM_Z, 1.0F);
        dataTracker.startTracking(SOURCE_HAND, 0);
    }

    @Override
    public void tick() {
        super.tick();

        HookState hookState = getHookState();

        if (hookState == HookState.RETURNING) {
            tickReturning();
            return;
        }

        if (hookState != HookState.FLYING) {
            tickAttached();
            return;
        }

        if (!checkedInstantHit) {
            checkedInstantHit = true;
            if (tryInstantHit()) {
                return;
            }
        }

        Vec3d start = getPos();
        Vec3d velocity = getAimDirection().multiply(SPEED);
        setVelocity(velocity);
        Vec3d end = start.add(velocity);
        HitResult hitResult = getCollision(start, end);

        if (hitResult.getType() != HitResult.Type.MISS) {
            end = hitResult.getPos();
        }

        move(MovementType.SELF, end.subtract(start));
        traveledDistance += start.distanceTo(getPos());
        setRotationFromDirection(getAimDirection());

        if (hitResult.getType() != HitResult.Type.MISS) {
            onCollision(hitResult);
            return;
        }

        if (traveledDistance >= HookshotConfig.MAX_RANGE || age > 100) {
            startReturning();
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
        Box searchBox = getBoundingBox().stretch(collisionEnd.subtract(start)).expand(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                world,
                this,
                start,
                collisionEnd,
                searchBox,
                entity -> entity.canHit() && !isOwner(entity));

        return entityHit != null ? entityHit : blockHit;
    }

    private boolean tryInstantHit() {
        Entity owner = getOwner();
        if (owner == null) {
            return false;
        }

        Vec3d direction = getAimDirection();
        Vec3d start = owner.getEyePos().add(direction.multiply(START_OFFSET));
        Vec3d end = start.add(direction.multiply(HookshotConfig.MAX_RANGE));
        HitResult hitResult = getCollision(start, end);

        if (hitResult.getType() == HitResult.Type.MISS || start.distanceTo(hitResult.getPos()) <= HookshotConfig.INSTANT_HIT_DISTANCE) {
            return false;
        }

        setPosition(hitResult.getPos());
        traveledDistance = start.distanceTo(hitResult.getPos());
        setRotationFromDirection(direction);

        if (!world.isClient) {
            onCollision(hitResult);
        }

        return true;
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
        Entity target = hitResult.getEntity();
        setHookState(HookState.ATTACHED_ENTITY);
        attachedEntityUuid = target.getUuid();
        setRotationFromDirection(getAimDirection());
        setVelocity(Vec3d.ZERO);
        setPosition(hitResult.getPos());
        startEntityGrapple(target);
        playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F, 1.2F);
    }

    private void onBlockHit(BlockHitResult hitResult) {
        Vec3d normal = Vec3d.of(hitResult.getSide().getVector());
        Vec3d attachedPos = hitResult.getPos().add(normal.multiply(BLOCK_SURFACE_OFFSET));

        if (isTooCloseToOwner(attachedPos)) {
            removeHook();
            return;
        }

        setHookState(HookState.ATTACHED_BLOCK);
        setRotationFromDirection(getAimDirection());
        setVelocity(Vec3d.ZERO);
        setPosition(attachedPos);
        startBlockGrapple(attachedPos);
        playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F, 1.0F);
    }

    private boolean isTooCloseToOwner(Vec3d position) {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayerEntity player)) {
            return false;
        }

        return player.getPos().squaredDistanceTo(position) <= HookshotConfig.GRAPPLE_RELEASE_DISTANCE * HookshotConfig.GRAPPLE_RELEASE_DISTANCE;
    }

    private void startBlockGrapple(Vec3d anchorPosition) {
        Entity owner = getOwner();

        if (owner instanceof ServerPlayerEntity player) {
            GrappleManager.startBlockGrapple(player, this, anchorPosition);
        }
    }

    private void startEntityGrapple(Entity target) {
        Entity owner = getOwner();

        if (owner instanceof ServerPlayerEntity player) {
            GrappleManager.startEntityGrapple(player, this, target);
        }
    }

    private void tickAttached() {
        setVelocity(Vec3d.ZERO);

        if (getHookState() == HookState.ATTACHED_ENTITY) {
            Entity target = getAttachedEntity();
            if (target == null) {
                if (!world.isClient) {
                    startReturning();
                }
                return;
            }

            setPosition(getEntityAnchorPosition(target));
        }

        attachedTicks++;
    }

    private Entity getAttachedEntity() {
        if (attachedEntityUuid == null) {
            return null;
        }

        Entity target = world instanceof ServerWorld serverWorld ? serverWorld.getEntity(attachedEntityUuid) : null;

        if (target == null || target.isRemoved() || !target.isAlive()) {
            return null;
        }

        return target;
    }

    private static Vec3d getEntityAnchorPosition(Entity entity) {
        return entity.getPos().add(0.0D, entity.getHeight() * 0.5D, 0.0D);
    }

    private void tickReturning() {
        returningTicks++;
        if (returningTicks > HookshotConfig.HOOK_RETURN_MAX_TICKS) {
            if (!world.isClient) {
                removeHook();
            }
            return;
        }

        Entity owner = getOwner();

        if (owner == null) {
            if (!world.isClient) {
                removeHook();
            }
            return;
        }

        Vec3d target = owner.getEyePos().subtract(0.0D, 0.25D, 0.0D);
        Vec3d toOwner = target.subtract(getPos());

        if (toOwner.lengthSquared() <= HookshotConfig.HOOK_RETURN_FINISH_DISTANCE * HookshotConfig.HOOK_RETURN_FINISH_DISTANCE) {
            if (!world.isClient) {
                removeHook();
            }
            return;
        }

        Vec3d direction = toOwner.normalize();
        Vec3d velocity = direction.multiply(HookshotConfig.HOOK_RETURN_SPEED);
        setAimDirection(direction);
        setRotationFromDirection(direction);
        setVelocity(velocity);
        move(MovementType.SELF, velocity);
    }

    private void removeHook() {
        setHookState(HookState.REMOVED);
        discard();
    }

    public void release() {
        if (getHookState() == HookState.REMOVED || getHookState() == HookState.RETURNING) {
            return;
        }

        startReturning();
    }

    public void removeImmediately() {
        removeHook();
    }

    private void startReturning() {
        setHookState(HookState.RETURNING);
        setVelocity(Vec3d.ZERO);
        returningTicks = 0;
    }

    private void setHookState(HookState state) {
        dataTracker.set(HOOK_STATE, state.ordinal());
    }

    public Vec3d getAimDirection() {
        Vec3d direction = new Vec3d(dataTracker.get(AIM_X), dataTracker.get(AIM_Y), dataTracker.get(AIM_Z));

        if (direction.lengthSquared() < 1.0E-7D) {
            return new Vec3d(0.0D, 0.0D, 1.0D);
        }

        return direction.normalize();
    }

    private void setAimDirection(Vec3d direction) {
        Vec3d normalized = direction.normalize();
        dataTracker.set(AIM_X, (float) normalized.x);
        dataTracker.set(AIM_Y, (float) normalized.y);
        dataTracker.set(AIM_Z, (float) normalized.z);
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
        attachedEntityUuid = nbt.containsUuid("AttachedEntity") ? nbt.getUuid("AttachedEntity") : null;
        attachedTicks = nbt.getInt("AttachedTicks");
        returningTicks = nbt.getInt("ReturningTicks");
        traveledDistance = nbt.getDouble("TraveledDistance");
        checkedInstantHit = nbt.getBoolean("CheckedInstantHit");
        setAimDirection(new Vec3d(nbt.getDouble("AimX"), nbt.getDouble("AimY"), nbt.getDouble("AimZ")));
        setSourceHand(nbt.getBoolean("OffHand") ? Hand.OFF_HAND : Hand.MAIN_HAND);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("HookState", getHookState().name());
        Optional.ofNullable(ownerUuid).ifPresent(uuid -> nbt.putUuid("Owner", uuid));
        Optional.ofNullable(attachedEntityUuid).ifPresent(uuid -> nbt.putUuid("AttachedEntity", uuid));
        nbt.putInt("AttachedTicks", attachedTicks);
        nbt.putInt("ReturningTicks", returningTicks);
        nbt.putDouble("TraveledDistance", traveledDistance);
        nbt.putBoolean("CheckedInstantHit", checkedInstantHit);
        Vec3d direction = getAimDirection();
        nbt.putDouble("AimX", direction.x);
        nbt.putDouble("AimY", direction.y);
        nbt.putDouble("AimZ", direction.z);
        nbt.putBoolean("OffHand", getSourceHand() == Hand.OFF_HAND);
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

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        ownerEntityId = packet.getEntityData();
        setRotationFromDirection(getAimDirection());
    }
}
