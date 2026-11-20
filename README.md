# 旅游团身份证登记 App

旅行社前台用的安卓工具：新建旅游团 → 批量选身份证照片 → 本地离线识别姓名/性别/身份证号 → 四重结构校验 → 人工兜底核对 → 打标签备注 → 导出 Excel / 纯文本 → 微信发送。

**为「不会用电脑、平时只用微信和抖音」的中老年使用者设计**：一屏一个大按钮、全程无手势、界面不出现任何专业词。

## 隐私与联网范围

- 客人的姓名、身份证号、照片**只存在这台手机的私有目录里**，不上传、不备份、没有统计和广告。
- 相册里的原图**只读**：不修改、不删除、不移动、不复制。
- 联网只为两件事：查 GitHub 上有没有新版本、下载新版本。域名通过 `network_security_config.xml` 白名单锁死，只放行 GitHub 相关域名。
- 不接任何第三方统计或崩溃上报服务；错误日志写在本机，脱敏后按天滚动、自动清理。

## 识别与校验

三个通道（原图 ROI 放大 / 二值化去反光 / 旋转上采样）各自识别，归一化后比对，再过四道闸：

1. 格式：18 位、第 7–14 位是合法日期、前 6 位省级行政区码合法
2. 校验码：GB 11643-1999 附录 B，加权因子 `Wi = 2^(18−i) mod 11`，模 11 查表 `1 0 X 9 8 7 6 5 4 3 2`
3. 出生日期交叉：号码第 7–14 位必须等于卡面「出生」栏
4. 性别交叉：号码第 17 位奇偶必须等于卡面「性别」栏

全部通过才自动填；任何一道不过就进人工复核，界面只报一句人话。人工改完还要再过一次校验码。

## 文档

设计方案（当前版本）：[docs/设计方案_v2.0.md](docs/设计方案_v2.0.md)

历史版本在 [docs/versions/](docs/versions/)。

## 构建

（工程骨架待建，工具链：JDK 17 + Android SDK platform 35 + build-tools 35）

```bash
./gradlew :app:assembleRelease
```

## 发布新版本

版本号规则（2026-11-20 更新）：**0.1.0 是第一个可用版本**，以后只动最后一位——0.1.0 → 0.1.1 → 0.1.2，前两位不动。`versionCode` 和 `versionName` 解耦，每次发布单独加一（必须单调递增，否则覆盖安装会报 `INSTALL_FAILED_VERSION_DOWNGRADE`）。当前 versionName=0.1.0 对应 versionCode=22。

```bash
# 1. 改 app/build.gradle.kts：versionName 最后一位 +1，versionCode +1
# 2. 在 ui/about/LegalText.kt 的 CHANGELOG 里加一条本次更新说明
# 3. 构建签名 APK
# 4. 算校验和并发布（tag 写 v0.1.X）
#    交付给使用者的那份固定叫「旅游登记工具.apk」，覆盖同名、不带版本号
certutil -hashfile app-release.apk SHA256
gh release create v0.1.1 app-release.apk app-release.apk.sha256 --notes "更新说明"
```

App 端会读取 `kamikazePrPr/tour-tool` 的最新 Release（用 `/releases` 列表而不是 `/releases/latest`——后者遇到只有 prerelease 时返回 404），下载后校验 SHA-256、包名、签名证书摘要，三道全过才拉起系统安装。Manifest 里必须保留 `REQUEST_INSTALL_PACKAGES`，缺了系统安装器会直接拒绝。

## 注意

- `release.keystore` **不进仓库**，务必在两处离线备份。签名一旦变化，旧版数据全部失效。
- 仓库必须公开：自动更新要在手机上读 Release 信息，私有仓库就得把 token 打进 APK。
