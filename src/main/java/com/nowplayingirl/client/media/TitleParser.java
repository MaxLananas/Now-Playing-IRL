package com.nowplayingirl.client.media;
import java.util.List;
/** Shared, allocation-light "Artist - Title" window-title parser used by every OS. */
public final class TitleParser {
    private static final String[] SEPS = {" - "," \u2013 "," \u2014 "," | "," : "};
    private static final List<String> NOISE = List.of(" - YouTube","YouTube"," - Spotify","Spotify","TIDAL"," - Deezer","Deezer"," - SoundCloud","SoundCloud"," - Apple Music","Apple Music"," - YouTube Music","YouTube Music","VLC media player"," - VLC","Mozilla Firefox","Google Chrome","Microsoft Edge"," - Brave","Opera GX","Opera","Vivaldi","Chromium");
    private TitleParser(){}
    public static MediaInfo parse(String raw, MusicApps.App app){
        if(raw==null) return null; String t=raw.trim();
        if(t.isEmpty()) return null;
        String low=t.toLowerCase();
        for(String n:NOISE){ if(low.endsWith(n.toLowerCase())||low.endsWith(" - "+n.toLowerCase())){ t=t.substring(0,t.length()-n.length()).replaceAll("[\\s\\-\u2013\u2014|]+$","").trim(); low=t.toLowerCase(); } }
        // foobar2000 style: [album] Artist - Title / AIMP & most players: Artist - Title
        if(t.startsWith("[")&&t.contains("] ")) t=t.substring(t.indexOf("] ")+2).trim();
        if(t.startsWith("(")&&t.contains(") ")) t=t.substring(t.indexOf(") ")+2).trim();
        for(String sep: SEPS){ int i=t.indexOf(sep.trim()); if(i>0){ String a=t.substring(0,i).trim(), b=t.substring(i+sep.trim().length()).trim(); if(!a.isEmpty()&&!b.isEmpty()&&!b.equalsIgnoreCase(app.display())){ MediaInfo m=new MediaInfo(b,a,app.display()); m.setAppId(app.id()); return m; } } }
        // Single-segment title (e.g. a video): keep it, artist = service
        if(t.length()>=2 && !t.equalsIgnoreCase(app.display())){ MediaInfo m=new MediaInfo(t,app.display(),app.display()); m.setAppId(app.id()); return m; }
        return null;
    }
    /** Guess the web service inside a browser window from its title. */
    public static MusicApps.App webService(String title){
        if(title==null) return null; String t=title.toLowerCase();
        String[][] sites={{"youtube music","youtube-music"},{"music.youtube","youtube-music"},{"youtube","youtube"},{"open.spotify","spotify-web"},{"spotify","spotify-web"},{"soundcloud","soundcloud"},{"deezer","deezer-web"},{"tidal","tidal"},{"music.apple","apple-music"},{"apple music","apple-music"},{"music.amazon","amazon-music"},{"amazon music","amazon-music"},{"bandcamp","bandcamp"},{"mixcloud","mixcloud"},{"audiomack","audiomack"},{"twitch","twitch"},{"vimeo","vimeo"},{"qobuz","qobuz"},{"pandora","pandora"},{"iheart","iheart"},{"tunein","tunein"},{"anghami","anghami"},{"jiosaavn","jiosaavn"},{"gaana","gaana"},{"audius","audius"},{"navidrome","navidrome"},{"jellyfin","jellyfin"},{"plex","plex"}};
        for(String[] s:sites) if(t.contains(s[0])){ MusicApps.App a=MusicApps.byId(s[1]); if(a!=null) return a; }
        return null;
    }
}
