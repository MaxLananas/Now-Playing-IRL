package com.nowplayingirl.client;
import net.minecraft.client.Minecraft; import net.minecraft.client.gui.screens.Screen; import net.minecraft.network.chat.Component;
import java.lang.reflect.*;
/** Small reflective bridge for API differences inside the 26.x line (26.1 vs 26.3 screen/message/HUD renames). Mojang names are official, so reflection by name is stable. */
public final class McCompat {
    private McCompat(){}
    private static Object get(Object target, String field){ try{ Field f=findField(target.getClass(),field); f.setAccessible(true); return f.get(target);}catch(Exception e){ return null; } }
    private static Field findField(Class<?> c,String n) throws NoSuchFieldException { while(c!=null){ try{ return c.getDeclaredField(n);}catch(NoSuchFieldException e){ c=c.getSuperclass(); } } throw new NoSuchFieldException(n); }
    private static Method findMethod(Class<?> c,String n,Class<?>... types){ while(c!=null){ for(Method m: c.getDeclaredMethods()){ if(m.getName().equals(n)&&m.getParameterCount()==types.length){ boolean ok=true; for(int i=0;i<types.length;i++) if(!m.getParameterTypes()[i].isAssignableFrom(types[i])){ ok=false; break; } if(ok) return m; } } c=c.getSuperclass(); } return null; }
    /** Creates a KeyMapping whatever the 26.x constructor shape is (Type+int+Category, Key+Category, ...). */
    public static net.minecraft.client.KeyMapping createKeyMapping(String name, String key, Object category){
        try{
            Class<?> kmClass = Class.forName("net.minecraft.client.KeyMapping");
            for(Constructor<?> c : kmClass.getConstructors()){
                Class<?>[] t = c.getParameterTypes();
                try{
                    if(t.length==4 && t[0]==String.class && t[2]==int.class && t[3].isAssignableFrom(category.getClass())){
                        Object type = Enum.valueOf((Class<Enum>) t[1], "KEYBOARD");
                        return (net.minecraft.client.KeyMapping) c.newInstance(name, type, keyCode(key), category);
                    }
                    if(t.length==3 && t[0]==String.class && t[2].isAssignableFrom(category.getClass())){
                        return (net.minecraft.client.KeyMapping) c.newInstance(name, keyObject(key), category);
                    }
                    if(t.length==4 && t[0]==String.class && t[3]==String.class){
                        Object type = Enum.valueOf((Class<Enum>) t[1], "KEYBOARD");
                        return (net.minecraft.client.KeyMapping) c.newInstance(name, type, keyCode(key), "key.nowplayingirl.category");
                    }
                    if(t.length==3 && t[0]==String.class && t[1]==int.class && t[2]==String.class){
                        return (net.minecraft.client.KeyMapping) c.newInstance(name, keyCode(key), "key.nowplayingirl.category");
                    }
                }catch(Exception ignored){}
            }
        }catch(Exception e){ throw new IllegalStateException("Cannot create KeyMapping "+name, e); }
        throw new IllegalStateException("No compatible KeyMapping constructor for "+name);
    }
    private static Object keyObject(String key) throws Exception {
        Class<?> ic = Class.forName("com.mojang.blaze3d.platform.InputConstants");
        Method m = ic.getMethod("getKey", String.class);
        return m.invoke(null, "key.keyboard."+key);
    }
    private static int keyCode(String key){
        try{ Object k=keyObject(key); Method v=k.getClass().getMethod("getValue"); return (int) v.invoke(k); }catch(Exception ignored){}
        for(String[] cf : new String[][]{{"org.lwjgl.glfw.GLFW","GLFW_KEY_"},{"org.lwjgl.sdl.SDLScancode","SDL_SCANCODE_"}}){
            try{ Class<?> c=Class.forName(cf[0]); return c.getField(cf[1]+key.toUpperCase()).getInt(null); }catch(Exception ignored){}
        }
        return key.toUpperCase().charAt(0);
    }
    public static void setScreen(Minecraft mc, Screen screen){
        Object gui=get(mc,"gui");
        if(gui!=null){ Method m=findMethod(gui.getClass(),"setScreen",Screen.class); if(m!=null){ try{ m.setAccessible(true); m.invoke(gui,screen); return; }catch(Exception ignored){} } }
        Method m=findMethod(mc.getClass(),"setScreen",Screen.class); if(m!=null){ try{ m.setAccessible(true); m.invoke(mc,screen); }catch(Exception ignored){} }
    }
    public static boolean noScreenOpen(Minecraft mc){
        Object gui=get(mc,"gui");
        if(gui!=null){ Object s=get(gui,"screen"); if(s!=null) return false; Method m=findMethod(gui.getClass(),"screen"); if(m!=null){ try{ m.setAccessible(true); return m.invoke(gui)==null; }catch(Exception ignored){} } }
        return get(mc,"screen")==null;
    }
    public static void showActionBar(Object player, Component message){
        if(player==null) return;
        Method m=findMethod(player.getClass(),"displayClientMessage",Component.class,boolean.class);
        if(m!=null){ try{ m.setAccessible(true); m.invoke(player,message,true); return; }catch(Exception ignored){} }
        m=findMethod(player.getClass(),"sendOverlayMessage",Component.class);
        if(m!=null){ try{ m.setAccessible(true); m.invoke(player,message); return; }catch(Exception ignored){} }
        m=findMethod(player.getClass(),"sendSystemMessage",Component.class);
        if(m!=null){ try{ m.setAccessible(true); m.invoke(player,message); }catch(Exception ignored){} }
    }
    public static boolean hudHidden(Minecraft mc){
        Object options=get(mc,"options");
        if(options!=null){ Object v=get(options,"hideGui"); if(v instanceof Boolean b) return b; }
        Object gui=get(mc,"gui"); Object hud=gui!=null?get(gui,"hud"):null;
        if(hud!=null){ Method m=findMethod(hud.getClass(),"isHidden"); if(m!=null){ try{ m.setAccessible(true); return Boolean.TRUE.equals(m.invoke(hud)); }catch(Exception ignored){} } }
        return false;
    }
}
