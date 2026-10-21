package com.clockx.dualtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public final class ClockSettings {
    private ClockSettings() {}
    public static SharedPreferences p(Context c) {return c.getSharedPreferences("clockx_configuration",Context.MODE_PRIVATE);}
    public static String homeZone(Context c){return p(c).getString("home","America/Chicago");}
    public static boolean followPhone(Context c){return p(c).getBoolean("follow",true);}
    public static String localZone(Context c){return followPhone(c)?TimeZone.getDefault().getID():p(c).getString("local","America/Chicago");}
    public static int localOffset(Context c){return p(c).getInt("local_shift",0);}
    public static int homeOffset(Context c){return p(c).getInt("home_shift",0);}
    public static int lumeMode(Context c){return p(c).getInt("lume",0);} // auto/on/off
    public static boolean smooth(Context c){return p(c).getBoolean("smooth",true);}
    public static boolean photoMode(Context c){return p(c).getBoolean("photo",false);}
    public static int size(Context c){return p(c).getInt("size",photoMode(c)?72:95);}
    public static int x(Context c){return p(c).getInt("x",50);}
    public static int y(Context c){return p(c).getInt("y",52);}
    public static int photoDim(Context c){return p(c).getInt("dim",20);}
    public static boolean dayOrNight(Context c, ZonedDateTime local){int mode=lumeMode(c);return mode==1||(mode==0&&(local.getHour()>=18||local.getHour()<6));}
    public static ZonedDateTime localTime(Context c,Instant now){return now.plusSeconds(localOffset(c)*60L).atZone(ZoneId.of(localZone(c)));}
    public static ZonedDateTime homeTime(Context c,Instant now){return now.plusSeconds(homeOffset(c)*60L).atZone(ZoneId.of(homeZone(c)));}
    public static Bitmap photo(Context c){File f=new File(c.getFilesDir(),"wallpaper_photo.jpg");if(!f.isFile())return null;return BitmapFactory.decodeFile(f.getAbsolutePath());}
    public static void checkZone(String zone){ZoneId.of(zone);}
}
