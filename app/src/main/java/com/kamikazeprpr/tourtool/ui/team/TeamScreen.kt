package com.kamikazeprpr.tourtool.ui.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 20.sp) }
            Spacer(Modifier.weight(1f))
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
                MemberRow(m)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MemberRow(m: Member) {
    val ok = m.status == Member.STATUS_AUTO_OK || m.status == Member.STATUS_MANUAL_OK
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(IdCard.genderLabel(m.gender))
                        if (m.idNumber.isNotBlank()) {
                            append("  ")
                            append(m.idNumber)
                        }
                    },
                    fontSize = 15.sp,
                    color = TextMuted,
                )
                if (!m.failReasons.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = m.failReasons,
                        fontSize = 14.sp,
                        color = if (ok) TextMuted else StateWarn,
                    )
                }
            }
            Text(
                text = if (ok) "好了" else "要你看一眼",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (ok) StateOk else StateWarn,
            )
        }
    }
}
