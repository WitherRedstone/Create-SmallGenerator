package com.chinaex123.create_small_generator.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    public static final ModConfigSpec SPEC;

    /**
     * 动力发电机每单位转速产生的FE能量
     * 默认值: 16 FE/t per RPM
     */
    public static ModConfigSpec.IntValue ENERGY_PER_STRESS;

    /**
     * 动力发电机消耗的应力容量(单位: su)
     * 默认值: 16 su
     */
    public static ModConfigSpec.DoubleValue STRESS_CAPACITY;

    /**
     * 动力发电机每次传输的最大能量
     * 默认值: 100000 FE/t
     */
    public static ModConfigSpec.IntValue MAX_TRANSFER_RATE;

    /**
     * 电动马达内部能量存储容量
     * 默认值: 100000 FE
     */
    public static ModConfigSpec.IntValue MOTOR_ENERGY_CAPACITY;

    /**
     * 电动马达每消耗1 FE产生的转速(RPM)
     * 默认值: 1 RPM per FE，最大转速限制为 256 RPM
     */
    public static ModConfigSpec.IntValue MOTOR_SPEED_PER_ENERGY;

    /**
     * 电动马达应力容量系数
     * 计算公式: 最终应力 = (消耗FE × 该值) / 当前转速
     * 默认值: 1
     */
    public static ModConfigSpec.IntValue MOTOR_STRESS_PER_ENERGY;

    /**
     * 电动马达每 tick 最大能量输入
     * 默认值: 25600 FE/t
     */
    public static ModConfigSpec.IntValue MOTOR_MAX_INPUT_RATE;

    /**
     * 电动马达最大应力输出
     * 默认值: 2560000 SU
     */
    public static ModConfigSpec.IntValue MOTOR_MAX_STRESS_OUTPUT;


    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("kinetic_dynamo");

        ENERGY_PER_STRESS = builder
                .comment("How much FE energy is produced per unit of rotation speed (FE/t per RPM)")
                .translation("config.create_small_generator.energy_per_stress")
                .defineInRange("energyPerStress", 16, 1, Integer.MAX_VALUE);

        STRESS_CAPACITY = builder
                .comment("The stress capacity consumed by the kinetic_dynamo block (in stress units)")
                .translation("config.create_small_generator.stress_capacity")
                .defineInRange("stressCapacity", 16.0, 1, 100.0);

        MAX_TRANSFER_RATE = builder
                .comment("Maximum energy transfer rate per tick (in FE/t)")
                .translation("config.create_small_generator.max_transfer_rate")
                .defineInRange("maxTransferRate", 100000, 100, Integer.MAX_VALUE);

        builder.pop();

        builder.push("electric_motor");

        MOTOR_ENERGY_CAPACITY = builder
                .comment("Internal energy storage capacity of electric motor (in FE)")
                .translation("config.create_small_generator.motor_energy_capacity")
                .defineInRange("motorEnergyCapacity", 100000, 1000, Integer.MAX_VALUE);

        MOTOR_SPEED_PER_ENERGY = builder
                .comment("Rotation speed generated per FE consumed (RPM per FE). Max speed is capped at 256 RPM.")
                .translation("config.create_small_generator.motor_speed_per_energy")
                .defineInRange("motorSpeedPerEnergy", 1, 1, 256);

        MOTOR_STRESS_PER_ENERGY = builder
                .comment("Stress capacity coefficient per FE consumed. Final stress output = (consumed_FE × this_value) / current_RPM. Default: 1")
                .translation("config.create_small_generator.motor_stress_per_energy")
                .defineInRange("motorStressPerEnergy", 2, 1, 100);

        MOTOR_MAX_INPUT_RATE = builder
                .comment("Maximum energy input rate per tick for electric motor (in FE/t)")
                .translation("config.create_small_generator.motor_max_input_rate")
                .defineInRange("motorMaxInputRate", 25600, 100, Integer.MAX_VALUE);

        MOTOR_MAX_STRESS_OUTPUT = builder
                .comment("Maximum stress output of electric motor (in SU)")
                .translation("config.create_small_generator.motor_max_stress_output")
                .defineInRange("motorMaxStressOutput", 512000, 100, Integer.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }
}
