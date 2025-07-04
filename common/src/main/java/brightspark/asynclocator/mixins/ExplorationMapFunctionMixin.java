package brightspark.asynclocator.mixins;

import brightspark.asynclocator.AsyncLocatorMod;
import brightspark.asynclocator.logic.ExplorationMapFunctionLogic;
import brightspark.asynclocator.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static brightspark.asynclocator.AsyncLocatorMod.INSTANCE;

@Mixin(ExplorationMapFunction.class)
public class ExplorationMapFunctionMixin {
	@Shadow
	@Final
	TagKey<Structure> destination;

	@Shadow
	@Final
	Holder<MapDecorationType> mapDecoration;

	@Shadow
	@Final
	byte zoom;

	@Shadow
	@Final
	int searchRadius;

	@Shadow
	@Final
	boolean skipKnownStructures;

	@Inject(
		method = "run",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"
		),
		locals = LocalCapture.CAPTURE_FAILSOFT,
		cancellable = true
	)
	public void updateMapAsync(
		ItemStack pStack,
		LootContext pContext,
		CallbackInfoReturnable<ItemStack> cir,
		Vec3 vec3,
		ServerLevel serverlevel
	) {
		if (!INSTANCE.getCONFIG().getExplorationMapEnabled().get()) return;

		AsyncLocatorMod.INSTANCE.getLOGGER().info("Intercepted ExplorationMapFunction#run call");
		ItemStack mapStack = ExplorationMapFunctionLogic.INSTANCE.updateMapAsync(
				pStack,
				serverlevel,
				BlockPos.containing(vec3),
				zoom,
				searchRadius,
				skipKnownStructures,
				mapDecoration,
				destination
		);
		cir.setReturnValue(mapStack);
	}
}
