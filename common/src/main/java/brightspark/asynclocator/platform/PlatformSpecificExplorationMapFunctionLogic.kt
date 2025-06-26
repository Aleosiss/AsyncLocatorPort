package brightspark.asynclocator.platform

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.UUID

object PlatformSpecificExplorationMapFunctionLogic {
  @JvmStatic
  @ExpectPlatform
  fun invalidateMap(mapStack: ItemStack, level: ServerLevel, invPos: BlockPos, asyncId: UUID) {
    throw UnsupportedOperationException()
  }

  @JvmStatic
  @ExpectPlatform
  fun updateMap(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos,
    scale: Int,
    destinationType: Holder<MapDecorationType>,
    invPos: BlockPos,
    displayName: Component,
    asyncId: UUID
  ) {
    throw UnsupportedOperationException()
  }
}
