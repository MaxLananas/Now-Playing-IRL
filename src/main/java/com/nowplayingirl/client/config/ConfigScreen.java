package com.nowplayingirl.client.config;

import com.nowplayingirl.client.NowPlayingClient;
import com.nowplayingirl.client.hud.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;

public class ConfigScreen extends Screen {

    private final Screen parent;
    private ModConfig config;
    private float previewRotation = 0f;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("nowplayingirl.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        config = NowPlayingClient.getInstance().getConfig();

        int centerX = this.width / 2;
        int y = 40;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // Enabled
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Enabled: " + (config.isEnabled() ? "§aON" : "§cOFF")),
            button -> {
                config.setEnabled(!config.isEnabled());
                button.setMessage(Text.literal("Enabled: " + (config.isEnabled() ? "§aON" : "§cOFF")));
            }
        ).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
        y += spacing;

        // Position
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Position: " + getPositionName(config.getPosition())),
            button -> {
                config.cyclePosition();
                button.setMessage(Text.literal("Position: " + getPositionName(config.getPosition())));
            }
        ).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
        y += spacing;

        // Theme
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Theme: " + getThemeColoredName(config.getTheme())),
            button -> {
                config.cycleTheme();
                button.setMessage(Text.literal("Theme: " + getThemeColoredName(config.getTheme())));
            }
        ).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
        y += spacing;

        // Scale
        addDrawableChild(new SliderWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            Text.literal("Scale: " + String.format("%.0f%%", config.getScale() * 100)),
            (config.getScale() - 0.5) / 1.5) {
            @Override
            protected void updateMessage() {
                double scale = 0.5 + value * 1.5;
                setMessage(Text.literal("Scale: " + String.format("%.0f%%", scale * 100)));
            }
            @Override
            protected void applyValue() {
                config.setScale((float) (0.5 + value * 1.5));
            }
        });
        y += spacing;

        // Opacity
        addDrawableChild(new SliderWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            Text.literal("Opacity: " + String.format("%.0f%%", config.getOpacity() * 100)),
            config.getOpacity()) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Opacity: " + String.format("%.0f%%", value * 100)));
            }
            @Override
            protected void applyValue() {
                config.setOpacity((float) value);
            }
        });
        y += spacing;

        // Rotation Speed
        addDrawableChild(new SliderWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
            Text.literal("Rotation: " + getRotationText(config.getRotationSpeed())),
            config.getRotationSpeed() / 5.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Rotation: " + getRotationText((float)(value * 5))));
            }
            @Override
            protected void applyValue() {
                config.setRotationSpeed((float) (value * 5));
            }
        });
        y += spacing;

        // Show Album Art
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Album Art: " + (config.isShowAlbumArt() ? "§aVisible" : "§7Hidden")),
            button -> {
                config.setShowAlbumArt(!config.isShowAlbumArt());
                button.setMessage(Text.literal("Album Art: " + (config.isShowAlbumArt() ? "§aVisible" : "§7Hidden")));
            }
        ).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
        y += spacing + 15;

        // Done & Reset
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§a✔ Done"),
            button -> close()
        ).dimensions(centerX - 100, y, 95, buttonHeight).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("§c✖ Reset"),
            button -> {
                config.reset();
                client.setScreen(new ConfigScreen(parent));
            }
        ).dimensions(centerX + 5, y, 95, buttonHeight).build());
    }

    private String getPositionName(ModConfig.Position pos) {
        return switch (pos) {
            case TOP_LEFT -> "Top Left";
            case TOP_RIGHT -> "Top Right";
            case BOTTOM_LEFT -> "Bottom Left";
            case BOTTOM_RIGHT -> "Bottom Right";
        };
    }

    private String getThemeColoredName(Theme theme) {
        return switch (theme) {
            case CHERRY_BLOSSOM -> "§d" + theme.getDisplayName();
            case PURPLE -> "§5" + theme.getDisplayName();
            case OCEAN -> "§b" + theme.getDisplayName();
            case FOREST -> "§a" + theme.getDisplayName();
            case SUNSET -> "§6" + theme.getDisplayName();
        };
    }

    private String getRotationText(float speed) {
        if (speed == 0) return "§7Disabled";
        if (speed < 1) return "§eSlow";
        if (speed < 2) return "§aNormal";
        if (speed < 3.5) return "§bFast";
        return "§c§lTURBO!";
    }

    @Override
    public void close() {
        config.save();
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //renderBackground(context, mouseX, mouseY, delta);

        Theme theme = config.getTheme();
        int titleColor = theme.getPrimaryColor() | 0xFF000000;

        // Title
        context.drawCenteredTextWithShadow(textRenderer, "§l♪ Now Playing IRL ♪", width / 2, 12, titleColor);
        context.drawCenteredTextWithShadow(textRenderer, "§7Configuration", width / 2, 24, 0xAAAAAA);

        // Preview Box - PLUS GRAND ET PLUS CLAIR
        drawPreviewBox(context, delta);

        // Keybind hints
        int hintY = height - 20;
        context.drawCenteredTextWithShadow(textRenderer,
            "§8[N] Toggle  |  [M] Menu  |  [,] Theme",
            width / 2, hintY, 0x888888);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPreviewBox(DrawContext context, float delta) {
        // Position et taille du preview - PLUS GRAND
        int previewX = width - 145;
        int previewY = 40;
        int previewW = 130;
        int previewH = 110;

        Theme theme = config.getTheme();
        int primaryColor = theme.getPrimaryColor() | 0xFF000000;
        int bgColor = theme.getBackgroundColor() | 0xFF000000;

        // ===== CADRE EXTÉRIEUR =====
        // Bordure
        context.fill(previewX - 3, previewY - 3, previewX + previewW + 3, previewY + previewH + 3, primaryColor);
        // Fond principal
        context.fill(previewX, previewY, previewX + previewW, previewY + previewH, bgColor);

        // ===== TITRE "PREVIEW" =====
        context.drawCenteredTextWithShadow(textRenderer, "§lPreview", previewX + previewW / 2, previewY - 14, 0xFFFFFF);

        // ===== DISQUE VINYLE - PLUS GRAND ET NET =====
        int discCenterX = previewX + 45;
        int discCenterY = previewY + 55;
        int discRadius = 35;

        // Update rotation
        previewRotation += config.getRotationSpeed() * 0.6f;
        if (previewRotation >= 360) previewRotation -= 360;

        // Dessiner le disque avec rotation
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(discCenterX, discCenterY);
        float angleRad = (float) Math.toRadians(previewRotation);
        Matrix3x2f rotation = new Matrix3x2f().rotate(angleRad);
        context.getMatrices().mul(
                rotation
        );

        // === DISQUE PRINCIPAL (cercle noir) ===
        drawFilledCircle(context, 0, 0, discRadius, 0xFF111111);

        // === SILLONS DU VINYLE ===
        int grooveColor = 0xFF222222;
        drawCircleOutline(context, 0, 0, discRadius - 4, grooveColor);
        drawCircleOutline(context, 0, 0, discRadius - 8, grooveColor);
        drawCircleOutline(context, 0, 0, discRadius - 12, grooveColor);
        drawCircleOutline(context, 0, 0, discRadius - 16, grooveColor);

        // === REFLET SUR LE DISQUE ===
        int reflectColor = 0x33FFFFFF;
        context.fill(-discRadius + 5, -3, -5, 0, reflectColor);

        // === LABEL CENTRAL (couleur du thème) ===
        int labelRadius = 12;
        drawFilledCircle(context, 0, 0, labelRadius, primaryColor);

        // === TROU CENTRAL ===
        drawFilledCircle(context, 0, 0, 3, 0xFF000000);

        context.getMatrices().popMatrix();

        // ===== TEXTE À DROITE DU DISQUE =====
        int textX = discCenterX + discRadius + 12;
        int textY = previewY + 25;
        int lineHeight = 14;

        // Titre
        context.drawTextWithShadow(textRenderer, "§fSong Title", textX, textY, 0xFFFFFF);
        textY += lineHeight;

        // Artiste
        int artistColor = (theme.getSecondaryColor() & 0x00FFFFFF) | 0xFF000000;
        context.drawTextWithShadow(textRenderer, "Artist", textX, textY, artistColor);
        textY += lineHeight;

        // Source
        int sourceColor = (theme.getAccentColor() & 0x00FFFFFF) | 0xFF000000;
        context.drawTextWithShadow(textRenderer, "§oSpotify", textX, textY, sourceColor);

        // ===== INDICATEURS EN BAS =====
        int infoY = previewY + previewH - 22;

        // Scale
        String scaleStr = String.format("%.0f%%", config.getScale() * 100);
        context.drawTextWithShadow(textRenderer, "§7Size: §f" + scaleStr, previewX + 8, infoY, 0xAAAAAA);

        // Opacity
        String opacityStr = String.format("%.0f%%", config.getOpacity() * 100);
        context.drawTextWithShadow(textRenderer, "§7Opacity: §f" + opacityStr, previewX + 8, infoY + 10, 0xAAAAAA);

        // ===== POINT QUI PULSE (indicateur "playing") =====
        float pulse = (float) (1 + Math.sin(System.currentTimeMillis() / 200.0) * 0.3);
        int dotSize = (int) (4 * pulse);
        int dotX = previewX + previewW - 12;
        int dotY = previewY + previewH - 12;
        context.fill(dotX - dotSize, dotY - dotSize, dotX + dotSize, dotY + dotSize, primaryColor);
    }

    /**
     * Dessine un cercle rempli (approximation avec des rectangles)
     */
    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radius * radius - y * y);
            context.fill(
                centerX - halfWidth,
                centerY + y,
                centerX + halfWidth,
                centerY + y + 1,
                color
            );
        }
    }

    /**
     * Dessine un contour de cercle
     */
    private void drawCircleOutline(DrawContext context, int centerX, int centerY, int radius, int color) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            int x1 = centerX + (int) (Math.cos(angle1) * radius);
            int y1 = centerY + (int) (Math.sin(angle1) * radius);
            int x2 = centerX + (int) (Math.cos(angle2) * radius);
            int y2 = centerY + (int) (Math.sin(angle2) * radius);

            // Dessiner un petit segment
            context.fill(Math.min(x1, x2), Math.min(y1, y2),
                        Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}