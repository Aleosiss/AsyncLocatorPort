package brightspark.asynclocator.mixins;

import brightspark.asynclocator.AsyncLocatorMod;
import brightspark.asynclocator.logic.EyeOfEnderData;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EyeOfEnder.class)
public class EyeOfEnderMixin implements EyeOfEnderData {
	private boolean locateTaskOngoing = false;

	@Override
	public void setLocateTaskOngoing(boolean locateTaskOngoing) {
		this.locateTaskOngoing = locateTaskOngoing;
	}

	/*
		Intercept EyeOfEnder#tick call and return after the super call if there's an ongoing locate task. This is to
		prevent the entity from moving or dying until we have a location result.
	 */
	@Inject(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;tick()V",
			shift = At.Shift.AFTER
		),
		cancellable = true
	)
	public void skipTick(CallbackInfo ci) {
		if (locateTaskOngoing) {
			AsyncLocatorMod.INSTANCE.getLOGGER().info("Intercepted EyeOfEnder#tick call - skipping tick");
			ci.cancel();
		}
	}
}
