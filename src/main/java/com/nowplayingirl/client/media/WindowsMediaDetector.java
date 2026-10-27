package com.nowplayingirl.client.media;
import com.nowplayingirl.NowPlayingIRLMod;
import com.sun.jna.Native; import com.sun.jna.platform.win32.*; import com.sun.jna.ptr.IntByReference;
import java.util.*;
/**
 * Windows detector v3 — ONE EnumWindows pass for every app (v2 did 3-4 passes).
 * Fixes issue #5 (AIMP): apps are matched by PROCESS NAME (aimp.exe, class AIMP.MainForm),
 * not by requiring the window title to contain the app name — AIMP's playing title is
 * just "Artist - Title", so the old title-contains-"aimp" check could never match.
 */
public class WindowsMediaDetector extends MediaDetector {
    private final Map<Integer,String> pidCache=new HashMap<>();
    private final char[] titleBuf=new char[1024], classBuf=new char[256], pathBuf=new char[1024];
    @Override public MediaInfo detect(){
        pidCache.clear();
        final MediaInfo[] best={null}; final MediaInfo[] paused={null};
        try{
            User32.INSTANCE.EnumWindows((hwnd,data)->{
                if(best[0]!=null) return false;
                if(!User32.INSTANCE.IsWindowVisible(hwnd)) return true;
                int len=User32.INSTANCE.GetWindowText(hwnd,titleBuf,titleBuf.length);
                if(len<=0) return true;
                String text=new String(titleBuf,0,len).trim(); if(text.isEmpty()) return true;
                IntByReference pid=new IntByReference(); User32.INSTANCE.GetWindowThreadProcessId(hwnd,pid);
                String proc=processName(pid.getValue()); if(proc==null) return true;
                MusicApps.App app=MusicApps.byWindowsProcess(proc);
                if(app==null){
                    // Browser window? Guess the web service (YouTube, Spotify Web, ...) from its title.
                    if(proc.matches("chrome|msedge|firefox|brave|opera.*|vivaldi|arc|chromium|thorium|zen|floorp|librewolf|waterfox")){
                        MusicApps.App web=TitleParser.webService(text);
                        if(web!=null&&!disabled(web)){ MediaInfo m=TitleParser.parse(text,web); if(m!=null){best[0]=m; return false;} }
                    }
                    return true;
                }
                if(disabled(app)) return true;
                // Idle windows are just the app name ("Spotify", "AIMP", "VLC media player") -> no track.
                if(text.equalsIgnoreCase(app.display())||text.equalsIgnoreCase(proc)) return true;
                MediaInfo m=TitleParser.parse(text,app);
                if(m!=null&&m.hasValidInfo()){ if(app.isBrowser()){ MusicApps.App web=TitleParser.webService(text); if(web!=null){ m=TitleParser.parse(text,web);} } best[0]=m; return false; }
                return true;
            },null);
        }catch(Exception e){ NowPlayingIRLMod.LOGGER.debug("Windows detection error: {}",e.getMessage()); }
        return best[0]!=null?best[0]:paused[0];
    }
    private String processName(int pid){
        String cached=pidCache.get(pid); if(cached!=null) return cached;
        String name="";
        WinNT.HANDLE h=Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION,false,pid);
        if(h!=null){ try{ IntByReference size=new IntByReference(pathBuf.length); if(Kernel32.INSTANCE.QueryFullProcessImageName(h,0,pathBuf,size)){ String path=new String(pathBuf,0,Math.min(size.getValue(),pathBuf.length)); int s=Math.max(path.lastIndexOf('\\'),path.lastIndexOf('/')); name=(s>=0?path.substring(s+1):path).toLowerCase(Locale.ROOT); } }finally{ Kernel32.INSTANCE.CloseHandle(h);} }
        pidCache.put(pid,name); return name;
    }
}
