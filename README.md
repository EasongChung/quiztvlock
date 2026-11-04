# 答题电视锁 (QuizTVLock)

面向 **Android 电视盒子 / 智能电视** 的儿童观看管理与答题应用，适合小学二年级左右的孩子：

- **看电视要先答题**：观看倒计时结束后自动弹出全屏答题页；
- **答对奖励时长**：每答对 1 题获得可配置的观看分钟数；
- **答错加题**：答错追加题目，并把错题加入复习队列优先重出；
- **自动暂停播放**：答题期间通过 `KEYCODE_MEDIA_PAUSE` 暂停视频，答完自动恢复；
- **开机自启 + 断电记忆**：重启后自动检测上轮答题是否完成，未完成直接进入答题；后台服务被杀后由 AlarmManager 心跳自动拉起；
- **防卸载**：支持设备管理员（一键）与设备所有者（ADB 命令升级，卸载按钮彻底禁用）；
- **进入 App 需独立密码**：防止孩子擅自进入设置页，密码可在 WebUI 统一修改；
- **强制黑屏休息**：连续观看达到设定时长自动黑屏、暂停播放，到点恢复；
- **家长局域网 WebUI**：手机浏览器打开 `http://电视IP:36789`，
  - 账号密码登录（默认 `admin` / `123456`，网页内可修改）；
  - 调整「观看时长 / 每轮题数 / 答对奖励 / 答错追加 / 休息时长 / 题库开关」；
  - 修改电视进入 App 密码、家长 PIN；
  - 题目增删、**txt 批量导入导出**、立即开始一轮答题、家长 PIN 解锁、重置观看时长、防卸载管理。

## 技术特点

| 项目 | 说明 |
| --- | --- |
| 依赖 | **零第三方依赖**，纯 Java + 系统 API（android.app.* / org.json） |
| 最低系统 | Android 5.0（API 21），兼容老旧电视 |
| targetSdk | 28 —— 故意不追新，避免 Android 10+ 的「后台 Activity 启动限制」导致答题页拉不起来 |
| 常驻方式 | 前台服务 + 看门狗，每秒检查答题页是否在前台 |
| 状态持久化 | SharedPreferences，断电重启不丢失 |

## 题库

内置二年级难度题库，全部为 **四选一**（英语也是选择题，不涉及口语识别）：

- **语文**：30 个常用字的拼音选择；
- **数学**：加法、减法、乘法口诀随机出题；
- **英语**：15 个基础单词的中文释义选择。

家长可在 WebUI 自由增删题目、按科目启停题库。

## 编译

无需本机环境，推送到 GitHub 即可自动编译：

1. 将本仓库推送到 GitHub；
2. Actions（`.github/workflows/build.yml`）自动编译出 release APK；
3. 在 Actions 运行记录的 Artifacts 里下载 `quiztvlock-apk`。

详见 [docs/BUILD.md](docs/BUILD.md)。

## 安装与使用

详见 [docs/USAGE.md](docs/USAGE.md)。简要：

```bash
adb connect <电视IP>:5555
adb install app-release.apk
# 安装后在电视桌面打开「答题电视锁」，一次即开始守护
```

## 与开源参考项目的关系

- [CleanYourTV](https://github.com/gathin23/cleanyourtv)（MIT）：用于清理电视系统广告/推荐组件、安装轻量桌面，**本 APK 不包含其代码**，仅在 [docs/ADB-CLEAN.md](docs/ADB-CLEAN.md) 中给出配合使用的步骤。
- [KidsLock](https://github.com/wplleo/kidslock)：**无开源许可证**。本项目仅参考其「前台服务 + 全屏答题 + 开机自启 + SharedPreferences 持久化」的架构思路，**全部代码均为原创**，未复制其代码，规避无授权二次分发的法律风险。

## 许可

MIT，见 [LICENSE](LICENSE)。
