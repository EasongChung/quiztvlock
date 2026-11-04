package com.quiztv.lock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * 强制黑屏休息页。
 * <p>连续观看达到「休息触发时长」后由 LockService 拉起：纯黑画面、暂停播放，
 * 到时间自动恢复可看。期间返回键无效，看门狗保证它在前台。
 */
public class RestActivity extends Activity {

    public static volatile boolean foreground = false;

    private PrefManager prefs;
    private TextView restText;
    private final Handler handler = new Handler();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long rem = prefs.getRestDeadlineMs() - System.currentTimeMillis();
            if (rem <= 0) {
                finish();
                return;
            }
            long totalSec = rem / 1000;
            long min = totalSec / 60, sec = totalSec % 60;
            restText.setText("休息一下，保护眼睛\n\n"
                    + String.format("%02d:%02d", min, sec) + " 后可以继续观看");
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);
        prefs = new PrefManager(this);
        restText = findViewById(R.id.restText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        foreground = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        foreground = false;
        super.onDestroy();
    }

    /** 休息没到时间，返回键无效 */
    @Override
    public void onBackPressed() { }
}
