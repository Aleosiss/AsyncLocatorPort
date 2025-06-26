package brightspark.asynclocator.extensions

import brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import java.util.UUID

object CustomDataExtensions {

  fun CompoundTag.removeAsyncLocatorData() {
    remove(KEY_LOCATING)
  }

  fun CompoundTag.hasAsyncLocatorData(): Boolean {
    return contains(KEY_LOCATING)
  }

  fun CustomData.hasAsyncLocatorData(): Boolean {
    return contains(KEY_LOCATING)
  }

  fun CustomData.getAsyncId(): UUID? {
    return this.copyTag().getUUID(KEY_LOCATING)
  }

  fun ItemStack.getAsyncId(): UUID? {
    return this.get(DataComponents.CUSTOM_DATA)?.getAsyncId()
  }

  fun ItemStack.hasAsyncLocatorData(): Boolean {
    return this.get(DataComponents.CUSTOM_DATA)?.hasAsyncLocatorData() ?: false
  }

  fun ItemStack.removeAsyncLocatorData() {
    if (this.get(DataComponents.CUSTOM_DATA)?.hasAsyncLocatorData() == true) {
      val modifiedData = this.get(DataComponents.CUSTOM_DATA)!!
        .copyTag()
        .apply { removeAsyncLocatorData() }
      this.set(DataComponents.CUSTOM_DATA, CustomData.of(modifiedData))
    }
  }
}
