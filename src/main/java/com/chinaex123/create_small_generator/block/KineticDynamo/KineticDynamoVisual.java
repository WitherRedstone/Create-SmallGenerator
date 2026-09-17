package com.chinaex123.create_small_generator.block.KineticDynamo;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

/**
 * 动能发电机方块实体的 Flywheel 可视化。
 * <p>
 * 继承自 Create 的 {@link KineticBlockEntityVisual}，负责在 Flywheel 可视化系统中
 * 渲染发电机的动力轴实例。当可视化可用时，由本类接管渲染，
 * 而 {@link KineticDynamoRenderer} 中的常规渲染会被跳过。
 */
public class KineticDynamoVisual extends KineticBlockEntityVisual<KineticDynamoBlockEntity> {

    /** 动力轴的旋转实例 */
    protected final RotatingInstance shaft;
    /** 方块朝向 */
    final Direction direction;
    /** 方块朝向的反方向（背面） */
    private final Direction opposite;

    /**
     * 构造动能发电机可视化实例。
     * <p>
     * 获取方块朝向与背面，创建动力轴旋转实例，
     * 并设置其位置、朝向及初始旋转状态。
     *
     * @param context     可视化上下文
     * @param blockEntity 动能发电机方块实体
     * @param partialTick 部分 tick 插值
     */
    public KineticDynamoVisual(VisualizationContext context, KineticDynamoBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        direction = blockState.getValue(KineticDynamoBlock.FACING);
        opposite = direction.getOpposite();

        // 创建半轴模型的旋转实例
        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();

        // 设置实例位置、朝向背面并应用初始状态
        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();
    }

    /**
     * 更新可视化实例。
     * <p>
     * 根据方块实体当前状态刷新旋转实例的转速等信息。
     *
     * @param pt 部分 tick 插值
     */
    @Override
    public void update(float pt) {
        shaft.setup(blockEntity)
                .setChanged();
    }

    /**
     * 更新光照。
     * <p>
     * 以方块背面作为光照参考位置，重新计算动力轴实例的光照。
     *
     * @param partialTick 部分 tick 插值
     */
    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);
    }

    /**
     * 删除可视化实例。
     * <p>
     * 释放动力轴旋转实例占用的资源。
     */
    @Override
    protected void _delete() {
        shaft.delete();
    }

    /**
     * 收集用于破坏动画的实例。
     * <p>
     * 将动力轴实例提供给消费者，用于播放方块破碎效果。
     *
     * @param consumer 实例消费者
     */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}