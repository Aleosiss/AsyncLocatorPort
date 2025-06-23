package brightspark.asynclocator.logic

import brightspark.asynclocator.ALConstants
import brightspark.asynclocator.AsyncLocator.LocateTask.thenOnServerThread
import brightspark.asynclocator.AsyncLocator.locateBiome
import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.mixins.LocateCommandAccess
import brightspark.asynclocator.platform.Services
import com.google.common.base.Stopwatch
import com.mojang.datafixers.util.Pair
import net.minecraft.Util
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceOrTagArgument
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.network.chat.Component
import net.minecraft.server.commands.LocateCommand
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.function.Consumer

object LocateCommandLogic {
  fun locateStructureAsync(
    sourceStack: CommandSourceStack,
    structureResult: ResourceOrTagKeyArgument.Result<Structure?>,
    holderset: HolderSet<Structure?>
  ) {
    val originPos = BlockPos.containing(sourceStack.position)
    val stopwatch = Stopwatch.createStarted(Util.TICKER)
    locateStructure(sourceStack.level, holderset, originPos, Services.CONFIG.structureSearchRadius(), false)
      .thenOnServerThread(Consumer<Pair<BlockPos?, Holder<Structure?>?>> { pair: Pair<BlockPos?, Holder<Structure?>?>? ->
        stopwatch.stop()
        if (pair != null) {
          ALConstants.logInfo("Location found - sending success back to command source")
          LocateCommand.showLocateResult(
            sourceStack,
            structureResult,
            originPos,
            pair,
            "commands.locate.structure.success",
            false,
            stopwatch.elapsed()
          )
        } else {
          ALConstants.logInfo("No location found - sending failure back to command source")
          sourceStack.sendFailure(
            Component.literal(
              LocateCommandAccess.getErrorFailed().create(structureResult.asPrintable()).message
            )
          )
        }
      })
  }

  fun locateBiomeAsync(
    sourceStack: CommandSourceStack,
    biomeResult: ResourceOrTagArgument.Result<Biome?>
  ) {
    val originPos = BlockPos.containing(sourceStack.position)
    val stopwatch = Stopwatch.createStarted(Util.TICKER)

    locateBiome(sourceStack.level, biomeResult, originPos, Services.CONFIG.biomeSearchRadius(), false)
      .thenOnServerThread(Consumer<Pair<BlockPos?, Holder<Biome?>?>?> { pair: Pair<BlockPos?, Holder<Biome?>?>? ->
        stopwatch.stop()
        if (pair != null) {
          ALConstants.logInfo("Location found - sending success back to command source")
          LocateCommand.showLocateResult(
            sourceStack,
            biomeResult,
            originPos,
            pair,
            "commands.locate.biome.success",
            false,
            stopwatch.elapsed()
          )
        } else {
          ALConstants.logInfo("No location found - sending failure back to command source")
          sourceStack.sendFailure(
            Component.literal(
              LocateCommandAccess.getErrorFailed().create(biomeResult.asPrintable()).message
            )
          )
        }
      })
  }
}
