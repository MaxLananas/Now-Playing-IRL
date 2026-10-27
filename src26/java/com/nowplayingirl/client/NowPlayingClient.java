package com.nowplayingirl.client;
import com.nowplayingirl.NowPlayingIRLMod;
import com.nowplayingirl.client.config.ConfigScreen;
import com.nowplayingirl.client.config.ModConfig;
import com.nowplayingirl.client.hud.NowPlayingHud;
import com.nowplayingirl.client.media.AlbumArtFetcher;
import com.nowplayingirl.client.media.MediaDetector;
import com.nowplayingirl.client.media.MediaInfo;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.concurrent.*;

@Environment(EnvType.CLIENT)
public class NowPlayingClient implements ClientModInitializer {
    private static NowPlayingClient instance;
    private ModConfig config; private NowPlayingHud hud; private MediaDetector mediaDetector; private AlbumArtFetcher albumArtFetcher;
    private ScheduledExecutorService executor; private volatile MediaInfo currentMedia;
    private KeyMapping toggleKey, configKey, cycleThemeKey;
    public static NowPlayingClient getInstance(){ return instance; }

    @Override public void onInitializeClient(){
        instance=this;
        NowPlayingIRLMod.LOGGER.info("Now Playing IRL initializing (MC 26.x)...");
        config=ModConfig.load();
        mediaDetector=MediaDetector.create();
        albumArtFetcher=new AlbumArtFetcher();
        hud=new NowPlayingHud(this);
        registerKeybindings();
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath(NowPlayingIRLMod.MOD_ID,"widget"),
            (graphics, deltaTracker) -> { if(config.isEnabled()) hud.render(graphics, 0f); });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        startMediaPolling();
        NowPlayingIRLMod.LOGGER.info("Now Playing IRL initialized!");
    }
    private void registerKeybindings(){
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("key.nowplayingirl","category"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nowplayingirl.toggle", InputConstants.Type.KEYBOARD, InputConstants.getKey("key.keyboard.n").getValue(), category));
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nowplayingirl.config", InputConstants.Type.KEYBOARD, InputConstants.getKey("key.keyboard.m").getValue(), category));
        cycleThemeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.nowplayingirl.cycle_theme", InputConstants.Type.KEYBOARD, InputConstants.getKey("key.keyboard.comma").getValue(), category));
    }
    private void onClientTick(Minecraft client){
        while(toggleKey.consumeClick()){
            config.setEnabled(!config.isEnabled()); config.save();
            McCompat.showActionBar(client.player, Component.literal("\u00a7d[Now Playing IRL] \u00a7fWidget: "+(config.isEnabled()?"\u00a7aON":"\u00a7cOFF")));
        }
        while(configKey.consumeClick()){ if(McCompat.noScreenOpen(client)) McCompat.setScreen(client, new ConfigScreen(null)); }
        while(cycleThemeKey.consumeClick()){
            config.cycleTheme(); config.save();
            McCompat.showActionBar(client.player, Component.literal("\u00a7d[Now Playing IRL] \u00a7fTheme: \u00a7b"+config.getTheme().getDisplayName()));
        }
    }
    private void startMediaPolling(){
        executor=Executors.newSingleThreadScheduledExecutor(r->{ Thread t=new Thread(r,"NowPlayingIRL-MediaPoller"); t.setDaemon(true); return t; });
        final int[] idlePolls={0};
        Runnable poll=new Runnable(){ @Override public void run(){
            try{
                MediaDetector.disabledApps=java.util.Set.copyOf(config.getDisabledApps());
                MediaInfo newMedia=mediaDetector.detect();
                if(newMedia!=null && !newMedia.equals(currentMedia)){
                    idlePolls[0]=0;
                    if(newMedia.hasValidInfo()) albumArtFetcher.fetchAsync(newMedia, newMedia::setAlbumArtTexture);
                    currentMedia=newMedia; hud.onMediaChanged(newMedia);
                } else if(newMedia==null && currentMedia!=null){ currentMedia=null; hud.onMediaChanged(null); }
                else if(newMedia!=null) idlePolls[0]=0;
                long base=config.getPollingIntervalMs();
                long delay=newMedia==null ? Math.min(base*(1L+Math.min(idlePolls[0]++,4)),5000L) : base;
                executor.schedule(this, delay, TimeUnit.MILLISECONDS);
            }catch(Exception e){ NowPlayingIRLMod.LOGGER.debug("Error polling media: {}", e.getMessage()); try{ executor.schedule(this,2000,TimeUnit.MILLISECONDS);}catch(Exception ignored){} }
        }};
        executor.schedule(poll,0,TimeUnit.MILLISECONDS);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }
    public void shutdown(){ if(executor!=null) executor.shutdownNow(); if(albumArtFetcher!=null) albumArtFetcher.shutdown(); }
    public ModConfig getConfig(){ return config; }
    public MediaInfo getCurrentMedia(){ return currentMedia; }
    public NowPlayingHud getHud(){ return hud; }
}
