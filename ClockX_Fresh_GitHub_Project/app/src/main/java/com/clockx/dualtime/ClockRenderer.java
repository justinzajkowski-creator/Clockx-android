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

/** Clean stationary artwork with a mathematically drawn calendar and separate moving hands. */
public final class ClockRenderer {
    private static final float DIM=1254f, CX=627f,CY=622f;
    private static final float AMX=246f, AMY=602f, DX=627f, DY=928f;
    private final Context context;
    private final Map<Integer,Bitmap> cache=new HashMap<>();
    private Bitmap cachedPhoto;private int photoRevision=-1;
    private static final int[] DAY={R.drawable.dial_day,R.drawable.day_date,R.drawable.day_ampm,R.drawable.day_gmt,R.drawable.day_hour,R.drawable.day_minute,R.drawable.day_seconds};
    private static final int[] NIGHT={R.drawable.dial_night,R.drawable.night_date,R.drawable.night_ampm,R.drawable.night_gmt,R.drawable.night_hour,R.drawable.night_minute,R.drawable.night_seconds};
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
    public ClockRenderer(Context c){context=c.getApplicationContext();}
    private Bitmap img(int id){Bitmap b=cache.get(id);if(b==null){BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.ARGB_8888;b=BitmapFactory.decodeResource(context.getResources(),id,opt);if(b!=null)cache.put(id,b);}return b;}
    private void layer(Canvas c,int id){Bitmap b=img(id);if(b!=null)c.drawBitmap(b,0,0,imagePaint);}
    public void drawDial(Canvas c,float left,float top,float width,Instant now,boolean showSeconds){
        ZonedDateTime local=ClockSettings.localTime(context,now),home=ClockSettings.homeTime(context,now);
        boolean night=ClockSettings.dayOrNight(context,local);
        int[] art=night?NIGHT:DAY;
        c.save();c.translate(left,top);c.scale(width/DIM,width/DIM);
        layer(c,art[0]);
        drawCalendar(c,local.getDayOfMonth());
        drawAmPm(c,local.getHour()<12);
        pivot(c,DX,DY,19f);pivotOrange(c,AMX,AMY,12f);
        // Orange GMT hand completes one revolution in 24 hours, per the reference's app behavior.
        float home24=(home.getHour()+home.getMinute()/60f+home.getSecond()/3600f)*15f;
        drawGmtHand(c,home24);
        float localH=((local.getHour()%12)+local.getMinute()/60f+local.getSecond()/3600f)*30f;
        float localM=(local.getMinute()+(local.getSecond()+local.getNano()/1e9f)/60f)*6f;
        float localS=(local.getSecond()+local.getNano()/1e9f)*6f;
        drawSilverHand(c,localH,335f,24f,night);
        drawSilverHand(c,localM,475f,17f,night);
        if(showSeconds)drawSecondsHand(c,localS);
        pivot(c,CX,CY,43f);
        c.restore();
    }


    private static Path handShape(float length,float halfWidth){
        Path h=new Path();h.moveTo(-halfWidth*.65f,35f);
        h.lineTo(-halfWidth,-length*.23f);h.lineTo(-halfWidth*.56f,-length+38f);
        h.lineTo(0,-length);h.lineTo(halfWidth*.56f,-length+38f);
        h.lineTo(halfWidth,-length*.23f);h.lineTo(halfWidth*.65f,35f);h.close();return h;
    }
    private static void drawSilverHand(Canvas c,float angle,float length,float halfWidth,boolean night){
        c.save();c.translate(CX,CY);c.rotate(angle);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Path body=handShape(length,halfWidth);
        c.save();c.translate(3,4);p.setColor(0x99030b16);c.drawPath(body,p);c.restore();
        p.setShader(new LinearGradient(-halfWidth,0,halfWidth,0,
            new int[]{0xff374c5a,0xffc5d2dd,0xffffffff,0xff8297a9,0xff344c60},null,Shader.TileMode.CLAMP));
        c.drawPath(body,p);p.setShader(null);
        Path inset=new Path();inset.moveTo(-halfWidth*.48f,-55);
        inset.lineTo(-halfWidth*.28f,-length+51);inset.lineTo(0,-length+27);
        inset.lineTo(halfWidth*.28f,-length+51);inset.lineTo(halfWidth*.48f,-55);inset.close();
        p.setColor(night?0xff91ffd0:0xffe9f0ed);c.drawPath(inset,p);
        p.setColor(0xfff7fbff);p.setStrokeWidth(1.6f);c.drawLine(0,25,0,-47,p);
        c.restore();
    }
    private static void drawGmtHand(Canvas c,float angle){
        c.save();c.translate(CX,CY);c.rotate(angle);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeWidth(9);
        p.setColor(0xffd75b0e);c.drawLine(0,32,0,-330,p);
        p.setStrokeWidth(3);p.setColor(0xffffb04c);c.drawLine(-1,30,-1,-330,p);
        Path arrow=new Path();arrow.moveTo(0,-380);arrow.lineTo(-25,-324);arrow.lineTo(25,-324);arrow.close();
        p.setColor(0xffff861d);c.drawPath(arrow,p);
        Path inset=new Path();inset.moveTo(0,-364);inset.lineTo(-14,-332);inset.lineTo(14,-332);inset.close();
        p.setColor(0xfff7f7ed);c.drawPath(inset,p);c.restore();
    }
    private static void drawSecondsHand(Canvas c,float angle){
        c.save();c.translate(CX,CY);c.rotate(angle);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xfff4f8fc);p.setStrokeWidth(3f);
        c.drawLine(0,90,0,-510,p);p.setStrokeWidth(7f);c.drawLine(0,60,0,96,p);c.restore();
    }
    // Draw every date position explicitly: no generated or retouched numerals.
    private static void drawCalendar(Canvas c,int day){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        c.save();c.translate(DX,DY);
        p.setColor(0xff050d18);c.drawCircle(0,0,227,p);
        p.setShader(new LinearGradient(-220,-220,220,220,new int[]{0xff244469,0xff102238,0xff244469},null,Shader.TileMode.CLAMP));
        c.drawCircle(0,0,223,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1f);p.setColor(0xff2b4b6d);
        for(int radius=28;radius<220;radius+=4)c.drawCircle(0,0,radius,p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(30f);
        p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.NORMAL));
        for(int date=1;date<=31;date++){
            c.save();c.rotate(date*360f/31f);
            p.setColor(0xffeff5ff);
            if((date&1)==1)c.drawText(Integer.toString(date),0,-177,p);
            else {p.setStrokeWidth(3f);c.drawLine(0,-210,0,-187,p);}
            c.restore();
        }
        c.save();c.rotate(day*360f/31f);
        Path hand=new Path();hand.moveTo(-6,24);hand.lineTo(-5,-141);hand.lineTo(0,-170);hand.lineTo(5,-141);hand.lineTo(6,24);hand.close();
        p.setColor(0xffe2e9ef);c.drawPath(hand,p);
        p.setColor(0xff6d8598);p.setStrokeWidth(2f);c.drawLine(0,20,0,-158,p);
        c.restore();c.restore();
    }
    private static void drawAmPm(Canvas c,boolean am){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        c.save();c.translate(AMX,AMY);c.rotate(am?-23f:23f);
        p.setColor(0xffff841c);p.setStrokeWidth(9f);c.drawLine(0,0,174,0,p);
        Path tip=new Path();tip.moveTo(190,0);tip.lineTo(163,-13);tip.lineTo(163,13);tip.close();
        p.setColor(0xfff5f8fa);c.drawPath(tip,p);
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

