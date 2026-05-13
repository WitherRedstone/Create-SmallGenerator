package com.chinaex123.create_small_generator.init;

import com.chinaex123.create_small_generator.CreateSmallGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CSGCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateSmallGenerator.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_SMALL_GENERATOR_TAB =
            CREATIVE_MODE_TAB.register("create_small_generator_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(CSGBlocks.KINETIC_DYNAMO.get()))
                    .title(Component.translatable("itemGroup.create_small_generator_tab"))
                    .displayItems((parameters, output) -> {

                        output.accept(CSGBlocks.KINETIC_DYNAMO.get()); // 动力发电机
                        output.accept(CSGBlocks.ELECTRIC_MOTOR.get()); // 电动马达

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
