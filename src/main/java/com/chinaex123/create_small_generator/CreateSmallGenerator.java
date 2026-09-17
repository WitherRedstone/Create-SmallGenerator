package com.chinaex123.create_small_generator;

import com.chinaex123.create_small_generator.block.ElectricMotor.ElectricMotorBlockEntity;
import com.chinaex123.create_small_generator.block.KineticDynamo.KineticDynamoBlockEntity;
import com.chinaex123.create_small_generator.config.CSGServerConfig;
import com.chinaex123.create_small_generator.init.CSGBlockEntities;
import com.chinaex123.create_small_generator.init.CSGBlocks;
import com.chinaex123.create_small_generator.init.CSGCreativeTabs;
import com.chinaex123.create_small_generator.init.CSGPartialModel;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(CreateSmallGenerator.MOD_ID)
public class CreateSmallGenerator {
    public static final String MOD_ID = "create_small_generator";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    static {
        REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }

    public CreateSmallGenerator(IEventBus modEventBus, ModContainer modContainer) {
        CSGPartialModel.register();
        REGISTRATE.setCreativeTab(CSGCreativeTabs.CREATE_SMALL_GENERATOR_TAB);
        REGISTRATE.registerEventListeners(modEventBus);

        CSGBlocks.register();
        CSGBlockEntities.register();
        CSGCreativeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CSGServerConfig.SPEC);

        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    CSGBlockEntities.KINETIC_DYNAMO.get(),
                    KineticDynamoBlockEntity::getEnergyHandler);
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                    CSGBlockEntities.ELECTRIC_MOTOR.get(),
                    ElectricMotorBlockEntity::getEnergyHandler);
        });

        modEventBus.addListener(FMLCommonSetupEvent.class, event -> {
            // 注册方块的应力影响值
            event.enqueueWork(() -> {
                BlockStressValues.IMPACTS.register(CSGBlocks.KINETIC_DYNAMO.get(),
                        () -> CSGServerConfig.STRESS_CAPACITY.get()
                );
            });
        });
    }

    public static ResourceLocation id(String path){
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
