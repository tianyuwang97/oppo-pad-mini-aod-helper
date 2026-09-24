# 全景息屏助手 · OPPO Pad Mini

让 OPPO Pad Mini 保持原生全景息屏。通过系统已有的调试入口，跳过约 10 秒后的全景息屏隐藏逻辑。无需 Root，没有定时点击、周期亮屏或常驻服务。

**已验证：OPPO Pad Mini OPD2515 / ColorOS 17.0.0.102。其他型号、系统版本暂不支持。**

## 下载

在 [Releases](https://github.com/tianyuwang97/oppo-pad-mini-aod-helper/releases) 下载 `OPPO-AOD-Helper.apk`。当前版本为 **1.1**，包名 `local.codex.panorama`。

首次安装需要电脑通过 ADB 授权。完成后，日常操作直接在平板进行，无需保持 USB 连接。

## 使用

1. 完成下方首次授权。
2. 打开“全景息屏助手”，点 **开启全景息屏保持**。
3. 按电源键进入全景息屏，观察是否持续显示。
4. 不需要保持时，点 **恢复默认关闭时间**，再重新锁屏。

应用显示最近一次指令结果，并非实时监测息屏开关。系统或 SystemUI 重启后，重新打开助手点一次开启。

“开机后尝试恢复保持”默认关闭，尚未实测，可能需要系统允许自启动。关闭开发者选项后的实际行为尚未单独测试；应用日常调用本身不依赖 ADB。

## 首次安装与授权

准备 Android SDK Platform Tools，在平板开启 USB 调试并允许自己的电脑。安装 APK 后执行：

```sh
adb install -r OPPO-AOD-Helper.apk
adb shell pm grant local.codex.panorama android.permission.DUMP
adb shell pm grant local.codex.panorama android.permission.PACKAGE_USAGE_STATS
adb shell appops set local.codex.panorama GET_USAGE_STATS allow
```

Android 17 的 ActivityManager 诊断入口同时要求前两项权限。助手会检查授权，并报告系统返回的错误。它不查询应用使用历史，也没有联网权限。

若 ColorOS 报错缺少 `GRANT_RUNTIME_PERMISSIONS`，参阅 [ColorOS 首次授权说明](docs/authorization.md)。首次授权过程中临时改变的系统保护开关应立即恢复。不要为日常使用长期关闭系统保护。

卸载重装需要重新检查授权；同签名覆盖更新通常保留授权。自行签名的 APK 不能直接覆盖使用其他密钥签名的 APK。

## 原理与边界

助手以自身应用 UID 执行：

```sh
/system/bin/dumpsys activity service com.android.systemui/.keyguard.KeyguardService debug keep_aod_on 1
```

恢复默认将最后一位改为 `0`。此命令设置 SystemUI 中 `AODDisplayUtil.mKeepAodOn` 的内存值；`PanoramicAodController` 的节能超时隐藏分支会检查它。应用不会修改系统 APK 或屏幕亮度。

这是厂商调试入口，并非官方全天息屏选项。开启功能仅接受 `Build.DISPLAY=OPD2515_17.0.0.102(CN01)`；恢复默认不限制构建号。系统更新后需要重新验证。尚未测试全天耗电、长期显示、所有传感器场景或开机自动恢复。

卸载助手前可先点恢复默认；卸载本身不一定清除已设置的内存开关，重启也可清除。

## 实测

在同一台设备、系统权限保护已恢复的情况下，通过助手自身按钮执行：

| 操作 | 锁屏后检查时间 | 显示状态 |
| --- | --- | --- |
| 恢复默认关闭时间 | 16 秒 | `OFF` |
| 开启全景息屏保持 | 35 秒 | `DOZE_SUSPEND`，电源状态 `Dozing` |

测试日期：2026-09-24。原始电脑命令测试也得到用户确认：超过 30 秒仍显示原生全景息屏。

## 从源码构建

依赖：Python 3、JDK 17、Android SDK Platform 35、Build Tools 35.0.0。无需 Gradle，也不依赖第三方 Java 库。

准备自己的签名密钥，使用 `keytool -genkeypair` 交互生成，妥善保存密码；签名密钥不包含在仓库中。将密码放入环境变量 `AOD_KEYSTORE_PASSWORD`，再执行：

```sh
python scripts/build.py --java-home /path/to/jdk-17 --android-jar /path/to/android-sdk/platforms/android-35/android.jar --build-tools /path/to/android-sdk/build-tools/35.0.0 --keystore /path/to/private-signing.p12 --alias aodhelper
```

Windows 可使用对应的 Windows 路径。产物为 `dist/OPPO-AOD-Helper.apk`。发布 APK 校验值见 [SHA256SUMS](SHA256SUMS)。

## 文件

- `app/`：Android 清单、图标和 Java 源码。
- `scripts/build.py`：本地编译、打包、签名及验证脚本。
- `docs/authorization.md`：ColorOS 首次授权排查。

项目与 OPPO 官方无关联。
