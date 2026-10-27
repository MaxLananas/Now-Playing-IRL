package com.nowplayingirl.client.hud;
import com.nowplayingirl.client.NowPlayingClient; import com.nowplayingirl.client.config.ModConfig; import com.nowplayingirl.client.media.MediaInfo;
import net.minecraft.client.MinecraftClient; import net.minecraft.client.font.TextRenderer; import net.minecraft.client.gl.RenderPipelines; import net.minecraft.client.gui.DrawContext; import net.minecraft.util.Identifier; import org.joml.Matrix3x2f;
public class NowPlayingHud {
    private final NowPlayingClient client; private final HudAnimator animator; private MediaInfo currentMedia; private long lastUpdateTime=System.currentTimeMillis();
    static final int WIDGET_WIDTH=210, WIDGET_HEIGHT=62, ALBUM_ART_SIZE=50, PADDING=8;
    public NowPlayingHud(NowPlayingClient c){client=c;animator=new HudAnimator();}
    public void onMediaChanged(MediaInfo m){ if(m!=null&&!m.equals(currentMedia))animator.triggerAppear(); currentMedia=m; }
    private int height(){ return client.getConfig().isCompactMode()?42:WIDGET_HEIGHT; }
    public void render(DrawContext ctx,float tickDelta){
        MinecraftClient mc=MinecraftClient.getInstance(); if(mc.player==null||mc.options.hudHidden) return;
        ModConfig cfg=client.getConfig(); if(!cfg.isEnabled()) return;
        if(currentMedia==null&&cfg.isHideWhenNoMedia()) return;
        long now=System.currentTimeMillis(); animator.update((now-lastUpdateTime)/16.67f,currentMedia!=null,cfg.getRotationSpeed()); lastUpdateTime=now;
        if(animator.isFullyHidden()) return;
        int sw=mc.getWindow().getScaledWidth(), sh=mc.getWindow().getScaledHeight(), h=height();
        float scale=cfg.getScale(); int w2=(int)(WIDGET_WIDTH*scale), h2=(int)(h*scale);
        int x,y; switch(cfg.getPosition()){ case TOP_LEFT->{x=10;y=10;} case TOP_RIGHT->{x=sw-w2-10;y=10;} case BOTTOM_LEFT->{x=10;y=sh-h2-10;} case TOP_CENTER->{x=(sw-w2)/2;y=10;} case BOTTOM_CENTER->{x=(sw-w2)/2;y=sh-h2-10;} default->{x=sw-w2-10;y=sh-h2-10;} }
        x+=cfg.getOffsetX()+(int)animator.getSlideOffset(); y+=cfg.getOffsetY()-(int)animator.getBounceOffset();
        float opacity=cfg.getOpacity()*animator.getVisibility(); Theme theme=cfg.getTheme();
        ctx.getMatrices().pushMatrix(); ctx.getMatrices().translate(x,y); ctx.getMatrices().scale(scale,scale);
        drawBackground(ctx,WIDGET_WIDTH,h,theme,opacity);
        if(currentMedia!=null&&currentMedia.hasValidInfo()) drawPlaying(ctx,mc,theme,opacity,cfg); else drawSilence(ctx,mc,theme,opacity);
        ctx.getMatrices().popMatrix();
    }
    private void drawBackground(DrawContext c,int w,int h,Theme t,float o){
        int bg=applyOpacity(t.getBackgroundColor(),o), bd=applyOpacity(t.getPrimaryColor(),o*.85f), ac=applyOpacity(t.getAccentColor(),o*.35f);
        c.fill(2,0,w-2,h,bg); c.fill(0,2,w,h-2,bg); c.fill(1,1,w-1,h-1,bg);
        c.fill(2,0,w-2,1,bd); c.fill(2,h-1,w-2,h,bd); c.fill(0,2,1,h-2,bd); c.fill(w-1,2,w,h-2,bd);
        c.fill(2,1,w-2,2,ac); // subtle top accent gradient line
    }
    private String scrolling(TextRenderer tr,String text,int maxPx,boolean enabled){
        if(text==null) return ""; if(!enabled||tr.getWidth(text)<=maxPx) return tr.trimToWidth(text,maxPx);
        String s=text+"   \u2022   "; long cycle=(long)(s.length()*350L); long off=(System.currentTimeMillis()%cycle)/350;
        String doubled=s+s; String sub=doubled.substring((int)off,Math.min(doubled.length(),(int)off+text.length()));
        return tr.trimToWidth(sub,maxPx);
    }
    private void drawPlaying(DrawContext ctx,MinecraftClient mc,Theme theme,float o,ModConfig cfg){
        TextRenderer tr=mc.textRenderer; int cx=PADDING, cy=PADDING, h=height();
        if(cfg.isShowAlbumArt()){ drawAlbumArt(ctx,cx,cy+1,Math.min(ALBUM_ART_SIZE-2,h-10),o); cx+=ALBUM_ART_SIZE+PADDING; }
        int maxPx=WIDGET_WIDTH-cx-PADDING;
        ctx.drawText(tr,scrolling(tr,currentMedia.getTitle(),maxPx,cfg.isScrollingText()),cx,cy+3,applyOpacity(theme.getTextColor(),o),false);
        ctx.drawText(tr,scrolling(tr,currentMedia.getArtist(),maxPx,cfg.isScrollingText()),cx,cy+16,applyOpacity(theme.getSecondaryColor(),o),false);
        if(cfg.isShowSource()&&!cfg.isCompactMode()&&currentMedia.getSource()!=null){
            String src=currentMedia.getAlbum()!=null&&!currentMedia.getAlbum().isBlank()?currentMedia.getSource()+" \u2022 "+currentMedia.getAlbum():currentMedia.getSource();
            ctx.drawText(tr,tr.trimToWidth(src,maxPx),cx,cy+29,applyOpacity(theme.getAccentColor(),o),false);
        }
        if(cfg.isShowProgressBar()&&currentMedia.progress()>=0){
            int bw=WIDGET_WIDTH-PADDING*2, bx=PADDING, by=h-7;
            ctx.fill(bx,by,bx+bw,by+3,applyOpacity(0xFF333333,o)); ctx.fill(bx,by,bx+(int)(bw*currentMedia.progress()),by+3,applyOpacity(theme.getPrimaryColor(),o));
        } else if(!cfg.isCompactMode()){
            int dot=(int)(4*animator.getPulseScale()), dx=WIDGET_WIDTH-PADDING-5, dy=h-PADDING-4;
            ctx.fill(dx-dot/2,dy-dot/2,dx+dot/2,dy+dot/2,applyOpacity(currentMedia.isPlaying()?theme.getPrimaryColor():0xFF888888,o));
        }
        if(!currentMedia.isPlaying()) ctx.drawText(tr,"\u275A\u275A",WIDGET_WIDTH-PADDING-14,cy+3,applyOpacity(theme.getTextColor(),o),false);
    }
    private void drawAlbumArt(DrawContext ctx,int x,int y,int size,float o){
        Identifier tex=currentMedia!=null?currentMedia.getAlbumArtTexture():null;
        ctx.getMatrices().pushMatrix(); ctx.getMatrices().translate(x+size/2f,y+size/2f);
        ctx.getMatrices().mul(new Matrix3x2f().rotate((float)Math.toRadians(animator.getAlbumRotation()))); ctx.getMatrices().translate(-size/2f,-size/2f);
        if(tex!=null) ctx.drawTexture(RenderPipelines.GUI_TEXTURED,tex,0,0,0f,0f,size,size,size,size); else drawVinyl(ctx,size,o);
        ctx.getMatrices().popMatrix();
    }
    private void drawVinyl(DrawContext ctx,int size,float o){
        Theme t=client.getConfig().getTheme(); ctx.fill(0,0,size,size,applyOpacity(0xFF1A1A1A,o));
        for(int i=4;i<size/2-6;i+=4){ ctx.fill(i,i,size-i,i+1,applyOpacity(0xFF2A2A2A,o)); ctx.fill(i,size-i-1,size-i,size-i,applyOpacity(0xFF2A2A2A,o)); }
        int ls=size/3; ctx.fill(size/2-ls/2,size/2-ls/2,size/2+ls/2,size/2+ls/2,applyOpacity(t.getPrimaryColor(),o)); ctx.fill(size/2-2,size/2-2,size/2+2,size/2+2,applyOpacity(0xFF000000,o));
    }
    private void drawSilence(DrawContext ctx,MinecraftClient mc,Theme theme,float o){
        TextRenderer tr=mc.textRenderer; String s="Silence..."; int tw=tr.getWidth(s); int tx=(WIDGET_WIDTH-tw)/2, ty=height()/2-4;
        ctx.drawText(tr,s,tx,ty,applyOpacity(theme.getTextColor(),o*.7f),false);
        ctx.drawText(tr,"\u266A",tx+tw+8,(int)(ty+animator.getSleepingNoteY()),applyOpacity(theme.getPrimaryColor(),o*.6f),false);
        float ph=animator.getSleepingNotePhase(); int zc=applyOpacity(theme.getSecondaryColor(),o*(float)Math.abs(Math.sin(ph*.5f))*.6f);
        for(int i=0;i<3;i++){ float zo=(ph+i*.7f)%3f; ctx.drawText(tr,i==2?"Z":"z",tx+tw+22+i*6,(int)(ty-5-zo*4),zc,false); }
    }
    static int applyOpacity(int color,float o){ int a=(int)(((color>>24)&0xFF)*Math.max(0f,Math.min(1f,o))); return (a<<24)|(color&0x00FFFFFF); }
}
