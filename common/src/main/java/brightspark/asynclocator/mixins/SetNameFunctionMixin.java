package brightspark.asynclocator.mixins;

import brightspark.asynclocator.AsyncLocatorMod;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.logic.ExplorationMapFunctionLogic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Consumer;

import static brightspark.asynclocator.AsyncLocatorMod.INSTANCE;

@Mixin(SetNameFunction.class)
public class SetNameFunctionMixin {

    @Redirect(
            // use ide to lookup bytecode for SetNameFunction#run
            method = "method_53386",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    public Object deferSetName(ItemStack stack, DataComponentType dataComponentType, Object name) {
        if (INSTANCE.getCONFIG().getExplorationMapEnabled().get() && CommonLogic.isEmptyPendingMap(stack)) {
            AsyncLocatorMod.INSTANCE.getLOGGER().debug("Intercepted SetNameFunction#run call");
            ExplorationMapFunctionLogic.cacheName(stack, (Component) name);
        } else
            stack.set(dataComponentType, name);
        return stack;
    }
}