package com.chinaex123.create_small_generator.block.KineticDynamo;

import com.chinaex123.create_small_generator.config.CommonConfig;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 动力发电机方块实体类，将输入的应力转换为FE能量并输出到前方相邻方块
 */
public class KineticDynamoBlockEntity extends GeneratingKineticBlockEntity {
    private float lastSpeed = 0;
    protected List<Entity> caughtEntities = new ArrayList<>();

    /** 能量缓冲区，存储本 tick 产生的能量供管道提取 */
    private int energyBuffer = 0;
    /** 每 tick 最大传输速率 */
    private final int maxTransferRate;

    /**
     * 能量存储处理器，允许外部从缓冲区提取能量
     */
    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // 发电机不接受外部能量输入
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(maxExtract, Math.min(energyBuffer, maxTransferRate));
            if (!simulate) {
                energyBuffer -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energyBuffer;
        }

        @Override
        public int getMaxEnergyStored() {
            return maxTransferRate;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public KineticDynamoBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.maxTransferRate = CommonConfig.MAX_TRANSFER_RATE.get();
    }

    /**
     * 获取此方块生成的转速
     * 由于这是消耗应力的发电机，不产生转速，因此返回0
     *
     * @return float 生成的转速值，固定为0
     */
    @Override
    public float getGeneratedSpeed() {
        return 0;
    }

    /**
     * 计算并返回应用到方块的应力值
     * 此方法用于确定方块消耗的应力单位数量
     *
     * @return float 返回应用的应力值，从配置读取
     */
    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = CommonConfig.STRESS_CAPACITY.get().floatValue();
        return CommonConfig.STRESS_CAPACITY.get().floatValue();
    }

    /**
     * 每刻执行的主要逻辑方法
     * 处理应力到FE能量的转换和输出
     * 检测转速变化并更新旋转状态
     * 智能能量输出：优先输出到方块能量存储，失败则尝试给物品充电
     */
    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        float speed = getSpeed();
        boolean speedChanged = Math.abs(speed - lastSpeed) > 0.01f;

        // 如果转速发生变化且不是客户端，则更新生成的旋转状态
        if (speedChanged && !level.isClientSide) {
            lastSpeed = speed;
            updateGeneratedRotation();
        }

        // 如果没有转速，不产生能量
        if (speed == 0) {
            energyBuffer = 0;
            return;
        }

        Direction front = getBlockState().getValue(KineticDynamoBlock.FACING);

        // 计算本 tick 产生的能量
        int energyToProduce = (int) (Math.abs(speed) * CommonConfig.ENERGY_PER_STRESS.get());
        if (energyToProduce <= 0) {
            return;
        }

        // 限制最大传输速率
        energyToProduce = Math.min(energyToProduce, maxTransferRate);

        // 1. 优先尝试输出到前方方块的能量存储
        IEnergyStorage blockEnergy = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                getBlockPos().relative(front),
                front.getOpposite()
        );

        if (blockEnergy != null && blockEnergy.canReceive()) {
            int transferred = blockEnergy.receiveEnergy(energyToProduce, false);
            if (transferred > 0) {
                // 剩余能量存入缓冲区
                energyBuffer = energyToProduce - transferred;
                setChanged();
                return;
            }
        }

        // 2. 尝试给置物台/容器中的物品充电
        Container itemStorage = HopperBlockEntity.getContainerAt(level, getBlockPos().relative(front));
        if (chargeItemsInContainer(itemStorage, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 3. 尝试给前方方块的物品处理器中的物品充电
        IItemHandler itemHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                getBlockPos().relative(front),
                front.getOpposite()
        );
        if (chargeItemsInHandler(itemHandler, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 4. 尝试给前方区域的掉落物充电
        if (chargeEntitiesInFront(front, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 5. 如果以上都失败，将能量存入缓冲区供管道提取
        energyBuffer = energyToProduce;
        setChanged();
    }

    /**
     * 为容器中的物品充电
     *
     * @param container 容器实例
     * @param energy 需要输出的能量
     * @return 是否成功充入能量
     */
    private boolean chargeItemsInContainer(Container container, int energy) {
        if (container == null) {
            return false;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            IEnergyStorage itemEnergy = container.getItem(i).getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive() && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
                int transferred = itemEnergy.receiveEnergy(energy, false);
                if (transferred > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 为物品处理器中的物品充电
     *
     * @param itemHandler 物品处理器
     * @param energy 需要输出的能量
     * @return 是否成功充入能量
     */
    private boolean chargeItemsInHandler(IItemHandler itemHandler, int energy) {
        if (itemHandler == null) {
            return false;
        }

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            IEnergyStorage itemEnergy = itemHandler.getStackInSlot(i).getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive() && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
                int transferred = itemEnergy.receiveEnergy(energy, false);
                if (transferred > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 为前方区域的实体（掉落物、玩家等）携带的物品充电
     *
     * @param front 前方方向
     * @param energy 需要输出的能量
     * @return 是否成功充入能量
     */
    private boolean chargeEntitiesInFront(Direction front, int energy) {
        if (level == null) {
            return false;
        }

        caughtEntities.clear();
        caughtEntities = level.getEntities(
                null,
                new AABB(getBlockPos().relative(front)).expandTowards(Vec3.atLowerCornerOf(front.getNormal()).scale(0))
        );

        for (Entity entity : caughtEntities) {
            // 尝试给掉落物持有的物品充电
            if (entity instanceof ItemEntity itemEntity) {
                IEnergyStorage itemEnergy = itemEntity.getItem().getCapability(Capabilities.EnergyStorage.ITEM);
                if (itemEnergy != null && itemEnergy.canReceive() && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
                    int transferred = itemEnergy.receiveEnergy(energy, false);
                    if (transferred > 0) {
                        return true;
                    }
                }
            }

            // 尝试给玩家背包中的物品充电
            if (entity instanceof Player player) {
                if (chargeItemsInContainer(player.getInventory(), energy)) {
                    return true;
                }

                IItemHandler playerHandler = player.getCapability(Capabilities.ItemHandler.ENTITY);
                if (chargeItemsInHandler(playerHandler, energy)) {
                    return true;
                }
            }

            // 尝试给其他实体的物品处理器充电
            IItemHandler entityHandler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (chargeItemsInHandler(entityHandler, energy)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定方向的能量处理器
     * 仅在正面方向提供能量提取接口（供管道拉取）
     *
     * @param side 查询的方向
     * @return IEnergyStorage 正面方向返回能量处理器，其他方向返回 null
     */
    @Nullable
    public IEnergyStorage getEnergyHandler(Direction side) {
        Direction front = getBlockState().getValue(KineticDynamoBlock.FACING);
        if (side == front) {
            return energyHandler;
        }
        return null;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.energyBuffer = compound.getInt("EnergyBuffer");
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("EnergyBuffer", this.energyBuffer);
    }

    /**
     * 添加到护目镜提示信息
     * 显示当前转速和能量输出信息
     *
     * @param tooltip 提示组件列表
     * @param isPlayerSneaking 玩家是否正在潜行
     * @return boolean 始终返回true表示已添加自定义提示
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        float speed = Math.abs(getSpeed());
        int energyProduced = (int) (speed * CommonConfig.ENERGY_PER_STRESS.get());
        int energyPerSecond = energyProduced * 20;

        // 添加每tick能量产出信息
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.energy_output",
                        CreateLang.number(energyProduced).component().withStyle(ChatFormatting.YELLOW),
                        CreateLang.text("FE/t").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 添加每秒能量产出信息
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.energy_per_second",
                        CreateLang.number(energyPerSecond).component().withStyle(ChatFormatting.GREEN),
                        CreateLang.text("FE/s").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 添加应力消耗信息
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.stress_consumed",
                        CreateLang.number(CommonConfig.STRESS_CAPACITY.get()).component().withStyle(ChatFormatting.RED))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        return true;
    }
}
