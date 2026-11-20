package com.kamikazeprpr.tourtool.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.ui.home.HomeViewModel
import com.kamikazeprpr.tourtool.ui.team.TeamScreen
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

/**
 * 首页：一个大按钮「新建一个旅游团」，下面是已有团。
 */
@Composable
fun HomeScreen() {
    val vm: HomeViewModel = viewModel()
    val ctx = LocalContext.current
    val teams by vm.teams.collectAsStateWithLifecycle()
    val counts by vm.memberCounts.collectAsStateWithLifecycle()
    var openTeamId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<Team?>(null) }

    val currentTeamId = openTeamId
    if (currentTeamId != null) {
        TeamScreen(teamId = currentTeamId, onBack = { openTeamId = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "退出",
                fontSize = 17.sp,
                color = TextMuted,
                modifier = Modifier
                    .clickable { (ctx as? android.app.Activity)?.finishAffinity() }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "旅游证件登记",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "把客人的身份证登记成表格",
            fontSize = 17.sp,
            color = TextMuted,
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { showNewDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("新建一个旅游团", fontSize = 22.sp)
        }

        UpdateBanner()

        Spacer(Modifier.height(24.dp))

        if (teams.isEmpty()) {
            Text(
                text = "还没有旅游团。\n先点上面的按钮建一个。",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            teams.forEach { team ->
                TeamCard(
                    team = team,
                    memberCount = counts[team.id] ?: 0,
                    onClick = { openTeamId = team.id },
                    onLongClick = { editingTeam = team },
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "长按旅行团条目可以改名、加日期、写备注，或者删掉整个团",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    editingTeam?.let { team ->
        EditTeamDialog(
            team = team,
            onDismiss = { editingTeam = null },
            onSave = { name, depart, end, note ->
                vm.saveTeam(team, name, depart, end, note)
                editingTeam = null
            },
            onDelete = {
                vm.deleteTeam(team)
                editingTeam = null
            },
        )
    }

    if (showNewDialog) {
        NewTeamDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                vm.createTeam(name)
                showNewDialog = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeamCard(
    team: Team,
    memberCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (memberCount == 0) "还没有登记客户" else "已经登记 $memberCount 位客户",
                    fontSize = 16.sp,
                    color = TextMuted,
                )
            }
            Text("›", fontSize = 26.sp, color = TextMuted)
        }
    }
}

/**
 * 长按旅行团条目出来的编辑框：改名、加出发/结束日期、写备注、删整个团。
 * 日期走系统的日期选择器，不让人手打 yyyy-MM-dd。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditTeamDialog(
    team: Team,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(team.name) }
    var depart by remember { mutableStateOf(team.departDate) }
    var end by remember { mutableStateOf(team.endDate) }
    var note by remember { mutableStateOf(team.note.orEmpty()) }
    var pickWhich by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑这个团", fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("团名", fontSize = 15.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                DateField("出发日期", depart) { pickWhich = 0 }
                Spacer(Modifier.height(12.dp))
                DateField("结束日期", end) { pickWhich = 1 }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注", fontSize = 15.sp) },
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(
                        "删除这个团",
                        fontSize = 16.sp,
                        color = com.kamikazeprpr.tourtool.ui.theme.StateBad,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, depart, end, note.ifBlank { null }) }) {
                Text("保存", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", fontSize = 18.sp, color = TextMuted)
            }
        },
    )

    pickWhich?.let { which ->
        val state = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickWhich = null },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    val text = ms?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                            .format(java.util.Date(it))
                    }
                    if (which == 0) depart = text else end = text
                    pickWhich = null
                }) { Text("确定", fontSize = 17.sp) }
            },
            dismissButton = {
                TextButton(onClick = { pickWhich = null }) { Text("取消", fontSize = 17.sp) }
            },
        ) {
            androidx.compose.material3.DatePicker(
                state = state,
                title = {
                    Text(
                        "选日期",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontSize = 16.sp,
                        color = TextMuted,
                    )
                },
                headline = {
                    // Material3 默认这里写 "Selected date"，直接换成日期本身
                    val ms = state.selectedDateMillis
                    Text(
                        text = ms?.let {
                            java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA)
                                .format(java.util.Date(it))
                        } ?: "还没选",
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 12.dp),
                        fontSize = 32.sp,
                        color = TextMain,
                    )
                },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确认删除？", fontSize = 22.sp) },
            text = { Text("整个「${team.name}」会连同里面登记的人一起删掉。", fontSize = 17.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(
                        "确认",
                        fontSize = 18.sp,
                        color = com.kamikazeprpr.tourtool.ui.theme.StateBad,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消", fontSize = 18.sp) }
            },
        )
    }
}

/**
 * 日期字段：外观跟上下两个输入框一致，但整块可点，点了弹系统日期选择器。
 * 用 enabled=false 让它不抢点击，颜色再单独调回来，不然会灰得像不可用。
 */
@Composable
private fun DateField(label: String, value: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        OutlinedTextField(
            // 空值塞一个空格：这样 label 会浮到边框上，跟有值的那个框对齐，
            // 不然一个浮起一个留在框里，看着不齐。
            value = prettyDate(value).ifEmpty { " " },
            onValueChange = {},
            enabled = false,
            readOnly = true,
            singleLine = true,
            label = { Text(label, fontSize = 15.sp) },
            placeholder = { Text("点这里选日期", fontSize = 17.sp) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "选日期",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = TextMain,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLabelColor = TextMuted,
                disabledPlaceholderColor = TextMuted,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/** 存的是 2026-11-19，给人看的时候写成 2026年11月19日 */
private fun prettyDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).parse(raw)
        java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(d!!)
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun NewTeamDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("这个团叫什么名字？", fontSize = 22.sp) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                placeholder = { Text("例如：11月大阪三日团", fontSize = 18.sp) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("确认", fontSize = 18.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 18.sp) }
        },
    )
}

/**
 * 有新版本时在首页挂一条横幅。不弹窗——老人最怕弹窗，一弹就慌。
 * 点了才下载；下载完过 sha256 / 包名 / 签名三道校验，再拉起系统安装界面。
 */
@Composable
private fun UpdateBanner() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<com.kamikazeprpr.tourtool.update.UpdateChecker.Release?>(null) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        com.kamikazeprpr.tourtool.data.AppLog.d("UPDATE", "开始检查版本")
        when (val r = com.kamikazeprpr.tourtool.update.UpdateChecker.check(ctx)) {
            is com.kamikazeprpr.tourtool.update.UpdateChecker.CheckResult.Newer -> release = r.release
            is com.kamikazeprpr.tourtool.update.UpdateChecker.CheckResult.Failed ->
                com.kamikazeprpr.tourtool.data.AppLog.d("UPDATE", "没查成：${r.reason}")
            com.kamikazeprpr.tourtool.update.UpdateChecker.CheckResult.UpToDate -> Unit
        }
    }

    val rel = release ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEDF3FB)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "有新版本 ${rel.version}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                hint?.let {
                    Text(it, fontSize = 14.sp, color = TextMuted)
                }
            }
            Button(
                onClick = {
                    busy = true
                    hint = null
                    scope.launch {
                        when (val r = com.kamikazeprpr.tourtool.update.UpdateChecker.download(ctx, rel)) {
                            is com.kamikazeprpr.tourtool.update.UpdateChecker.InstallResult.Ready ->
                                com.kamikazeprpr.tourtool.update.UpdateChecker.install(ctx, r.file)
                            is com.kamikazeprpr.tourtool.update.UpdateChecker.InstallResult.Rejected ->
                                hint = r.reason
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (busy) "下载中…" else "更新", fontSize = 16.sp)
            }
        }
    }
}
