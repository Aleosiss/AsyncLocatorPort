package brightspark.asynclocator.mixins;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.logic.MerchantLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MerchantMixin extends Entity {
    public MerchantMixin(EntityType<?> $$0, Level $$1) {
        super($$0, $$1);
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void onMerchantTick(CallbackInfo ci) {
        LivingEntity actual = (LivingEntity) (Object) this;
        if(!this.level().isClientSide && actual instanceof AbstractVillager villager) {
            if(actual.tickCount % 20 != 0) {
                return; // Only tick every second
            }
            MerchantLogic.tickMerchantOffers((ServerLevel) this.level(), villager);
        }
    }
}
