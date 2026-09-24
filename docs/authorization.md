# ColorOS 17 首次授权排查

本说明仅记录 OPD2515、ColorOS 17.0.0.102 上实际遇到的情况。

## 已连接电脑，但 pm grant 被拒绝

若出现：

```text
Neither user 2000 nor current process has android.permission.GRANT_RUNTIME_PERMISSIONS
```

本机原因是 ColorOS 权限拦截。此版本的相关开发者开关显示为 **禁用系统优化**，默认可能隐藏；其他版本不一定相同。

操作前先读出并记录入口显示标记的原值：

```sh
adb shell settings get secure system_opt_enable
```

仅显示入口：

```sh
adb shell settings put secure system_opt_enable 1
```

重新进入开发者选项，找到“禁用系统优化”。显示入口本身不会关闭保护。实际打开开关前，阅读系统确认页：系统提示可能增加耗电、影响系统功能并降低安全保护。只有本人接受此临时变更时才继续。

暂时开启后，执行 README 中的两条 `pm grant` 和 `appops set` 命令。完成后立即关闭“禁用系统优化”，恢复保护。不要为了使用助手长期保留该开关。

本机可检查：

```sh
adb shell getprop persist.sys.permission.enable
```

恢复保护后本机返回 `true`。入口标记也应恢复原值：若之前是 `null`，执行：

```sh
adb shell settings delete secure system_opt_enable
```

若之前有其他值，用 `settings put secure system_opt_enable 原值` 恢复它。

## 助手提示系统未接受调用

若错误包含 `missing android.permission.PACKAGE_USAGE_STATS permission`，说明只授予了 DUMP，首次授权未完成。补齐 README 中的使用情况访问权限，再恢复系统保护。

不要把“指令已发送”当作长期显示保证。重新锁屏并观察；程序不会持续唤醒屏幕，也不会实时读取内存开关。
