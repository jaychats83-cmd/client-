package starry.util.render.shader;

import net.minecraft.client.MinecraftClient;

import starry.util.render.font.FontRenderer;
import starry.util.render.font.Fonts;
import starry.util.render.pipeline.*;

public class RenderCore {

    private final RectPipeline rectPipeline;
    private final OutlinePipeline outlinePipeline;
    private final GlowOutlinePipeline glowOutlinePipeline;
    private final TexturePipeline texturePipeline;
    private final BlurPipeline blurPipeline;
    private final KawaseBlurPipeline kawaseBlurPipeline;
    private final GlassCompositePipeline glassCompositePipeline;
    private final GlassHandsRenderer glassHandsRenderer;
    private final FontRenderer fontRenderer;
    private final MaskDiffPipeline maskDiffPipeline;

    private boolean fontsLoaded = false;
    private boolean arcInitialized = false;
    private boolean arcOutlineInitialized = false;

    public RenderCore() {
        this.rectPipeline = new RectPipeline();
        this.outlinePipeline = new OutlinePipeline();
        this.glowOutlinePipeline = new GlowOutlinePipeline();
        this.texturePipeline = new TexturePipeline();
        this.blurPipeline = new BlurPipeline();
        this.kawaseBlurPipeline = new KawaseBlurPipeline();
        this.glassCompositePipeline = new GlassCompositePipeline();
        this.glassHandsRenderer = new GlassHandsRenderer();
        this.maskDiffPipeline = new MaskDiffPipeline();
        this.fontRenderer = new FontRenderer();
    }

    private void ensureFontsLoaded() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        fontRenderer.loadAllFonts(Fonts.getRegistry());
    }

    private void ensureArcInitialized() {
        if (arcInitialized) return;
        arcInitialized = true;
        Arc2D.init();
    }

    private void ensureArcOutlineInitialized() {
        if (arcOutlineInitialized) return;
        arcOutlineInitialized = true;
        ArcOutline2D.init();
    }

    public void initArc() {
        ensureArcInitialized();
    }

    public void initArcOutline() {
        ensureArcOutlineInitialized();
    }

    public RectPipeline getRectPipeline() {
        return rectPipeline;
    }

    public OutlinePipeline getOutlinePipeline() {
        return outlinePipeline;
    }

    public GlowOutlinePipeline getGlowOutlinePipeline() {
        return glowOutlinePipeline;
    }

    public TexturePipeline getTexturePipeline() {
        return texturePipeline;
    }

    public BlurPipeline getBlurPipeline() {
        return blurPipeline;
    }

    public KawaseBlurPipeline getKawaseBlurPipeline() {
        return kawaseBlurPipeline;
    }

    public GlassCompositePipeline getGlassCompositePipeline() {
        return glassCompositePipeline;
    }

    public GlassHandsRenderer getGlassHandsRenderer() {
        return glassHandsRenderer;
    }

    public FontRenderer getFontRenderer() {
        ensureFontsLoaded();
        return fontRenderer;
    }

    public MaskDiffPipeline getMaskDiffPipeline() {
        return maskDiffPipeline;
    }

    public MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    public void close() {
        rectPipeline.close();
        outlinePipeline.close();
        glowOutlinePipeline.close();
        texturePipeline.close();
        blurPipeline.close();
        kawaseBlurPipeline.close();
        glassCompositePipeline.close();
        glassHandsRenderer.close();
        maskDiffPipeline.close();
        fontRenderer.close();
        Arc2D.shutdown();
        ArcOutline2D.shutdown();
    }
}