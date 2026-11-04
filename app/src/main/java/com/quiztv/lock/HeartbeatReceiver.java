package com.quiztv.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * AlarmManager 心跳：每分钟检查前台服务是否还活着，
 * 若被电视系统内存清理杀掉则自动拉起（targetSdk 28 允许后台拉起服务）。
 */
public class HeartbeatReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (LockService.instance != null && HeartbeatAlarm.scheduled(ctx)) return;
        Intent i = new Intent(ctx, LockService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }
}
