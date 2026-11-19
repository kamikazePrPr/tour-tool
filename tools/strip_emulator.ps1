# 卸载模拟器上的预装应用（只卸「用户 0」层级，系统分区文件还在，随时可用
# `adb shell pm install-existing <包名>` 装回来）。
#
# 保留的一律不动：SystemUI、设置、桌面、权限控制器、安装器、输入法、WebView、
# 媒体库（照片选择器要用）、文件选择器、GMS 骨架，以及所有 overlay/rro 包。
# 卸掉任何 overlay 包都会让系统起不来。

$adb = "D:\AIsoft\AndroidSdk\platform-tools\adb.exe"

$backup = "D:\AIsoft\dl\emulator-packages-before-uninstall.txt"
& cmd /c "`"$adb`" shell pm list packages 2>&1" | Out-File -FilePath $backup -Encoding utf8
Write-Host "package list backed up -> $backup"

$targets = @(
    # Google 全家桶
    "com.google.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.google.android.gm",
    "com.google.android.apps.maps",
    "com.google.android.apps.photos",
    "com.google.android.apps.docs",
    "com.google.android.calendar",
    "com.google.android.apps.messaging",
    "com.google.android.dialer",
    "com.google.android.contacts",
    "com.google.android.deskclock",
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.wellbeing",
    "com.google.android.apps.safetyhub",
    "com.google.android.apps.wallpaper",
    "com.google.android.apps.wallpaper.nexus",
    "com.google.android.apps.customization.pixel",
    "com.google.android.marvin.talkback",
    "com.google.android.marvin.talkbackoverlay",
    "com.google.android.projection.gearhead",
    "com.google.android.tts",
    "com.google.android.feedback",
    "com.google.android.partnersetup",
    "com.google.android.markup",
    "com.google.android.avatarpicker",
    "com.google.android.as",
    "com.google.android.as.oss",
    "com.google.android.federatedcompute",
    "com.google.android.ondevicepersonalization.services",
    "com.google.android.adservices.api",
    "com.google.android.sdksandbox",
    "com.google.android.odad",
    "com.google.android.rkpdapp",
    "com.google.android.devicelockcontroller",
    "com.google.android.healthconnect.controller",
    "com.google.android.health.connect.backuprestore",
    "com.google.android.apps.restore",
    "com.google.mainline.adservices",
    "com.google.mainline.telemetry",
    # 系统里可有可无的
    "com.android.chrome",
    "com.android.egg",
    "com.android.traceur",
    "com.android.stk",
    "com.android.printspooler",
    "com.google.android.printservice.recommendation",
    "com.android.bips",
    "com.android.bookmarkprovider",
    "com.android.wallpaper.livepicker",
    "com.android.dreams.basic",
    "com.android.nfc",
    "com.android.emergency",
    "com.android.calllogbackup",
    "com.android.musicfx",
    "com.android.soundpicker",
    "com.google.android.soundpicker",
    "com.android.htmlviewer",
    "com.android.simappdialog",
    "com.android.managedprovisioning"
)

$ok = 0
$failed = @()
foreach ($p in $targets) {
    $out = & cmd /c "`"$adb`" shell pm uninstall --user 0 $p 2>&1" | Out-String
    $out = $out.Trim()
    if ($out -match "Success") {
        $ok++
        Write-Host "OK    $p"
    } else {
        $failed += "$p  ->  $out"
        Write-Host "SKIP  $p  ($out)"
    }
}

Write-Host ""
Write-Host "uninstalled: $ok / $($targets.Count)"
if ($failed.Count -gt 0) {
    Write-Host "skipped:"
    $failed | ForEach-Object { Write-Host "  $_" }
}
