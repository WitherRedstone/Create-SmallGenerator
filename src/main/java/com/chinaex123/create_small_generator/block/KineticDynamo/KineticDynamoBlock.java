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
 * 动能发电机方块。
 * <p>
 * 继承自 Create 的 {@link DirectionalKineticBlock}，实现 {@link IBE} 以绑定方块实体。
 * 方块具有朝向属性，朝向决定动力输入端与能量输出端的方向。
 * 放置时根据玩家视线方向与是否按下 Shift 键确定最终朝向。
 */
public class KineticDynamoBlock extends DirectionalKineticBlock implements IBE<KineticDynamoBlockEntity> {

    /**
     * 构造动能发电机方块。
     *
     * @param properties 方块属性
     */
    public KineticDynamoBlock(Properties properties) {
        super(properties);
    }

    /**
     * 确定方块放置时的状态。
     * <p>
     * 优先获取玩家视线对应的朝向，若为垂直方向则改用水平朝向。
     * 未按下 Shift 时朝向取反，使正面朝向玩家。
     *
     * @param context 放置上下文
     * @return 带有最终朝向的方块状态
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferredFacing = getPreferredFacing(context);

        // 视线朝向为垂直或无效时，改用水平朝向
        if (preferredFacing == null || preferredFacing.getAxis() == Direction.Axis.Y) {
            preferredFacing = context.getHorizontalDirection();
        }

        // 未按下 Shift 时正面朝向玩家
        boolean shiftKeyDown = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction finalFacing = shiftKeyDown ? preferredFacing : preferredFacing.getOpposite();

        return defaultBlockState().setValue(FACING, finalFacing);
    }

    /**
     * 获取旋转轴。
     * <p>
     * 旋转轴与方块朝向的轴一致。
     *
     * @param state 方块状态
     * @return 旋转轴
     */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /**
     * 判断指定面是否有动力轴。
     * <p>
     * 仅在与朝向相反的一面（即背面）存在动力轴。
     *
     * @param world 世界读取器
     * @param pos   方块位置
     * @param state 方块状态
     * @param face  待判断的面
     * @return 该面为背面时返回 true
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    /**
     * 获取方块实体类。
     *
     * @return 动能发电机方块实体类
     */
    @Override
    public Class<KineticDynamoBlockEntity> getBlockEntityClass() {
        return KineticDynamoBlockEntity.class;
    }

    /**
     * 获取方块实体类型。
     *
     * @return 动能发电机方块实体类型
     */
    @Override
    public BlockEntityType<? extends KineticDynamoBlockEntity> getBlockEntityType() {
        return CSGBlockEntities.KINETIC_DYNAMO.get();
    }
}