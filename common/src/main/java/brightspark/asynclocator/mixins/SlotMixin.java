package brightspark.asynclocator.mixins;

import brightspark.asynclocator.AsyncLocatorMod;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.platform.Services;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static brightspark.asynclocator.AsyncLocatorMod.INSTANCE;

@Mixin(Slot.class)
public abstract class SlotMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Inject(
		method = "mayPickup",
		at = @At(value = "HEAD"),
		cancellable = true
	)
	public void preventPickupOfPendingExplorationMap(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (INSTANCE.getCONFIG().getExplorationMapEnabled().get() && CommonLogic.isEmptyPendingMap(getItem())) {
			AsyncLocatorMod.INSTANCE.getLOGGER().debug("Intercepted Slot#mayPickup call");
			cir.setReturnValue(false);
		}
	}
}
