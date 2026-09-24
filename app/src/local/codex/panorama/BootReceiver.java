package local.codex.panorama;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            !context.getSharedPreferences(AodControl.PREFS,0).getBoolean("boot", false)) return;
        PendingResult result = goAsync();
        new Thread(() -> {
            try { AodControl.apply(context, true); }
            catch (Exception e) {
                context.getSharedPreferences(AodControl.PREFS,0).edit()
                    .putString("last_result", "开机恢复未完成："+e.getMessage()).putLong("last_time",System.currentTimeMillis()).apply();
                Log.w("PanoramaHelper", "Boot apply failed", e);
            } finally { result.finish(); }
        }, "aod-boot-once").start();
    }
}
