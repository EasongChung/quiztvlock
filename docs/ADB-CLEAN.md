# 可选：先用 CleanYourTV 收拾电视

[CleanYourTV](https://github.com/gathin23/cleanyourtv)（MIT 协议）通过 ADB
停用电视厂商的广告、推荐、遥测组件并安装轻量桌面，**与本应用独立**，可按需配合使用。

> 本 APK 不包含 CleanYourTV 的任何代码，此处仅为配合步骤摘要。

## 适用范围

- 安卓电视 / Google TV / 安卓盒子 ✅
- 三星 Tizen、LG webOS、Roku、Hisense VIDAA ❌（无 ADB）

## 步骤

```bash
git clone https://github.com/Gathin23/cleanyourtv
cd cleanyourtv

python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50            # 换成你的电视 IP

# 1. 探测电视型号与系统版本
tools/venv/bin/python -I tools/tv.py --probe

# 2. 应用适配清单（examples/<品牌型号>/debloat.txt），先备份再停用
tools/disable.sh examples/xiaomi-mi-tv-4a/debloat.txt

# 3. 编译并安装轻量桌面（需要 JDK 17 + Android SDK 34）
TV_HOST=192.168.1.50 launcher/build-and-install.sh
```

## 一键还原（随时反悔）

```bash
tools/restore.sh examples/<品牌型号>/debloat.txt
```

## 与「答题电视锁」的配合顺序

推荐顺序：**原桌面可用 → 装 CleanYourTV 桌面 → 安装答题电视锁**。

1. CleanYourTV 负责「桌面干净、无广告推送」；
2. 答题电视锁负责「看电视先答题」，两者互不依赖；
3. 即使答题电视锁出问题，卸载即可，不影响 CleanYourTV 的清理结果；
4. 反过来若想还原 CleanYourTV 的修改，用 `tools/restore.sh`。
