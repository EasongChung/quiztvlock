# 编译与签名

## 方式一：GitHub 云端编译（推荐，无需本机环境）

1. 将本仓库推送到 GitHub 的 `main` 分支；
2. Actions（`.github/workflows/build.yml`）自动：
   - JDK 17 + Android SDK 34 + Gradle 7.6.2；
   - `gradle assembleRelease` 产出未签名 APK；
   - 用仓库 Secrets 中的自有 keystore 做 **zipalign + apksigner 签名**；
3. 在 **Actions** 页面点最近一次成功的 `Build APK` → 页面底部 **Artifacts** 下载 `quiztvlock-apk`。

签名证书信息（请妥善保管，私钥文件在 `release/quiztv.p12`，已 gitignore）：

| 项 | 值 |
| --- | --- |
| 证书文件 | `release/quiztv.p12`（本地保存，勿上传） |
| 类型 | PKCS12，RSA-2048，有效期 50 年 |
| 密钥库口令 | `QuizTV@2026!Key` |
| GitHub Secrets | `QUIZTV_KEYSTORE_BASE64`（证书 base64）、`QUIZTV_STORE_PASS`（口令）、`EASONG_QTV_SIGN_USER` |

**同一 keystore 签出的 APK 指纹一致，可直接覆盖升级安装。**

> 想换成自己的证书：本地 `New-SelfSignedCertificate` 或 `keytool` 生成 p12，
> `base64` 后更新 `QUIZTV_KEYSTORE_BASE64`、修改 `QUIZTV_STORE_PASS` 即可。

## 方式二：本机编译

要求：JDK 17、Gradle 7.6+、Android SDK platform 34 + build-tools 34。

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
cd quiztvlock
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk
# 用 release 目录下的 quiztv.p12 签名：
zipalign -f -p 4 app-release-unsigned.apk aligned.apk
apksigner sign --ks release/quiztv.p12 --ks-type pkcs12 \
  --ks-pass pass:QuizTV@2026!Key --key-pass pass:QuizTV@2026!Key \
  --out app-release.apk aligned.apk
```

## 安全提示

- 私钥文件 `quiztv.p12` 与口令属高危数据，**不要提交到仓库、不要发到公开群**；
- 本仓库靠 `.gitignore`（`*.p12`、`/release/`）防止误提交，请勿修改。
