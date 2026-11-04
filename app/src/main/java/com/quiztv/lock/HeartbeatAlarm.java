package com.quiztv.lock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * 心跳闹钟管理：服务存活时每 60 秒一次自检脉冲。
 * <p>AlarmManager 闹钟在 App 被杀后依然保留（重启才清除），
 * 配合 RECEIVE_BOOT_COMPLETED 补上开机后的重新注册。
 */
public class HeartbeatAlarm {

    private static final int REQ = 9001;

    public static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, HeartbeatReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60_000L, 60_000L, pi);
    }

    /** 闹钟是否已注册（PendingIntent 存在即视为已注册） */
    public static boolean scheduled(Context ctx) {
        Intent i = new Intent(ctx, HeartbeatReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }
}
