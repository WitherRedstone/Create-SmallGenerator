package com.chinaex123.create_small_generator.block.ElectricMotor;

import com.chinaex123.create_small_generator.config.CommonConfig;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电动马达方块实体
 * 将 FE 能量转换为动能（应力）输出
 */
public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity {

    /** 马达是否处于激活状态（有能量输入时激活） */
    private boolean active = false;

    /** 当前产生的转速（RPM） */
    private float currentSpeed = 0;

    /** 记录当前 tick 的实际 FE 消耗量 */
    private int currentConsumption = 0;

    /** 内部存储的 FE 能量值 */
    private int storedEnergy = 0;

    /** 能量存储容量上限（从配置读取） */
    private final int capacity;

    /** 每 tick 最大能量输入速率（从配置读取） */
    private final int maxTransfer;

    /**
     * FE 能量存储处理器
     * 实现 NeoForge 的能量存储接口，处理能量的存入和提取
     */
    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = Math.min(maxReceive, Math.min(capacity - storedEnergy, maxTransfer));
            if (!simulate) {
                storedEnergy += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(maxExtract, storedEnergy);
            if (!simulate) {
                storedEnergy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return storedEnergy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    /**
     * 构造函数
     *
     * @param typeIn 方块实体类型
     * @param pos 方块位置
     * @param state 方块状态
     */
    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.capacity = CommonConfig.MOTOR_ENERGY_CAPACITY.get();
        this.maxTransfer = CommonConfig.MOTOR_MAX_INPUT_RATE.get();
    }

    /**
     * 获取马达产生的转速
     * Create 框架调用此方法获取该方块作为动力源提供的转速
     *
     * @return 当前转速（RPM），未激活时返回 0
     */
    @Override
    public float getGeneratedSpeed() {
        if (!active) {
            return 0;
        }
        return currentSpeed;
    }

    /**
     * 计算并返回该方块提供的应力容量
     * Create 框架会将返回值乘以当前转速得到总应力输出
     * 因此需要返回"每 RPM 的应力容量"而非总应力
     *
     * 计算公式：返回值为 (实际消耗FE × 应力系数 / 当前转速)
     * 最终应力 = 返回值 × 转速 = 实际消耗FE × 应力系数（受最大值限制）
     *
     * @return 每 RPM 的应力容量，未激活时返回 0
     */
    @Override
    public float calculateAddedStressCapacity() {
        if (!active || currentSpeed <= 0) {
            this.lastCapacityProvided = 0;
            return 0;
        }

        // 计算本 tick 产生的总应力
        int stressPerEnergy = CommonConfig.MOTOR_STRESS_PER_ENERGY.get();
        int totalStress = currentConsumption * stressPerEnergy;

        // 应用最大应力限制
        int maxStress = CommonConfig.MOTOR_MAX_STRESS_OUTPUT.get();
        int limitedStress = Math.min(totalStress, maxStress);

        // Create 会将返回值乘以转速，所以需要返回"每 RPM 的应力容量"
        float stressPerRPM = limitedStress / currentSpeed;

        this.lastCapacityProvided = limitedStress;
        return stressPerRPM;
    }

    /**
     * 每 tick 执行的主要逻辑
     * 处理 FE 能量消耗、转速计算和应力输出
     */
    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        // 每刻开始时重置消耗量
        currentConsumption = 0;

        // 计算本 tick 应该消耗多少 FE
        int targetConsumption = maxTransfer;
        int actualConsumption = Math.min(storedEnergy, targetConsumption);

        boolean wasActive = active;
        active = actualConsumption > 0;

        int consumed = 0;
        int newSpeed = 0;

        if (active) {
            // 根据最大应力限制反推最大允许的 FE 消耗
            int maxStress = CommonConfig.MOTOR_MAX_STRESS_OUTPUT.get();
            int stressPerEnergy = CommonConfig.MOTOR_STRESS_PER_ENERGY.get();
            int maxAllowedConsumption = maxStress / stressPerEnergy;

            // 限制实际消耗不超过最大应力对应的 FE 量
            int limitedConsumption = Math.min(actualConsumption, maxAllowedConsumption);

            // 消耗 FE
            consumed = energyHandler.extractEnergy(limitedConsumption, false);
            if (consumed > 0) {
                // 记录实际消耗（用于应力计算）
                currentConsumption = consumed;

                // 转速受限于 256 RPM
                newSpeed = Math.min(consumed * CommonConfig.MOTOR_SPEED_PER_ENERGY.get(), 256);
            } else {
                active = false;
                currentConsumption = 0;
            }
        } else {
            currentConsumption = 0;
        }

        // 更新转速
        if (newSpeed != currentSpeed) {
            currentSpeed = newSpeed;
            setChanged();
        }

        // 状态变化时标记
        if (wasActive != active) {
            setChanged();
        }

        updateGeneratedRotation();
    }

    /**
     * 从 NBT 标签读取方块实体数据
     * 用于世界保存/加载和数据包同步
     *
     * @param compound NBT 标签数据
     * @param registries 注册表提供者
     * @param clientPacket 是否为客户端数据包
     */
    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.active = compound.getBoolean("Active");
        this.currentSpeed = compound.getFloat("CurrentSpeed");
        this.storedEnergy = compound.getInt("StoredEnergy");
        this.currentConsumption = compound.getInt("CurrentConsumption");
    }

    /**
     * 将方块实体数据写入 NBT 标签
     * 用于世界保存/加载和数据包同步
     *
     * @param compound NBT 标签数据
     * @param registries 注册表提供者
     * @param clientPacket 是否为客户端数据包
     */
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putBoolean("Active", this.active);
        compound.putFloat("CurrentSpeed", this.currentSpeed);
        compound.putInt("StoredEnergy", this.storedEnergy);
        compound.putInt("CurrentConsumption", this.currentConsumption);
    }

    /**
     * 获取能量存储处理器的静态方法
     * 用于 NeoForge 能力系统注册
     *
     * @param be 方块实体实例
     * @param side 查询的方向
     * @return 能量存储处理器，仅在正面方向返回
     */
    @Nullable
    public static IEnergyStorage getEnergyHandler(ElectricMotorBlockEntity be, Direction side) {
        return be.getEnergyHandler(side);
    }

    /**
     * 获取指定方向的能量存储处理器
     * 仅在马达正面（FACING 方向）提供能量接口
     *
     * @param side 查询的方向
     * @return 能量存储处理器，非正面方向返回 null
     */
    @Nullable
    private IEnergyStorage getEnergyHandler(Direction side) {
        Direction front = getBlockState().getValue(ElectricMotorBlock.FACING);
        if (side == front) {
            return energyHandler;
        }
        return null;
    }

    /**
     * 添加到 goggles（工程师护目镜）提示信息
     * 显示马达的当前状态：转速、能量消耗、应力输出等信息
     *
     * @param tooltip 提示文本列表
     * @param isPlayerSneaking 玩家是否潜行
     * @return 总是返回 true，表示已添加自定义提示
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        int speed = Math.round(Math.abs(getSpeed()));

        // 显示实际消耗
        int energyConsumed = currentConsumption;
        int energyPerSecond = energyConsumed * 20;

        // 计算应力（整数）
        int stressGenerated = energyConsumed * CommonConfig.MOTOR_STRESS_PER_ENERGY.get();
        int maxStress = CommonConfig.MOTOR_MAX_STRESS_OUTPUT.get();

        // 应用最大值限制
        stressGenerated = Math.min(stressGenerated, maxStress);

        CreateLang.translate("create_small_generator.tooltip.electric_motor.speed",
                        CreateLang.number(speed).component().withStyle(ChatFormatting.AQUA),
                        CreateLang.text("RPM").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.translate("create_small_generator.tooltip.electric_motor.energy_consumed",
                        CreateLang.number(energyConsumed).component().withStyle(ChatFormatting.RED),
                        CreateLang.text("FE/t").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.translate("create_small_generator.tooltip.electric_motor.energy_per_second",
                        CreateLang.number(energyPerSecond).component().withStyle(ChatFormatting.YELLOW),
                        CreateLang.text("FE/s").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.translate("create_small_generator.tooltip.electric_motor.stress_generated",
                        CreateLang.number(stressGenerated).component().withStyle(ChatFormatting.GREEN),
                        CreateLang.text("/").component().withStyle(ChatFormatting.GRAY),
                        CreateLang.number(maxStress).component().withStyle(ChatFormatting.GOLD))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        return true;
    }
}
