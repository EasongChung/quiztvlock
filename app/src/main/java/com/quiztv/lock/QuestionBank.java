package com.quiztv.lock;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 题库：启动时若为空则播种小学二年级题库（语文拼音 / 数学 / 英语单词），
 * 之后家长在 WebUI 增删的题目与内置题一起持久化。
 *
 * 全部题型均为 4 选 1 选择题（含英语），适配遥控器操作。
 */
public class QuestionBank {
    private static final String SUBJECTS[] = {"语文", "数学", "英语"};

    private final PrefManager prefs;
    private final List<Question> questions = new ArrayList<>();
    private final List<String> review = new ArrayList<>(); // 答错题目 id，优先复习
    private final Random rnd = new Random();
    private int seq = 0;

    public QuestionBank(Context ctx) {
        this.prefs = new PrefManager(ctx);
        load();
    }

    private void load() {
        // 复习队列
        String r = prefs.getReviewIds();
        if (r != null && !r.isEmpty()) {
            for (String s : r.split(",")) if (!s.isEmpty()) review.add(s);
        }
        String json = prefs.getQuestionsJson();
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    JSONArray opts = o.getJSONArray("options");
                    String[] os = new String[opts.length()];
                    for (int k = 0; k < opts.length(); k++) os[k] = opts.getString(k);
                    questions.add(new Question(
                            o.getString("id"), o.getString("stem"), os,
                            o.getInt("answer"), o.optString("subject", "语文")));
                }
            } catch (Exception e) {
                questions.clear();
            }
        }
        if (questions.isEmpty()) {
            seed();
        }
        for (Question q : questions) {
            try {
                int n = Integer.parseInt(q.id.substring(1));
                if (n > seq) seq = n;
            } catch (Exception ignore) { }
        }
    }

    /** 播种：语文拼音 30 题、英语单词 15 题、数学 36 题（随机生成） */
    private void seed() {
        String[][] pinyin = {
                {"的", "de"}, {"我", "wǒ"}, {"是", "shì"}, {"上", "shàng"}, {"下", "xià"},
                {"多", "duō"}, {"少", "shǎo"}, {"见", "jiàn"}, {"白", "bái"}, {"田", "tián"},
                {"电", "diàn"}, {"也", "yě"}, {"长", "cháng"}, {"出", "chū"}, {"飞", "fēi"},
                {"马", "mǎ"}, {"鸟", "niǎo"}, {"云", "yún"}, {"公", "gōng"}, {"车", "chē"},
                {"请", "qǐng"}, {"串", "chuàn"}, {"丢", "diū"}, {"踢", "tī"}, {"鼓", "gǔ"},
                {"游", "yóu"}, {"暖", "nuǎn"}, {"照", "zhào"}, {"满", "mǎn"}, {"穿", "chuān"}
        };
        List<String> pool = new ArrayList<>();
        for (String[] p : pinyin) pool.add(p[1]);
        for (String[] p : pinyin) {
            List<String> opts = new ArrayList<>();
            opts.add(p[1]);
            while (opts.size() < 4) {
                String cand = pool.get(rnd.nextInt(pool.size()));
                if (!opts.contains(cand)) opts.add(cand);
            }
            Collections.shuffle(opts);
            addInternal("「" + p[0] + "」的拼音是？", opts.toArray(new String[0]),
                    opts.indexOf(p[1]), "语文", false);
        }

        String[][] en = {
                {"cat", "猫"}, {"dog", "狗"}, {"book", "书"}, {"pen", "笔"}, {"sun", "太阳"},
                {"moon", "月亮"}, {"water", "水"}, {"apple", "苹果"}, {"milk", "牛奶"},
                {"school", "学校"}, {"teacher", "老师"}, {"red", "红色"}, {"blue", "蓝色"},
                {"big", "大的"}, {"small", "小的"}
        };
        List<String> zhPool = new ArrayList<>();
        for (String[] e : en) zhPool.add(e[1]);
        for (String[] e : en) {
            List<String> opts = new ArrayList<>();
            opts.add(e[1]);
            while (opts.size() < 4) {
                String cand = zhPool.get(rnd.nextInt(zhPool.size()));
                if (!opts.contains(cand)) opts.add(cand);
            }
            Collections.shuffle(opts);
            addInternal("「" + e[0] + "」的中文意思是？", opts.toArray(new String[0]),
                    opts.indexOf(e[1]), "英语", false);
        }

        // 数学：10 以内加法、两位数减一位数、乘法口诀
        for (int i = 0; i < 12; i++) {
            int a = 1 + rnd.nextInt(40), b = 1 + rnd.nextInt(9);
            int c = a + b;
            String[] o = intOptions(c);
            addInternal(a + " + " + b + " = ?", o, indexOf(o, c), "数学", false);
        }
        for (int i = 0; i < 12; i++) {
            int a = 20 + rnd.nextInt(70), b = 1 + rnd.nextInt(19);
            int c = a - b;
            String[] o = intOptions(c);
            addInternal(a + " − " + b + " = ?", o, indexOf(o, c), "数学", false);
        }
        for (int i = 0; i < 12; i++) {
            int a = 2 + rnd.nextInt(8), b = 2 + rnd.nextInt(8);
            int c = a * b;
            String[] o = intOptions(c);
            addInternal(a + " × " + b + " = ?", o, indexOf(o, c), "数学", false);
        }

        save();
        prefs.setSeeded(true);
    }

    private String[] intOptions(int correct) {
        Set<Integer> set = new HashSet<>();
        set.add(correct);
        while (set.size() < 4) {
            int d = correct + (rnd.nextInt(11) - 5) * (rnd.nextBoolean() ? 1 : -1);
            if (d >= 0 && d != correct) set.add(d);
        }
        List<Integer> l = new ArrayList<>(set);
        Collections.shuffle(l);
        String[] r = new String[l.size()];
        for (int i = 0; i < l.size(); i++) r[i] = String.valueOf(l.get(i));
        return r;
    }

    private int indexOf(String[] arr, int val) {
        for (int i = 0; i < arr.length; i++) {
            if (Integer.parseInt(arr[i]) == val) return i;
        }
        return 0;
    }

    private void addInternal(String stem, String[] options, int answer, String subject, boolean persist) {
        Question q = new Question("q" + (++seq), stem, options, answer, subject);
        questions.add(q);
        if (persist) save();
    }

    private synchronized void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Question q : questions) {
                JSONObject o = new JSONObject();
                o.put("id", q.id);
                o.put("stem", q.stem);
                o.put("options", new JSONArray(Arrays.asList(q.options)));
                o.put("answer", q.answer);
                o.put("subject", q.subject);
                arr.put(o);
            }
            prefs.setQuestionsJson(arr.toString());
        } catch (Exception ignore) { }
    }

    /** 取下一题：禁用的题库不出现；有错题时 40% 概率优先复习 */
    public synchronized Question nextQuestion() {
        List<Question> usable = new ArrayList<>();
        for (Question q : questions) {
            if (prefs.isBankEnabled(q.subject)) usable.add(q);
        }
        if (usable.isEmpty()) return null;
        if (!review.isEmpty() && rnd.nextInt(10) < 4) {
            String rid = review.get(rnd.nextInt(review.size()));
            for (Question q : usable) {
                if (q.id.equals(rid)) return q;
            }
        }
        return usable.get(rnd.nextInt(usable.size()));
    }

    public synchronized void markWrong(String id) {
        if (!review.contains(id)) review.add(id);
        prefs.setReviewIds(join(review, ","));
    }

    private static String join(List<String> l, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(l.get(i));
        }
        return sb.toString();
    }

    // ---------- WebUI 使用的增删接口 ----------
    public synchronized boolean addQuestion(String stem, String[] options, int answer, String subject) {
        if (options == null || options.length != 4 || answer < 0 || answer > 3) return false;
        for (String s : options) if (s == null || s.trim().isEmpty()) return false;
        addInternal(stem.trim(), options.clone(), answer, subject, true);
        return true;
    }

    public synchronized boolean removeQuestion(String id) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).id.equals(id)) {
                questions.remove(i);
                review.remove(id);
                prefs.setReviewIds(join(review, ","));
                save();
                return true;
            }
        }
        return false;
    }

    public synchronized List<Question> all() {
        return new ArrayList<>(questions);
    }

    public synchronized int size() {
        return questions.size();
    }

    public static String[] subjects() {
        return SUBJECTS.clone();
    }
}
                                                                                                                                                                       