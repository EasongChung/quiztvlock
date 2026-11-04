package com.quiztv.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 电视端家长设置：遥控器点击按钮循环切换数值。
 * WebUI（http://电视IP:36789）提供更完整的管理界面。
 */
public class MainActivity extends Activity {

    private PrefManager prefs;
    private TextView statusView, webuiTips;
    private final Handler handler = new Handler();

    private final int[] watchOpts = {15, 30, 45, 60, 90, 120};
    private final int[] perCorrectOpts = {3, 5, 10, 15};
    private final int[] perRoundOpts = {3, 5, 10, 20};
    private final int[] extraOpts = {1, 2, 3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new PrefManager(this);

        statusView = findViewById(R.id.statusView);
        webuiTips = findViewById(R.id.webuiTips);

        bindCycle(R.id.btnWatchMinutes, "观看时长（分钟后进入答题）", watchOpts,
                new Getter() { @Override public int get() { return prefs.getWatchMinutes(); } },
                new Setter() { @Override public void set(int v) { prefs.setWatchMinutes(v); } });
        bindCycle(R.id.btnPerCorrect, "答对每题奖励（分钟）", perCorrectOpts,
                new Getter() { @Override public int get() { return prefs.getMinutesPerCorrect(); } },
                new Setter() { @Override public void set(int v) { prefs.setMinutesPerCorrect(v); } });
        bindCycle(R.id.btnPerRound, "每轮答题数量", perRoundOpts,
                new Getter() { @Override public int get() { return prefs.getQuestionsPerRound(); } },
                new Setter() { @Override public void set(int v) { prefs.setQuestionsPerRound(v); } });
        bindCycle(R.id.btnExtraWrong, "答错追加题数", extraOpts,
                new Getter() { @Override public int get() { return prefs.getExtraPerWrong(); } },
                new Setter() { @Override public void set(int v) { prefs.setExtraPerWrong(v); } });

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuard();
            }
        });

        findViewById(R.id.btnTestLock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuard();
                if (LockService.instance != null) {
                    LockService.instance.startRound(prefs.getQuestionsPerRound());
                }
            }
        });

        startGuard();
    }

    private interface Getter { int get(); }
    private interface Setter { void set(int v); }

    private void bindCycle(int btnId, final String label, final int[] opts,
                           final Getter getter, final Setter setter) {
        final Button b = findViewById(btnId);
        b.setText(label + "：" + getter.get());
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cur = getter.get();
                int idx = 0;
                for (int i = 0; i < opts.length; i++) if (opts[i] == cur) { idx = i; break; }
                int nv = opts[(idx + 1) % opts.length];
                setter.set(nv);
                b.setText(label + "：" + nv);
            }
        });
    }

    private void startGuard() {
        Intent i = new Intent(this, LockService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        handler.postDelayed(refreshTask, 2000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() { refresh(); handler.postDelayed(this, 2000); }
    };

    private void refresh() {
        boolean active = prefs.isQuizActive();
        long rem = active
                ? prefs.getPauseRemainingMs()
                : prefs.getDeadlineMs() - System.currentTimeMillis();
        rem = Math.max(0L, rem);
        statusView.setText((active ? "【答题中】" : "【观看中】")
                + " 剩余 " + (rem / 60000) + " 分钟"
                + " · 本轮待答 " + prefs.getPending() + " 题"
                + " · PIN：" + prefs.getPin());
        webuiTips.setText("家长手机管理：连同一 Wi-Fi，浏览器打开\nhttp://"
                + Util.getIp(this) + ":" + HttpAdminServer.PORT
                + "\n默认账号 admin 密码 123456（可在网页里改）");
    }
}
