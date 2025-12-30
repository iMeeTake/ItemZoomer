package com.imeetake.itemzoomer.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ZoomedItemBufferSource implements MultiBufferSource {

    private static RenderTarget currentTarget = null;
    private static final Map<RenderType, RenderType> renderTypeCache = new HashMap<>();

    public static final RenderStateShard.OutputStateShard ZOOMED_OUTPUT = new RenderStateShard.OutputStateShard(
            "zoomed_item_target",
            () -> currentTarget != null ? currentTarget : Minecraft.getInstance().getMainRenderTarget()
    );

    private final MultiBufferSource.BufferSource delegate;

    public ZoomedItemBufferSource(MultiBufferSource.BufferSource delegate) {
        this.delegate = delegate;
    }

    public static void setTarget(RenderTarget target) {
        currentTarget = target;
    }

    public static void clearTarget() {
        currentTarget = null;
    }

    public static void clearCache() {
        renderTypeCache.clear();
    }

    public static int getCacheSize() {
        return renderTypeCache.size();
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        RenderType redirected = getRedirectedRenderType(renderType);
        return delegate.getBuffer(redirected);
    }

    public void endBatch() {
        delegate.endBatch();
    }

    public void endBatch(RenderType renderType) {
        RenderType redirected = getRedirectedRenderType(renderType);
        delegate.endBatch(redirected);
    }

    private RenderType getRedirectedRenderType(RenderType original) {
        if (currentTarget == null) {
            return original;
        }

        return renderTypeCache.computeIfAbsent(original, this::createRedirectedRenderType);
    }

    private RenderType createRedirectedRenderType(RenderType original) {
        if (!(original instanceof RenderType.CompositeRenderType composite)) {
            return original;
        }

        try {
            RenderType.CompositeState originalState = getCompositeState(composite);
            if (originalState == null) {
                return original;
            }

            var builder = RenderType.CompositeState.builder();
            var builderClass = builder.getClass();

            invokeBuilderMethod(builderClass, builder, "setTextureState",
                    RenderStateShard.EmptyTextureStateShard.class, getTextureState(originalState));
            invokeBuilderMethod(builderClass, builder, "setLightmapState",
                    RenderStateShard.LightmapStateShard.class, getLightmapState(originalState));
            invokeBuilderMethod(builderClass, builder, "setOverlayState",
                    RenderStateShard.OverlayStateShard.class, getOverlayState(originalState));
            invokeBuilderMethod(builderClass, builder, "setLayeringState",
                    RenderStateShard.LayeringStateShard.class, getLayeringState(originalState));
            invokeBuilderMethod(builderClass, builder, "setOutputState",
                    RenderStateShard.OutputStateShard.class, ZOOMED_OUTPUT);
            invokeBuilderMethod(builderClass, builder, "setTexturingState",
                    RenderStateShard.TexturingStateShard.class, getTexturingState(originalState));
            invokeBuilderMethod(builderClass, builder, "setLineState",
                    RenderStateShard.LineStateShard.class, getLineState(originalState));

            RenderType.CompositeState newState = invokeCreateCompositeState(builderClass, builder, getOutlineProperty(originalState));

            return RenderType.create(
                    "zoomed_" + original.toString(),
                    original.bufferSize(),
                    original.affectsCrumbling(),
                    original.sortOnUpload(),
                    composite.getRenderPipeline(),
                    newState
            );
        } catch (Exception e) {
            return original;
        }
    }

    private <T> void invokeBuilderMethod(Class<?> builderClass, Object builder, String methodName,
                                         Class<T> paramType, T value) throws Exception {
        var method = builderClass.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        method.invoke(builder, value);
    }

    private RenderType.CompositeState invokeCreateCompositeState(Class<?> builderClass, Object builder,
                                                                 RenderType.OutlineProperty outlineProperty) throws Exception {
        var method = builderClass.getDeclaredMethod("createCompositeState", RenderType.OutlineProperty.class);
        method.setAccessible(true);
        return (RenderType.CompositeState) method.invoke(builder, outlineProperty);
    }

    private RenderType.CompositeState getCompositeState(RenderType.CompositeRenderType composite) {
        try {
            var field = RenderType.CompositeRenderType.class.getDeclaredField("state");
            field.setAccessible(true);
            return (RenderType.CompositeState) field.get(composite);
        } catch (Exception e) {
            return null;
        }
    }

    private RenderStateShard.EmptyTextureStateShard getTextureState(RenderType.CompositeState state) {
        try {
            var field = RenderType.CompositeState.class.getDeclaredField("textureState");
            field.setAccessible(true);
            return (RenderStateShard.EmptyTextureStateShard) field.get(state);
        } catch (Exception e) {
            return RenderStateShard.NO_TEXTURE;
        }
    }

    private RenderStateShard.LightmapStateShard getLightmapState(RenderType.CompositeState state) {
        try {
            var states = getStates(state);
            for (var s : states) {
                if (s instanceof RenderStateShard.LightmapStateShard lm) {
                    return lm;
                }
            }
        } catch (Exception ignored) {
        }
        return RenderStateShard.NO_LIGHTMAP;
    }

    private RenderStateShard.OverlayStateShard getOverlayState(RenderType.CompositeState state) {
        try {
            var states = getStates(state);
            for (var s : states) {
                if (s instanceof RenderStateShard.OverlayStateShard ov) {
                    return ov;
                }
            }
        } catch (Exception ignored) {
        }
        return RenderStateShard.NO_OVERLAY;
    }

    private RenderStateShard.LayeringStateShard getLayeringState(RenderType.CompositeState state) {
        try {
            var states = getStates(state);
            for (var s : states) {
                if (s instanceof RenderStateShard.LayeringStateShard lay) {
                    return lay;
                }
            }
        } catch (Exception ignored) {
        }
        return RenderStateShard.NO_LAYERING;
    }

    private RenderStateShard.TexturingStateShard getTexturingState(RenderType.CompositeState state) {
        try {
            var states = getStates(state);
            for (var s : states) {
                if (s instanceof RenderStateShard.TexturingStateShard tex) {
                    return tex;
                }
            }
        } catch (Exception ignored) {
        }
        return RenderStateShard.DEFAULT_TEXTURING;
    }

    private RenderStateShard.LineStateShard getLineState(RenderType.CompositeState state) {
        try {
            var states = getStates(state);
            for (var s : states) {
                if (s instanceof RenderStateShard.LineStateShard line) {
                    return line;
                }
            }
        } catch (Exception ignored) {
        }
        return RenderStateShard.DEFAULT_LINE;
    }

    private RenderType.OutlineProperty getOutlineProperty(RenderType.CompositeState state) {
        try {
            var field = RenderType.CompositeState.class.getDeclaredField("outlineProperty");
            field.setAccessible(true);
            return (RenderType.OutlineProperty) field.get(state);
        } catch (Exception e) {
            return RenderType.OutlineProperty.NONE;
        }
    }

    @SuppressWarnings("unchecked")
    private Iterable<RenderStateShard> getStates(RenderType.CompositeState state) {
        try {
            var field = RenderType.CompositeState.class.getDeclaredField("states");
            field.setAccessible(true);
            return (Iterable<RenderStateShard>) field.get(state);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}