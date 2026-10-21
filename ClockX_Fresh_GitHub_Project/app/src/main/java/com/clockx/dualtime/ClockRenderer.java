package com.clockx.dualtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/** Uses the two explicitly approved artwork images and independently movable layers. */
public final class ClockRenderer {
    private static final float DIM=1254f, CX=621f,CY=614f;
    private static final float AMX=273f, AMY=601f, DX=621f, DY=924f;
    private final Context context;
    private final Map<Integer,Bitmap> cache=new HashMap<>();
    private Bitmap cachedPhoto;private int photoRevision=-1;
    private static final int[] DAY={R.drawable.dial_day,R.drawable.day_date,R.drawable.day_ampm,R.drawable.day_gmt,R.drawable.day_hour,R.drawable.day_minute,R.drawable.day_seconds};
    private static final int[] NIGHT={R.drawable.dial_night,R.drawable.night_date,R.drawable.night_ampm,R.drawable.night_gmt,R.drawable.night_hour,R.drawable.night_minute,R.drawable.night_seconds};
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
    public ClockRenderer(Context c){context=c.getApplicationContext();}
    private Bitmap img(int id){Bitmap b=cache.get(id);if(b==null){BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.ARGB_8888;b=BitmapFactory.decodeResource(context.getResources(),id,opt);if(b!=null)cache.put(id,b);}return b;}
    private void layer(Canvas c,int id){Bitmap b=img(id);if(b!=null)c.drawBitmap(b,0,0,imagePaint);}
    private void rotated(Canvas c,int id,float delta,float px,float py){c.save();c.rotate(delta,px,py);layer(c,id);c.restore();}
    private static float angle(float x,float y,float cx,float cy){return (float)Math.toDegrees(Math.atan2(x-cx,cy-y));}
    private static float baseHour=angle(350,400,CX,CY),baseMinute=angle(1035,345,CX,CY),baseGMT=angle(786,871,CX,CY),baseSecond=angle(1025,975,CX,CY),baseDate=angle(437,1027,DX,DY);
    public void drawDial(Canvas c,float left,float top,float width,Instant now,boolean showSeconds){
        ZonedDateTime local=ClockSettings.localTime(context,now),home=ClockSettings.homeTime(context,now);
        boolean night=ClockSettings.dayOrNight(context,local);
        int[] art=night?NIGHT:DAY;
        c.save();c.translate(left,top);c.scale(width/DIM,width/DIM);
        layer(c,art[0]);
        // 31 evenly spaced calendar positions; 31 is at twelve o'clock.
        float dateTarget=local.getDayOfMonth()*360f/31f;
        rotated(c,art[1],dateTarget-baseDate,DX,DY);
        // Original approved reference has a fan indicator at left. AM points up, PM down.
        rotated(c,art[2],local.getHour()<12?-55f:0f,AMX,AMY);
        // Orange GMT hand completes one revolution in 24 hours, per the reference's app behavior.
        float home24=(home.getHour()+home.getMinute()/60f+home.getSecond()/3600f)*15f;
        rotated(c,art[3],home24-baseGMT,CX,CY);
        float localH=((local.getHour()%12)+local.getMinute()/60f+local.getSecond()/3600f)*30f;
        float localM=(local.getMinute()+(local.getSecond()+local.getNano()/1e9f)/60f)*6f;
        float localS=(local.getSecond()+local.getNano()/1e9f)*6f;
        rotated(c,art[4],localH-baseHour,CX,CY);
        rotated(c,art[5],localM-baseMinute,CX,CY);
        if(showSeconds)rotated(c,art[6],localS-baseSecond,CX,CY);
        pivot(c,CX,CY,29f);pivot(c,DX,DY,19f);pivotOrange(c,AMX,AMY,18f);
        c.restore();
    }
    private static void pivot(Canvas c,float x,float y,float r){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xff1a2b40);c.drawCircle(x+2,y+3,r+6,p);
        p.setShader(new LinearGradient(x-r,y-r,x+r,y+r,new int[]{0xff78818a,0xfff5f6f6,0xff89949c,0xff273a4a},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);
        p.setColor(0xff101b2b);c.drawCircle(x,y,r*.22f,p);p.setColor(0xffd7dfe6);c.drawCircle(x-2,y-2,r*.09f,p);
    }
    private static void pivotOrange(Canvas c,float x,float y,float r){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xff3b1c0d);c.drawCircle(x,y,r+4,p);p.setColor(0xffff821e);c.drawCircle(x,y,r,p);p.setColor(0xffa7b0b8);c.drawCircle(x,y,r*.43f,p);
    }
    public void drawScreen(Canvas canvas,int width,int height,Instant now,boolean live){
        canvas.drawColor(0xff03070e);
        if(ClockSettings.photoMode(context)){
            int revision=ClockSettings.p(context).getInt("photo_revision",0);
            if(photoRevision!=revision){invalidatePhoto();photoRevision=revision;}
            if(cachedPhoto==null)cachedPhoto=ClockSettings.photo(context);
            Bitmap photo=cachedPhoto;
            if(photo!=null){float factor=Math.max(width/(float)photo.getWidth(),height/(float)photo.getHeight());int srcW=Math.min(photo.getWidth(),Math.round(width/factor));int srcH=Math.min(photo.getHeight(),Math.round(height/factor));int x=(photo.getWidth()-srcW)/2,y=(photo.getHeight()-srcH)/2;
                canvas.drawBitmap(photo,new android.graphics.Rect(x,y,x+srcW,y+srcH),new RectF(0,0,width,height),imagePaint);
                Paint dim=new Paint();dim.setColor(Color.argb(Math.round(ClockSettings.photoDim(context)*2.55f),0,0,0));canvas.drawRect(0,0,width,height,dim);

            }
        }
        float size=Math.min(width*ClockSettings.size(context)/100f,height*.94f);
        float x=width*ClockSettings.x(context)/100f-size/2f;
        float y=height*ClockSettings.y(context)/100f-size/2f;
        drawDial(canvas,x,y,size,now,live);
    }
    public Bitmap makeWidget(int size){
        Bitmap result=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(result);drawDial(c,0f,0f,size,Instant.now(),true);return result;
    }
    public void invalidatePhoto(){if(cachedPhoto!=null&&!cachedPhoto.isRecycled())cachedPhoto.recycle();cachedPhoto=null;}
    public void release(){invalidatePhoto();for(Bitmap b:cache.values())if(!b.isRecycled())b.recycle();cache.clear();}
}
