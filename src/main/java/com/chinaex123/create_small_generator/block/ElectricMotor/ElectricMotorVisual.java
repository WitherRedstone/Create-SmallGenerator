package com.chinaex123.create_small_generator.block.ElectricMotor;

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
 * 电动马达方块的可视化类，用于Flywheel渲染引擎
 * 处理传动杆模型的实例化、更新和光照计算
 */
public class ElectricMotorVisual extends KineticBlockEntityVisual<ElectricMotorBlockEntity> {
    protected final RotatingInstance shaft;
    final Direction direction;
    private final Direction opposite;

    /**
     * 构造函数，初始化可视化实例
     *
     * @param context 可视化上下文
     * @param blockEntity 电力引擎方块实体
     * @param partialTick 部分刻数
     */
    public ElectricMotorVisual(VisualizationContext context, ElectricMotorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        direction = blockState.getValue(ElectricMotorBlock.FACING);
        opposite = direction.getOpposite();

        // 创建旋转实例用于传动杆模型
        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();

        shaft.setup(blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, opposite)
                .setChanged();
    }

    /**
     * 更新方法，每帧调用以更新实例状态
     *
     * @param pt 部分刻数
     */
    @Override
    public void update(float pt) {
        shaft.setup(blockEntity)
                .setChanged();
    }

    /**
     * 更新光照信息
     *
     * @param partialTick 部分刻数
     */
    @Override
    public void updateLight(float partialTick) {
        BlockPos behind = pos.relative(opposite);
        relight(behind, shaft);
    }

    /**
     * 删除可视化实例
     */
    @Override
    protected void _delete() {
        shaft.delete();
    }

    /**
     * 收集破碎实例用于渲染破坏动画
     *
     * @param consumer 实例消费者
     */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}
