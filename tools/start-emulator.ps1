# 启动模拟器并把窗口固定到屏幕左侧固定位置。
#
# 模拟器是控制台程序，直接起会顺带弹出一个黑色 cmd 窗口；这里把标准输出重定向到文件，
# 那个控制台就不会出现。窗口位置由 Qt 记在 AVD 里，每次退出时的状态会被沿用，
# 所以启动后用 Win32 的 MoveWindow 把它钉死，避免每次打开位置都飘。

param(
    [string]$Avd = "tourtest",
    [int]$X = 60,
    [int]$Y = 40,
    [int]$Width = 420,
    [int]$Height = 900
)

$ErrorActionPreference = "SilentlyContinue"
$sdk = "D:\AIsoft\AndroidSdk"
$emu = Join-Path $sdk "emulator\emulator.exe"

# 先清掉旧实例，否则同一个 AVD 不能同时跑两个
Get-Process | Where-Object { $_.ProcessName -match "qemu-system|emulator" } | Stop-Process -Force
Start-Sleep -Seconds 3

Start-Process -FilePath $emu `
    -ArgumentList "-avd", $Avd, "-no-snapshot-load", "-no-boot-anim", "-gpu", "swiftshader_indirect" `
    -RedirectStandardOutput "D:\AIsoft\dl\emu-out.log" `
    -RedirectStandardError "D:\AIsoft\dl\emu-err.log" `
    -WindowStyle Hidden

# 等窗口出现，然后钉位置
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WinPos {
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h, int x, int y, int w, int hh, bool repaint);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
}
"@

for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 2
    $p = Get-Process -Name "qemu-system-x86_64" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne [IntPtr]::Zero } |
        Select-Object -First 1
    if ($p) {
        [WinPos]::MoveWindow($p.MainWindowHandle, $X, $Y, $Width, $Height, $true) | Out-Null
        [WinPos]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
        Write-Host "emulator window pinned at ${X},${Y} ${Width}x${Height}"
        break
    }
}

# 等系统起来
$adb = Join-Path $sdk "platform-tools\adb.exe"
for ($i = 0; $i -lt 90; $i++) {
    $b = (& cmd /c "`"$adb`" shell getprop sys.boot_completed 2>&1" | Out-String).Trim()
    if ($b -eq "1") {
        Write-Host "boot completed"
        break
    }
    Start-Sleep -Seconds 5
}
