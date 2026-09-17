package com.chinaex123.create_small_generator.init;

import com.chinaex123.create_small_generator.block.ElectricMotor.ElectricMotorBlockEntity;
import com.chinaex123.create_small_generator.block.ElectricMotor.ElectricMotorRenderer;
import com.chinaex123.create_small_generator.block.ElectricMotor.ElectricMotorVisual;
import com.chinaex123.create_small_generator.block.KineticDynamo.KineticDynamoBlockEntity;
import com.chinaex123.create_small_generator.block.KineticDynamo.KineticDynamoRenderer;
import com.chinaex123.create_small_generator.block.KineticDynamo.KineticDynamoVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.chinaex123.create_small_generator.CreateSmallGenerator.REGISTRATE;

public class CSGBlockEntities {

    /** 动力发电机 */
    public static final BlockEntityEntry<KineticDynamoBlockEntity> KINETIC_DYNAMO = REGISTRATE
            .blockEntity("kinetic_dynamo", KineticDynamoBlockEntity::new)
            .visual(() -> KineticDynamoVisual::new, false)
            .validBlocks(CSGBlocks.KINETIC_DYNAMO)
            .renderer(() -> KineticDynamoRenderer::new)
            .register();

    /** 电动马达 */
    public static final BlockEntityEntry<ElectricMotorBlockEntity> ELECTRIC_MOTOR = REGISTRATE
            .blockEntity("electric_motor", ElectricMotorBlockEntity::new)
            .visual(() -> ElectricMotorVisual::new, false)
            .validBlocks(CSGBlocks.ELECTRIC_MOTOR)
            .renderer(() -> ElectricMotorRenderer::new)
            .register();

    public static void register() {}
}
