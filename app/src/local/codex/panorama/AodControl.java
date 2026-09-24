package local.codex.panorama;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class AodControl {
    static final String PREFS = "aod_helper";
    static boolean hasPermission(Context c) {
        return missingPermission(c).isEmpty();
    }
    static String missingPermission(Context c) {
        if (c.checkSelfPermission("android.permission.DUMP") != PackageManager.PERMISSION_GRANTED)
            return "需要首次电脑授权（系统诊断权限）";
        if (c.checkSelfPermission("android.permission.PACKAGE_USAGE_STATS") != PackageManager.PERMISSION_GRANTED)
            return "授权尚未完成：缺少使用情况访问权限";
        android.app.AppOpsManager ops = c.getSystemService(android.app.AppOpsManager.class);
        int mode = ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), c.getPackageName());
        if (mode != android.app.AppOpsManager.MODE_ALLOWED && mode != android.app.AppOpsManager.MODE_DEFAULT)
            return "使用情况访问已关闭，请在系统设置中允许本应用访问";
        return "";
    }
    static String apply(Context c, boolean enabled) throws Exception {
        String missing = missingPermission(c);
        if (!missing.isEmpty()) throw new Exception(missing);
        if (!"OPD2515".equals(Build.MODEL)) throw new Exception("当前工具仅适用于 OPPO Pad Mini OPD2515。");
        if (enabled && !"OPD2515_17.0.0.102(CN01)".equals(Build.DISPLAY))
            throw new Exception("此系统版本尚未验证，请先检查兼容性。恢复默认功能仍可使用。\n" + Build.DISPLAY);
        String output = run(new String[]{"/system/bin/dumpsys", "activity", "service",
                "com.android.systemui/.keyguard.KeyguardService", "debug", "keep_aod_on", enabled ? "1" : "0"});
        if (output.contains("Permission Denial") || output.contains("SecurityException") || output.contains("Exception")
                || !output.contains("SERVICE com.android.systemui/.keyguard.KeyguardService"))
            throw new Exception("系统未接受调用：\n" + output);
        String message = enabled ? "保持指令已发送。请锁屏后观察是否持续显示。" : "恢复指令已发送。请重新锁屏观察默认超时。";
        c.getSharedPreferences(PREFS, 0).edit().putString("last_result", message)
                .putLong("last_time", System.currentTimeMillis()).apply();
        Log.i("PanoramaHelper", "apply=" + enabled + " uid=" + android.os.Process.myUid() + " result=" + output.replace('\n',' '));
        return message;
    }
    private static String run(String[] args) throws Exception {
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream stream = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int n;
                while ((n=stream.read(buffer)) >= 0) {
                    if (bytes.size() < 32000) bytes.write(buffer,0,Math.min(n,32000-bytes.size()));
                }
            } catch (Exception ignored) { }
        }, "aod-command-output");
        reader.start();
        if (!process.waitFor(6, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            reader.join(1000);
            throw new Exception("调用超时，请稍后再试。");
        }
        reader.join(1000);
        String output = bytes.toString(StandardCharsets.UTF_8.name());
        if (process.exitValue() != 0) throw new Exception("调用失败：\n" + output);
        return output;
    }
}
