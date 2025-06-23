package brightspark.asynclocator.mixins;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.logic.ExplorationMapFunctionLogic;
import brightspark.asynclocator.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SetNameFunction.class)
public class SetNameFunctionMixin {
    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"
            )
    )
    public ItemStack deferSetName(ItemStack stack, DataComponentType dataComponentType, Component name) {
        if (Services.CONFIG.explorationMapEnabled() && CommonLogic.isEmptyPendingMap(stack)) {
            ALConstants.logDebug("Intercepted SetNameFunction#run call");
            ExplorationMapFunctionLogic.cacheName(stack, name);
        } else
            stack.set(dataComponentType, name);
        return stack;
    }
}