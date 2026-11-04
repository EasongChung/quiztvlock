package com.quiztv.lock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * 守护服务：开机自启后常驻
 * <ul>
 *   <li>倒计时：到点拉起答题页；</li>
 *   <li>看门狗：答题期间被切走 → 立即拉回；</li>
 *   <li>媒体键：答题期间反复发送 MEDIA_PAUSE 暂停视频，答完发 MEDIA_PLAY 恢复；</li>
 *   <li>开机/重启后读取持久化状态：上轮未答完 → 直接进入答题。</li>
 * </ul>
 */
public class LockService extends android.app.Service {

    public static LockService instance;

    private static final int NOTIF_ID = 1001;
    private static final String CHANNEL_ID = "quiztv_service";

    private PrefManager prefs;
    private QuestionBank bank;
    private HttpAdminServer server;
    private AudioManager am;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long lastPauseSent = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = new PrefManager(this);
        bank = new QuestionBank(this);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);

        // 首次运行：按家长设置的观看时长开始计时
        if (prefs.getDeadlineMs() == 0L) {
            resetTimer(prefs.getWatchMinutes());
        }

        startForegroundCompat();

        try {
            server = new HttpAdminServer(this, prefs, bank);
            server.start();
        } catch (Exception e) {
            Toast.makeText(this, "WebUI 启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        handler.postDelayed(tick, 1000L);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                checkState();
            } catch (Throwable t) {
                // 任何异常都不能让看门狗停转
            }
            handler.postDelayed(this, 1000L);
        }
    };

    private void checkState() {
        if (prefs.isQuizActive()) {
            // 答题页必须在前台，被切走就拉回
            if (!QuizActivity.foreground) {
                Intent i = new Intent(this, QuizActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
            // 答题期间持续暂停播放（MEDIA_PAUSE 是暂停而非播放/暂停切换，重复发安全）
            if (SystemClock.uptimeMillis() - lastPauseSent > 4000L) {
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
                lastPauseSent = SystemClock.uptimeMillis();
            }
        } else {
            long rem = prefs.getDeadlineMs() - System.currentTimeMillis();
            if (rem <= 0) {
                // 观看时长用完：拉起答题，奖励时长从 0 计起
                startRound(prefs.getQuestionsPerRound());
            }
        }
    }

    /** 开始一轮答题：冻结当前剩余观看时长 */
    public void startRound(int count) {
        if (!prefs.isQuizActive()) {
            long rem = prefs.getDeadlineMs() - System.currentTimeMillis();
            prefs.setPauseRemainingMs(rem > 0 ? rem : 0L);
        }
        prefs.setQuizActive(true);
        prefs.setPending(count);
        prefs.setCorrectInRound(0);
        Intent i = new Intent(this, QuizActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    /** 本轮答完：奖励时长生效，恢复播放 */
    public void finishRound() {
        prefs.setQuizActive(false);
        long rem = prefs.getPauseRemainingMs();
        prefs.setDeadlineMs(System.currentTimeMillis() + (rem > 0 ? rem : 0L));
        prefs.setPauseRemainingMs(-1L);
        QuizActivity.foreground = false;
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    /** 家长解锁：不改变时长储备，直接结束本轮 */
    public void parentUnlock() {
        finishRound();
    }

    /** 重置观看时长（WebUI / 设置页可调） */
    public void resetTimer(int minutes) {
        prefs.setQuizActive(false);
        prefs.setPauseRemainingMs(-1L);
        prefs.setDeadlineMs(System.currentTimeMillis() + minutes * 60000L);
    }

    private void sendMediaKey(int code) {
        try {
            long now = SystemClock.uptimeMillis();
            am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
            am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
        } catch (Throwable ignore) {
        }
    }

    private void startForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "答题锁守护",
                        NotificationManager.IMPORTANCE_LOW);
                getSystemService(NotificationManager.class).createNotificationChannel(ch);
            }
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CHANNEL_ID);
            } else {
                b = new Notification.Builder(this);
            }
            Notification n = b.setContentTitle("答题电视锁")
                    .setContentText("守护运行中 · WebUI http://" + Util.getIp(this)
                            + ":" + HttpAdminServer.PORT)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setOngoing(true)
                    .build();
            startForeground(NOTIF_ID, n);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (server != null) server.stop();
        instance = null;
        super.onDestroy();
    }
}
