package com.kamikazeprpr.tourtool.ui.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kamikazeprpr.tourtool.BuildConfig
import com.kamikazeprpr.tourtool.data.AppLog
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted
import com.kamikazeprpr.tourtool.update.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

/**
 * 关于页：开发者、更新日志、检查更新、错误日志、隐私声明、开源许可、免责声明。
 * 每一项都就地展开成全文，不弹对话框——长文在弹窗里滚很难受。
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var section by remember { mutableStateOf<Section?>(null) }

    val current = section
    if (current != null) {
        SectionScreen(current, onBack = { section = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BackRow(onBack, "返回")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("关于", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(14.dp))

            Text("开发人员：${LegalText.DEVELOPER}", fontSize = 17.sp, color = TextMain)
            Spacer(Modifier.height(6.dp))
            Text("版本 ${BuildConfig.VERSION_NAME}", fontSize = 15.sp, color = TextMuted)
            Spacer(Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))

            MenuRow("更新日志") { section = Section.CHANGELOG }
            MenuRow("检查更新") { section = Section.CHECK_UPDATE }
            MenuRow("错误日志") { section = Section.LOGS }
            MenuRow("用户隐私与协议") { section = Section.PRIVACY }
            MenuRow("开源许可") { section = Section.LICENSE }
            MenuRow("免责声明") { section = Section.DISCLAIMER }

            Spacer(Modifier.height(24.dp))
            Text(
                "本软件以 MIT 许可协议开源。",
                fontSize = 13.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class Section(val title: String) {
    CHANGELOG("更新日志"),
    CHECK_UPDATE("检查更新"),
    LOGS("错误日志"),
    PRIVACY("用户隐私与协议"),
    LICENSE("开源许可"),
    DISCLAIMER("免责声明"),
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 18.sp, color = TextMain, modifier = Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = TextMuted)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SectionScreen(section: Section, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BackRow(onBack, section.title)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(section.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(16.dp))

            when (section) {
                Section.CHANGELOG -> ChangelogContent()
                Section.CHECK_UPDATE -> CheckUpdateContent()
                Section.LOGS -> LogsContent()
                Section.PRIVACY -> Paragraphs(LegalText.PRIVACY_PARAGRAPHS)
                Section.DISCLAIMER -> Paragraphs(LegalText.DISCLAIMER_PARAGRAPHS)
                Section.LICENSE -> LicenseContent()
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Paragraphs(list: List<String>) {
    list.forEach { text ->
        Text(text = text, fontSize = 16.sp, lineHeight = 26.sp, color = TextMain)
        Spacer(Modifier.height(15.dp))
    }
}

@Composable
private fun ChangelogContent() {
    LegalText.CHANGELOG.forEach { release ->
        Text(
            text = "${release.version}　${release.date}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
        )
        Spacer(Modifier.height(8.dp))
        release.notes.forEach { note ->
            Text("· $note", fontSize = 15.sp, lineHeight = 24.sp, color = TextMain)
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LicenseContent() {
    Text("项目中使用的第三方组件：", fontSize = 15.sp, color = TextMuted)
    Spacer(Modifier.height(10.dp))
    LegalText.THIRD_PARTY.forEach { (name, license, usage) ->
        Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        Text("$license · $usage", fontSize = 14.sp, color = TextMuted)
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(14.dp))
    Text(LegalText.MIT_TITLE, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
    Spacer(Modifier.height(8.dp))
    Text(
        text = LegalText.MIT_TEXT,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(14.dp))
    Text("这是本项目自身源码所适用的许可协议。", fontSize = 13.sp, color = TextMuted)
}

@Composable
private fun CheckUpdateContent() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var release by remember { mutableStateOf<UpdateChecker.Release?>(null) }

    Text("当前版本：${BuildConfig.VERSION_NAME}", fontSize = 16.sp, color = TextMain)
    Spacer(Modifier.height(14.dp))

    RowButton(
        label = if (busy) "正在检查…" else "检查更新",
        enabled = !busy,
    ) {
        busy = true
        message = null
        release = null
        scope.launch {
            when (val r = UpdateChecker.check(ctx, force = true)) {
                is UpdateChecker.CheckResult.Newer -> {
                    release = r.release
                    message = "有新版本 ${r.release.version}"
                }
                UpdateChecker.CheckResult.UpToDate -> message = "已经是最新版本了"
                is UpdateChecker.CheckResult.Failed -> message = "没查成：${r.reason}"
            }
            busy = false
        }
    }

    message?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }

    release?.let { rel ->
        Spacer(Modifier.height(14.dp))
        RowButton(label = if (busy) "下载中…" else "下载并安装 ${rel.version}", enabled = !busy) {
            busy = true
            scope.launch {
                when (val r = UpdateChecker.download(ctx, rel)) {
                    is UpdateChecker.InstallResult.Ready -> UpdateChecker.install(ctx, r.file)
                    is UpdateChecker.InstallResult.Rejected -> message = r.reason
                }
                busy = false
            }
        }
    }
}

@Composable
private fun RowButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(label, fontSize = 17.sp)
    }
}

@Composable
private fun LogsContent() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Text(
        "日志只记运行情况，不含姓名和完整身份证号（号码只留前 3 后 2），保存在本机，7 天后自动删除。",
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(14.dp))

    val files = remember { AppLog.logFiles(ctx) }
    Text("当前日志：${files.size} 个文件", fontSize = 16.sp, color = TextMain)
    Spacer(Modifier.height(14.dp))

    RowButton(label = "导出日志并发给开发者", enabled = files.isNotEmpty()) {
        scope.launch {
            try {
                val dir = File(ctx.cacheDir, "logs").apply { mkdirs() }
                val out = File(dir, "tour-tool-logs-${System.currentTimeMillis()}.zip")
                AppLog.exportBundle(ctx, out)
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", out)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(intent, "把日志发到哪里"))
                message = "已经打包好了，选个应用发出去"
            } catch (e: Exception) {
                message = "打包失败：${e.message}"
            }
        }
    }

    message?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
    }
}
