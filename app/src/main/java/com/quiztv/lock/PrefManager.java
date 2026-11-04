package com.quiztv.lock;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 持久化管理：家长设置 + 运行时状态（断电不丢失）。
 *
 * 命名约定：setXXX 直接 apply，调用方不需关心同步。
 */
public class PrefManager {
    private static final String PREFS = "quiztv";
    private final SharedPreferences sp;

    public PrefManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- 家长设置 ----------
    public String getPin() { return sp.getString("pin", "1234"); }
    public void setPin(String v) { sp.edit().putString("pin", v).apply(); }

    /** 观看多长时间后进入答题（分钟） */
    public int getWatchMinutes() { return sp.getInt("watchMinutes", 30); }
    public void setWatchMinutes(int v) { sp.edit().putInt("watchMinutes", v).apply(); }

    /** 每答对 1 题奖励的观看分钟数 */
    public int getMinutesPerCorrect() { return sp.getInt("minutesPerCorrect", 5); }
    public void setMinutesPerCorrect(int v) { sp.edit().putInt("minutesPerCorrect", v).apply(); }

    /** 每轮答题数量 */
    public int getQuestionsPerRound() { return sp.getInt("questionsPerRound", 5); }
    public void setQuestionsPerRound(int v) { sp.edit().putInt("questionsPerRound", v).apply(); }

    /** 答错 1 题追加的题数 */
    public int getExtraPerWrong() { return sp.getInt("extraPerWrong", 1); }
    public void setExtraPerWrong(int v) { sp.edit().putInt("extraPerWrong", v).apply(); }

    // ---------- 题库开关（WebUI 可切换） ----------
    public boolean isBankEnabled(String subject) {
        if ("语文".equals(subject)) return sp.getBoolean("bankYuwen", true);
        if ("数学".equals(subject)) return sp.getBoolean("bankMath", true);
        if ("英语".equals(subject)) return sp.getBoolean("bankEnglish", true);
        return true;
    }
    public void setBankEnabled(String subject, boolean v) {
        SharedPreferences.Editor e = sp.edit();
        if ("语文".equals(subject)) e.putBoolean("bankYuwen", v);
        else if ("数学".equals(subject)) e.putBoolean("bankMath", v);
        else if ("英语".equals(subject)) e.putBoolean("bankEnglish", v);
        else return;
        e.apply();
    }

    // ---------- WebUI 账号 ----------
    public String getWebUser() { return sp.getString("webUser", "admin"); }
    public void setWebUser(String v) { sp.edit().putString("webUser", v).apply(); }
    public String getWebPass() { return sp.getString("webPass", "123456"); }
    public void setWebPass(String v) { sp.edit().putString("webPass", v).apply(); }

    // ---------- 题库持久化 ----------
    public String getQuestionsJson() { return sp.getString("questionsJson", ""); }
    public void setQuestionsJson(String v) { sp.edit().putString("questionsJson", v).apply(); }
    public boolean isSeeded() { return sp.getBoolean("seeded", false); }
    public void setSeeded(boolean v) { sp.edit().putBoolean("seeded", v).apply(); }
    public String getReviewIds() { return sp.getString("reviewIds", ""); }
    public void setReviewIds(String v) { sp.edit().putString("reviewIds", v).apply(); }

    // ---------- 运行时状态 ----------
    /** 当前是否处于「必须答完题」的锁屏轮次 */
    public boolean isQuizActive() { return sp.getBoolean("quizActive", false); }
    public void setQuizActive(boolean v) { sp.edit().putBoolean("quizActive", v).apply(); }

    /** 本轮剩余题目数（答错会增加） */
    public int getPending() { return sp.getInt("pending", 0); }
    public void setPending(int v) { sp.edit().putInt("pending", v).apply(); }

    /** 本轮已答对数 */
    public int getCorrectInRound() { return sp.getInt("correctInRound", 0); }
    public void setCorrectInRound(int v) { sp.edit().putInt("correctInRound", v).apply(); }

    /** 答题期间冻结的剩余观看毫秒数 */
    public long getPauseRemainingMs() { return sp.getLong("pauseRemaining", -1L); }
    public void setPauseRemainingMs(long v) { sp.edit().putLong("pauseRemaining", v).apply(); }

    /** 观看倒计时归零的时刻（epoch 毫秒） */
    public long getDeadlineMs() { return sp.getLong("deadline", 0L); }
    public void setDeadlineMs(long v) { sp.edit().putLong("deadline", v).apply(); }
}
