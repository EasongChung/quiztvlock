package com.quiztv.lock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.app.Activity;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 全屏答题页。
 * <p>
 * 关键行为：
 * <ul>
 *   <li>答题期间 {@link LockService} 每秒检测本页是否在前台，被切走会立刻拉回；</li>
 *   <li>返回键置空（无法退出）；HOME 键会被系统消化但桌面在本页之后，看不到；</li>
 *   <li>每答对 1 题奖励观看时长（家长可配），答错追加题数；</li>
 *   <li>本轮题目全部答完（pending 归零）→ 恢复播放，回到正常倒计时。</li>
 * </ul>
 */
public class QuizActivity extends Activity {

    /** 服务看门狗用它判断是否需要把答题页拉回前台 */
    public static volatile boolean foreground = false;

    private PrefManager prefs;
    private QuestionBank bank;
    private Question current;
    private TextView stemView, statusView;
    private Button[] opts = new Button[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        prefs = new PrefManager(this);
        bank = new QuestionBank(this);

        stemView = findViewById(R.id.stem);
        statusView = findViewById(R.id.status);
        int[] ids = {R.id.opt0, R.id.opt1, R.id.opt2, R.id.opt3};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            opts[i] = findViewById(ids[i]);
            opts[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { answer(idx); }
            });
        }
        findViewById(R.id.parentUnlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { askPin(); }
        });

        showNext();
    }

    @Override
    protected void onResume() { super.onResume(); foreground = true; }
    @Override
    protected void onPause() { super.onPause(); foreground = false; }
    @Override
    protected void onDestroy() { super.onDestroy(); foreground = false; }

    /** 拦截返回键：答题期间不允许退出 */
    @Override
    public void onBackPressed() { }

    private void showNext() {
        current = bank.nextQuestion();
        if (current == null) {
            stemView.setText(R.string.bank_empty);
            statusView.setText("");
            for (Button b : opts) { b.setText(""); b.setEnabled(false); }
            return;
        }
        stemView.setText(current.stem);
        for (int i = 0; i < 4; i++) {
            opts[i].setText(current.options[i]);
            opts[i].setEnabled(true);
        }
        updateStatus();
        opts[0].requestFocus();
    }

    private void updateStatus() {
        statusView.setText("本轮还剩 " + prefs.getPending() + " 题  ·  已答对 "
                + prefs.getCorrectInRound() + " 题");
    }

    private void answer(int idx) {
        if (current == null) return;
        if (idx == current.answer) {
            prefs.setPending(prefs.getPending() - 1);
            prefs.setCorrectInRound(prefs.getCorrectInRound() + 1);
            // 答对奖励时长，注入「冻结的剩余时长」
            prefs.setPauseRemainingMs(prefs.getPauseRemainingMs()
                    + prefs.getMinutesPerCorrect() * 60000L);
            if (prefs.getPending() <= 0) {
                Toast.makeText(this, "全部答对，可以看电视啦！", Toast.LENGTH_SHORT).show();
                finishRound();
            } else {
                Toast.makeText(this, "答对了，+" + prefs.getMinutesPerCorrect() + " 分钟",
                        Toast.LENGTH_SHORT).show();
                showNext();
            }
        } else {
            // 答错：追加题目，并把错题加入复习队列
            prefs.setPending(prefs.getPending() + prefs.getExtraPerWrong());
            bank.markWrong(current.id);
            Toast.makeText(this, "答错了，加做 " + prefs.getExtraPerWrong() + " 题",
                    Toast.LENGTH_SHORT).show();
            showNext();
        }
    }

    private void finishRound() {
        if (LockService.instance != null) LockService.instance.finishRound();
        finish();
    }

    private void askPin() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("家长解锁（输入 PIN 码）")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (et.getText().toString().equals(prefs.getPin())) {
                            finishRound();
                        } else {
                            Toast.makeText(QuizActivity.this, "PIN 码错误", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
