package com.chinaex123.create_small_generator.block.KineticDynamo;

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
 * 动力发电机方块类，定义方块的放置方向、旋转轴和应力输入面等属性
 */
public class KineticDynamoBlock extends DirectionalKineticBlock implements IBE<KineticDynamoBlockEntity> {

    public KineticDynamoBlock(Properties properties) {
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
     * 传动杆只能连接到方块的背面(应力输入面)
     *
     * @param world 世界读取器
     * @param pos 方块位置
     * @param state 方块状态
     * @param face 检查的方向
     * @return boolean 如果该方向是方块背面则返回true，否则返回false
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    /**
     * 获取此方块对应的方块实体类
     *
     * @return Class<DynamoBlockEntity> 发电机方块实体类
     */
    @Override
    public Class<KineticDynamoBlockEntity> getBlockEntityClass() {
        return KineticDynamoBlockEntity.class;
    }

    /**
     * 获取此方块对应的方块实体类型
     *
     * @return BlockEntityType<? extends DynamoBlockEntity> 发电机方块实体类型
     */
    @Override
    public BlockEntityType<? extends KineticDynamoBlockEntity> getBlockEntityType() {
        return CSGBlockEntities.KINETIC_DYNAMO.get();
    }
}
