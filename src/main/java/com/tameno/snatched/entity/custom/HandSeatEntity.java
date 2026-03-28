package com.tameno.snatched.entity.custom;

import com.tameno.snatched.*;
import com.tameno.snatched.config.SnatcherSettings;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.sound.*;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.Optional;
import java.util.UUID;

public class HandSeatEntity extends Entity {

    private static final TrackedData<Optional<UUID>> HAND_OWNER_ID = DataTracker.registerData(HandSeatEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> EATING_TICKS = DataTracker.registerData(HandSeatEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int EATING_DURATION = 32;

    private PlayerEntity handOwner;

    public HandSeatEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        Optional<java.util.UUID> handOwnerId = this.dataTracker.get(HAND_OWNER_ID);
        if (handOwnerId.isPresent()) {
            this.handOwner = this.getWorld().getPlayerByUuid(handOwnerId.get());
        }
    }

    public void setHandOwner(PlayerEntity newHandOwner) {
        this.handOwner = newHandOwner;
        this.dataTracker.set(HAND_OWNER_ID, Optional.of(newHandOwner.getUuid()));
    }

    public void updateHandPosition() {
        final Entity passenger = this.getFirstPassenger();
        if (passenger == null) return;
        SnatcherSettings settings = ((Snatcher) this.handOwner).snatched$getSnatcherSettings();

        double ownerSize = Snatched.getSize(this.handOwner);
        double passengerSize = Snatched.getSize(passenger);
        double distance = ownerSize + passengerSize * 2.0;
        double side = 1.0;
        if (settings.flipWhenUsingLeftHandAsMainHand && this.handOwner.getMainArm() == Arm.LEFT) {
            side = -1.0;
        }

        Vec3d pos = settings.holdPosition;
        pos = pos.multiply(new Vec3d(distance * side, distance, distance));
        pos = pos.rotateX(this.handOwner.getPitch() * -0.01745329251f);
        pos = pos.rotateY(this.handOwner.getYaw() * -0.01745329251f);
        pos = pos.add(this.handOwner.getPos());
        pos = pos.add(0,  this.handOwner.getEyeHeight(this.handOwner.getPose()) - passengerSize / 2.0, 0);

        final int ticks = dataTracker.get(EATING_TICKS);
        if (ticks > 0) {
            Vec3d mouthPos = this.handOwner.getPos();
            mouthPos = mouthPos.add(0, this.handOwner.getEyeHeight(this.handOwner.getPose()) - passenger.getHeight() * 2.0, 0);
            pos = mouthPos.lerp(pos, (double) ticks / (double) EATING_DURATION);
        }

        this.setPosition(pos);
    }

    public void startEating() {
        if (dataTracker.get(EATING_TICKS) > 0) return;
        dataTracker.set(EATING_TICKS, EATING_DURATION);
    }

    public void stopEating() {
        dataTracker.set(EATING_TICKS, 0);
    }

    private void finishEating() {
        if (getWorld().isClient) return;
        Entity toEat = getFirstPassenger();
        if (!(toEat instanceof LivingEntity living)) return;
        float yourMaxHealth = handOwner.getMaxHealth();
        if (yourMaxHealth <= 0f) {
            yourMaxHealth = 0.0001f;
        }
        float itsMaxHealth = living.getMaxHealth();
        if (itsMaxHealth <= 0f) {
            itsMaxHealth = 0.0001f;
        }
        float itsHealth = living.getHealth();

        final double HEALTH_IMPORTANCE = 0.3;
        final double FILLING_MULTIPLIER = 12.0;

        double itsBigness = MathHelper.lerp(HEALTH_IMPORTANCE, Snatched.getSize(living), itsMaxHealth);
        Snatched.LOGGER.info("bigness: {}", itsBigness);
        Snatched.LOGGER.info("itsHealth: {}", itsHealth);
        Snatched.LOGGER.info("itsMaxHealth: {}", itsMaxHealth);
        Snatched.LOGGER.info("Math pow stuff: {}", Math.pow(yourMaxHealth, 0.33));
        double haunches = (
            itsBigness
            * (itsHealth / itsMaxHealth)
            / Math.pow(yourMaxHealth, 0.33)
            * FILLING_MULTIPLIER
        );

        if (handOwner instanceof Snatcher snatcher) {
            int foodLevel = (int) (haunches * 2.0);
            Snatched.LOGGER.info("Adding {}...", foodLevel);
            snatcher.addFoodLevel(foodLevel);
        }
        living.kill();

        getWorld().playSound(
            null,
            handOwner.getX(),
            handOwner.getY(),
            handOwner.getZ(),
            SoundEvents.ENTITY_PLAYER_BURP,
            SoundCategory.PLAYERS,
            0.5f,
            getWorld().getRandom().nextFloat() * 0.1f + 0.9f
        );
    }

    @Override
    public void tick() {

        if (handOwner == null) {
            Optional<java.util.UUID> handOwnerId = this.dataTracker.get(HAND_OWNER_ID);
            if (handOwnerId.isPresent()) {
                this.handOwner = this.getWorld().getPlayerByUuid(handOwnerId.get());
            }
        }

        boolean isValid = !(
            this.handOwner == null ||
            this.handOwner.isRemoved() ||
            this.getFirstPassenger() == null ||
            this.handOwner.getWorld() != this.getWorld()
        );

        // if (this.getWorld().isClient()) {
        //     if (!isValid) {
        //         return;
        //     }
        //     updateHandPosition();
        // }

        if (!isValid) {
            this.discard();
            return;
        }

        int ticks = dataTracker.get(EATING_TICKS);
        if (ticks > 0) {
            ticks -= 1;
            dataTracker.set(EATING_TICKS, ticks);

            if (ticks % 4 == 0 && ticks > 0) {
                getWorld().playSound(
                    null,
                    handOwner.getX(),
                    handOwner.getY(),
                    handOwner.getZ(),
                    SoundEvents.ENTITY_GENERIC_EAT,
                    SoundCategory.PLAYERS,
                    0.5f + 0.5f * getWorld().getRandom().nextInt(2),
                    (getWorld().getRandom().nextFloat() - getWorld().getRandom().nextFloat()) * 0.2f + 1.0f
                );
            }

            if (ticks == 0) {
                finishEating();
            }
        }

        updateHandPosition();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.0;
    }

    @Override
    protected void initDataTracker() {
        Optional<java.util.UUID> handOwnerId;
        if (this.handOwner == null) {
            handOwnerId = Optional.empty();
        } else {
            handOwnerId = Optional.of(this.handOwner.getUuid());
        }
        this.dataTracker.startTracking(HAND_OWNER_ID, handOwnerId);
        this.dataTracker.startTracking(EATING_TICKS, 0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return true;
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    @Override
    public boolean canAvoidTraps() {
        return true;
    }

    @Override
    public boolean canHit() {
        return false;
    }
}
