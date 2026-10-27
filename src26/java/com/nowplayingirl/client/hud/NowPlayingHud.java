package com.nowplayingirl.client.hud;
import com.nowplayingirl.client.NowPlayingClient; import com.nowplayingirl.client.config.ModConfig; import com.nowplayingirl.client.media.MediaInfo;
import net.minecraft.client.Minecraft; import net.minecraft.client.gui.Font; import net.minecraft.client.gui.GuiGraphicsExtractor; import net.minecraft.client.renderer.RenderPipelines; import net.minecraft.resources.Identifier;
public class NowPlayingHud {
    private final NowPlayingClient client; private final HudAnimator animator; private MediaInfo currentMedia; private long lastUpdateTime=System.currentTimeMillis();
    static final int WIDGET_WIDTH=210, WIDGET_HEIGHT=62, ALBUM_ART_SIZE=50, PADDING=8;
    public NowPlayingHud(NowPlayingClient c){client=c;animator=new HudAnimator();}
    public void onMediaChanged(MediaInfo m){ if(m!=null&&!m.equals(currentMedia))animator.triggerAppear(); currentMedia=m; }
    private int height(){ return client.getConfig().isCompactMode()?42:WIDGET_HEIGHT; }
    public void render(GuiGraphicsExtractor ctx,float tickDelta){
        Minecraft mc=Minecraft.getInstance(); if(mc.player==null||com.nowplayingirl.client.McCompat.hudHidden(mc)) return;
        ModConfig cfg=client.getConfig(); if(!cfg.isEnabled()) return;
        if(currentMedia==null&&cfg.isHideWhenNoMedia()) return;
        long now=System.currentTimeMillis(); animator.update((now-lastUpdateTime)/16.67f,currentMedia!=null,cfg.getRotationSpeed()); lastUpdateTime=now;
        if(animator.isFullyHidden()) return;
        int sw=ctx.guiWidth(), sh=ctx.guiHeight(), h=height();
        float scale=cfg.getScale(); int w2=(int)(WIDGET_WIDTH*scale), h2=(int)(h*scale);
        int x,y; switch(cfg.getPosition()){ case TOP_LEFT->{x=10;y=10;} case TOP_RIGHT->{x=sw-w2-10;y=10;} case BOTTOM_LEFT->{x=10;y=sh-h2-10;} case TOP_CENTER->{x=(sw-w2)/2;y=10;} case BOTTOM_CENTER->{x=(sw-w2)/2;y=sh-h2-10;} default->{x=sw-w2-10;y=sh-h2-10;} }
        x+=cfg.getOffsetX()+(int)animator.getSlideOffset(); y+=cfg.getOffsetY()-(int)animator.getBounceOffset();
        float opacity=cfg.getOpacity()*animator.getVisibility(); Theme theme=cfg.getTheme();
        ctx.pose().pushMatrix(); ctx.pose().translate(x,y); ctx.pose().scale(scale,scale);
        drawBackground(ctx,WIDGET_WIDTH,h,theme,opacity);
        if(currentMedia!=null&&currentMedia.hasValidInfo()) drawPlaying(ctx,mc,theme,opacity,cfg); else drawSilence(ctx,mc,theme,opacity);
        ctx.pose().popMatrix();
    }
    private void drawBackground(GuiGraphicsExtractor c,int w,int h,Theme t,float o){
        int bg=applyOpacity(t.getBackgroundColor(),o), bd=applyOpacity(t.getPrimaryColor(),o*.85f), ac=applyOpacity(t.getAccentColor(),o*.35f);
        c.fill(2,0,w-2,h,bg); c.fill(0,2,w,h-2,bg); c.fill(1,1,w-1,h-1,bg);
        c.fill(2,0,w-2,1,bd); c.fill(2,h-1,w-2,h,bd); c.fill(0,2,1,h-2,bd); c.fill(w-1,2,w,h-2,bd);
        c.fill(2,1,w-2,2,ac);
    }
    private String scrolling(Font tr,String text,int maxPx,boolean enabled){
        if(text==null) return ""; if(!enabled||tr.width(text)<=maxPx) return tr.plainSubstrByWidth(text,maxPx);
        String s=text+"   \u2022   "; long cycle=(long)(s.length()*350L); long off=(System.currentTimeMillis()%cycle)/350;
        String doubled=s+s; String sub=doubled.substring((int)off,Math.min(doubled.length(),(int)off+text.length()));
        return tr.plainSubstrByWidth(sub,maxPx);
    }
    private void drawPlaying(GuiGraphicsExtractor ctx,Minecraft mc,Theme theme,float o,ModConfig cfg){
        Font tr=mc.font; int cx=PADDING, cy=PADDING, h=height();
        if(cfg.isShowAlbumArt()){ drawAlbumArt(ctx,cx,cy+1,Math.min(ALBUM_ART_SIZE-2,h-10),o); cx+=ALBUM_ART_SIZE+PADDING; }
        int maxPx=WIDGET_WIDTH-cx-PADDING;
        ctx.text(tr,scrolling(tr,currentMedia.getTitle(),maxPx,cfg.isScrollingText()),cx,cy+3,applyOpacity(theme.getTextColor(),o));
        ctx.text(tr,scrolling(tr,currentMedia.getArtist(),maxPx,cfg.isScrollingText()),cx,cy+16,applyOpacity(theme.getSecondaryColor(),o));
        if(cfg.isShowSource()&&!cfg.isCompactMode()&&currentMedia.getSource()!=null){
            String src=currentMedia.getAlbum()!=null&&!currentMedia.getAlbum().isBlank()?currentMedia.getSource()+" \u2022 "+currentMedia.getAlbum():currentMedia.getSource();
            ctx.text(tr,tr.plainSubstrByWidth(src,maxPx),cx,cy+29,applyOpacity(theme.getAccentColor(),o));
        }
        if(cfg.isShowProgressBar()&&currentMedia.progress()>=0){
            int bw=WIDGET_WIDTH-PADDING*2, bx=PADDING, by=h-7;
            ctx.fill(bx,by,bx+bw,by+3,applyOpacity(0xFF333333,o)); ctx.fill(bx,by,bx+(int)(bw*currentMedia.progress()),by+3,applyOpacity(theme.getPrimaryColor(),o));
        } else if(!cfg.isCompactMode()){
            int dot=(int)(4*animator.getPulseScale()), dx=WIDGET_WIDTH-PADDING-5, dy=h-PADDING-4;
            ctx.fill(dx-dot/2,dy-dot/2,dx+dot/2,dy+dot/2,applyOpacity(currentMedia.isPlaying()?theme.getPrimaryColor():0xFF888888,o));
        }
        if(!currentMedia.isPlaying()) ctx.text(tr,"\u275A\u275A",WIDGET_WIDTH-PADDING-14,cy+3,applyOpacity(theme.getTextColor(),o));
    }
    private void drawAlbumArt(GuiGraphicsExtractor ctx,int x,int y,int size,float o){
        Identifier tex=currentMedia!=null?currentMedia.getAlbumArtTexture():null;
        ctx.pose().pushMatrix(); ctx.pose().translate(x+size/2f,y+size/2f); ctx.pose().rotate((float)Math.toRadians(animator.getAlbumRotation())); ctx.pose().translate(-size/2f,-size/2f);
        if(tex!=null) ctx.blit(RenderPipelines.GUI_TEXTURED,tex,0,0,0f,0f,size,size,size,size); else drawVinyl(ctx,size,o);
        ctx.pose().popMatrix();
    }
    private void drawVinyl(GuiGraphicsExtractor ctx,int size,float o){
        Theme t=client.getConfig().getTheme(); ctx.fill(0,0,size,size,applyOpacity(0xFF1A1A1A,o));
        for(int i=4;i<size/2-6;i+=4){ ctx.fill(i,i,size-i,i+1,applyOpacity(0xFF2A2A2A,o)); ctx.fill(i,size-i-1,size-i,size-i,applyOpacity(0xFF2A2A2A,o)); }
        int ls=size/3; ctx.fill(size/2-ls/2,size/2-ls/2,size/2+ls/2,size/2+ls/2,applyOpacity(t.getPrimaryColor(),o)); ctx.fill(size/2-2,size/2-2,size/2+2,size/2+2,applyOpacity(0xFF000000,o));
    }
    private void drawSilence(GuiGraphicsExtractor ctx,Minecraft mc,Theme theme,float o){
        Font tr=mc.font; String s="Silence..."; int tw=tr.width(s); int tx=(WIDGET_WIDTH-tw)/2, ty=height()/2-4;
        ctx.text(tr,s,tx,ty,applyOpacity(theme.getTextColor(),o*.7f));
        ctx.text(tr,"\u266A",tx+tw+8,(int)(ty+animator.getSleepingNoteY()),applyOpacity(theme.getPrimaryColor(),o*.6f));
        float ph=animator.getSleepingNotePhase(); int zc=applyOpacity(theme.getSecondaryColor(),o*(float)Math.abs(Math.sin(ph*.5f))*.6f);
        for(int i=0;i<3;i++){ float zo=(ph+i*.7f)%3f; ctx.text(tr,i==2?"Z":"z",tx+tw+22+i*6,(int)(ty-5-zo*4),zc); }
    }
    static int applyOpacity(int color,float o){ int a=(int)(((color>>24)&0xFF)*Math.max(0f,Math.min(1f,o))); return (a<<24)|(color&0x00FFFFFF); }
}
