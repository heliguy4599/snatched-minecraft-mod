package com.tameno.snatched.mixin;

import com.tameno.snatched.Snatched;
import com.tameno.snatched.Snatcher;
import com.tameno.snatched.config.SnatcherSettings;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Snatcher {

    @Unique private static final int FOOD_REPLENISH_TICK_MODULO = 20;

    @Shadow public abstract void startFallFlying();

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    @Shadow public abstract boolean giveItemStack(ItemStack stack);

    @Shadow public abstract PlayerInventory getInventory();

    @Shadow public abstract void resetLastAttackedTicks();

    @Shadow public abstract void setFireTicks(int fireTicks);

    @Unique private int foodLevelToAdd = 0;

    @Unique private UUID snatched$currentHandSeatUuid;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void snatchedPlayerTick(CallbackInfo callbackInfo) {
        if (getWorld().getTime() % FOOD_REPLENISH_TICK_MODULO != 0) return;
        if (this.foodLevelToAdd <= 0) return;
        ((PlayerEntity) (Object) this).getHungerManager().add(1, 20);
        this.foodLevelToAdd -= 1;
    }

    public void addFoodLevel(int foodLevel) {
        this.foodLevelToAdd += foodLevel;
    }

    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat) {
        if (newHandSeat == null) {
            this.snatched$currentHandSeatUuid = null;
            return;
        }
        this.snatched$currentHandSeatUuid = newHandSeat.getUuid();
    }

    public HandSeatEntity snatched$getCurrentHandSeat(World world) {
        if (this.snatched$currentHandSeatUuid == null) {
            return null;
        }
        HandSeatEntity handSeat = (HandSeatEntity) ((ServerWorld) world).getEntity(this.snatched$currentHandSeatUuid);
        if (handSeat == null) {
            return null;
        }
        if (handSeat.isRemoved()) {
            return null;
        }
        return handSeat;
    }

    public SnatcherSettings snatched$getSnatcherSettings() {
        SnatcherSettings settings = Snatched.allSnatcherSettings.get(this.getUuid());
        if (settings == null) {
            return new SnatcherSettings();
        }
        return settings;
    }
}
