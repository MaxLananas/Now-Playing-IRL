package com.nowplayingirl.client;

import com.nowplayingirl.NowPlayingIRLMod;
import com.nowplayingirl.client.config.ConfigScreen;
import com.nowplayingirl.client.config.ModConfig;
import com.nowplayingirl.client.hud.NowPlayingHud;
import com.nowplayingirl.client.media.AlbumArtFetcher;
import com.nowplayingirl.client.media.MediaDetector;
import com.nowplayingirl.client.media.MediaInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public class NowPlayingClient implements ClientModInitializer {
    
    private static NowPlayingClient instance;
    private ModConfig config;
    private NowPlayingHud hud;
    private MediaDetector mediaDetector;
    private AlbumArtFetcher albumArtFetcher;
    private ScheduledExecutorService executor;
    private volatile MediaInfo currentMedia;
    
    // Keybindings
    private KeyBinding.Category keybindCategory;
    private KeyBinding toggleKey;
    private KeyBinding configKey;
    private KeyBinding cycleThemeKey;
    
    public static NowPlayingClient getInstance() {
        return instance;
    }
    
    @Override
    public void onInitializeClient() {
        instance = this;
        
        NowPlayingIRLMod.LOGGER.info("Now Playing IRL initializing...");
        
        // Charger la config
        config = ModConfig.load();
        
        // Initialiser le détecteur média
        mediaDetector = MediaDetector.create();
        
        // Initialiser le fetcher de pochettes
        albumArtFetcher = new AlbumArtFetcher();
        
        // Initialiser le HUD
        hud = new NowPlayingHud(this);
        
        // Enregistrer les keybindings
        registerKeybindings();
        
        // Enregistrer le callback de rendu HUD
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            if (config.isEnabled()) {
                float tickDelta = renderTickCounter.getTickProgress(true);
                hud.render(drawContext, tickDelta);
            }
        });
        
        // Tick events pour les keybindings
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Démarrer le polling média en arrière-plan
        startMediaPolling();
        
        NowPlayingIRLMod.LOGGER.info("Now Playing IRL initialized!");
    }
    
    private void registerKeybindings() {
        // Toggle HUD on/off
        keybindCategory = KeyBinding.Category.create(Identifier.of("key.nowplayingirl.category"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nowplayingirl.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            keybindCategory

        ));
        
        // Ouvrir le menu de configuration
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nowplayingirl.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            keybindCategory
        ));
        
        // Changer de thème rapidement
        cycleThemeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.nowplayingirl.cycle_theme",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,  // Touche ","
            keybindCategory
        ));
    }
    
    private void onClientTick(MinecraftClient client) {
        // Toggle HUD
        while (toggleKey.wasPressed()) {
            config.setEnabled(!config.isEnabled());
            config.save();
            
            // Afficher un message dans le chat
            if (client.player != null) {
                String status = config.isEnabled() ? "§aON" : "§cOFF";
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("§d[Now Playing IRL] §fWidget: " + status), 
                    true  // Action bar
                );
            }
        }
        
        // Ouvrir le menu de config
        while (configKey.wasPressed()) {
            // Ne pas ouvrir si on est déjà dans un écran (sauf le jeu)
            if (client.currentScreen == null) {
                client.setScreen(new ConfigScreen(null));
            }
        }
        
        // Cycle theme
        while (cycleThemeKey.wasPressed()) {
            config.cycleTheme();
            config.save();
            
            if (client.player != null) {
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("§d[Now Playing IRL] §fTheme: §b" + config.getTheme().getDisplayName()), 
                    true
                );
            }
        }
    }
    
    private void startMediaPolling() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NowPlayingIRL-MediaPoller");
            t.setDaemon(true);
            return t;
        });
        
        executor.scheduleAtFixedRate(() -> {
            try {
                MediaInfo newMedia = mediaDetector.detect();
                
                if (newMedia != null && !newMedia.equals(currentMedia)) {
                    if (newMedia.hasValidInfo()) {
                        albumArtFetcher.fetchAsync(newMedia, texture -> {
                            newMedia.setAlbumArtTexture(texture);
                        });
                    }
                    currentMedia = newMedia;
                    hud.onMediaChanged(newMedia);
                } else if (newMedia == null && currentMedia != null) {
                    currentMedia = null;
                    hud.onMediaChanged(null);
                }
                
            } catch (Exception e) {
                NowPlayingIRLMod.LOGGER.debug("Error polling media: {}", e.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (albumArtFetcher != null) {
            albumArtFetcher.shutdown();
        }
    }
    
    public ModConfig getConfig() {
        return config;
    }
    
    public MediaInfo getCurrentMedia() {
        return currentMedia;
    }
    
    public NowPlayingHud getHud() {
        return hud;
    }
}