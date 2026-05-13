package com.chinaex123.create_small_generator.block.ElectricMotor;

import com.chinaex123.create_small_generator.init.CSGBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电动马达方块类，将应力转换为能量输出
 * 功能与动力发电机相反：消耗应力产生旋转动力
 */
public class ElectricMotorBlock extends DirectionalKineticBlock implements IBE<ElectricMotorBlockEntity> {

    public ElectricMotorBlock(Properties properties) {
        super(properties);
    }

    /**
     * 获取方块放置时的状态
     * 根据玩家视角和潜行状态确定方块的朝向
     *
     * @param context 方块放置上下文
     * @return BlockState 放置后的方块状态，包含正确的朝向信息
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferredFacing = getPreferredFacing(context);

        // 如果首选方向为空或为垂直方向，则使用玩家的水平方向
        if (preferredFacing == null || preferredFacing.getAxis() == Direction.Axis.Y) {
            preferredFacing = context.getHorizontalDirection();
        }

        boolean shiftKeyDown = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction finalFacing = shiftKeyDown ? preferredFacing : preferredFacing.getOpposite();

        return defaultBlockState().setValue(FACING, finalFacing);
    }

    /**
     * 获取方块的旋转轴
     * 根据方块的朝向确定其旋转轴方向
     *
     * @param state 方块状态
     * @return Direction.Axis 方块的旋转轴
     */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /**
     * 检查指定方向是否有传动杆连接
     * 传动杆只能连接到方块的正面(应力输出面)
     *
     * @param world 世界读取器
     * @param pos 方块位置
     * @param state 方块状态
     * @param face 检查的方向
     * @return boolean 如果该方向是方块正面则返回true，否则返回false
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    /**
     * 获取此方块对应的方块实体类
     *
     * @return Class<ElectricMotorBlockEntity> 电力引擎方块实体类
     */
    @Override
    public Class<ElectricMotorBlockEntity> getBlockEntityClass() {
        return ElectricMotorBlockEntity.class;
    }

    /**
     * 获取此方块对应的方块实体类型
     *
     * @return BlockEntityType<? extends ElectricMotorBlockEntity> 电力引擎方块实体类型
     */
    @Override
    public BlockEntityType<? extends ElectricMotorBlockEntity> getBlockEntityType() {
        return CSGBlockEntities.ELECTRIC_MOTOR.get();
    }
}
