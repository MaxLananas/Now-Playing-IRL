package com.nowplayingirl.client.hud;


import com.nowplayingirl.client.NowPlayingClient;
import com.nowplayingirl.client.config.ModConfig;
import com.nowplayingirl.client.media.MediaInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public class NowPlayingHud {
    
    private final NowPlayingClient client;
    private final HudAnimator animator;
    private MediaInfo currentMedia;
    private long lastUpdateTime;
    
    // Dimensions du widget
    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 60;
    private static final int ALBUM_ART_SIZE = 50;
    private static final int PADDING = 8;
    
    public NowPlayingHud(NowPlayingClient client) {
        this.client = client;
        this.animator = new HudAnimator();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public void onMediaChanged(MediaInfo newMedia) {
        if (newMedia != null && !newMedia.equals(currentMedia)) {
            animator.triggerAppear();
        }
        currentMedia = newMedia;
    }
    
    public void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        
        ModConfig config = client.getConfig();
        if (!config.isEnabled()) return;
        
        // Update animations
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 16.67f;
        lastUpdateTime = currentTime;
        animator.update(deltaTime, currentMedia != null, config.getRotationSpeed());
        
        // Skip rendering if fully hidden
        if (animator.isFullyHidden()) return;
        
        // Calculate position
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        float scale = config.getScale();
        int scaledWidth = (int) (WIDGET_WIDTH * scale);
        int scaledHeight = (int) (WIDGET_HEIGHT * scale);
        
        int x, y;
        switch (config.getPosition()) {
            case TOP_LEFT -> {
                x = 10;
                y = 10;
            }
            case TOP_RIGHT -> {
                x = screenWidth - scaledWidth - 10;
                y = 10;
            }
            case BOTTOM_LEFT -> {
                x = 10;
                y = screenHeight - scaledHeight - 10;
            }
            default -> { // BOTTOM_RIGHT
                x = screenWidth - scaledWidth - 10;
                y = screenHeight - scaledHeight - 10;
            }
        }
        
        // Apply slide animation
        x += (int) animator.getSlideOffset();
        y -= (int) animator.getBounceOffset();
        
        // Apply opacity
        float opacity = config.getOpacity() * animator.getVisibility();
        
        // Push matrix for scaling
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        
        // Get theme colors
        Theme theme = config.getTheme();
        
        // Draw background
        drawBackground(context, 0, 0, WIDGET_WIDTH, WIDGET_HEIGHT, theme, opacity);
        
        // Draw content
        if (currentMedia != null && currentMedia.hasValidInfo()) {
            drawPlayingContent(context, mc, theme, opacity, config);
        } else {
            drawSilenceContent(context, mc, theme, opacity);
        }
        
        context.getMatrices().popMatrix();
    }
    
    private void drawBackground(DrawContext context, int x, int y, int width, int height, Theme theme, float opacity) {
        int bgColor = applyOpacity(theme.getBackgroundColor(), opacity);
        int borderColor = applyOpacity(theme.getPrimaryColor(), opacity * 0.8f);
        
        // Main background
        context.fill(x + 2, y, x + width - 2, y + height, bgColor);
        context.fill(x, y + 2, x + width, y + height - 2, bgColor);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);
        
        // Border
        context.fill(x + 2, y, x + width - 2, y + 1, borderColor);
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, borderColor);
        context.fill(x, y + 2, x + 1, y + height - 2, borderColor);
        context.fill(x + width - 1, y + 2, x + width, y + height - 2, borderColor);
    }
    
    private void drawPlayingContent(DrawContext context, MinecraftClient mc, Theme theme, float opacity, ModConfig config) {
        TextRenderer textRenderer = mc.textRenderer;
        int textColor = applyOpacity(theme.getTextColor(), opacity);
        int secondaryTextColor = applyOpacity(theme.getSecondaryColor(), opacity);
        int accentColor = applyOpacity(theme.getAccentColor(), opacity);
        
        int contentX = PADDING;
        int contentY = PADDING;
        
        // Draw album art if enabled
        if (config.isShowAlbumArt()) {
            drawAlbumArt(context, contentX, contentY + 1, ALBUM_ART_SIZE - 2, opacity);
            contentX += ALBUM_ART_SIZE + PADDING;
        }
        
        // Title
        String title = currentMedia.getDisplayTitle();
        if (title != null) {
            context.drawText(textRenderer, title, contentX, contentY + 4, textColor, false);
        }
        
        // Artist
        String artist = currentMedia.getDisplayArtist();
        if (artist != null) {
            context.drawText(textRenderer, artist, contentX, contentY + 18, secondaryTextColor, false);
        }
        
        // Source indicator
        String source = currentMedia.getSource();
        if (source != null) {
            context.drawText(textRenderer, source, contentX, contentY + 34, accentColor, false);
        }
        
        // Playing indicator (pulsating dot)
        float pulse = animator.getPulseScale();
        int dotX = WIDGET_WIDTH - PADDING - 6;
        int dotY = WIDGET_HEIGHT - PADDING - 6;
        int dotSize = (int) (4 * pulse);
        int dotColor = applyOpacity(theme.getPrimaryColor(), opacity);
        context.fill(dotX - dotSize/2, dotY - dotSize/2, dotX + dotSize/2, dotY + dotSize/2, dotColor);
    }
    
    private void drawAlbumArt(DrawContext context, int x, int y, int size, float opacity) {
        Identifier texture = currentMedia != null ? currentMedia.getAlbumArtTexture() : null;
        
        context.getMatrices().pushMatrix();
        
        // Center point for rotation
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        
        context.getMatrices().translate(centerX, centerY);
        float angleRad = (float) Math.toRadians(animator.getAlbumRotation());
        Matrix3x2f rotation = new Matrix3x2f().rotate(angleRad);
        context.getMatrices().mul(
                rotation
        );
        context.getMatrices().translate(-size / 2f, -size / 2f);
        
        if (texture != null) {
            // Draw actual album art avec la nouvelle API 1.21.4
            //RenderSystem.enableBlend();
            //RenderSystem.setShaderColor(1f, 1f, 1f, opacity);
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                0, 0,
                0f, 0f,
                size, size,
                size, size
            );
            
            //RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            //RenderSystem.disableBlend();
        } else {
            // Draw placeholder (vinyl record style)
            drawVinylPlaceholder(context, 0, 0, size, opacity);
        }
        
        context.getMatrices().popMatrix();
    }

    private void drawVinylPlaceholder(DrawContext context, int x, int y, int size, float opacity) {
        Theme theme = client.getConfig().getTheme();
        int diskColor = applyOpacity(0xFF1a1a1a, opacity);
        int grooveColor = applyOpacity(0xFF2a2a2a, opacity);
        int labelColor = applyOpacity(theme.getPrimaryColor(), opacity);
        int centerColor = applyOpacity(0xFF000000, opacity);
        
        int cx = x + size / 2;
        int cy = y + size / 2;
        int radius = size / 2;
        
        // Outer disk
        context.fill(x, y, x + size, y + size, diskColor);
        
        // Grooves
        for (int i = 4; i < radius - 8; i += 4) {
            int grooveOffset = i;
            context.fill(x + grooveOffset, y + grooveOffset, 
                        x + size - grooveOffset, y + grooveOffset + 1, grooveColor);
            context.fill(x + grooveOffset, y + size - grooveOffset - 1, 
                        x + size - grooveOffset, y + size - grooveOffset, grooveColor);
        }
        
        // Center label
        int labelSize = size / 3;
        int labelX = cx - labelSize / 2;
        int labelY = cy - labelSize / 2;
        context.fill(labelX, labelY, labelX + labelSize, labelY + labelSize, labelColor);
        
        // Center hole
        int holeSize = 4;
        context.fill(cx - holeSize/2, cy - holeSize/2, cx + holeSize/2, cy + holeSize/2, centerColor);
    }
    
    private void drawSilenceContent(DrawContext context, MinecraftClient mc, Theme theme, float opacity) {
        TextRenderer textRenderer = mc.textRenderer;
        int textColor = applyOpacity(theme.getTextColor(), opacity * 0.7f);
        int noteColor = applyOpacity(theme.getPrimaryColor(), opacity * 0.5f);
        
        // "Silence..." text
        String silenceText = "Silence...";
        int textWidth = textRenderer.getWidth(silenceText);
        int textX = (WIDGET_WIDTH - textWidth) / 2;
        int textY = WIDGET_HEIGHT / 2 - 4;
        
        context.drawText(textRenderer, silenceText, textX, textY, textColor, false);
        
        // Sleeping note animation
        float noteY = animator.getSleepingNoteY();
        float phase = animator.getSleepingNotePhase();
        
        String note = "♪";
        int noteX = textX + textWidth + 8;
        context.drawText(textRenderer, note, noteX, (int)(textY + noteY), noteColor, false);
        
        // Z's floating up
        float zAlpha = (float) Math.abs(Math.sin(phase * 0.5f));
        int zColor = applyOpacity(theme.getSecondaryColor(), opacity * zAlpha * 0.5f);
        
        for (int i = 0; i < 3; i++) {
            float zOffset = (phase + i * 0.5f) % 3f;
            int zX = noteX + 12 + i * 6;
            int zY = (int)(textY - 5 - zOffset * 4);
            String z = i == 2 ? "Z" : "z";
            context.drawText(textRenderer, z, zX, zY, zColor, false);
        }
    }
    
    private int applyOpacity(int color, float opacity) {
        int alpha = (int) (((color >> 24) & 0xFF) * opacity);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}