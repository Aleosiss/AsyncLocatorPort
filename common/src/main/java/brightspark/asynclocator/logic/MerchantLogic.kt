package brightspark.asynclocator.logic

import brightspark.asynclocator.ALConstants
import brightspark.asynclocator.AsyncLocator.LocateTask.thenOnServerThread
import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.logic.CommonLogic.updateMap
import brightspark.asynclocator.mixins.MerchantOfferAccess
import brightspark.asynclocator.platform.Services
import com.mojang.datafixers.util.Pair
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.Optional
import java.util.function.Consumer

object MerchantLogic {
  @Deprecated("Use {@link CommonLogic#createEmptyMap()} instead")
  fun createEmptyMap(): ItemStack {
    return CommonLogic.createEmptyMap()
  }

  fun invalidateMap(merchant: AbstractVillager, mapStack: ItemStack) {
    mapStack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.map"))
    merchant.offers
      .stream()
      .filter { offer: MerchantOffer -> offer.result == mapStack }
      .findFirst()
      .ifPresentOrElse(
        Consumer<MerchantOffer> { offer: MerchantOffer -> removeOffer(merchant, offer) }
      ) { ALConstants.logWarn("Failed to find merchant offer for map") }
  }

  fun removeOffer(merchant: AbstractVillager, offer: MerchantOffer) {
    if (Services.CONFIG.removeOffer()) {
      if (merchant.offers.remove(offer)) ALConstants.logInfo("Removed merchant map offer")
      else ALConstants.logWarn("Failed to remove merchant map offer")
    } else {
      (offer as MerchantOfferAccess).setMaxUses(0)
      offer.setToOutOfStock()
      ALConstants.logInfo("Marked merchant map offer as out of stock")
    }
  }

  fun tickMerchantOffers(level: ServerLevel?, merchant: AbstractVillager) {
    merchant.offers
      .stream()
      .map { obj: MerchantOffer -> obj.result }
      .filter { result: ItemStack -> result.`is`(Items.FILLED_MAP) }
      .forEach { offer: ItemStack ->
        offer.inventoryTick(level, merchant, -1, false)
      }
  }

  fun handleLocationFound(
    level: ServerLevel,
    merchant: AbstractVillager,
    mapStack: ItemStack,
    displayName: String?,
    destinationType: Holder<MapDecorationType?>,
    pos: BlockPos?
  ) {
    if (pos == null) {
      ALConstants.logInfo("No location found - invalidating merchant offer")

      invalidateMap(merchant, mapStack)
    } else {
      ALConstants.logInfo("Location found - updating treasure map in merchant offer")
      updateMap(mapStack, level, pos, 2, destinationType, displayName)
    }

    if (merchant.tradingPlayer is ServerPlayer) {
      ALConstants.logInfo("Player {} currently trading - updating merchant offers", tradingPlayer)

      tradingPlayer.sendMerchantOffers(
        tradingPlayer.containerMenu.containerId,
        merchant.offers,
        if (merchant is Villager) merchant.villagerData.level else 1,
        merchant.villagerXp,
        merchant.showProgressBar(),
        merchant.canRestock()
      )
    }
  }

  fun updateMapAsync(
    pTrader: Entity,
    emeraldCost: Int,
    displayName: String?,
    destinationType: Holder<MapDecorationType?>,
    maxUses: Int,
    villagerXp: Int,
    destination: TagKey<Structure?>
  ): MerchantOffer? {
    return updateMapAsyncInternal(
      pTrader,
      emeraldCost,
      maxUses,
      villagerXp,
      MapUpdateTask { level: ServerLevel, merchant: AbstractVillager, mapStack: ItemStack ->
        locateStructure(level, destination, merchant.blockPosition(), 100, true)
          .thenOnServerThread(Consumer { pos: BlockPos? ->
            handleLocationFound(
              level,
              merchant,
              mapStack,
              displayName,
              destinationType,
              pos
            )
          })
      }
    )
  }

  fun updateMapAsync(
    pTrader: Entity,
    emeraldCost: Int,
    displayName: String?,
    destinationType: Holder<MapDecorationType?>,
    maxUses: Int,
    villagerXp: Int,
    structureSet: HolderSet<Structure?>
  ): MerchantOffer? {
    return updateMapAsyncInternal(
      pTrader,
      emeraldCost,
      maxUses,
      villagerXp,
      MapUpdateTask { level: ServerLevel, merchant: AbstractVillager, mapStack: ItemStack ->
        locateStructure(level, structureSet, merchant.blockPosition(), 100, true)
          .thenOnServerThread(Consumer { pair: Pair<BlockPos?, Holder<Structure?>?> ->
            handleLocationFound(
              level,
              merchant,
              mapStack,
              displayName,
              destinationType,
              pair.first
            )
          })
      }
    )
  }

  private fun updateMapAsyncInternal(
    trader: Entity, emeraldCost: Int, maxUses: Int, villagerXp: Int, task: MapUpdateTask
  ): MerchantOffer? {
    if (trader is AbstractVillager) {
      val mapStack = CommonLogic.createEmptyMap()
      task.apply(trader.level() as ServerLevel, trader, mapStack)

      return MerchantOffer(
        ItemCost(Items.EMERALD, emeraldCost),
        Optional.of(ItemCost(Items.COMPASS)),
        mapStack,
        maxUses,
        villagerXp,
        0.2f
      )
    } else {
      ALConstants.logInfo(
        "Merchant is not of type {} - not running async logic",
        AbstractVillager::class.java.simpleName
      )
      return null
    }
  }

  interface MapUpdateTask {
    fun apply(level: ServerLevel?, merchant: AbstractVillager?, mapStack: ItemStack?)
  }
}
