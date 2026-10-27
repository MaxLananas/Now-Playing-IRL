package com.nowplayingirl.client;
import net.minecraft.client.option.KeyBinding; import net.minecraft.client.util.InputUtil; import net.minecraft.util.Identifier; import java.lang.reflect.*;
/** Creates key bindings on both old (String category, <=1.21.8) and new (KeyBinding.Category, 1.21.9+) mappings. */
public final class KeyBindingCompat {
    private KeyBindingCompat(){}
    public static KeyBinding create(String id,int code){
        try{
            for(Constructor<?> c: KeyBinding.class.getConstructors()){
                Class<?>[] t=c.getParameterTypes(); if(t.length!=4||t[0]!=String.class||t[2]!=int.class) continue;
                Object cat;
                if(t[3]==String.class) cat="key.nowplayingirl.category";
                else { Method create=t[3].getMethod("create",Identifier.class); cat=create.invoke(null,Identifier.of("key.nowplayingirl.category")); }
                return (KeyBinding)c.newInstance(id,InputUtil.Type.KEYSYM,code,cat);
            }
        }catch(Exception e){ throw new IllegalStateException("Cannot create key binding "+id,e); }
        throw new IllegalStateException("No compatible KeyBinding constructor for "+id);
    }
}
