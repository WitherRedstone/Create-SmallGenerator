package com.chinaex123.create_small_generator.block.KineticDynamo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * 动能发电机方块实体渲染器。
 * <p>
 * 继承自 Create 的 {@link KineticBlockEntityRenderer}，负责渲染发电机的动力轴部分。
 * 当 Flywheel 可视化可用时跳过常规渲染，由可视化系统接管；
 * 否则手动渲染半轴模型，并根据旋转速度计算旋转角度。
 */
public class KineticDynamoRenderer extends KineticBlockEntityRenderer<KineticDynamoBlockEntity> {

    /**
     * 构造动能发电机渲染器。
     *
     * @param context 渲染器上下文
     */
    public KineticDynamoRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 执行安全渲染逻辑。
     * <p>
     * 若当前环境支持 Flywheel 可视化则直接返回，避免重复渲染。
     * 否则获取方块朝向，渲染背面的半轴模型，并根据当前转速计算旋转角度。
     *
     * @param be           动能发电机方块实体
     * @param partialTicks 部分 tick 插值
     * @param ms           姿态栈
     * @param buffer       多重缓冲区来源
     * @param light        光照值
     * @param overlay      覆盖层
     */
    @Override
    protected void renderSafe(KineticDynamoBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 可视化可用时交由可视化系统处理，跳过常规渲染
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        Direction direction = be.getBlockState().getValue(KineticDynamoBlock.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        // 分别获取方块背面与正面的光照值
        int lightBehind = LevelRenderer.getLightColor(Objects.requireNonNull(be.getLevel()), be.getBlockPos().relative(direction.getOpposite()));
        int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));

        // 缓存朝向背面的半轴模型
        SuperByteBuffer shaftHalf =
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float speed = be.getSpeed();

        // 对转速进行钳制，避免过快或过慢导致视觉异常
        if (speed > 0)
            speed = Mth.clamp(speed, 80, 64 * 20);
        if (speed < 0)
            speed = Mth.clamp(speed, -64 * 20, -80);

        // 根据时间与速度计算旋转角度（弧度）
        float angle = (time * speed * 3 / 10f) % 360;
        angle = angle / 180f * (float) Math.PI;

        // 应用标准动能旋转变换并渲染半轴
        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
    }
}