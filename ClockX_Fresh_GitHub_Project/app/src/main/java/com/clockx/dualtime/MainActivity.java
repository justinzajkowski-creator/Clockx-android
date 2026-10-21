package com.clockx.dualtime;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** No account, no internet, no dangerous overlay permissions. Photo selection uses SAF. */
public final class MainActivity extends Activity {
    private static final int NAVY=0xff071326,PANEL=0xff10243b,WHITE=0xfff1f5fa,SOFT=0xffb7c7dc,ORANGE=0xfff8a455;
    private static final int PICK_PHOTO=17;
    private AutoCompleteTextView localZone,homeZone;
    private CheckBox follow, smooth, photo;
    private Spinner lume;
    private SeekBar size,xpos,ypos,dim;
    private TextView sizeText,xText,yText,dimText,localAdjust,homeAdjust,status;
    private int localOffset,homeOffset;
    private ClockRenderer painter;
    private Preview preview;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){@Override public void run(){if(preview!=null){preview.invalidate();if(status!=null){java.time.ZonedDateTime local=ClockSettings.localTime(MainActivity.this,Instant.now()),home=ClockSettings.homeTime(MainActivity.this,Instant.now());status.setText("Local "+local.toLocalTime().withNano(0)+"  ·  GMT "+home.toLocalTime().withNano(0));}handler.postDelayed(this,250);}}};
    @Override public void onCreate(Bundle state){super.onCreate(state);painter=new ClockRenderer(this);localOffset=ClockSettings.localOffset(this);homeOffset=ClockSettings.homeOffset(this);setContentView(makeUi());}
    @Override public void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override public void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    @Override public void onDestroy(){handler.removeCallbacks(ticker);painter.release();super.onDestroy();}

    private View makeUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(NAVY);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(17),dp(22),dp(17),dp(40));scroll.addView(root);
        TextView title=txt("CLOCKX  ·  DUAL TIME",22,WHITE,true);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView tagline=txt("APPROVED BLUE DIAL  •  DAY + LUME",11,ORANGE,true);tagline.setGravity(Gravity.CENTER);root.addView(tagline,space(4));
        preview=new Preview();root.addView(preview,new LinearLayout.LayoutParams(-1,dp(320)));
        status=txt("",12,SOFT,false);status.setGravity(Gravity.CENTER);root.addView(status,space(3));
        root.addView(button("① ADD ROUND HOME-SCREEN WIDGET",true,v->{if(save())pinWidget();}),space(14));
        root.addView(txt("A transparent, resizable watch over your current wallpaper. Tap the widget later to edit its settings.",12,SOFT,false),space(4));
        root.addView(button("② APPLY FULL-SCREEN / LOCK-SCREEN CLOCK",true,v->{if(save())applyWallpaper();}),space(14));
        root.addView(txt("Samsung's wallpaper picker lets you select Lock screen, Home screen, or both. The clock is live wallpaper behind the unlock/notification UI.",12,SOFT,false),space(4));
        root.addView(heading("WALLPAPER MODE"),space(23));
        LinearLayout panel=panel();root.addView(panel,space(8));
        photo=new CheckBox(this);photo.setText("Show the clock over my own photo");photo.setTextColor(WHITE);photo.setChecked(ClockSettings.photoMode(this));panel.addView(photo);
        panel.addView(txt("Off: immersive blue watch against OLED black. On: your chosen image fills the screen, with the round watch layered over it.",12,SOFT,false));
        panel.addView(button("CHOOSE PHOTO FROM PHONE",false,v->choosePhoto()),space(8));
        panel.addView(txt("PHOTO DARKENING",12,WHITE,true),space(14));dimText=txt("",11,SOFT,false);panel.addView(dimText);
        dim=seek(panel,0,75,ClockSettings.photoDim(this),n->dimText.setText(n+"% darker"));
        root.addView(heading("WATCH PLACEMENT"),space(22));
        LinearLayout layoutPanel=panel();root.addView(layoutPanel,space(8));
        layoutPanel.addView(txt("SIZE",12,WHITE,true));sizeText=txt("",12,SOFT,false);layoutPanel.addView(sizeText);
        size=seek(layoutPanel,45,100,ClockSettings.size(this),n->sizeText.setText(n+"% of screen width"));
        layoutPanel.addView(txt("HORIZONTAL",12,WHITE,true),space(12));xText=txt("",12,SOFT,false);layoutPanel.addView(xText);
        xpos=seek(layoutPanel,20,80,ClockSettings.x(this),n->xText.setText(n+"% across screen"));
        layoutPanel.addView(txt("VERTICAL",12,WHITE,true),space(12));yText=txt("",12,SOFT,false);layoutPanel.addView(yText);
        ypos=seek(layoutPanel,15,85,ClockSettings.y(this),n->yText.setText(n+"% down screen"));
        root.addView(heading("TIME ZONES"),space(22));
        LinearLayout times=panel();root.addView(times,space(8));
        follow=new CheckBox(this);follow.setText("Local time follows phone (recommended)");follow.setTextColor(WHITE);follow.setChecked(ClockSettings.followPhone(this));times.addView(follow);
        List<String> zones=new ArrayList<>(ZoneId.getAvailableZoneIds());Collections.sort(zones);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,zones);
        times.addView(txt("LOCAL TIME ZONE (main silver hands)",12,WHITE,true),space(11));
        localZone=cityField(adapter,ClockSettings.p(this).getString("local","America/Chicago"));times.addView(localZone);
        times.addView(txt("HOME / GMT TIME ZONE (orange arrow)",12,WHITE,true),space(12));
        homeZone=cityField(adapter,ClockSettings.homeZone(this));times.addView(homeZone);
        times.addView(txt("Corrections are optional. Leave at zero for real timezone-based time.",12,SOFT,false),space(10));
        localAdjust=txt("",12,WHITE,true);times.addView(adjustRow("Local correction",true),space(8));
        homeAdjust=txt("",12,WHITE,true);times.addView(adjustRow("GMT correction",false),space(8));
        updateAdjust();
        root.addView(heading("LIGHTING & ANIMATION"),space(22));
        LinearLayout effects=panel();root.addView(effects,space(8));
        effects.addView(txt("LUME MODE",12,WHITE,true));
        lume=new Spinner(this);ArrayAdapter<String> list=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Automatic: 6 PM–6 AM local","Always night / lume","Always daytime"});lume.setAdapter(list);lume.setSelection(ClockSettings.lumeMode(this));effects.addView(lume);
        smooth=new CheckBox(this);smooth.setText("Sweep seconds smoothly on live wallpaper");smooth.setTextColor(WHITE);smooth.setChecked(ClockSettings.smooth(this));effects.addView(smooth,space(8));
        effects.addView(txt("Home-screen widgets are snapshots: Android may delay updates. Continuous sweeping animation works in live wallpaper mode.",12,SOFT,false));
        root.addView(button("SAVE ALL SETTINGS",false,v->{if(save())toast("Settings saved");}),space(22));
        root.addView(txt("Tip: Samsung Settings > Wallpaper and style > Lock screen lets you resize, reposition, or hide its built-in clock if it overlaps. The wallpaper does not control Always On Display.",12,SOFT,false),space(13));
        return scroll;
    }
    private boolean save(){
        try{
            String home=homeZone.getText().toString().trim(),local=localZone.getText().toString().trim();ClockSettings.checkZone(home);if(!follow.isChecked())ClockSettings.checkZone(local);else if(local.isEmpty())local="America/Chicago";
            ClockSettings.p(this).edit().putString("home",home).putString("local",local).putBoolean("follow",follow.isChecked())
            .putInt("home_shift",homeOffset).putInt("local_shift",localOffset).putInt("lume",lume.getSelectedItemPosition())
            .putBoolean("smooth",smooth.isChecked()).putBoolean("photo",photo.isChecked()).putInt("size",size.getProgress()+45)
            .putInt("x",xpos.getProgress()+20).putInt("y",ypos.getProgress()+15).putInt("dim",dim.getProgress()).apply();
            ClockWidgetProvider.updateAll(this);preview.invalidate();return true;
        }catch(Exception ex){toast("Please choose a valid time zone, e.g. America/Chicago");return false;}
    }
    private void pinWidget(){AppWidgetManager manager=AppWidgetManager.getInstance(this);ComponentName name=new ComponentName(this,ClockWidgetProvider.class);if(android.os.Build.VERSION.SDK_INT>=26&&manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(name,null,null);else toast("Long-press empty Home screen > Widgets > ClockX Dual Time");}
    private void applyWallpaper(){try{
        Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,ClockWallpaperService.class));startActivity(i);
    }catch(Exception ex){try{Intent i=new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);startActivity(i);}catch(Exception e){toast("Open Settings > Wallpapers > Live wallpapers > ClockX");}}}
    private void choosePhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_PHOTO);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_PHOTO||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(InputStream input=getContentResolver().openInputStream(data.getData())){
            File file=new File(getFilesDir(),"wallpaper_photo.jpg");
            // Copy a resized JPEG to avoid huge photo decode overhead while rendering.
            byte[] bytes=new byte[8192];File tmp=new File(getCacheDir(),"picked_photo");
            try(OutputStream os=new FileOutputStream(tmp)){int n;while((n=input.read(bytes))!=-1)os.write(bytes,0,n);}
            android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(tmp.getAbsolutePath(),options);
            options.inJustDecodeBounds=false;options.inSampleSize=Math.max(1,Integer.highestOneBit(Math.max(options.outWidth,options.outHeight)/1800));
            android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeFile(tmp.getAbsolutePath(),options);
            if(bitmap==null)throw new IllegalArgumentException("Unsupported picture");
            try(OutputStream o=new FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,88,o);}bitmap.recycle();tmp.delete();
            photo.setChecked(true);ClockSettings.p(this).edit().putBoolean("photo",true).putInt("photo_revision",ClockSettings.p(this).getInt("photo_revision",0)+1).apply();
            painter.invalidatePhoto();preview.invalidate();toast("Photo loaded. Tap Apply to see it on the lock screen.");
        }catch(Exception e){toast("Could not open this photo; try JPG or PNG.");}
    }
    private void updateAdjust(){if(localAdjust!=null)localAdjust.setText("Local: "+format(localOffset));if(homeAdjust!=null)homeAdjust.setText("GMT: "+format(homeOffset));}
    private static String format(int n){return (n>0?"+":"")+n+" minutes";}
    private LinearLayout adjustRow(String name,boolean local){LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.addView(txt(name,11,SOFT,false));LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);group.addView(bar);bar.addView(button("−60",false,v->shift(local,-60)),new LinearLayout.LayoutParams(0,dp(42),1));bar.addView(button("−15",false,v->shift(local,-15)),new LinearLayout.LayoutParams(0,dp(42),1));bar.addView(local?localAdjust:homeAdjust,new LinearLayout.LayoutParams(0,-2,2));bar.addView(button("+15",false,v->shift(local,15)),new LinearLayout.LayoutParams(0,dp(42),1));bar.addView(button("+60",false,v->shift(local,60)),new LinearLayout.LayoutParams(0,dp(42),1));return group;}
    private void shift(boolean local,int minutes){if(local)localOffset=Math.max(-720,Math.min(720,localOffset+minutes));else homeOffset=Math.max(-720,Math.min(720,homeOffset+minutes));updateAdjust();}
    private AutoCompleteTextView cityField(ArrayAdapter<String> a,String initial){AutoCompleteTextView f=new AutoCompleteTextView(this);f.setTextColor(WHITE);f.setHintTextColor(SOFT);f.setTextSize(14);f.setSingleLine(true);f.setAdapter(a);f.setThreshold(1);f.setText(initial);return f;}
    private interface Changed {void accept(int n);}
    private SeekBar seek(LinearLayout box,int min,int max,int value,Changed change){SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(Math.max(0,Math.min(max-min,value-min)));box.addView(s);change.accept(s.getProgress()+min);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar x,int p,boolean user){change.accept(p+min);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});return s;}
    private TextView heading(String s){return txt(s,15,WHITE,true);}
    private LinearLayout panel(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(13),dp(11),dp(13),dp(13));GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(15));b.setBackground(bg);return b;}
    private Button button(String title,boolean major,View.OnClickListener listener){Button b=new Button(this);b.setText(title);b.setTextSize(major?12:11);b.setAllCaps(false);b.setTextColor(WHITE);GradientDrawable d=new GradientDrawable();d.setColor(major?0xff265072:0xff1a3752);d.setCornerRadius(dp(11));b.setBackground(d);b.setOnClickListener(listener);return b;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLineSpacing(dp(2),1f);return t;}
    private LinearLayout.LayoutParams space(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    final class Preview extends View{
        Preview(){super(MainActivity.this);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();if(w==0)return;
            // Show the full circular dial with transparent corners so the selected art is legible.
            float size=Math.min(w,h)*.94f; c.drawColor(NAVY);
            painter.drawDial(c,(w-size)/2,(h-size)/2,size,Instant.now(),true);

        }
    }
}
