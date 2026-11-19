package com.kamikazeprpr.tourtool.ui.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamikazeprpr.tourtool.core.IdCard
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.ui.theme.StateBad
import com.kamikazeprpr.tourtool.ui.theme.StateOk
import com.kamikazeprpr.tourtool.ui.theme.StateWarn
import com.kamikazeprpr.tourtool.ui.theme.TextMain
import com.kamikazeprpr.tourtool.ui.theme.TextMuted

@Composable
fun TeamScreen(teamId: Long, onBack: () -> Unit) {
    val vm: TeamViewModel = viewModel(key = "team-$teamId", factory = TeamViewModel.factory(teamId))
    val team by vm.team.collectAsStateWithLifecycle(initialValue = null)
    val members by vm.members.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val message by vm.lastMessage.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(100)
    ) { uris ->
        if (uris.isNotEmpty()) vm.importImages(uris)
    }

    var editingMemberId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Member?>(null) }
    var tagTarget by remember { mutableStateOf<Member?>(null) }
    var confirmClearTags by remember { mutableStateOf(false) }
    var tagVersion by remember { mutableStateOf(0) }

    val memberTags by produceState(
        initialValue = emptyMap<Long, List<com.kamikazeprpr.tourtool.data.db.MemberTagFull>>(),
        members,
        tagVersion,
    ) { value = vm.tagsByMember() }

    val allTags by produceState(
        initialValue = emptyList<com.kamikazeprpr.tourtool.data.db.Tag>(),
        tagVersion,
    ) { value = vm.allTags() }

    val tagsByMember by produceState(
        initialValue = emptyMap<Long, List<String>>(),
        members.size,
    ) { value = vm.tagsForExport() }

    if (showExport) {
        com.kamikazeprpr.tourtool.ui.export.ExportScreen(
            teamName = team?.name.orEmpty(),
            members = members,
            tagsByMember = tagsByMember,
            onBack = { showExport = false },
        )
        return
    }

    val editing = members.firstOrNull { it.id == editingMemberId }
    if (editing != null) {
        com.kamikazeprpr.tourtool.ui.member.MemberEditScreen(
            member = editing,
            onSave = { n, g, num ->
                vm.saveManual(editing, n, g, num)
                editingMemberId = null
            },
            onBack = { editingMemberId = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "返回",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (editMode) "完成" else "编辑",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { editMode = !editMode }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        Text(
            text = team?.name ?: "…",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (members.isEmpty()) "还没有登记人" else "已经登记 ${members.size} 人",
            fontSize = 17.sp,
            color = TextMuted,
        )

        Spacer(Modifier.height(20.dp))

        val busy = progress != null
        Button(
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (busy) "正在读取…" else "选客人的身份证照片", fontSize = 21.sp)
        }

        val p = progress
        if (p != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "正在读取第 ${p.current} 张，一共 ${p.total} 张",
                fontSize = 18.sp,
                color = TextMain,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { p.current.toFloat() / p.total.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "已经好的 ${p.autoOk} 个，要你看一眼的 ${p.needReview} 个",
                fontSize = 15.sp,
                color = TextMuted,
            )
        }

        val msg = message
        if (msg != null) {
            Spacer(Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF3FB)),
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 17.sp,
                    color = TextMain,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (members.isEmpty() && !busy) {
            Text(
                text = "点上面的按钮，从相册里选中客人的身份证照片（一次可以选很多张），软件会自动认字。",
                fontSize = 17.sp,
                lineHeight = 27.sp,
                color = TextMuted,
            )
        } else {
            members.forEach { m ->
                MemberRow(
                    m = m,
                    tags = memberTags[m.id].orEmpty(),
                    editMode = editMode,
                    onClick = { if (!editMode) editingMemberId = m.id },
                    onLongClick = { if (!editMode) tagTarget = m },
                    onDelete = { deleteTarget = m },
                    onRemoveTag = { tagId ->
                        vm.removeMemberTag(m.id, tagId)
                        tagVersion++
                    },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            if (editMode) {
                Button(
                    onClick = { confirmClearTags = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = StateBad,
                    ),
                ) {
                    Text("清除全部标签", fontSize = 19.sp)
                }
            } else {
                Button(
                    onClick = { showExport = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("做成表格，发出去", fontSize = 19.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmClearTags) {
        AlertDialog(
            onDismissRequest = { confirmClearTags = false },
            title = { Text("确认清除？", fontSize = 22.sp) },
            text = {
                Text(
                    "这一团所有人身上的标签都会清掉。标签本身还在，以后还能再勾。",
                    fontSize = 17.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllTags()
                    tagVersion++
                    confirmClearTags = false
                }) { Text("确认", fontSize = 18.sp, color = StateBad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearTags = false }) { Text("取消", fontSize = 18.sp) }
            },
        )
    }

    tagTarget?.let { target ->
        TagDialog(
            member = target,
            allTags = allTags,
            selectedIds = memberTags[target.id].orEmpty().map { it.id }.toSet(),
            onDismiss = { tagTarget = null },
            onAddTag = { label -> vm.addTag(label) },
            onTagCreated = { tagVersion++ },
            onSave = { ids ->
                vm.saveMemberTags(target.id, ids)
                tagTarget = null
                tagVersion++
            },
        )
    }

    deleteTarget?.let { target ->        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除？", fontSize = 22.sp) },
            text = {
                Text(
                    "删掉之后「${target.name.ifBlank { "这一条" }}」不会出现在名单和表格里。",
                    fontSize = 17.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMember(target)
                    deleteTarget = null
                }) { Text("确认", fontSize = 18.sp, color = StateBad) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消", fontSize = 18.sp) }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MemberRow(
    m: Member,
    tags: List<com.kamikazeprpr.tourtool.data.db.MemberTagFull>,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRemoveTag: (Long) -> Unit,
) {
    val ok = m.status == Member.STATUS_AUTO_OK || m.status == Member.STATUS_MANUAL_OK
    val reading = m.failReasons.orEmpty().split("；").filter { it.isNotBlank() }

    // 号码那一行：读到了就显示号码；没读到说"没读出"，读到的不是身份证号就说"错误"
    val numLine = when {
        m.idNumber.isNotBlank() -> m.idNumber
        ok -> ""
        reading.any { it.contains("没读出身份证号") } -> "没读出身份证号"
        else -> "身份证号错误"
    }
    val numIsProblem = m.idNumber.isBlank() && !ok
    // 小字只剩下"具体哪里不对"，避免跟上面那行重复
    val detail = reading.filterNot { it == numLine }.joinToString("；")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = m.name.ifBlank { "（没读到姓名）" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        tags.forEach { tag ->
                            val style = com.kamikazeprpr.tourtool.ui.theme.TagColors.of(tag.colorIndex)
                            Row(
                                modifier = Modifier
                                    .background(style.bg, RoundedCornerShape(6.dp))
                                    .padding(start = 8.dp, end = if (editMode) 4.dp else 8.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = tag.label,
                                    fontSize = 13.sp,
                                    color = style.fg,
                                )
                                if (editMode) {
                                    // 编辑模式下每块标签尾巴上挂个小红叉，点了直接摘掉，不弹确认
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { onRemoveTag(tag.id) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删掉这个标签",
                                            tint = StateBad,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        val g = IdCard.genderLabel(m.gender)
                        if (g.isNotEmpty()) append(g)
                        if (numLine.isNotEmpty()) {
                            if (isNotEmpty()) append("  ")
                            append(numLine)
                        }
                    },
                    fontSize = 15.sp,
                    color = if (numIsProblem) StateBad else TextMuted,
                )
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail,
                        fontSize = 14.sp,
                        color = if (ok) TextMuted else StateWarn,
                    )
                }
            }
            if (editMode) {
                // 编辑模式下右侧换成红叉，点它先问一句再删
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除这一条",
                        tint = StateBad,
                        modifier = Modifier.size(26.dp),
                    )
                }
            } else if (ok) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已经核对通过",
                    tint = StateOk,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                Text(
                    text = "要你看一眼",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StateWarn,
                )
            }
        }
    }
}
