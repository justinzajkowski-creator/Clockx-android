package com.clockx.dualtime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.RemoteViews;

/** Snapshot RemoteViews widget. Android may defer the requested one-minute alarms in Doze. */
public class ClockWidgetProvider extends AppWidgetProvider {
    public static final String REFRESH="com.clockx.dualtime.REFRESH";
    @Override public void onEnabled(Context c){updateAll(c);}
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)updateOne(c,manager,id);schedule(c);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);String a=i.getAction();if(REFRESH.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_DATE_CHANGED.equals(a)||Intent.ACTION_BOOT_COMPLETED.equals(a))updateAll(c);}
    @Override public void onDisabled(Context c){AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(alarms!=null)alarms.cancel(alarmIntent(c));}
    public static void updateAll(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,ClockWidgetProvider.class));if(ids.length==0)return;for(int id:ids)updateOne(c,manager,id);schedule(c);}
    private static void updateOne(Context c,AppWidgetManager manager,int id){
        ClockRenderer renderer=new ClockRenderer(c);Bitmap bmp=renderer.makeWidget(320);
        RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.widget);views.setImageViewBitmap(R.id.clock_image,bmp);
        Intent open=new Intent(c,MainActivity.class);PendingIntent pending=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.clock_image,pending);
        manager.updateAppWidget(id,views);renderer.release();
    }
    private static PendingIntent alarmIntent(Context c){Intent i=new Intent(c,ClockWidgetProvider.class).setAction(REFRESH);return PendingIntent.getBroadcast(c,1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static void schedule(Context c){AlarmManager alarm=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(alarm==null)return;long now=System.currentTimeMillis(),next=(now/60000L+1)*60000L+1500L;
        if(Build.VERSION.SDK_INT>=23)alarm.setAndAllowWhileIdle(AlarmManager.RTC,next,alarmIntent(c));else alarm.set(AlarmManager.RTC,next,alarmIntent(c));
    }
}
