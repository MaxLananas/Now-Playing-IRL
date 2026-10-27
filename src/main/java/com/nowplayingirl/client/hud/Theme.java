package com.nowplayingirl.client.hud;
public enum Theme {
    CHERRY_BLOSSOM("Cherry Blossom",0xFFFFB7C5,0xFFFFC0CB,0xFFFFFFFF,0xFF8B4557,0xCC1A0A14),
    PURPLE("Purple",0xFF9B59B6,0xFFBB8FCE,0xFFFFFFFF,0xFF6C3483,0xCC1A0A20),
    OCEAN("Ocean",0xFF3498DB,0xFF85C1E9,0xFFFFFFFF,0xFF21618C,0xCC0A141A),
    FOREST("Forest",0xFF27AE60,0xFF82E0AA,0xFFFFFFFF,0xFF1E8449,0xCC0A1A14),
    SUNSET("Sunset",0xFFE74C3C,0xFFF1948A,0xFFFFFFFF,0xFFC0392B,0xCC1A140A),
    MIDNIGHT("Midnight",0xFF5D6DFF,0xFF9AA5FF,0xFFFFFFFF,0xFF2B3491,0xCC05060F),
    NEON("Neon Cyber",0xFF00FFD1,0xFF7C4DFF,0xFFFFFFFF,0xFF00BFA5,0xCC030A0A),
    GOLD("Royal Gold",0xFFF1C40F,0xFFF9E79F,0xFFFFFFFF,0xFF9A7D0A,0xCC141004),
    MONO("Monochrome",0xFFEAEAEA,0xFFBDBDBD,0xFFFFFFFF,0xFF616161,0xCC0A0A0A),
    LAVA("Lava",0xFFFF5722,0xFFFFAB91,0xFFFFFFFF,0xFFBF360C,0xCC170805),
    ICE("Glacier Ice",0xFFA5F2F3,0xFFE0FFFF,0xFFFFFFFF,0xFF4FB3BF,0xCC061214),
    SAKURA_NIGHT("Sakura Night",0xFFFF6B9D,0xFFFFB7C5,0xFFFFFFFF,0xFF8E2455,0xCC160510);
    private final String displayName; private final int primaryColor,secondaryColor,textColor,accentColor,backgroundColor;
    Theme(String n,int p,int s,int t,int a,int b){displayName=n;primaryColor=p;secondaryColor=s;textColor=t;accentColor=a;backgroundColor=b;}
    public String getDisplayName(){return displayName;} public int getPrimaryColor(){return primaryColor;} public int getSecondaryColor(){return secondaryColor;} public int getTextColor(){return textColor;} public int getAccentColor(){return accentColor;} public int getBackgroundColor(){return backgroundColor;}
    public Theme next(){Theme[] v=values(); return v[(ordinal()+1)%v.length];}
    public Theme prev(){Theme[] v=values(); return v[(ordinal()-1+v.length)%v.length];}
}
