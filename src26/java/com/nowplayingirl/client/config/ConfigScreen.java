package com.nowplayingirl.client.config;
import com.nowplayingirl.client.NowPlayingClient; import com.nowplayingirl.client.hud.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor; import net.minecraft.client.gui.components.AbstractSliderButton; import net.minecraft.client.gui.components.Button; import net.minecraft.client.gui.screens.Screen; import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
public class ConfigScreen extends Screen {
    private final Screen parent; private ModConfig config; private float previewRotation=0f;
    public ConfigScreen(Screen parent){ super(Component.translatable("nowplayingirl.config.title")); this.parent=parent; }
    private Button btn(String label,int x,int y,int w,Consumer<Button> onClick){ return addRenderableWidget(Button.builder(Component.literal(label), b->onClick.accept(b)).pos(x,y).size(w,20).build()); }
    @Override protected void init(){
        config=NowPlayingClient.getInstance().getConfig();
        int cx=width/2, y=40, bw=200, spacing=24;
        btn("Enabled: "+(config.isEnabled()?"\u00a7aON":"\u00a7cOFF"),cx-bw/2,y,bw,b->{config.setEnabled(!config.isEnabled()); b.setMessage(Component.literal("Enabled: "+(config.isEnabled()?"\u00a7aON":"\u00a7cOFF")));}); y+=spacing;
        btn("Position: "+posName(config.getPosition()),cx-bw/2,y,bw,b->{config.cyclePosition(); b.setMessage(Component.literal("Position: "+posName(config.getPosition())));}); y+=spacing;
        btn("Theme: \u00a7d"+config.getTheme().getDisplayName(),cx-bw/2,y,bw,b->{config.cycleTheme(); b.setMessage(Component.literal("Theme: \u00a7d"+config.getTheme().getDisplayName()));}); y+=spacing;
        addRenderableWidget(new AbstractSliderButton(cx-bw/2,y,bw,20,Component.literal("Scale: "+String.format("%.0f%%",config.getScale()*100)),(config.getScale()-0.5)/1.5){
            @Override protected void updateMessage(){ setMessage(Component.literal("Scale: "+String.format("%.0f%%",(0.5+value*1.5)*100))); }
            @Override protected void applyValue(){ config.setScale((float)(0.5+value*1.5)); }}); y+=spacing;
        addRenderableWidget(new AbstractSliderButton(cx-bw/2,y,bw,20,Component.literal("Opacity: "+String.format("%.0f%%",config.getOpacity()*100)),config.getOpacity()){
            @Override protected void updateMessage(){ setMessage(Component.literal("Opacity: "+String.format("%.0f%%",value*100))); }
            @Override protected void applyValue(){ config.setOpacity((float)value); }}); y+=spacing;
        addRenderableWidget(new AbstractSliderButton(cx-bw/2,y,bw,20,Component.literal("Rotation: "+rotText(config.getRotationSpeed())),config.getRotationSpeed()/5.0){
            @Override protected void updateMessage(){ setMessage(Component.literal("Rotation: "+rotText((float)(value*5)))); }
            @Override protected void applyValue(){ config.setRotationSpeed((float)(value*5)); }}); y+=spacing;
        btn("Album Art: "+(config.isShowAlbumArt()?"\u00a7aVisible":"\u00a77Hidden"),cx-bw/2,y,bw,b->{config.setShowAlbumArt(!config.isShowAlbumArt()); b.setMessage(Component.literal("Album Art: "+(config.isShowAlbumArt()?"\u00a7aVisible":"\u00a77Hidden")));}); y+=spacing;
        toggle(cx-bw/2,y,98,"Progress Bar",config.isShowProgressBar(),config::setShowProgressBar); toggle(cx+2,y,98,"Source",config.isShowSource(),config::setShowSource); y+=spacing;
        toggle(cx-bw/2,y,98,"Compact",config.isCompactMode(),config::setCompactMode); toggle(cx+2,y,98,"Hide Silence",config.isHideWhenNoMedia(),config::setHideWhenNoMedia); y+=spacing;
        toggle(cx-bw/2,y,98,"Scrolling Text",config.isScrollingText(),config::setScrollingText);
        btn("Poll: "+config.getPollingIntervalMs()+"ms",cx+2,y,98,b->{ int[] vals={250,500,1000,2000,5000}; int cur=config.getPollingIntervalMs(), nxt=vals[0]; for(int i=0;i<vals.length;i++) if(vals[i]==cur) nxt=vals[(i+1)%vals.length]; config.setPollingIntervalMs(nxt); b.setMessage(Component.literal("Poll: "+nxt+"ms")); });
        y+=spacing+15;
        btn("\u00a7a\u2714 Done",cx-100,y,95,b->onClose());
        btn("\u00a7c\u2716 Reset",cx+5,y,95,b->{config.reset(); minecraft.setScreen(new ConfigScreen(parent));});
    }
    private void toggle(int x,int y,int w,String label,boolean initial,Consumer<Boolean> setter){
        final boolean[] state={initial};
        btn(label+": "+(state[0]?"\u00a7aON":"\u00a7cOFF"),x,y,w,b->{ state[0]=!state[0]; setter.accept(state[0]); b.setMessage(Component.literal(label+": "+(state[0]?"\u00a7aON":"\u00a7cOFF"))); });
    }
    private String posName(ModConfig.Position p){ return switch(p){ case TOP_LEFT->"Top Left"; case TOP_RIGHT->"Top Right"; case BOTTOM_LEFT->"Bottom Left"; case BOTTOM_RIGHT->"Bottom Right"; case TOP_CENTER->"Top Center"; case BOTTOM_CENTER->"Bottom Center"; }; }
    private String rotText(float s){ if(s==0) return "\u00a77Disabled"; if(s<1) return "\u00a7eSlow"; if(s<2) return "\u00a7aNormal"; if(s<3.5f) return "\u00a7bFast"; return "\u00a7c\u00a7lTURBO!"; }
    @Override public void onClose(){ config.save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen(){ return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractRenderState(g,mouseX,mouseY,delta);
        Theme theme=config.getTheme(); int titleColor=theme.getPrimaryColor()|0xFF000000;
        g.centeredText(font,"\u00a7l\u266A Now Playing IRL \u266A",width/2,12,titleColor);
        g.centeredText(font,"\u00a77Configuration",width/2,24,0xAAAAAA);
        drawPreview(g,delta);
        g.centeredText(font,"\u00a78[N] Toggle  |  [M] Menu  |  [,] Theme",width/2,height-20,0x888888);
    }
    private void drawPreview(GuiGraphicsExtractor g,float delta){
        int px=width-145, py=40, pw=130, ph=110; Theme theme=config.getTheme();
        int primary=theme.getPrimaryColor()|0xFF000000, bg=theme.getBackgroundColor()|0xFF000000;
        g.fill(px-3,py-3,px+pw+3,py+ph+3,primary); g.fill(px,py,px+pw,py+ph,bg);
        g.centeredText(font,"\u00a7lPreview",px+pw/2,py-14,0xFFFFFF);
        previewRotation+=config.getRotationSpeed()*0.6f; if(previewRotation>=360) previewRotation-=360;
        int dcx=px+45, dcy=py+55, r=35;
        g.pose().pushMatrix(); g.pose().translate(dcx,dcy); g.pose().rotate((float)Math.toRadians(previewRotation));
        circle(g,0,0,r,0xFF111111); ring(g,0,0,r-4,0xFF222222); ring(g,0,0,r-8,0xFF222222); ring(g,0,0,r-12,0xFF222222); ring(g,0,0,r-16,0xFF222222);
        g.fill(-r+5,-3,-5,0,0x33FFFFFF); circle(g,0,0,12,primary); circle(g,0,0,3,0xFF000000);
        g.pose().popMatrix();
        int tx=dcx+r+12, ty=py+25;
        g.text(font,"\u00a7fSong Title",tx,ty,0xFFFFFF); ty+=14;
        g.text(font,"Artist",tx,ty,(theme.getSecondaryColor()&0x00FFFFFF)|0xFF000000); ty+=14;
        g.text(font,"\u00a7oSpotify",tx,ty,(theme.getAccentColor()&0x00FFFFFF)|0xFF000000);
        g.text(font,"\u00a77Size: \u00a7f"+String.format("%.0f%%",config.getScale()*100),px+8,py+ph-22,0xAAAAAA);
        g.text(font,"\u00a77Opacity: \u00a7f"+String.format("%.0f%%",config.getOpacity()*100),px+8,py+ph-12,0xAAAAAA);
        float pulse=(float)(1+Math.sin(System.currentTimeMillis()/200.0)*0.3); int ds=(int)(4*pulse), dx=px+pw-12, dy=py+ph-12;
        g.fill(dx-ds,dy-ds,dx+ds,dy+ds,primary);
    }
    private void circle(GuiGraphicsExtractor g,int cx,int cy,int r,int color){ for(int yy=-r;yy<=r;yy++){ int hw=(int)Math.sqrt(r*r-yy*yy); g.fill(cx-hw,cy+yy,cx+hw,cy+yy+1,color); } }
    private void ring(GuiGraphicsExtractor g,int cx,int cy,int r,int color){ int seg=32; for(int i=0;i<seg;i++){ double a1=2*Math.PI*i/seg,a2=2*Math.PI*(i+1)/seg; int x1=cx+(int)(Math.cos(a1)*r),y1=cy+(int)(Math.sin(a1)*r),x2=cx+(int)(Math.cos(a2)*r),y2=cy+(int)(Math.sin(a2)*r); g.fill(Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2)+1,Math.max(y1,y2)+1,color); } }
}
