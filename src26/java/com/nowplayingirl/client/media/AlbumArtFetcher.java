package com.nowplayingirl.client.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nowplayingirl.NowPlayingIRLMod;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AlbumArtFetcher {
    
    private static final String ITUNES_API = "https://itunes.apple.com/search";
    private static final String DEEZER_API = "https://api.deezer.com/search";
    
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Map<String, Identifier> cache = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>(64,0.75f,true){ @Override protected boolean removeEldestEntry(Map.Entry<String,Identifier> e){ return size()>48; } });
    private final Set<String> inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger textureCounter = new AtomicInteger(0);
    
    public AlbumArtFetcher() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "NowPlayingIRL-ArtFetcher");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void fetchAsync(MediaInfo media, Consumer<Identifier> callback) {
        if (media == null || !media.hasValidInfo()) {
            callback.accept(null);
            return;
        }
        
        String cacheKey = media.getArtist() + "|" + media.getTitle();
        if (cache.containsKey(cacheKey)) { callback.accept(cache.get(cacheKey)); return; }
        if (!inFlight.add(cacheKey)) return;
        executor.submit(() -> {
            try {
                // 0. Artwork URL provided directly by the player (MPRIS mpris:artUrl) - no search needed
                String artUrl = (media.getArtUrl() != null && !media.getArtUrl().isBlank()) ? media.getArtUrl() : null;
                if (artUrl == null) artUrl = fetchFromItunes(media.getArtist(), media.getTitle());
                if (artUrl == null) artUrl = fetchFromDeezer(media.getArtist(), media.getTitle());
                
                if (artUrl != null) {
                    Identifier texture = downloadAndRegisterTexture(artUrl);
                    if (texture != null) {
                        cache.put(cacheKey, texture); inFlight.remove(cacheKey);
                        callback.accept(texture);
                        return;
                    }
                }
                
                inFlight.remove(cacheKey); callback.accept(null);
            } catch (Exception e) {
                inFlight.remove(cacheKey); NowPlayingIRLMod.LOGGER.debug("Failed to fetch album art: {}", e.getMessage());
                callback.accept(null);
            }
        });
    }
    
    private String fetchFromItunes(String artist, String title) {
        try {
            String query = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
            String url = ITUNES_API + "?term=" + query + "&media=music&limit=1";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray results = json.getAsJsonArray("results");
                
                if (results != null && results.size() > 0) {
                    JsonObject first = results.get(0).getAsJsonObject();
                    String artUrl = first.get("artworkUrl100").getAsString();
                    // Obtenir une image plus grande
                    return artUrl.replace("100x100", "300x300");
                }
            }
        } catch (Exception e) {
            NowPlayingIRLMod.LOGGER.debug("iTunes API error: {}", e.getMessage());
        }
        return null;
    }
    
    private String fetchFromDeezer(String artist, String title) {
        try {
            String query = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
            String url = DEEZER_API + "?q=" + query + "&limit=1";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray data = json.getAsJsonArray("data");
                
                if (data != null && data.size() > 0) {
                    JsonObject first = data.get(0).getAsJsonObject();
                    JsonObject album = first.getAsJsonObject("album");
                    if (album != null) {
                        return album.get("cover_medium").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            NowPlayingIRLMod.LOGGER.debug("Deezer API error: {}", e.getMessage());
        }
        return null;
    }
    
    private Identifier downloadAndRegisterTexture(String imageUrl) {
        try {
            if (imageUrl.startsWith("file://")) { byte[] local = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(java.net.URI.create(imageUrl))); return registerBytes(local); }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                byte[] imageData = response.body();
                
                // Convertir en PNG si nécessaire
                byte[] pngData = convertToPng(imageData);
                
                // Créer l'identifier unique
                int id = textureCounter.incrementAndGet();
                Identifier textureId = Identifier.fromNamespaceAndPath(NowPlayingIRLMod.MOD_ID, "dynamic/album_" + id);
                
                // Enregistrer sur le thread principal de Minecraft
                Minecraft client = Minecraft.getInstance();
                CountDownLatch latch = new CountDownLatch(1);
                final Identifier[] result = {null};
                
                client.execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(pngData));
                        DynamicTexture texture = new DynamicTexture(() -> "meow",image);
                        client.getTextureManager().register(textureId, texture);
                        result[0] = textureId;
                    } catch (Exception e) {
                        NowPlayingIRLMod.LOGGER.debug("Failed to register texture: {}", e.getMessage());
                    }
                    latch.countDown();
                });
                
                latch.await(5, TimeUnit.SECONDS);
                return result[0];
            }
        } catch (Exception e) {
            NowPlayingIRLMod.LOGGER.debug("Failed to download image: {}", e.getMessage());
        }
        return null;
    }
    
    private Identifier registerBytes(byte[] imageData) {
        try { byte[] pngData = convertToPng(imageData); int id = textureCounter.incrementAndGet(); Identifier textureId = Identifier.fromNamespaceAndPath(NowPlayingIRLMod.MOD_ID, "dynamic/album_" + id);
            Minecraft client = Minecraft.getInstance(); CountDownLatch latch = new CountDownLatch(1); final Identifier[] result = {null};
            client.execute(() -> { try { NativeImage image = NativeImage.read(new ByteArrayInputStream(pngData)); DynamicTexture texture = new DynamicTexture(() -> "nowplayingirl-album", image); client.getTextureManager().register(textureId, texture); result[0] = textureId; } catch (Exception ignored) {} latch.countDown(); });
            latch.await(5, TimeUnit.SECONDS); return result[0]; } catch (Exception e) { return null; }
    }
    private byte[] convertToPng(byte[] imageData) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    public void shutdown() {
        executor.shutdownNow();
    }
    
    public void clearCache() {
        cache.clear();
    }
}