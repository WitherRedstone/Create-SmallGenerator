package com.chinaex123.create_small_generator.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CSGServerConfig {
    public static final ModConfigSpec SPEC;

    public static ModConfigSpec.IntValue ENERGY_PER_STRESS;
    public static ModConfigSpec.DoubleValue STRESS_CAPACITY;
    public static ModConfigSpec.IntValue MAX_TRANSFER_RATE;

    public static ModConfigSpec.IntValue MOTOR_ENERGY_CAPACITY;
    public static ModConfigSpec.IntValue MOTOR_SPEED_PER_ENERGY;
    public static ModConfigSpec.IntValue MOTOR_STRESS_PER_ENERGY;
    public static ModConfigSpec.IntValue MOTOR_MAX_INPUT_RATE;
    public static ModConfigSpec.IntValue MOTOR_MAX_STRESS_OUTPUT;


    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("动力发电机").push("Kinetic Dynamo");
        ENERGY_PER_STRESS = builder
                .comment("每单位转速产生的能量")
                .comment("Energy produced per unit of rotation speed")
                .defineInRange("energyPerStress", 16, 1, Integer.MAX_VALUE);
        STRESS_CAPACITY = builder
                .comment("每 tick 消耗的应力容量")
                .comment("Kinetic Dynamo by the dynamo power generator")
                .defineInRange("stressCapacity", 16.0, 1, Integer.MAX_VALUE);
        MAX_TRANSFER_RATE = builder
                .comment("每 tick 传输的最大能量")
                .comment("Maximum energy per tick")
                .defineInRange("maxTransferRate", 100000, 100, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("电动马达").push("Electric Motor");
        MOTOR_ENERGY_CAPACITY = builder
                .comment("内部能量存储容量")
                .comment("Internal energy storage capacity")
                .defineInRange("motorEnergyCapacity", 100000, 1000, Integer.MAX_VALUE);
        MOTOR_SPEED_PER_ENERGY = builder
                .comment("每消耗1 FE产生的转速")
                .comment("Rotation speed generated per 1 FE consumed")
                .defineInRange("motorSpeedPerEnergy", 1, 1, 256);
        MOTOR_STRESS_PER_ENERGY = builder
                .comment("每消耗1 FE产生的应力容量系数（最终应力 = 消耗FE × 该值 ÷ 转速）")
                .comment("Stress capacity coefficient per 1 FE consumed (final stress = FE consumed × this value ÷ RPM)")
                .defineInRange("motorStressPerEnergy", 2, 1, Integer.MAX_VALUE);
        MOTOR_MAX_INPUT_RATE = builder
                .comment("每 tick 最大能量输入")
                .comment("Maximum energy input per tick")
                .defineInRange("motorMaxInputRate", 25600, 100, Integer.MAX_VALUE);
        MOTOR_MAX_STRESS_OUTPUT = builder
                .comment("最大应力输出")
                .comment("Maximum stress output")
                .defineInRange("motorMaxStressOutput", 512000, 100, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }
}
