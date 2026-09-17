package com.chinaex123.create_small_generator.init;

import com.chinaex123.create_small_generator.block.ElectricMotor.ElectricMotorBlock;
import com.chinaex123.create_small_generator.block.KineticDynamo.KineticDynamoBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.foundation.data.*;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import static com.chinaex123.create_small_generator.CreateSmallGenerator.REGISTRATE;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class CSGBlocks {

    /** 动力发电机 */
    public static final BlockEntry<KineticDynamoBlock> KINETIC_DYNAMO = REGISTRATE
            .block("kinetic_dynamo", KineticDynamoBlock::new)
            .initialProperties(() -> Blocks.STONE)
            .properties(p -> p
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.WOOD)
                    .strength(3.0f, 6.0f)
                    .isRedstoneConductor((state, getter, pos) -> false)
                    .noOcclusion()
                    .lightLevel((state) -> 0)
            )
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .item()
            .transform(customItemModel())
            .transform(axeOrPickaxe())
            .recipe((blockSpawnerBlockDataGenContext, registrateRecipeProvider) ->
                    MechanicalCraftingRecipeBuilder.shapedRecipe(blockSpawnerBlockDataGenContext.get())
                            .patternLine( " FGF ")
                            .patternLine( "FCDCF")
                            .patternLine( "GBABG")
                            .patternLine( "FCECF")
                            .patternLine( " FGF ")
                            .key('A', Ingredient.of(AllBlocks.BRASS_CASING))
                            .key('B', Ingredient.of(CSGItemTags.neoforgeTag("storage_blocks/copper")))
                            .key('C', Ingredient.of(AllItems.ANDESITE_ALLOY))
                            .key('D', Ingredient.of(AllBlocks.SHAFT))
                            .key('E', Ingredient.of(CSGItemTags.neoforgeTag("storage_blocks/redstone")))
                            .key('F', Ingredient.of(CSGItemTags.neoforgeTag("gems/amethyst")))
                            .key('G', Ingredient.of(AllItems.BRASS_INGOT))
                            .build(registrateRecipeProvider))
            .register();

    /** 电动马达 */
    public static final BlockEntry<ElectricMotorBlock> ELECTRIC_MOTOR = REGISTRATE
            .block("electric_motor", ElectricMotorBlock::new)
            .initialProperties(() -> Blocks.STONE)
            .properties(p -> p
                    .mapColor(MapColor.PODZOL)
                    .sound(SoundType.WOOD)
                    .strength(3.0f, 6.0f)
                    .isRedstoneConductor((state, getter, pos) -> false)
                    .noOcclusion()
                    .lightLevel((state) -> 0)
            )
            .blockstate(BlockStateGen.directionalBlockProvider(true))
            .item()
            .transform(customItemModel())
            .transform(axeOrPickaxe())
            .recipe((blockSpawnerBlockDataGenContext, registrateRecipeProvider) ->
                    MechanicalCraftingRecipeBuilder.shapedRecipe(blockSpawnerBlockDataGenContext.get())
                            .patternLine( " FGF ")
                            .patternLine( "FCDCF")
                            .patternLine( "GBABG")
                            .patternLine( "FCECF")
                            .patternLine( " FGF ")
                            .key('A', Ingredient.of(AllBlocks.BRASS_CASING))
                            .key('B', Ingredient.of(CSGItemTags.neoforgeTag("storage_blocks/copper")))
                            .key('C', Ingredient.of(CSGItemTags.neoforgeTag("gems/diamond")))
                            .key('D', Ingredient.of(AllBlocks.SHAFT))
                            .key('E', Ingredient.of(CSGItemTags.neoforgeTag("storage_blocks/redstone")))
                            .key('F', Ingredient.of(CSGItemTags.neoforgeTag("gems/amethyst")))
                            .key('G', Ingredient.of(AllItems.BRASS_INGOT))
                            .build(registrateRecipeProvider))
            .register();

//    public static final BlockEntry<DynamoBlock> KINETIC_DYNAMO = REGISTRATE
//            .block("kinetic_dynamo", DynamoBlock::new)
//            .initialProperties(SharedProperties::stone)
//            .properties(p -> p
//                    .mapColor(MapColor.PODZOL)
//                    .sound(SoundType.WOOD)
//                    .isRedstoneConductor((state, getter, pos) -> false)
//                    .noOcclusion()
//                    .lightLevel((state) -> 0)
//            )
//            .register();

//    public static final BlockEntry<DynamoBlock> KINETIC_DYNAMO =
//            REGISTRATE.block("kinetic_dynamo", DynamoBlock::new)
//                    .initialProperties(SharedProperties::stone)
//                    .simpleItem()
//                    .register();


    public static void register() {}
}
