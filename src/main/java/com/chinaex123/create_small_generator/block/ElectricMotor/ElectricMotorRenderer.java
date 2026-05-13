package com.chinaex123.create_small_generator.block.ElectricMotor;

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
 * 渲染电动马达方块的传动杆模型
 */
public class ElectricMotorRenderer extends KineticBlockEntityRenderer<ElectricMotorBlockEntity> {
    public ElectricMotorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 安全渲染方法，渲染电力引擎方块的传动杆模型
     * 根据方块转速计算旋转角度并应用旋转变换
     *
     * @param be 电力引擎方块实体
     * @param partialTicks 部分刻数，用于平滑动画
     * @param ms 姿态栈，用于变换矩阵
     * @param buffer 多重缓冲区源
     * @param light 光照等级
     * @param overlay 覆盖层纹理
     */
    @Override
    protected void renderSafe(ElectricMotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        Direction direction = be.getBlockState().getValue(ElectricMotorBlock.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        // 获取背面和正面的光照等级
        int lightBehind = LevelRenderer.getLightColor(Objects.requireNonNull(be.getLevel()), be.getBlockPos().relative(direction.getOpposite()));
        int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));

        // 创建传动杆半模型缓冲区
        SuperByteBuffer shaftHalf =
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float speed = be.getSpeed();

        // 限制转速范围以确保合理的旋转速度
        if (speed > 0)
            speed = Mth.clamp(speed, 80, 64 * 20);
        if (speed < 0)
            speed = Mth.clamp(speed, -64 * 20, -80);

        // 计算旋转角度
        float angle = (time * speed * 3 / 10f) % 360;
        angle = angle / 180f * (float) Math.PI;

        // 应用标准动能旋转变换并渲染
        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
    }
}
