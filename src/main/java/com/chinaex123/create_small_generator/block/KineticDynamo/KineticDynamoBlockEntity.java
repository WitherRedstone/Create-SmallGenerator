package com.chinaex123.create_small_generator.block.KineticDynamo;

import com.chinaex123.create_small_generator.config.CSGServerConfig;
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

/**
 * 动能发电机方块实体。
 * <p>
 * 继承自 Create 的 {@link GeneratingKineticBlockEntity}，将旋转动能转化为 FE 能量。
 * 转速越高，每 tick 产出的能量越多，产出量受配置的最大传输速率限制。
 * 能量优先输出到正面的能量容器，其次为物品容器中的可充能物品、
 * 物品处理器中的可充能物品，最后为正面区域内的实体（含掉落物与玩家）。
 * 若均无法输出，则将能量暂存于内部缓冲区。
 */
public class KineticDynamoBlockEntity extends GeneratingKineticBlockEntity {

    /** 上一次记录的转速，用于检测转速变化并更新旋转 */
    private float lastSpeed = 0;
    /** 缓存的正面区域实体列表，用于充能检测 */
    protected List<Entity> caughtEntities = new ArrayList<>();

    /** 内部能量缓冲区（FE），当无法对外输出时暂存能量 */
    private int energyBuffer = 0;
    /** 最大传输速率（FE/t），由配置决定 */
    private final int maxTransferRate;

    /**
     * 能量处理能力实现。
     * <p>
     * 仅允许提取能量，禁止外部输入；提取量受缓冲区与最大传输速率限制。
     */
    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        /** 禁止外部输入能量 */
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        /** 允许提取能量，提取量受缓冲区与最大传输速率限制 */
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

    /**
     * 构造动能发电机方块实体。
     *
     * @param typeIn 方块实体类型
     * @param pos    方块位置
     * @param state  方块状态
     */
    public KineticDynamoBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.maxTransferRate = CSGServerConfig.MAX_TRANSFER_RATE.get();
    }

    /**
     * 获取产生的旋转速度。
     * <p>
     * 动能发电机不产生旋转，始终返回 0。
     *
     * @return 恒为 0
     */
    @Override
    public float getGeneratedSpeed() {
        return 0;
    }

    /**
     * 计算施加的应力。
     * <p>
     * 该方块作为动能消耗方，始终按配置施加固定应力。
     *
     * @return 配置的应力值
     */
    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = CSGServerConfig.STRESS_CAPACITY.get().floatValue();
        return CSGServerConfig.STRESS_CAPACITY.get().floatValue();
    }

    /**
     * 每 tick 更新逻辑。
     * <p>
     * 仅在服务端执行：根据转速计算应产出能量，
     * 依次尝试输出到正面能量容器、物品容器、物品处理器与实体，
     * 若均失败则暂存至内部缓冲区。
     */
    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        float speed = getSpeed();
        boolean speedChanged = Math.abs(speed - lastSpeed) > 0.01f;

        // 转速变化时更新旋转状态
        if (speedChanged && !level.isClientSide) {
            lastSpeed = speed;
            updateGeneratedRotation();
        }

        // 无转速时清空缓冲区并跳过
        if (speed == 0) {
            energyBuffer = 0;
            return;
        }

        Direction front = getBlockState().getValue(KineticDynamoBlock.FACING);

        // 根据转速与配置系数计算应产出能量
        int energyToProduce = (int) (Math.abs(speed) * CSGServerConfig.ENERGY_PER_STRESS.get());
        if (energyToProduce <= 0) {
            return;
        }

        // 产出量不超过最大传输速率
        energyToProduce = Math.min(energyToProduce, maxTransferRate);

        // 优先输出到正面的能量容器
        IEnergyStorage blockEnergy = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                getBlockPos().relative(front),
                front.getOpposite()
        );

        if (blockEnergy != null && blockEnergy.canReceive()) {
            int transferred = blockEnergy.receiveEnergy(energyToProduce, false);
            if (transferred > 0) {
                energyBuffer = energyToProduce - transferred;
                setChanged();
                return;
            }
        }

        // 其次尝试为正面物品容器中的可充能物品充能
        Container itemStorage = HopperBlockEntity.getContainerAt(level, getBlockPos().relative(front));
        if (chargeItemsInContainer(itemStorage, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 再次尝试为正面物品处理器中的可充能物品充能
        IItemHandler itemHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                getBlockPos().relative(front),
                front.getOpposite()
        );
        if (chargeItemsInHandler(itemHandler, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 最后尝试为正面区域内的实体充能
        if (chargeEntitiesInFront(front, energyToProduce)) {
            energyBuffer = 0;
            return;
        }

        // 均无法输出时暂存至缓冲区
        energyBuffer = energyToProduce;
        setChanged();
    }

    /**
     * 为容器中的可充能物品充能。
     *
     * @param container 目标容器
     * @param energy    可用能量
     * @return 若有物品成功接收能量则返回 true
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
     * 为物品处理器中的可充能物品充能。
     *
     * @param itemHandler 目标物品处理器
     * @param energy      可用能量
     * @return 若有物品成功接收能量则返回 true
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
     * 为正面区域内的实体充能。
     * <p>
     * 支持掉落物、玩家（含物品栏与实体能力）以及其他拥有物品能力的实体。
     *
     * @param front  方块朝向
     * @param energy 可用能量
     * @return 若有实体成功接收能量则返回 true
     */
    private boolean chargeEntitiesInFront(Direction front, int energy) {
        if (level == null) {
            return false;
        }

        caughtEntities.clear();
        // 获取正面方块位置范围内的所有实体
        caughtEntities = level.getEntities(
                null,
                new AABB(getBlockPos().relative(front)).expandTowards(Vec3.atLowerCornerOf(front.getNormal()).scale(0))
        );

        for (Entity entity : caughtEntities) {
            // 掉落物：直接为物品充能
            if (entity instanceof ItemEntity itemEntity) {
                IEnergyStorage itemEnergy = itemEntity.getItem().getCapability(Capabilities.EnergyStorage.ITEM);
                if (itemEnergy != null && itemEnergy.canReceive() && itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {
                    int transferred = itemEnergy.receiveEnergy(energy, false);
                    if (transferred > 0) {
                        return true;
                    }
                }
            }

            // 玩家：为物品栏与实体物品能力充能
            if (entity instanceof Player player) {
                if (chargeItemsInContainer(player.getInventory(), energy)) {
                    return true;
                }

                IItemHandler playerHandler = player.getCapability(Capabilities.ItemHandler.ENTITY);
                if (chargeItemsInHandler(playerHandler, energy)) {
                    return true;
                }
            }

            // 其他实体：为实体物品能力充能
            IItemHandler entityHandler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (chargeItemsInHandler(entityHandler, energy)) {
                return true;
            }
        }
        return false;
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
    public IEnergyStorage getEnergyHandler(Direction side) {
        Direction front = getBlockState().getValue(KineticDynamoBlock.FACING);
        if (side == front) {
            return energyHandler;
        }
        return null;
    }

    /**
     * 从 NBT 读取数据。
     * <p>
     * 除父类数据外，还读取内部能量缓冲区。
     */
    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        this.energyBuffer = compound.getInt("EnergyBuffer");
    }

    /**
     * 将数据写入 NBT。
     * <p>
     * 除父类数据外，还写入内部能量缓冲区。
     */
    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("EnergyBuffer", this.energyBuffer);
    }

    /**
     * 添加护目镜提示信息。
     * <p>
     * 显示当前能量产出（FE/t）、每秒能量产出（FE/s）以及消耗的应力。
     *
     * @param tooltip        提示信息列表
     * @param isPlayerSneaking 玩家是否潜行
     * @return 始终返回 true
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        float speed = Math.abs(getSpeed());
        int energyProduced = (int) (speed * CSGServerConfig.ENERGY_PER_STRESS.get());
        int energyPerSecond = energyProduced * 20;

        // 每秒能量产出行
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.energy_output",
                        CreateLang.number(energyProduced).component().withStyle(ChatFormatting.YELLOW),
                        CreateLang.text("FE/t").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 每秒能量产出（FE/s）行
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.energy_per_second",
                        CreateLang.number(energyPerSecond).component().withStyle(ChatFormatting.GREEN),
                        CreateLang.text("FE/s").component().withStyle(ChatFormatting.GRAY))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        // 消耗应力行
        CreateLang.translate("create_small_generator.tooltip.kinetic_dynamo.stress_consumed",
                        CreateLang.number(CSGServerConfig.STRESS_CAPACITY.get()).component().withStyle(ChatFormatting.RED))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        return true;
    }
}