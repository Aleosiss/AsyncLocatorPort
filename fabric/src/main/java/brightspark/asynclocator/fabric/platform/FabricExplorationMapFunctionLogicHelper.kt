package brightspark.asynclocator.fabric.platform

import brightspark.asynclocator.MapManager
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.logic.CommonLogic
import brightspark.asynclocator.platform.services.ExplorationMapFunctionLogicHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.UUID
import java.util.function.BiConsumer

class FabricExplorationMapFunctionLogicHelper : ExplorationMapFunctionLogicHelper {
  override fun invalidateMap(mapStack: ItemStack, level: ServerLevel, invPos: BlockPos, asyncId: UUID) {
    handleUpdateMapInChest(mapStack, level, invPos, asyncId) { chest: ChestBlockEntity, slot: Int? ->
      chest.setItem(slot!!, ItemStack(Items.MAP))
    }
  }

  override fun updateMap(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos,
    scale: Int,
    destinationType: Holder<MapDecorationType>,
    invPos: BlockPos,
    displayName: Component,
    asyncId: UUID
  ) {
    CommonLogic.updateMap(mapStack, level, pos, scale, destinationType, displayName)
    // Shouldn't need to set the stack in its slot again, as we're modifying the same instance
    handleUpdateMapInChest(mapStack, level, invPos, asyncId) { _, _ -> }
  }

  companion object {
    private fun handleUpdateMapInChest(
      mapStack: ItemStack,
      level: ServerLevel,
      invPos: BlockPos,
      asyncId: UUID,
      handleSlotFound: BiConsumer<ChestBlockEntity, Int>
    ) {
      val be = level.getBlockEntity(invPos)
      if (be is ChestBlockEntity) {
        for (i in 0..<be.containerSize) {
          val slotStack = be.getItem(i)
          if (slotStack == mapStack) {
            handleSlotFound.accept(be, i)
            CommonLogic.broadcastChestChanges(level, be)
            // since we found the map in the chest, no longer need the data in map manager
            MapManager.INSTANCE.removeLocateOperation(asyncId)
            return
          }
        }
      } else {
        LOG.warn("Couldn't find chest block entity on block {} at {}", level.getBlockState(invPos), invPos)
      }
    }
  }
}
