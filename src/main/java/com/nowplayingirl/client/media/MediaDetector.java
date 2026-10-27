package com.nowplayingirl.client.media;
import com.nowplayingirl.NowPlayingIRLMod;
import java.util.*;
public abstract class MediaDetector {
    public abstract MediaInfo detect();
    /** Apps the user disabled in the config (by id); checked once per poll, no allocation in hot path when empty. */
    public static volatile Set<String> disabledApps = Set.of();
    static boolean disabled(MusicApps.App a){ return a!=null && disabledApps.contains(a.id()); }
    public static MediaDetector create(){
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
        if(os.contains("win")) return new WindowsMediaDetector();
        if(os.contains("mac")) return new MacOSMediaDetector();
        return new LinuxMediaDetector();
    }
}
// ---------- macOS: ONE osascript process for every app (was: one per app) ----------
class MacOSMediaDetector extends MediaDetector {
    private static final String SCRIPT = buildScript();
    private static String buildScript(){
        StringBuilder script=new StringBuilder();
        for(MusicApps.App a: MusicApps.APPS){ if(a.macApp().isBlank()) continue;
            String app=a.macApp().replace("\\","").replace("\"","'");
            script.append("try\n tell application \"").append(app).append("\"\n  if it is running then\n   try\n    if player state is playing then return \"").append(a.id()).append("|\" & (get artist of current track) & \"|\" & (get name of current track)\n   end try\n  end if\n end tell\nend try\n");
        }
        return script.toString();
    }
    @Override public MediaInfo detect(){
        if(SCRIPT.isEmpty()) return null;
        String out=ProcessRunner.run(List.of("osascript","-e",SCRIPT),1200);
        if(out.isEmpty()) return null;
        String[] p=out.split("\\|",3); if(p.length<3) return null;
        MusicApps.App app=MusicApps.byId(p[0].trim()); if(app==null||disabled(app)) return null;
        MediaInfo m=new MediaInfo(p[2].trim(),p[1].trim(),app.display()); m.setAppId(app.id()); return m.hasValidInfo()?m:null;
    }
}
// ---------- Linux: ONE playerctl process for every MPRIS player (was: 3+ per player) ----------
class LinuxMediaDetector extends MediaDetector {
    private static final String SEP="\u001f";
    @Override public MediaInfo detect(){
        String fmt="{{playerName}}"+SEP+"{{status}}"+SEP+"{{xesam:artist}}"+SEP+"{{xesam:title}}"+SEP+"{{xesam:album}}"+SEP+"{{mpris:artUrl}}"+SEP+"{{position}}"+SEP+"{{mpris:length}}";
        String out=ProcessRunner.run(List.of("playerctl","-a","metadata","--format",fmt),1200);
        if(out.isEmpty()) return null;
        MediaInfo best=null;
        for(String line: out.split("\\R")){
            String[] f=line.split(SEP,-1); if(f.length<4) continue;
            MusicApps.App app=MusicApps.byLinuxPlayer(f[0].trim());
            String display=app!=null?app.display():cap(f[0].trim());
            if(app!=null&&disabled(app)) continue;
            boolean playing=f[1].trim().equalsIgnoreCase("Playing");
            String artist=f[2].trim(), title=f[3].trim();
            if(title.isEmpty()) continue;
            if(artist.isEmpty()) artist=display;
            MediaInfo m=new MediaInfo(title,artist,display); if(app!=null)m.setAppId(app.id());
            if(f.length>4)m.setAlbum(f[4].trim()); if(f.length>5&&!f[5].isBlank())m.setArtUrl(f[5].trim());
            try{ if(f.length>6&&!f[6].isBlank())m.setPositionMs(Long.parseLong(f[6].trim())/1000); if(f.length>7&&!f[7].isBlank())m.setDurationMs(Long.parseLong(f[7].trim())/1000);}catch(NumberFormatException ignored){}
            m.setPlaying(playing);
            if(playing) return m; if(best==null) best=m;
        }
        return best;
    }
    private String cap(String s){ return s==null||s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }
}
