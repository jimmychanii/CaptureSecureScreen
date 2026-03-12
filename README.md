# Android Capture Secure Screen
A tool that can take screenshots of secure activity using adb or Shizuku.

通过Shizuku权限，对屏幕进行截图，包含secure flag screen。
也可以直接通过adb shell截屏：
```
adb push classes.dex /data/local/tmp/
adb shell CLASSPATH=/data/local/tmp/classes.dex app_process /data/local/tmp com.xgs.main.Main

```


Using Shizuku, you can take screenshots of all screens, including those containing the secure flag.
You can also take a screenshot of the secure page directly via adb shell:
```
adb push classes.dex /data/local/tmp/
adb shell CLASSPATH=/data/local/tmp/classes.dex app_process /data/local/tmp com.xgs.main.Main

```
