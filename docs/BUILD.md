# 编译说明

## 方式一：GitHub 云端编译（推荐，无需本机环境）

1. 在 GitHub 新建仓库（例如 `quiztvlock`）；
2. 把本目录全部文件推送到 `main` 分支；
3. GitHub Actions（`.github/workflows/build.yml`）自动触发：
   - JDK 17 + Android SDK 34 + Gradle 8.2；
   - 执行 `gradle assembleRelease`；
4. 进入仓库的 **Actions** 页面 → 点最近一次成功的 `Build APK` 运行 →
   页面底部 **Artifacts** 区域下载 `quiztvlock-apk`（含 APK）。

## 方式二：本机编译

要求：JDK 17、Gradle 8.0+、Android SDK platform 34 + build-tools 34。

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
cd quiztvlock
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 签名

当前 Cloud Build 使用 **debug 签名**，便于自测。
正式长期使用建议换成自己的签名（密钥请妥善保管，不要提交到仓库）：

```bash
keytool -genkey -v -keystore quiztv.keystore -alias quiztv -keyalg RSA -validity 10000
```

然后在 `app/build.gradle` 中替换：

```groovy
android {
    signingConfigs {
        release {
            storeFile file('quiztv.keystore')
            storePassword '***'
            keyAlias 'quiztv'
            keyPassword '***'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

**安全提示**：密钥与密码属高危数据，建议改为读取 `local.properties` 或环境变量，
**不要**写死在提交到仓库的文件里。
