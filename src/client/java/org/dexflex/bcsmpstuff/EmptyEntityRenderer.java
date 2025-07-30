package org.dexflex.bcsmpstuff;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    public EmptyEntityRenderer(Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public Identifier getTexture(T entity) {
        return null;
    }

    @Override
    public void render(
            T entity, float yaw, float tickDelta,
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
            int light) {
    }
}
