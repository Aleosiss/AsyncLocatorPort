package brightspark.asynclocator.extensions

import brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING
import brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING_MANAGED
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData

object CustomDataExtensions {

  fun CompoundTag.removeAsyncLocatorData() {
    remove(KEY_LOCATING)
    remove(KEY_LOCATING_MANAGED)
  }

  fun CompoundTag.hasAsyncLocatorData(): Boolean {
    return contains(KEY_LOCATING) || contains(KEY_LOCATING_MANAGED)
  }

  fun CustomData.hasAsyncLocatorData(): Boolean {
    return contains(KEY_LOCATING) || contains(KEY_LOCATING_MANAGED)
  }
}