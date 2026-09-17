package com.chinaex123.create_small_generator.block.ElectricMotor;

import com.chinaex123.create_small_generator.config.CSGServerConfig;
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
 * 电动马达方块实体。
 * <p>
 * 继承自 Create 的 {@link GeneratingKineticBlockEntity}，作为动能来源。
 * 从正面接收 FE 能量，按配置比例将能量转化为旋转速度与应力输出。
 * 能量消耗、转速上限、应力上限均由服务端配置决定。
 */
public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity {

    /** 当前是否处于运行状态（有能量消耗且产出转速） */
    private boolean active = false;
    /** 当前输出的旋转速度（RPM） */
    private float currentSpeed = 0;
    /** 当前每 tick 消耗的能量（FE/t） */
    private int currentConsumption = 0;
    /** 内部存储的能量（FE） */
    private int storedEnergy = 0;
    /** 能量存储上限（FE），由配置决定 */
    private final int capacity;
    /** 最大能量输入速率（FE/t），由配置决定 */
    private final int maxTransfer;

    /**
     * 能量处理能力实现。
     * <p>
     * 允许外部通过正面输入或提取能量，接收与提取均受容量和最大传输速率限制。
     * 非模拟操作会更新内部能量并标记变更。
     */
    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // 接收量受输入速率和剩余容量双重限制
            int received = Math.min(maxReceive, Math.min(capacity - storedEnergy, maxTransfer));
            if (!simulate) {
                storedEnergy += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            // 提取量不能超过当前存储
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
     * 构造电动马达方块实体。
     *
     * @param typeIn 方块实体类型
     * @param pos    方块位置
     * @param state  方块状态
     */
    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.capacity = CSGServerConfig.MOTOR_ENERGY_CAPACITY.get();
        this.maxTransfer = CSGServerConfig.MOTOR_MAX_INPUT_RATE.get();
    }

    /**
     * 获取当前产生的旋转速度。
     *
     * @return 运行中返回当前速度，否则返回 0
     */
    @Override
    public float getGeneratedSpeed() {
        if (!active) {
            return 0;
        }
        return currentSpeed;
    }

    /**
     * 计算附加的应力容量。
     * <p>
     * 根据当前能量消耗与配置的每单位能量应力系数计算总应力，
     * 再受最大应力输出限制，最终换算为每 RPM 的应力值。
     *
     * @return 每 RPM 的应力容量，无输出时返回 0
     */
    @Override
    public float calculateAddedStressCapacity() {
        if (!active || currentSpeed <= 0) {
            this.lastCapacityProvided = 0;
            return 0;
        }

        int stressPerEnergy = CSGServerConfig.MOTOR_STRESS_PER_ENERGY.get();
        int totalStress = currentConsumption * stressPerEnergy;

        int maxStress = CSGServerConfig.MOTOR_MAX_STRESS_OUTPUT.get();
        int limitedStress = Math.min(totalStress, maxStress);

        float stressPerRPM = limitedStress / currentSpeed;

        this.lastCapacityProvided = limitedStress;
        return stressPerRPM;
    }

    /**
     * 每 tick 更新逻辑。
     * <p>
     * 仅在服务端执行：根据当前存储能量确定是否运行，
     * 按配置限制消耗能量并换算转速，随后同步旋转状态。
     */
    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        currentConsumption = 0;

        int targetConsumption = maxTransfer;
        int actualConsumption = Math.min(storedEnergy, targetConsumption);

        boolean wasActive = active;
        active = actualConsumption > 0;

        int consumed = 0;
        int newSpeed = 0;

        if (active) {
            int maxStress = CSGServerConfig.MOTOR_MAX_STRESS_OUTPUT.get();
            int stressPerEnergy = CSGServerConfig.MOTOR_STRESS_PER_ENERGY.get();
            // 由最大应力反推允许的最大能量消耗
            int maxAllowedConsumption = maxStress / stressPerEnergy;

            int limitedConsumption = Math.min(actualConsumption, maxAllowedConsumption);

            consumed = energyHandler.extractEnergy(limitedConsumption, false);
            if (consumed > 0) {
                currentConsumption = consumed;

                // 转速由消耗能量换算，并限制在 256 RPM 以内
                newSpeed = Math.min(consumed * CSGServerConfig.MOTOR_SPEED_PER_ENERGY.get(), 256);
            } else {
                active = false;
                currentConsumption = 0;
            }
        } else {
            currentConsumption = 0;
        }

        if (newSpeed != currentSpeed) {
            currentSpeed = newSpeed;
            setChanged();
        }

        if (wasActive != active) {
            setChanged();
        }

        updateGeneratedRotation();
    }

    /**
     * 从 NBT 读取数据。
     * <p>
     * 除父类数据外，还读取运行状态、当前转速、存储能量与当前消耗。
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
     * 将数据写入 NBT。
     * <p>
     * 除父类数据外，还写入运行状态、当前转速、存储能量与当前消耗。
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
     * 静态获取能量处理能力的入口。
     *
     * @param be   方块实体实例
     * @param side 访问方向
     * @return 对应方向的能量处理器，无能力时返回 null
     */
    @Nullable
    public static IEnergyStorage getEnergyHandler(ElectricMotorBlockEntity be, Direction side) {
        return be.getEnergyHandler(side);
    }

    /**
     * 获取指定方向的能量处理能力。
     * <p>
     * 仅当访问方向为方块正面时返回能量处理器，其余方向返回 null。
     *
     * @param side 访问方向
     * @return 正面返回能量处理器，否则返回 null
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
     * 添加护目镜提示信息。
     * <p>
     * 显示当前转速、每 tick 能量消耗、每秒能量消耗以及产生的应力/最大应力。
     *
     * @param tooltip        提示信息列表
     * @param isPlayerSneaking 玩家是否潜行
     * @return 始终返回 true
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        int speed = Math.round(Math.abs(getSpeed()));

        int energyConsumed = currentConsumption;
        int energyPerSecond = energyConsumed * 20;

        int stressGenerated = energyConsumed * CSGServerConfig.MOTOR_STRESS_PER_ENERGY.get();
        int maxStress = CSGServerConfig.MOTOR_MAX_STRESS_OUTPUT.get();

        stressGenerated = Math.min(stressGenerated, maxStress);

        // 转速行
        CreateLang.translate("create_small_generator.tooltip.electric_motor.speed",
                        CreateLang.number(speed).component().withStyle(ChatFormatting.AQUA),
                        CreateLang.text("RPM").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 每 tick 能量消耗行
        CreateLang.translate("create_small_generator.tooltip.electric_motor.energy_consumed",
                        CreateLang.number(energyConsumed).component().withStyle(ChatFormatting.RED),
                        CreateLang.text("FE/t").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 每秒能量消耗行
        CreateLang.translate("create_small_generator.tooltip.electric_motor.energy_per_second",
                        CreateLang.number(energyPerSecond).component().withStyle(ChatFormatting.YELLOW),
                        CreateLang.text("FE/s").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 应力产出 / 上限行
        CreateLang.translate("create_small_generator.tooltip.electric_motor.stress_generated",
                        CreateLang.number(stressGenerated).component().withStyle(ChatFormatting.GREEN),
                        CreateLang.text("/").component().withStyle(ChatFormatting.GRAY),
                        CreateLang.number(maxStress).component().withStyle(ChatFormatting.GOLD))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        return true;
    }
}