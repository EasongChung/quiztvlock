package com.quiztv.lock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 极简局域网 WebUI 服务器（零依赖，ServerSocket + 原生 JSON）。
 * <p>
 * 访问：http://电视IP:36789/  —— 首次需账号登录（默认 admin / 123456）。
 * <p>
 * 路由：
 * <pre>
 *  GET  /                      管理页面（无需登录，页面内自行弹登录）
 *  POST /api/login             账号密码 → token
 *  GET  /api/status            状态（需 token）
 *  GET  /api/questions         题目列表（需 token）
 *  POST /api/question          添加题目（需 token）
 *  POST /api/question/delete   删除题目（需 token）
 *  POST /api/lock              立即开始一轮答题（需 token）
 *  POST /api/unlock            家长 PIN 解锁（需 token）
 *  POST /api/reset             重置观看时长（需 token）
 *  POST /api/settings          修改设置：观看时长/题数/题库开关（需 token）
 *  POST /api/account           修改 WebUI 账号密码（需 token）
 * </pre>
 */
public class HttpAdminServer {
    public static final int PORT = 36789;

    private final LockService service;
    private final PrefManager prefs;
    private final QuestionBank bank;
    private final Random rnd = new Random();
    private final Map<String, Long> tokens = new HashMap<>();
    private static final long TOKEN_TTL = 24L * 3600 * 1000;

    private ServerSocket server;
    private volatile boolean running;

    public HttpAdminServer(LockService service, PrefManager prefs, QuestionBank bank) {
        this.service = service;
        this.prefs = prefs;
        this.bank = bank;
    }

    public void start() {
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(PORT);
                    while (running) {
                        final Socket s = server.accept();
                        new Thread(new Runnable() {
                            @Override
                            public void run() { handle(s); }
                        }).start();
                    }
                } catch (Exception e) {
                    if (running) e.printStackTrace();
                }
            }
        }, "quiztv-web").start();
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignore) { }
        synchronized (tokens) { tokens.clear(); }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(15000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            String line = reader.readLine();
            if (line == null) return;
            String[] head = line.split(" ");
            if (head.length < 2) return;
            String method = head[0];
            String rawPath = head[1];

            int q = rawPath.indexOf('?');
            String path = q >= 0 ? rawPath.substring(0, q) : rawPath;
            String query = q >= 0 ? rawPath.substring(q + 1) : "";

            Map<String, String> params = parseParams(query);

            int contentLength = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break;
                String lo = line.toLowerCase();
                if (lo.startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (Exception ignore) { }
                }
            }
            if (contentLength > 0) {
                StringBuilder body = new StringBuilder();
                for (int i = 0; i < contentLength; i++) {
                    int c = reader.read();
                    if (c < 0) break;
                    body.append((char) c);
                }
                params.putAll(parseParams(body.toString()));
            }

            OutputStream os = s.getOutputStream();
            route(os, method, path, params);
            os.flush();
        } catch (Exception ignore) {
        } finally {
            try { s.close(); } catch (Exception ignore) { }
        }
    }

    private Map<String, String> parseParams(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) {
                try {
                    m.put(URLDecoder.decode(kv.substring(0, i), "UTF-8"),
                            URLDecoder.decode(kv.substring(i + 1), "UTF-8"));
                } catch (Exception ignore) { }
            }
        }
        return m;
    }

    private void write(OutputStream os, String mime, byte[] data) throws Exception {
        String head = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + mime + "; charset=utf-8\r\n"
                + "Content-Length: " + data.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        os.write(head.getBytes("UTF-8"));
        os.write(data);
    }

    private void writeJson(OutputStream os, JSONObject o) {
        try {
            write(os, "application/json", o.toString().getBytes("UTF-8"));
        } catch (Exception ignore) { }
    }

    private void writeFail(OutputStream os, String msg) {
        JSONObject o = new JSONObject();
        try { o.put("ok", false); o.put("msg", msg); } catch (Exception ignore) { }
        writeJson(os, o);
    }

    private void writeOk(OutputStream os) {
        JSONObject o = new JSONObject();
        try { o.put("ok", true); } catch (Exception ignore) { }
        writeJson(os, o);
    }

    /** 登录校验：token 放 query 或 body 均可 */
    private boolean authed(Map<String, String> p) {
        String t = p.get("token");
        if (t == null) return false;
        synchronized (tokens) {
            Long at = tokens.get(t);
            if (at == null) return false;
            if (System.currentTimeMillis() - at > TOKEN_TTL) {
                tokens.remove(t);
                return false;
            }
        }
        return true;
    }

    private void route(OutputStream os, String method, String path, Map<String, String> p) {
        try {
            if ("/".equals(path) || "/index.html".equals(path) || path.startsWith("/admin")) {
                byte[] html = readAsset("admin.html");
                if (html == null) write(os, "text/plain", "admin.html missing".getBytes("UTF-8"));
                else write(os, "text/html", html);
                return;
            }
            if (!path.startsWith("/api/")) {
                write(os, "text/plain", "not found".getBytes("UTF-8"));
                return;
            }

            if ("/api/login".equals(path)) {
                String u = p.get("user"), w = p.get("pass");
                if (u != null && w != null && u.equals(prefs.getWebUser()) && w.equals(prefs.getWebPass())) {
                    String token = Long.toHexString(rnd.nextLong()) + Long.toHexString(rnd.nextLong());
                    synchronized (tokens) { tokens.put(token, System.currentTimeMillis()); }
                    JSONObject o = new JSONObject();
                    o.put("ok", true);
                    o.put("token", token);
                    writeJson(os, o);
                } else {
                    writeFail(os, "账号或密码错误");
                }
                return;
            }

            // 以下接口需登录
            if (!authed(p)) {
                writeFail(os, "未登录");
                return;
            }

            if ("/api/status".equals(path)) {
                writeJson(os, statusJson());
            } else if ("/api/questions".equals(path)) {
                JSONArray arr = new JSONArray();
                for (Question q : bank.all()) {
                    JSONObject j = new JSONObject();
                    j.put("id", q.id);
                    j.put("stem", q.stem);
                    j.put("subject", q.subject);
                    j.put("answer", q.options[q.answer]);
                    arr.put(j);
                }
                JSONObject o = new JSONObject();
                o.put("ok", true);
                o.put("questions", arr);
                writeJson(os, o);
            } else if ("/api/question".equals(path)) {
                String stem = p.get("stem");
                String[] opts = new String[]{p.get("opt0"), p.get("opt1"), p.get("opt2"), p.get("opt3")};
                int ans;
                try { ans = Integer.parseInt(p.get("correct")); }
                catch (Exception e) { writeFail(os, "correct 参数非法"); return; }
                String subject = p.get("subject");
                if (subject == null || subject.isEmpty()) subject = "语文";
                if (bank.addQuestion(stem, opts, ans, subject)) writeOk(os);
                else writeFail(os, "参数不完整：需题干、4 个非空选项、正确选项 0-3");
            } else if ("/api/question/delete".equals(path)) {
                if (bank.removeQuestion(p.get("id"))) writeOk(os);
                else writeFail(os, "题目不存在");
            } else if ("/api/lock".equals(path)) {
                int count;
                try { count = Integer.parseInt(p.get("count")); }
                catch (Exception e) { count = prefs.getQuestionsPerRound(); }
                if (service == null) { writeFail(os, "服务未运行"); return; }
                service.startRound(count);
                writeOk(os);
            } else if ("/api/unlock".equals(path)) {
                String pin = p.get("pin");
                if (pin != null && pin.equals(prefs.getPin())) {
                    service.parentUnlock();
                    writeOk(os);
                } else {
                    writeFail(os, "PIN 码错误");
                }
            } else if ("/api/reset".equals(path)) {
                int min;
                try { min = Integer.parseInt(p.get("minutes")); }
                catch (Exception e) { min = prefs.getWatchMinutes(); }
                service.resetTimer(min);
                writeOk(os);
            } else if ("/api/settings".equals(path)) {
                applyInt(p, "watchMinutes", new Setter() {
                    @Override public void set(int v) { prefs.setWatchMinutes(v); }
                });
                applyInt(p, "minutesPerCorrect", new Setter() {
                    @Override public void set(int v) { prefs.setMinutesPerCorrect(v); }
                });
                applyInt(p, "questionsPerRound", new Setter() {
                    @Override public void set(int v) { prefs.setQuestionsPerRound(v); }
                });
                applyInt(p, "extraPerWrong", new Setter() {
                    @Override public void set(int v) { prefs.setExtraPerWrong(v); }
                });
                for (String s : QuestionBank.subjects()) {
                    String v = p.get("bank_" + s);
                    if (v != null) prefs.setBankEnabled(s, "1".equals(v) || "true".equals(v));
                }
                writeOk(os);
            } else if ("/api/account".equals(path)) {
                String oldU = p.get("oldUser"), oldP = p.get("oldPass");
                if (oldU != null && oldP != null
                        && oldU.equals(prefs.getWebUser()) && oldP.equals(prefs.getWebPass())) {
                    String nu = p.get("newUser"), np = p.get("newPass");
                    boolean changed = false;
                    if (nu != null && !nu.trim().isEmpty()) { prefs.setWebUser(nu.trim()); changed = true; }
                    if (np != null && np.length() >= 4) { prefs.setWebPass(np); changed = true; }
                    if (changed) writeOk(os);
                    else writeFail(os, "新账号或密码无效（密码至少 4 位）");
                } else {
                    writeFail(os, "原账号或密码错误");
                }
            } else {
                writeFail(os, "unknown api");
            }
        } catch (Exception e) {
            writeFail(os, "server error: " + e.getMessage());
        }
    }

    private interface Setter { void set(int v); }

    private void applyInt(Map<String, String> p, String key, Setter setter) {
        String v = p.get(key);
        if (v == null) return;
        try {
            int n = Integer.parseInt(v);
            if (n > 0) setter.set(n);
        } catch (Exception ignore) { }
    }

    private JSONObject statusJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("quizActive", prefs.isQuizActive());
        o.put("pending", prefs.getPending());
        o.put("correct", prefs.getCorrectInRound());
        long rem = prefs.isQuizActive()
                ? prefs.getPauseRemainingMs()
                : prefs.getDeadlineMs() - System.currentTimeMillis();
        o.put("remainingMin", Math.max(0L, rem / 60000L));
        o.put("totalQuestions", bank.size());

        JSONObject st = new JSONObject();
        st.put("watchMinutes", prefs.getWatchMinutes());
        st.put("minutesPerCorrect", prefs.getMinutesPerCorrect());
        st.put("questionsPerRound", prefs.getQuestionsPerRound());
        st.put("extraPerWrong", prefs.getExtraPerWrong());
        for (String s : QuestionBank.subjects()) st.put("bank_" + s, prefs.isBankEnabled(s));
        o.put("settings", st);

        o.put("webUser", prefs.getWebUser());
        o.put("ip", Util.getIp(service));
        o.put("port", PORT);
        return o;
    }

    private byte[] readAsset(String name) throws Exception {
        InputStream is = service.getAssets().open(name);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }
}
