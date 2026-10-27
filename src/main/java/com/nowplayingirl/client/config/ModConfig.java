package com.nowplayingirl.client.config;
import com.google.gson.*; import com.nowplayingirl.NowPlayingIRLMod; import com.nowplayingirl.client.hud.Theme; import net.fabricmc.loader.api.FabricLoader;
import java.io.*; import java.nio.file.*; import java.util.*;
public class ModConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH=FabricLoader.getInstance().getConfigDir().resolve("nowplayingirl.json");
    private boolean enabled=true, showAlbumArt=true, showProgressBar=true, showSource=true, compactMode=false, hideWhenNoMedia=false, scrollingText=true;
    private Position position=Position.BOTTOM_RIGHT;
    private float scale=1.0f, opacity=0.9f, rotationSpeed=1.0f;
    private Theme theme=Theme.CHERRY_BLOSSOM;
    private int pollingIntervalMs=1000, offsetX=0, offsetY=0;
    private List<String> disabledApps=new ArrayList<>();
    public enum Position{ TOP_LEFT,TOP_RIGHT,BOTTOM_LEFT,BOTTOM_RIGHT,TOP_CENTER,BOTTOM_CENTER;
        public Position next(){Position[] v=values(); return v[(ordinal()+1)%v.length];} }
    public boolean isEnabled(){return enabled;} public Position getPosition(){return position;} public float getScale(){return scale;} public float getOpacity(){return opacity;} public float getRotationSpeed(){return rotationSpeed;} public boolean isShowAlbumArt(){return showAlbumArt;} public Theme getTheme(){return theme;} public int getPollingIntervalMs(){return pollingIntervalMs;}
    public boolean isShowProgressBar(){return showProgressBar;} public boolean isShowSource(){return showSource;} public boolean isCompactMode(){return compactMode;} public boolean isHideWhenNoMedia(){return hideWhenNoMedia;} public boolean isScrollingText(){return scrollingText;} public int getOffsetX(){return offsetX;} public int getOffsetY(){return offsetY;} public List<String> getDisabledApps(){return disabledApps==null?List.of():disabledApps;}
    public void setEnabled(boolean e){enabled=e;} public void setPosition(Position p){position=p;} public void setScale(float s){scale=Math.max(0.5f,Math.min(2.5f,s));} public void setOpacity(float o){opacity=Math.max(0.1f,Math.min(1f,o));} public void setRotationSpeed(float s){rotationSpeed=Math.max(0f,Math.min(5f,s));} public void setShowAlbumArt(boolean s){showAlbumArt=s;} public void setTheme(Theme t){theme=t;} public void setShowProgressBar(boolean v){showProgressBar=v;} public void setShowSource(boolean v){showSource=v;} public void setCompactMode(boolean v){compactMode=v;} public void setHideWhenNoMedia(boolean v){hideWhenNoMedia=v;} public void setScrollingText(boolean v){scrollingText=v;} public void setPollingIntervalMs(int v){pollingIntervalMs=Math.max(250,Math.min(10000,v));}
    public void cycleTheme(){theme=theme.next();} public void cyclePosition(){position=position.next();}
    public void reset(){enabled=true;position=Position.BOTTOM_RIGHT;scale=1f;opacity=.9f;rotationSpeed=1f;showAlbumArt=true;showProgressBar=true;showSource=true;compactMode=false;hideWhenNoMedia=false;scrollingText=true;theme=Theme.CHERRY_BLOSSOM;pollingIntervalMs=1000;offsetX=0;offsetY=0;disabledApps=new ArrayList<>();save();}
    public static ModConfig load(){ if(Files.exists(CONFIG_PATH)){ try(Reader r=Files.newBufferedReader(CONFIG_PATH)){ ModConfig c=GSON.fromJson(r,ModConfig.class); if(c!=null){ if(c.theme==null)c.theme=Theme.CHERRY_BLOSSOM; if(c.position==null)c.position=Position.BOTTOM_RIGHT; if(c.disabledApps==null)c.disabledApps=new ArrayList<>(); c.setScale(c.scale); c.setOpacity(c.opacity); c.setPollingIntervalMs(c.pollingIntervalMs); return c; } }catch(Exception e){ NowPlayingIRLMod.LOGGER.error("Failed to load config, using defaults",e);} } ModConfig c=new ModConfig(); c.save(); return c; }
    public void save(){ try{ Files.createDirectories(CONFIG_PATH.getParent()); try(Writer wr=Files.newBufferedWriter(CONFIG_PATH)){ GSON.toJson(this,wr);} }catch(Exception e){ NowPlayingIRLMod.LOGGER.error("Failed to save config",e);} }
}
