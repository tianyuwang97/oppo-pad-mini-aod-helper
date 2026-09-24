package local.codex.panorama;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private final int bg=Color.rgb(244,247,247), ink=Color.rgb(24,46,53), accent=Color.rgb(0,105,89);
    private TextView status, result;
    private Button enable, restore;
    private Switch boot;
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView v=new TextView(this); v.setText(value);v.setTextSize(size);v.setTextColor(ink);
        v.setPadding(0,dp(8),0,dp(8)); v.setLineSpacing(dp(3),1);return v;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(28),dp(28),dp(28),dp(28));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets system=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            v.setPadding(system.left,system.top,system.right,system.bottom);return insets;
        });
        setContentView(scroll);
        if (getWindow().getDecorView().getWindowInsetsController()!=null)
            getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(8,8);
        TextView eyebrow=text("OPPO PAD MINI  /  本机控制",13);eyebrow.setTextColor(accent);content.addView(eyebrow);
        TextView title=text("让全景，停留。",34);title.setTypeface(null,Typeface.BOLD);content.addView(title);
        content.addView(text("保持原生全景息屏，随时恢复默认。",17));
        status=text("检查授权…",16);status.setTextColor(accent);content.addView(status);
        enable=button("开启全景息屏保持", true);content.addView(enable);enable.setOnClickListener(v->apply(true));
        restore=button("恢复默认关闭时间",false);content.addView(restore);restore.setOnClickListener(v->apply(false));
        result=text("",15);content.addView(result);
        boot=new Switch(this);boot.setText("开机后尝试恢复保持");boot.setTextSize(16);boot.setTextColor(ink);boot.setPadding(0,dp(24),0,dp(12));
        boot.setChecked(getSharedPreferences(AodControl.PREFS,0).getBoolean("boot",false));
        boot.setOnCheckedChangeListener((b,on)->getSharedPreferences(AodControl.PREFS,0).edit().putBoolean("boot",on).apply());content.addView(boot);
        content.addView(text("此项默认关闭。可能需要在系统中允许本应用自启动；开机恢复尚未在这台设备上实测。若未恢复，打开本应用点击开启即可。",13));
        TextView heading=text("使用说明",18);heading.setTypeface(null,Typeface.BOLD);content.addView(heading);
        content.addView(text("首次由电脑授权后，在本机点按钮即可。开启后无需让本应用保持运行。\n\n点击开启后，按电源键进入全景息屏，再观察是否持续显示。系统或系统界面进程重启后，需要重新开启。\n\n页面记录的是最近一次指令结果，不代表当前息屏开关的实时状态。",15));
        content.addView(text("适配版本：OPD2515 · ColorOS 17.0.0.102\n本应用无联网权限，不收集或上传数据。",12));
    }
    private Button button(String label, boolean primary) {
        Button b=new Button(this);b.setText(label);b.setTextSize(17);b.setAllCaps(false);b.setTextColor(primary?Color.WHITE:accent);
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(16));shape.setColor(primary?accent:Color.WHITE);if(!primary)shape.setStroke(dp(1),0xffb6cdc8);
        b.setBackground(shape);b.setBackgroundTintList(ColorStateList.valueOf(primary?accent:Color.WHITE));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.topMargin=dp(14);b.setLayoutParams(lp);return b;
    }
    @Override public void onResume() { super.onResume();refresh(); }
    private void refresh() {
        status.setText(AodControl.hasPermission(this)?"首次授权已就绪":AodControl.missingPermission(this));
        long time=getSharedPreferences(AodControl.PREFS,0).getLong("last_time",0);
        String previous=getSharedPreferences(AodControl.PREFS,0).getString("last_result","尚未通过本应用发送指令。");
        result.setText(time==0?previous:"最近操作 · "+DateFormat.getDateTimeInstance().format(new Date(time))+"\n"+previous);
    }
    private void apply(boolean enabled) {
        enable.setEnabled(false);restore.setEnabled(false);result.setText("正在调用系统…");
        new Thread(()->{
            String message;
            try { message=AodControl.apply(this,enabled); }
            catch(Exception e) { message=e.getMessage();android.util.Log.e("PanoramaHelper", "Apply failed",e); }
            String finalMessage=message;
            runOnUiThread(()->{
                enable.setEnabled(true);restore.setEnabled(true);
                if(!enabled && finalMessage.startsWith("恢复指令")) boot.setChecked(false);
                result.setText(finalMessage);
            });
        },"aod-command").start();
    }
}
