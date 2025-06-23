package brightspark.asynclocator.logic

import brightspark.asynclocator.AsyncLocator.locateBiome
import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.AsyncLocatorMod.CONFIG
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.mixins.LocateCommandAccess
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

object LocateCommandLogic {
  fun locateStructureAsync(
    sourceStack: CommandSourceStack,
    structureResult: ResourceOrTagKeyArgument.Result<Structure>,
    holderset: HolderSet<Structure>
  ) {
    val originPos = BlockPos.containing(sourceStack.position)
    val stopwatch = Stopwatch.createStarted(Util.TICKER)
    locateStructure(sourceStack.level, holderset, originPos, CONFIG.structureSearchRadius.get(), false)
      .thenOnServerThread { pair: Pair<BlockPos, Holder<Structure>>? ->
        stopwatch.stop()
        if (pair != null) {
          LOG.info("Location found - sending success back to command source")
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
          LOG.info("No location found - sending failure back to command source")
          val message = LocateCommandAccess.getErrorFailed().create(structureResult.asPrintable()).message
            ?: "null"
          sourceStack.sendFailure(Component.literal(message))
        }
      }
  }

  fun locateBiomeAsync(sourceStack: CommandSourceStack, biomeResult: ResourceOrTagArgument.Result<Biome>) {
    val originPos = BlockPos.containing(sourceStack.position)
    val stopwatch = Stopwatch.createStarted(Util.TICKER)

    locateBiome(sourceStack.level, biomeResult, originPos, CONFIG.biomeSearchRadius.get(), false)
      .thenOnServerThread { pair ->
        stopwatch.stop()
        if (pair != null) {
          LOG.info("Location found - sending success back to command source")
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
          val message = LocateCommandAccess.getErrorFailed().create(biomeResult.asPrintable()).message
            ?: "null"
          LOG.info("No location found - sending failure back to command source")
          sourceStack.sendFailure(Component.literal(message))
        }
      }
  }
}
