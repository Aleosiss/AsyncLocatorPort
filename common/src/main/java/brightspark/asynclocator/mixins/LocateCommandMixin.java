package brightspark.asynclocator.mixins;

import brightspark.asynclocator.AsyncLocatorMod;
import brightspark.asynclocator.logic.LocateCommandLogic;
import brightspark.asynclocator.platform.Services;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static brightspark.asynclocator.AsyncLocatorMod.INSTANCE;

@Mixin(LocateCommand.class)
public class LocateCommandMixin {
	@Shadow @Final private static DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND;

	@Inject(
		method = "locateStructure",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;findNearestMapStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderSet;Lnet/minecraft/core/BlockPos;IZ)Lcom/mojang/datafixers/util/Pair;"
		),
		cancellable = true,
		locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private static void findStructureLocationAsync(
		CommandSourceStack sourceStack,
		ResourceOrTagKeyArgument.Result<Structure> structureResult,
		CallbackInfoReturnable<Integer> cir,
		Registry<Structure> registry,
		HolderSet<Structure> holderset
	) {
		if (!INSTANCE.getCONFIG().getLocateCommandEnabled().get()) return;

		CommandSource source = ((CommandSourceStackAccess) sourceStack).getSource();
		if (source instanceof ServerPlayer || source instanceof MinecraftServer) {
			INSTANCE.getLOGGER().debug("Intercepted LocateCommand#locate structure call");
			LocateCommandLogic.INSTANCE.locateStructureAsync(sourceStack, structureResult, holderset);
			cir.setReturnValue(0);
		}
	}

	@Inject(
			method = "locateBiome",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;findClosestBiome3d(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;III)Lcom/mojang/datafixers/util/Pair;"
			),
			cancellable = true
	)
	private static void findBiomeLocationAsync(
			CommandSourceStack sourceStack,
			ResourceOrTagArgument.Result<Biome> biomeResult,
			CallbackInfoReturnable<Integer> cir
	) {
		if (!INSTANCE.getCONFIG().getLocateCommandEnabled().get()) return;

		CommandSource source = ((CommandSourceStackAccess) sourceStack).getSource();
		if (source instanceof ServerPlayer || source instanceof MinecraftServer) {
			INSTANCE.getLOGGER().debug("Intercepted LocateCommand#locate biome call");
			LocateCommandLogic.INSTANCE.locateBiomeAsync(sourceStack, biomeResult);
			cir.setReturnValue(0);
		}
	}
}