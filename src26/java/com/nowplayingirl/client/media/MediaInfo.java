package com.nowplayingirl.client.media;
import net.minecraft.resources.Identifier;
import java.util.Objects;
public class MediaInfo {
    private String title, artist, album, source, appId, artUrl;
    private Identifier albumArtTexture;
    private boolean playing = true;
    private long positionMs = -1, durationMs = -1;
    private final long timestamp = System.currentTimeMillis();
    public MediaInfo(){}
    public MediaInfo(String title,String artist,String source){this.title=title;this.artist=artist;this.source=source;}
    public boolean hasValidInfo(){return title!=null&&!title.isBlank()&&artist!=null&&!artist.isBlank();}
    public String getTitle(){return title;} public void setTitle(String t){title=t;}
    public String getArtist(){return artist;} public void setArtist(String a){artist=a;}
    public String getAlbum(){return album;} public void setAlbum(String a){album=a;}
    public String getSource(){return source;} public void setSource(String s){source=s;}
    public String getAppId(){return appId;} public void setAppId(String a){appId=a;}
    public String getArtUrl(){return artUrl;} public void setArtUrl(String a){artUrl=a;}
    public Identifier getAlbumArtTexture(){return albumArtTexture;} public void setAlbumArtTexture(Identifier t){albumArtTexture=t;}
    public boolean isPlaying(){return playing;} public void setPlaying(boolean p){playing=p;}
    public long getPositionMs(){return positionMs;} public void setPositionMs(long p){positionMs=p;}
    public long getDurationMs(){return durationMs;} public void setDurationMs(long d){durationMs=d;}
    public long getTimestamp(){return timestamp;}
    public float progress(){ return durationMs>0&&positionMs>=0 ? Math.max(0f,Math.min(1f,positionMs/(float)durationMs)) : -1f; }
    public String getDisplayTitle(){return truncate(title,30);} public String getDisplayArtist(){return truncate(artist,25);}
    static String truncate(String s,int max){return s==null||s.length()<=max?s:s.substring(0,max-3)+"...";}
    @Override public boolean equals(Object o){if(this==o)return true; if(!(o instanceof MediaInfo m))return false; return Objects.equals(title,m.title)&&Objects.equals(artist,m.artist)&&Objects.equals(album,m.album)&&Objects.equals(source,m.source)&&playing==m.playing;}
    @Override public int hashCode(){return Objects.hash(title,artist,album,source);}
    @Override public String toString(){return String.format("%s - %s [%s]",artist,title,source);}
}
