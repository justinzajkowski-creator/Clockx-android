package com.clockx.dualtime;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.content.SharedPreferences;
import java.time.Instant;

/** A legitimate system live wallpaper: usable as lock screen, home screen, or both in the Android picker. */
public class ClockWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine(){return new ClockEngine();}
    class ClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final ClockRenderer renderer=new ClockRenderer(ClockWallpaperService.this);
        private volatile boolean visible=false;private int w,h;
        private final Runnable tick=new Runnable(){@Override public void run(){if(!visible)return;render();handler.postDelayed(this,ClockSettings.smooth(ClockWallpaperService.this)?33L:1000L);}};
        ClockEngine(){ClockSettings.p(ClockWallpaperService.this).registerOnSharedPreferenceChangeListener(this);setOffsetNotificationsEnabled(false);}
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){super.onSurfaceChanged(holder,format,width,height);w=width;h=height;if(visible)restart();}
        @Override public void onVisibilityChanged(boolean next){super.onVisibilityChanged(next);visible=next;restart();}
        @Override public void onSurfaceDestroyed(SurfaceHolder holder){visible=false;handler.removeCallbacks(tick);super.onSurfaceDestroyed(holder);}
        @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key){if(visible)restart();}
        private void restart(){handler.removeCallbacks(tick);if(visible)handler.post(tick);}
        private void render(){SurfaceHolder holder=getSurfaceHolder();Canvas c=null;try{c=holder.lockCanvas();if(c!=null)renderer.drawScreen(c,c.getWidth(),c.getHeight(),Instant.now(),true);}catch(RuntimeException ignored){}finally{if(c!=null)try{holder.unlockCanvasAndPost(c);}catch(RuntimeException ignored){}}}
        @Override public void onDestroy(){visible=false;handler.removeCallbacks(tick);ClockSettings.p(ClockWallpaperService.this).unregisterOnSharedPreferenceChangeListener(this);renderer.release();super.onDestroy();}
    }
}
