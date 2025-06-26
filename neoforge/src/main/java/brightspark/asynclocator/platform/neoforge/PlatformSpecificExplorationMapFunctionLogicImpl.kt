package brightspark.asynclocator.platform.neoforge

import brightspark.asynclocator.MapManager
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.logic.CommonLogic.broadcastChestChanges
import brightspark.asynclocator.logic.CommonLogic.updateMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.IItemHandlerModifiable
import java.util.UUID
import java.util.function.BiConsumer

object PlatformSpecificExplorationMapFunctionLogicImpl {
  @JvmStatic
  fun invalidateMap(mapStack: ItemStack, level: ServerLevel, invPos: BlockPos, asyncId: UUID) {
    handleUpdateMapInChest(
      mapStack, level, invPos, asyncId
    ) { handler: IItemHandler, slot: Int ->
      if (handler is IItemHandlerModifiable) {
        handler.setStackInSlot(slot, ItemStack(Items.MAP))
      } else {
        handler.extractItem(slot, Item.DEFAULT_MAX_STACK_SIZE, false)
        handler.insertItem(slot, ItemStack(Items.MAP), false)
      }
    }
  }

  @JvmStatic
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
    updateMap(mapStack, level, pos, scale, destinationType, displayName)
    // Shouldn't need to set the stack in its slot again, as we're modifying the same instance
    handleUpdateMapInChest(
      mapStack, level, invPos, asyncId
    ) { _: IItemHandler, _: Int -> }
  }

  private fun handleUpdateMapInChest(
    mapStack: ItemStack,
    level: ServerLevel,
    invPos: BlockPos,
    asyncId: UUID,
    handleSlotFound: BiConsumer<IItemHandler, Int>
  ) {
    val be = level.getBlockEntity(invPos)
    if (be != null) {
      val itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, invPos, Direction.valueOf(""))
      if (itemHandler != null) {
        for (i in 0..<itemHandler.slots) {
          val slotStack = itemHandler.getStackInSlot(i)
          if (slotStack == mapStack) {
            handleSlotFound.accept(itemHandler, i)
            broadcastChestChanges(level, be)
            // since we found the map in the chest, no longer need the data in map manager
            MapManager.INSTANCE.removeLocateOperation(asyncId)
            return
          }
        }
      } else {
        LOG.warn(
          "Couldn't find item handler capability on chest {} at {}",
          be.javaClass.simpleName, invPos
        )
      }
    } else {
      LOG.warn(
        "Couldn't find block entity on chest {} at {}",
        level.getBlockState(invPos), invPos
      )
    }
  }
}
