package brightspark.asynclocator.platform.services

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.UUID

interface ExplorationMapFunctionLogicHelper {
  fun invalidateMap(mapStack: ItemStack?, level: ServerLevel?, pos: BlockPos?, uuid: UUID?)

  fun updateMap(
    mapStack: ItemStack?,
    level: ServerLevel?,
    pos: BlockPos?,
    scale: Int,
    destinationType: Holder<MapDecorationType?>?,
    invPos: BlockPos?,
    displayName: Component?,
    asyncId: UUID?
  )
}
