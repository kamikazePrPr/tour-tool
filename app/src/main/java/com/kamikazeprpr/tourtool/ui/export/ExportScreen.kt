package com.kamikazeprpr.tourtool.ui.export

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.export.ExportService
import com.kamikazeprpr.tourtool.ui.theme.DeepIndigo
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted
import java.io.File

/**
 * 做成表格：挑列、定顺序、看预览、选格式，然后直接发给微信。
 * 预览和最终文件都来自同一次 buildTable，所见即所得。
 */
@Composable
fun ExportScreen(
    teamName: String,
    members: List<Member>,
    tagsByMember: Map<Long, List<String>>,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var columns by remember {
        mutableStateOf(ExportService.Column.entries.filter { it.defaultOn })
    }
    var sortBy by remember { mutableStateOf(ExportService.SortBy.ENTRY) }
    var descending by remember { mutableStateOf(false) }
    var asExcel by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    val table = remember(members, tagsByMember, columns, sortBy, descending) {
        ExportService.buildTable(members, tagsByMember, columns, sortBy, descending)
    }

    fun doExport() {
        val ext = if (asExcel) "xlsx" else "txt"
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, ExportService.fileName(teamName, ext))
        try {
            if (asExcel) ExportService.writeXlsx(file, table, teamName.take(31)) else ExportService.writeTxt(file, table)
        } catch (e: Exception) {
            message = "生成表格失败：${e.message}"
            return
        }

        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (asExcel) {
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            } else {
                "text/plain"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // 装了微信就直接进微信，免得老人在一堆图标里找
        val wechat = ctx.packageManager.getLaunchIntentForPackage("com.tencent.mm")
        if (wechat != null) {
            intent.setPackage("com.tencent.mm")
            try {
                ctx.startActivity(intent)
                message = "已经打开微信，选一个聊天发出去就行"
                return
            } catch (_: Exception) {
                // 微信装是装了但接不了，退回系统选择器
            }
        }
        try {
            ctx.startActivity(Intent.createChooser(intent, "发到哪里"))
            message = "已经生成表格，选一个应用发出去就行"
        } catch (e: Exception) {
            message = "没有能分享的应用"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
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
            Text("返回", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text("做成表格", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(4.dp))
            Text("$teamName · 共 ${members.size} 人", fontSize = 15.sp, color = TextMuted)

            Spacer(Modifier.height(16.dp))
            Text("要哪几列", fontSize = 15.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowChips(
                items = ExportService.Column.entries,
                label = { it.title },
                selected = { it in columns },
                onClick = { col ->
                    columns = if (col in columns) {
                        (columns - col).ifEmpty { columns }   // 至少留一列
                    } else {
                        ExportService.Column.entries.filter { it in columns || it == col }
                    }
                },
            )

            Spacer(Modifier.height(16.dp))
            Text("按什么排", fontSize = 15.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowChips(
                items = ExportService.SortBy.entries,
                label = { it.label },
                selected = { it == sortBy },
                onClick = { sortBy = it },
                perRow = 2,
                chipWidth = 170.dp,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { descending = !descending },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (descending) "现在是倒着排" else "现在是正着排", fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text("格式", fontSize = 15.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormatChip("Excel 表格", asExcel) { asExcel = true }
                FormatChip("纯文本", !asExcel) { asExcel = false }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "预览（前 8 行，共 ${table.rows.size} 行）",
                fontSize = 15.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            TablePreview(table.header, table.rows.take(8), table.textColumns)

            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Button(
                onClick = { doExport() },
                enabled = members.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("做成表格，发出去", fontSize = 19.sp)
            }
        }
    }
}

@Composable
private fun <T> FlowChips(
    items: List<T>,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
    perRow: Int = 4,
    chipWidth: Dp = 84.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val isOn = selected(item)
                    Column(
                        modifier = Modifier
                            .height(46.dp)
                            .width(chipWidth)
                            .background(
                                color = if (isOn) DeepIndigo else Color.White,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onClick(item) }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = label(item),
                            fontSize = 15.sp,
                            color = if (isOn) Color.White else TextMain,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .height(46.dp)
            .width(130.dp)
            .background(
                color = if (selected) DeepIndigo else Color.White,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, fontSize = 15.sp, color = if (selected) Color.White else TextMain)
    }
}

@Composable
private fun TablePreview(header: List<String>, rows: List<List<String>>, textColumns: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(8.dp),
    ) {
        Row {
            header.forEachIndexed { i, h ->
                Text(
                    text = h,
                    modifier = Modifier.width(if (i in textColumns) 150.dp else 78.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        rows.forEach { row ->
            Row {
                row.forEachIndexed { i, cell ->
                    Text(
                        text = cell,
                        modifier = Modifier.width(if (i in textColumns) 200.dp else 78.dp),
                        fontSize = 14.sp,
                        color = TextMain,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if (rows.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("还没有人", fontSize = 14.sp, color = TextMuted)
            }
        }
    }
}
